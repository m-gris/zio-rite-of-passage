package com.rockthejvm.reviewboard.pages

import zio.*

import com.raquo.laminar.codecs.*
import com.raquo.laminar.api.L.{*, given}

import com.rockthejvm.reviewboard.core.ZJS.*
import com.rockthejvm.reviewboard.components.Anchors
import com.rockthejvm.reviewboard.domain.data.Company
import com.rockthejvm.reviewboard.http.endpoints.CompanyEndpoints
import com.rockthejvm.reviewboard.components.FilterPannel
import com.rockthejvm.reviewboard.components.CompanyComponents



object CompaniesPage {

  // components
  val filterPanel = FilterPannel
  val firstBatch = EventBus[List[Company]]()
  val companyEvents: EventStream[List[Company]] =

    // "initial" populating of the page
    firstBatch.events.mergeWith {

      // updates / refreshes based on the filters...
      filterPanel.triggerFilters.flatMap {
      newFilter => useBackend(_.companyEndpoints.searchEndpoint(newFilter))
                    .toEventStream
          }

      }

  // // for the zio effect to "push" its result to
  // // so that laminar can use it as an event stream
  // val companiesBus = EventBus[List[Company]]()
  //
  // def performBackendCall(): Unit = {
  //   val companiesZIO = useBackend(_.companyEndpoints.getAllEndpoint(()))
  //   companiesZIO.emitTo(companiesBus)
  // }

  def apply() =
      sectionTag(
        onMountCallback(_ => useBackend(backend => backend.companyEndpoints.getAllEndpoint( () )).emitTo(firstBatch)),
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
              filterPanel()
            ),
            div(
              cls := "col-lg-8",
              // children is a 'reactive field'
              // that will be updated depending on
              // the event bus
              children <-- companyEvents
                            // rendered into
                            // an eventStream of HTML Elements
                            // pushed as children to the div we're in
                            .map(companies => companies.map(render))
            )
          )
        )
      )


    def render(company: Company) =

      div(

        cls := "jvm-recent-companies-cards",

        div(
          cls := "jvm-recent-companies-card-img",
          CompanyComponents.renderImage(company)
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
          CompanyComponents.renderOverview(company),
        ),

      CompanyComponents.renderAction(company)

      )
}
