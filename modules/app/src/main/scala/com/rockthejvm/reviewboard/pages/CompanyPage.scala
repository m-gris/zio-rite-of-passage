package com.rockthejvm.reviewboard.pages

import java.time.Instant

import zio.*
import org.scalajs.dom
import com. raquo. laminar.api.L.{*, given}

import com.rockthejvm.reviewboard.common.*
import com.rockthejvm.reviewboard.domain.data.*
import com.rockthejvm.reviewboard.core.ZJS.*

object CompanyPage {

 dom.console.log("CompanyPage object initialized")

  val dummyReviews = List(
    Review(
      1,
      1,
      1L,
      5,
      5,
      5,
      5,
      5,
      "This is a pretty good company. They write Scala and that's great",
      Instant.now(),
      Instant.now()
    ),
    Review(
      1,
      1,
      1L,
      3,
      4,
      3,
      4,
      4,
      "Pretty average. Not sure what to think about it. But here's some Markdown: _italics_, **bold**, ~strikethrough~.",
      Instant.now(),
      Instant.now()
    ),
    Review(
      1,
      1,
      1L,
      1,
      1,
      1,
      1,
      1,
      "Hate it with a passion.",
      Instant.now(),
      Instant.now()
    )
  )

  // the same functions as the company cards in the company list page

  private def renderCompanyPicture(company: Company) =
    img(
      cls := "img-fluid",
      src := company.image.getOrElse(Constants.logoPlaceholder),
      alt := company.name
    )

  private def renderDetail(icon: String, value: String) =
    div(
      cls := "company-detail",
      i(cls := s"fa fa-$icon company-detail-icon"),
      p(
        cls := "company-detail-value",
        value
      )
    )

  private def fullLocationString(company: Company): String =
    (company.location, company.country) match {
      case (Some(location), Some(country)) => s"$location, $country"
      case (Some(location), None)          => location
      case (None, Some(country))           => country
      case (None, None)                    => "N/A"
    }

  private def renderOverview(company: Company) =
    div(
      cls := "company-summary",
      renderDetail("location-dot", fullLocationString(company)),
      renderDetail("tags", company.tags.mkString(", "))
    )

  def render(company: Company) = List(

    // dom.console.log(s"Rendering company: $company")
    // dom.console.log(s"Company image: ${company.image}")
    // dom.console.log(s"Logo placeholder: ${Constants.logoPlaceholder}")

    div(
      cls := "row jvm-companies-details-top-card",
      div(
        cls := "col-md-12 p-0",
        div(
          cls := "jvm-companies-details-card-profile-img",
          // renderCompanyPicture(company)
        ),
        div(
          cls := "jvm-companies-details-card-profile-title",
          h1(company.name),
          div(
            cls := "jvm-companies-details-card-profile-company-details-company-and-location",
            renderOverview(company)
          )
        ),
        div(
          cls := "jvm-companies-details-card-apply-now-btn",
          button(
            `type` := "button",
            cls    := "btn btn-warning",
            "Add a review"
          )
        )
      )
    ),

    div(
      cls := "container-fluid",
      renderCompanySummary, // TODO
      dummyReviews.map(renderStaticReview),
      div(
        cls := "container",
        div(
          cls := "rok-last",
          div(
            cls := "row invite-row",
            div(
              cls := "col-md-6 col-sm-6 col-6",
              span(
                cls := "rock-apply",
                p("Do you represent this company?"),
                p("Invite people to leave reviews.")
              )
            ),
            div(
              cls := "col-md-6 col-sm-6 col-6",
              a(
                href   := company.url,
                target := "blank",
                button(`type` := "button", cls := "rock-action-btn", "Invite people")
                // TODO: add new people
              )
            )
          )
        )
      )
    )

  )

  def renderCompanySummary =
    div(
      cls := "container",
      div(
        cls := "markdown-body overview-section",
        div(
          cls := "company-description",
          "TODO company summary"
        )
      )
    )

  def renderStaticReview(review: Review) =
    div(
      cls := "container",
      div(
        cls := "markdown-body overview-section",
        // TODO add a highlight if this is "your" review
        div(
          cls := "company-description",
          div(
            cls := "review-summary",
            renderStaticReviewDetail("Would Recommend", review.wouldRecommend),
            renderStaticReviewDetail("Management", review.management),
            renderStaticReviewDetail("Culture", review.culture),
            renderStaticReviewDetail("Salary", review.salary),
            renderStaticReviewDetail("Benefits", review.benefits)
          ),
          // TODO parse this Markdown
          div(
            cls := "review-content",
            review.review
          ),
          div(cls := "review-posted", "Posted (TODO) a million years ago")
        )
      )
    )

  def renderStaticReviewDetail(detail: String, score: Int) =
    div(
      cls := "review-detail",
      span(cls := "review-detail-name", s"$detail: "),
      (1 to score).toList.map(_ =>
        svg.svg(
          svg.cls     := "review-rating",
          svg.viewBox := "0 0 32 32",
          svg.path(
            svg.d := "m15.1 1.58-4.13 8.88-9.86 1.27a1 1 0 0 0-.54 1.74l7.3 6.57-1.97 9.85a1 1 0 0 0 1.48 1.06l8.62-5 8.63 5a1 1 0 0 0 1.48-1.06l-1.97-9.85 7.3-6.57a1 1 0 0 0-.55-1.73l-9.86-1.28-4.12-8.88a1 1 0 0 0-1.82 0z"
          )
        )
      )
    )


  enum Status:
    case LOADING
    case NOT_FOUND
    case OK(company: Company)

  val fetchCompanyBus = EventBus[Option[Company]]()   // receives the backend response

  // transform events in the bus in to into status
  val status: Signal[Status] = fetchCompanyBus.events.scanLeft(Status.LOADING){
    // scanLeft pattern + enums (common in reactive programming)
    // implements a state machine that accumulates state over time.
    // computes next state based on current state and new event
    (_, maybeCompany) => maybeCompany match
      case Some(company) => Status.OK(company)
      case None          => Status.NOT_FOUND
  }

  def apply(companyId: Long) = {

    dom.console.log(s"CompanyPage.apply called with id: $companyId")

    div(

      cls := "container-fluid the-rock",

      onMountCallback( _ =>
          useBackend(_.companyEndpoints.getByIdEndpoint(companyId.toString))
            .emitTo(fetchCompanyBus)
        ),

      children <-- status.map { s =>
        dom.console.log(s"Mapping status: $s")
        s match {
          case Status.LOADING => List(div("loading..."))
          case Status.NOT_FOUND => List(div("company not found..."))
          case Status.OK(company) => render(company)
        }
      }

    )

  }


}
