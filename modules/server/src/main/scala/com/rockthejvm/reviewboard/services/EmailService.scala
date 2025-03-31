package com.rockthejvm.reviewboard.services

import zio.*
import java.util.Properties
import javax.mail.Session
import javax.mail.Authenticator
import javax.mail.PasswordAuthentication
import javax.mail.internet.MimeMessage
import javax.mail.Message
import javax.mail.Transport

import com.rockthejvm.reviewboard.config.*

trait EmailService {
  def sendEmail(to: String, subject: String, content: String): Task[Unit]
  def sendRecoveryEmail(to: String, otp: String): Task[Unit] =
    val subject = "Rock the JVM: Password Recovery"
    val content = s"""
      <div style="
          border: 1px solid black;
          padding: 20px;
          font-family: sans-serif;
          line-height: 2;
          font-size: 20px;
      ">
        <h1>Rock the JVM: Password Recovery</h1>
        <p>Your One-Time-Password is: <strong>$otp</strong></p>
        <p>😘 from RTJVM</p>
      </div>
    """
    sendEmail(to, subject, content)

}

class EmailServiceLive private (emailConfig: EmailConfig) extends EmailService {

  private val host: String = emailConfig.host
  private val port: Int = emailConfig.port
  private val user: String = emailConfig.user
  private val pwd: String = emailConfig.pwd


  override def sendEmail(to: String, subject: String, content: String): Task[Unit] =
    val message = for {
      props   <- propsResource
      session <- createSession(props)
      message <- createMessage(session, user, to, subject, content)
      _       <- Console.printLine("Recovery Email Sent!")
    } yield message
    message.map(Transport.send)


  private val propsResource: Task[Properties] = {

    val prop = new Properties
    prop.put("mail.smtp.auth", true)
    prop.put("mail.smtp.starttls.enable", "true")
    prop.put("mail.smtp.host", host)
    prop.put("mail.smtp.port", port)
    prop.put("mail.smtp.ssl.trust", host)
    ZIO.succeed(prop)

  }

  private def createSession(props: Properties): Task[Session] = ZIO.attempt {

    val auth = new Authenticator {
      override protected def getPasswordAuthentication(): PasswordAuthentication =
        new PasswordAuthentication(user, pwd)
    }

    Session.getInstance(props, auth)
  }

  private def createMessage(
    session: Session,
    from: String,
    to: String,
    subject: String,
    content: String
  ): Task[MimeMessage] =

    val message =new MimeMessage(session)
    message.setFrom(from)
    message.setRecipients(Message.RecipientType.TO, to)
    message.setSubject(subject)
    message.setContent(content, "text/html; charset=utf-8")
    ZIO.succeed(message)



}

object EmailServiceLive {

  val layer = ZLayer {
    ZIO.service[EmailConfig].map(config => new EmailServiceLive(config))
  }

  val configuredLayer =
    Configs.makeLayer[EmailConfig]("rockthejvm.email") >>> layer
}


object EmailServiceDemo extends ZIOAppDefault {

  val program = for {
    emailService <- ZIO.service[EmailService]
    _            <- emailService.sendRecoveryEmail(
                                  to="amnesic@gmail.com",
                                  otp="ABC1234",
                                  )
    _            <- Console.printLine("Email Sent!")
  } yield ()

  override def run: ZIO[Any & (ZIOAppArgs & Scope), Any, Any] =
    program.provide(EmailServiceLive.configuredLayer)
}
