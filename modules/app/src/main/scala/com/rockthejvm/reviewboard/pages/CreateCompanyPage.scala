package com.rockthejvm.reviewboard.pages

import zio.*
import org.scalajs.dom.*
import org.scalajs.dom.html.Element
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import com.rockthejvm.reviewboard.common.Constants.urlRegex
import com.rockthejvm.reviewboard.http.requests.CompanyCreationRequest
import org.scalajs.dom.HTMLImageElement

import com.rockthejvm.reviewboard.core.ZJS.*
import com.rockthejvm.reviewboard.domain.data.Company
case class CreateCompanyState(
  name: String = "",
  url: String = "",
  location: Option[String] = None,
  country: Option[String] = None,
  industry: Option[String] = None,
  image: Option[String] = None, // BASE 64 -- easy to render on the frontend
  tags: List[String] = List(),
  override val showStatus: Boolean = false,
  upStreamStatus: Option[Either[String, String]] = None,
  ) extends FormState {

  def potentialErrors: List[Option[String]] =
    List(
      Option.when(name.isEmpty)("`name` field cannot be empty"),
      Option.unless(url.startsWith("http://") || url.startsWith("https://") || url.isEmpty)(
        s"`$url` is not a valid url (must start with http:// or https://)"
      ),
      Option.unless(url.isEmpty || url.matches(urlRegex))(
        s"`$url` is not a valid url format"
      ),
      ) ++ upStreamStatus.map(_.left.toOption).toList

  def maybeSuccess: Option[String] =
    upStreamStatus.flatMap(_.toOption)

  def toRequest: CompanyCreationRequest = CompanyCreationRequest(
    name=name,
    url=url,
    location=location,
    country=country,
    industry=industry,
    image=image,
    tags=Option(tags).filter(_.nonEmpty)
    )

}


object CreateCompanyPage extends FormPage[CreateCompanyState](title="Create Company"){

  override val blankSlate = CreateCompanyState()

  override val state = Var(blankSlate)

  console.log("CreateCompanyPage initializing")
  console.log(s"Do we have state? ${state}")

  val submiter = Observer[CreateCompanyState] { thisState =>
    if thisState.hasErrors then {
      state.update(_.copy(showStatus=true))
    } else {
      useBackend(_.companyEndpoints.createEndpoint(thisState.toRequest))
        // IF SUCCESSFUL BACKEND-CALL
        .map { (response: Company) =>
          state.update(_.copy(showStatus = true, upStreamStatus = Some(Right("Company Created"))))
        }
        // IF FAILURE IN BACKEND-CALL
        .tapError { (error: Throwable) =>
          ZIO.succeed {
            state.update(_.copy(showStatus = true, upStreamStatus = Some(Left(error.getMessage))))
          }
        }
        // ZIO EFFECT DESCRIBED .. Now must execut it !
        .runJs
    }
  }

  override def renderChildren(): List[ReactiveHtmlElement[Element]] = List(

    renderInput("Company Name", "name", "text", true, "ACME Inc", (s, v) => s.copy(name = v)),

    renderInput("Company URL", "url", "text", true, "https://acme.com", (s, v) => s.copy(url = v)),

    renderLogoUpdload("Company Logo", "logo"),

    img(src <-- state.signal.map(_.image.getOrElse(""))),

    renderInput("Location", "location", "text", false, "Somewhere", (s, v) => s.copy(location = Some(v))),

    renderInput("Country", "country", "text", false, "Some country", (s, v) => s.copy(country = Some (v))),

    renderInput("Industry", "industry", "text", false, "Functional code", (s, v) => s.copy(industry = Some(v))),

    renderInput("Tags - separate by `,`", "tags", "text", false, "Scala, Zio", (s, v) => s.copy(tags = v.split(",").map(_.trim).toList)),

    button(
      `type` := "button",
      "Post Company",
      onClick
        .preventDefault // i.e override the default behavior
        .mapTo(state.now()) /* submitting/emitting the current state */ --> submiter /* Observer[State]*/
    )

    )

  private def renderLogoUpdload(name: String, uid: String, isRequired: Boolean = false) = {

    div(
      cls := "row",
      div(
        cls := "col-md-12",
        div(
          cls := "form-input",
          label(
            forId := "form-id-1-todo",
            cls   := "form-label",
            if isRequired then span("*") else span(),
            name,
          ),
        input(
          `type`      := "file",
          cls         := "form-control",
          idAttr      := uid,
          accept      := "image/*",

          onChange.mapToFiles --> fileUploader
        )
      )
    )
  )
  }

  private def computeDimensions(width: Int, height: Int): (Int, Int)= {
    if width > height
      then {
        val ratio = width * 1.0 / 256
        val newWidth = width / ratio
        val newHeight = height / ratio
        (newWidth.toInt, newHeight.toInt)
    } else
      computeDimensions(height, width)
  }

  val fileUploader = (files: List[File]) => {
    // take the file
    val maybeFile = files.headOption // the list could be empty, if the user ultimately `cancel` the file selection
                         .filter(_.size > 0)
    maybeFile.foreach { file =>
      // make a fileReader
      val reader = new FileReader
      // register onload callback => when done, populate the state with reader.result
      // `onload`, i.e when done reading the file
      reader.onload = { ( _: ProgressEvent) =>

         // draw the picture into a 256x256 canvas
         // make a fake img tag (not rendered)
         val fakeImage = document.createElement("img") // a general Element => must be DOWNCASTED
                                 .asInstanceOf[HTMLImageElement]

         // this being async, we must register an event listener
         fakeImage.addEventListener(
           `type`="load",
           listener={ (_: Event) =>
             val canvas = document.createElement("canvas").asInstanceOf[HTMLCanvasElement]
             val context /*a image renderer*/ = canvas.getContext("2d").asInstanceOf[CanvasRenderingContext2D]
             val (width, height) = computeDimensions(fakeImage.width, fakeImage.height)
             canvas.width = width
             canvas.height = height
             // render the image on the original canvas
             context.drawImage(fakeImage, 0, 0, width, height)
             state.update(_.copy(image=Some(canvas.toDataURL(file.`type`))))
           }
          )
         // trigger the load event
         fakeImage.src = reader.result.toString // the original content of the image that has been uploaded

      }
      // trigger the read
      reader.readAsDataURL(file)
      }
    }
}

