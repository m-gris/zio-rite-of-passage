package com.rockthejvm.reviewboard.pages

import zio.*
import sttp.model.Uri.UriContext
import com.raquo.laminar.codecs.*
import com.raquo.laminar.api.L.{*, given}
import sttp.client3.impl.zio.FetchZioBackend
import sttp.tapir.client.sttp.SttpClientInterpreter

import com.rockthejvm.reviewboard.components.Anchors
import com.rockthejvm.reviewboard.domain.data.Company
import com.rockthejvm.reviewboard.http.endpoints.CompanyEndpoints



object CompaniesPage {

  val dummyCompany = Company(1L, "zi dumpany", "dumpany name", "dummy.com", Some("Ubud"), Some("Bali"))

  // for the zio effect to "push" its result to
  // so that laminar can use it as an event stream
  val companiesBus = EventBus[List[Company]]()

  def performBackendCall(): Unit = {
    val companyEndpoint =
      // reminder: this create an ANONYMOUS CLASS
      // that implements the `CompanyEndpoints` trait
      new CompanyEndpoints {} // no abstract method... no override...
    val getAllCompaniesEndpoint = companyEndpoint.getAllEndpoint
    val backend = FetchZioBackend() // an http backend can send request & expect resposnes
    val interpreter = SttpClientInterpreter()
    val request = interpreter
        .toRequestThrowDecodeFailures(getAllCompaniesEndpoint, Some(uri"http://localhost:8080"))
        .apply( () /* the R of the endpoint... in our case Unit */)
    val companiesZIO = backend.send(request)
                              .map(response => response.body)
                              // submerge failures with ZIO.absolve (the opposite of either)
                              // turning a ZIO[R, Nothing, Either[E, A]] into a ZIO[R, E, A]
                              .absolve

    // NEED TO RUN THE ZIO EFFET 'MANUALLY' / 'UNSAFELY'
    // Because our frontend is not a ZIO Native APP (we use Laminar for the "logic")
    // ZIO will be used as an 'auxiliary' to fetch things from the backend
    // We will do so by:
    //  - running the effect
    //  - and surface its output as an event stream in laminar
    //  ( by pushing the effect's output to an event bus)
    Unsafe.unsafe { implicit unsafe =>
      Runtime.default.unsafe.fork(
        companiesZIO.tap( /* when the effect completes */
          list => ZIO.attempt(companiesBus.emit(list))
          )
        )
    }

  }

  def apply() =
      sectionTag(
        //////////////////////////////////////////////////////////
        /* allows to perform side effectcs, i.e (),
         * when the sectionTag is added (mounted?) to the html */
        onMountCallback(_ => performBackendCall()),
        //////////////////////////////////////////////////////////

        cls := "section-1",
        div(
          cls := "container company-list-hero",
          h1(
            cls := "company-list-title",
            "Rock the JVM Companies Board"
          )
        ),
        div(
          cls := "container",
          div(
            cls := "row jvm-recent-companies-body",
            div(
              cls := "col-lg-4",
              div("TODO filter panel here")
            ),
            div(
              cls := "col-lg-8",
              // children is a 'reactive field'
              // that will be updated depending on
              // the event bus
              children <-- companiesBus
                            .events // eventStream of List[Company]
                            // rendered into
                            // an eventStream of HTML Elements
                            // pushed as children to the div we're in
                            .map(companies => companies.map(render))
            )
          )
        )
      )

    def renderImage(company: Company) =
      img(
        cls := "img-fluid",
        src := company.image.getOrElse("TODO"),
        alt := company.name
        )

    def renderDetails(icon: String, value: String) =
      div(
        cls := "company-detail",
        i(cls := s"fa fa-${icon} company-detail-icon"),
        p(
          cls := "company-detail-value",
          value
        )
      )

    def fullyLocate(company: Company): String =
      (company.location, company.country) match
        case (None, None) => "N/A"
        case (None, Some(country)) => country
        case (Some(location), None) => location
        case (Some(location), Some(country)) => s"${location} - ${country}"

    def renderOverview(company: Company) =
          div(
            cls := "company-summary",
            renderDetails("location-dot", fullyLocate(company)),
            renderDetails("tags", company.tags.mkString(", ")),
          )


    def renderAction(company: Company) =
        div(
          cls := "jvm-recent-companies-card-btn-apply",
          a(
            href   := company.url,
            target := "blank",
            button(
              `type` := "button",
              cls    := "btn btn-danger rock-action-btn",
              "Website"
            )
          )
        )

    def render(company: Company) =

      div(

        cls := "jvm-recent-companies-cards",

        div(
          cls := "jvm-recent-companies-card-img",
          renderImage(company)
        ),

        div(
          cls := "jvm-recent-companies-card-contents",
          h5(
            Anchors.renderNavLink(
              company.name,
              s"/company/${company.id}",
              "company-title-link"
            )
          ),
          renderOverview(company),
        ),

      renderAction(company)

      )
}
