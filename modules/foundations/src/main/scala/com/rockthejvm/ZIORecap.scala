package com.rockthejvm

import zio.*

import scala.io.StdIn
import javax.sql.ConnectionPoolDataSource

object ZIORecap extends ZIOAppDefault {

  // reminder ZIOs are data structures DESCRIBING arbitrary computations (including side effects)
  // "effects" = "COMPUTATIONS AS VALUES"

  // basics
  val meaningOfLife: ZIO[ // 3 type parameters: R, E, A
                      Any,     // R - the dependencies / environment / context
                      Nothing, // E - the "error channel", the error that MIGHT be thrown
                      Int      // A - the expected value
                      ] = ZIO.succeed(42)

  val failure: ZIO[Any, String, Nothing] = ZIO.fail("Oupssy... Something went wrong")

  val suspension: ZIO[Any,
                      Throwable, // the evaluation of the suspend effect might throw an exception...
                      Int] = ZIO.suspend(meaningOfLife)

  // map / flatMap
  val betterMOL: ZIO[Any, Nothing, Int] = meaningOfLife.map(_ * 2)
  val printedMOL: UIO[Unit] /* type alias for the ZIO[Any, Nothing, Int] */ =
        meaningOfLife.flatMap(mol =>ZIO.succeed(println(mol)))

  val smallProgram = for {
    _ <- Console.printLine("What's your name ?")
    name <- ZIO.succeed(StdIn.readLine())
    _ <- Console.printLine(s"Welcome to ZIO, $name")
  } yield ()


  // error handling - "type safe" errors (important point)
  // ZIO will catch errors / throwables and store those AS DATA
  // ==>>>> "ERRORS AS DATA" <<<===


  val anAttempt: ZIO[Any, Throwable, Int] /* aka Task[Int] */ = ZIO.attempt {
    // some expression that might fail...
    val word: String = null
    word.length
  }


  // allows to catch errors "effectfully" (i.e a error value wrapped in an effect)
  val caughtError = anAttempt.catchAll(e => ZIO.succeed("Failed - returning something else"))
  val selectivelyCaught = anAttempt.catchSome {
    case e: RuntimeException => ZIO.succeed(s"Ignoring runtime exception: $e")
    case _ => ZIO.succeed("Ignoring everything else")
  }


  // FIBERS --- for async / concurrency / parallelism
  val aDelayedValue: UIO[Int] =
                      ZIO.sleep(1.seconds)
                      *> // "and then" operator
                      Random.nextIntBetween(0, 100)

  val sequentialPair = for {
    a <- aDelayedValue
    b <- aDelayedValue
  } yield (a, b) // will take 2 seconds (because sequential)

  val parallelPair = for {
    fibA <- aDelayedValue.fork // signal to start a fiber & returns another effect
    fibB <- aDelayedValue.fork
    a <- fibA.join
    b <- fibB.join
  } yield (a, b) // only takes 1 second thanks to 'forking' => both are run IN PARALLEL


  val interruptedFiber = for {
    fib <- aDelayedValue.onInterrupt(ZIO.succeed(println("@@@ I'm being interrupted @@@"))).fork
    _   <- ZIO.sleep(500.millis) *> ZIO.succeed(println("### Cancelling Fiber ###")) *> fib.interrupt
    result <- fib.join
  } yield result

  val ignoredInterruption = for {
    fib <- ZIO.uninterruptible(aDelayedValue.onInterrupt(ZIO.succeed(println("@@@ I'm being interrupted @@@")))).fork
    _   <- ZIO.sleep(500.millis) *> ZIO.succeed(println("### Cancelling Fiber ###")) *> fib.interrupt
    result <- fib.join
  } yield result

  // many APIs on top of Fibers
  val aPairPar_V2 = aDelayedValue.zip(aDelayedValue) // in parallel ... exactly what we did above...
  val randomX10 = ZIO.collectAllPar( // AKA "traverse" in FP community
    /*
     * pass a sequence of ZIOs
     * &
     * each will be run on different fibers in parallel
     * &
     * their result will be collected into the resulting Seq
     */
    (1 to 10).map(_ => aDelayedValue)
  )
  // many other APIs: reduceAllPar, mergeAllPar, foreachPar etc..


  // DEPENDENCIES
  case class User(name: String, email: String)

  // coded "top - down"
  // the UserSubscription depends on:
  //    |
  //    |___EmailService
  //    |
  //    |___UserDatabase which depends on:
  //        |
  //        |___ConnectionPool which depends on:
  //            |
  //            |___Connection
  //
  class UserSubscription(email: EmailService, db: UserDatabase) {
    def subscribe(user: User): Task[Unit] = for  {
      _ <- email.sendTo(user)
      _ <- db.insert(user)
      _ <- ZIO.succeed(s"subscribed $user")
      } yield ()
  }

  object UserSubscription {
    val live: ZLayer[EmailService with UserDatabase, Nothing, UserSubscription] =
      ZLayer.fromFunction( (emailS, dbS) => new UserSubscription(emailS, dbS) )
  }

  class EmailService {
    def sendTo(user: User): Task[Unit] =
      ZIO.succeed(s"Email sent to $user.email")
  }

  object EmailService {
    val live: ZLayer[Any, Nothing, EmailService] =
      ZLayer.succeed( new EmailService ) // we simply "lift" the service into a Layer
  }

  class UserDatabase(pool: ConnectionPool) {
    def insert(user: User): Task[Unit] = ZIO.succeed(s"inserted $user")
  }

  object UserDatabase {
    val live: ZLayer[ConnectionPool, Nothing, UserDatabase] =
      ZLayer.fromFunction(new UserDatabase(_) )
  }

  class ConnectionPool(n: Int) {
    def get: Task[Connection] = ZIO.succeed(Connection())
  }

  object ConnectionPool {
    def live(n: Int): ZLayer[Any, Nothing, ConnectionPool] =
      ZLayer.succeed(ConnectionPool(n))
  }

  case class Connection()

  def subscribe(user: User): ZIO[UserSubscription, Throwable, Unit] = for {
    sub <- ZIO.service[UserSubscription] // we only declare the "top level" / final "dependency"
    _   <- sub.subscribe(user)
  } yield ()


  val program = for {
    _ <- subscribe(User("John", "j@g.com"))
    _ <- subscribe(User("Mike", "m@g.com"))
    _ <- ZIO.succeed( println("something...") )
  } yield ()


  def run = program.provide(
    // pass the ALL the dependencies...
    // in ANY order...
    // ZIO will figure out the
    // DEPENDENCY GRAPH
    ConnectionPool.live(10),
    UserDatabase.live,
    EmailService.live,
    UserSubscription.live
    )
}
