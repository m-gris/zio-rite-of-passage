package com.rockthejvm.reviewboard.pages

import java.time.Instant

import zio.*
import org.scalajs.dom
import com. raquo. laminar.api.L.{*, given}

import com.rockthejvm.reviewboard.common.*
import com.rockthejvm.reviewboard.domain.data.*
import com.rockthejvm.reviewboard.core.ZJS.*
import com.rockthejvm.reviewboard.components.CompanyComponents
import com.rockthejvm.reviewboard.core.Session
import com.rockthejvm.reviewboard.components.AddReviewCard
import com.rockthejvm.reviewboard.components.Time
import com.rockthejvm.reviewboard.components.Markdown
import com.raquo.laminar.DomApi

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


  enum Status:
    case LOADING
    case NOT_FOUND
    case OK(company: Company)

  // REACTIVATE VARIABLES

  val addReviewCardActive = Var[Boolean](false)
  val fetchCompanyBus = EventBus[Option[Company]]()   // receives the backend response
  val triggerRefreshBus = EventBus[Unit]()

  def refreshReviews(companyId: Long) =
    useBackend(_.reviewEndpoints.getByCompanyIdEndpoint(companyId))
      .toEventStream
      .mergeWith(triggerRefreshBus.events.flatMap(_ =>
        useBackend(_.reviewEndpoints.getByCompanyIdEndpoint(companyId))
          .toEventStream
          )
        )

  def reviewsSignal(companyId: Long): Signal[List[Review]] = fetchCompanyBus.events.flatMap {
    case None => EventStream.empty
    case Some(company) => refreshReviews(companyId)

  }.scanLeft(List[Review]())((_, list) => list)

  // transform events in the bus in to into status
  val status: Signal[Status] = fetchCompanyBus.events.scanLeft(Status.LOADING){
    // scanLeft pattern + enums (common in reactive programming)
    // implements a state machine that accumulates state over time.
    // computes next state based on current state and new event
    (_, maybeCompany) => maybeCompany match
      case Some(company) => Status.OK(company)
      case None          => Status.NOT_FOUND
  }

  // "RENDERERS"

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
          case Status.OK(company) => render(company, reviewsSignal(companyId))
        }
      },

    )

  }


  def render(company: Company, reviewsSignal: Signal[List[Review]]) = List(

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
            CompanyComponents.renderOverview(company)
          )
        ),
        child <-- Session.userState.signal.map(maybeUser => maybeRenderUserAction(maybeUser, reviewsSignal)),
      )
    ),

    div(
      cls := "container-fluid",
      renderCompanySummary, // TODO
      children <-- addReviewCardActive.signal
        .map(isActive => Option.when(isActive)(
          AddReviewCard(
            company.id,
            onCancel = () => addReviewCardActive.set(false),
            triggerBus = triggerRefreshBus,
            ).apply()
          )
        )
        .map(_.toList),
      children <-- reviewsSignal.map(_.map(renderReview)),
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

  def maybeRenderUserAction(maybeUserSession: Option[UserSession], reviews: Signal[List[Review]]) =

    maybeUserSession match  {

      case None =>
        div(
          cls := "jvm-companies-details-card-apply-now-btn",
          "You must be logged in to post a review"
          )

      case Some(userSession) =>
          div(
            cls := "jvm-companies-details-card-apply-now-btn",
            child <-- reviews
              .map(_.find(_.userId ==  userSession.id)) // Signal[Option[Review]]
              .map {
                case None => button(
                              `type` := "button",
                              cls    := "btn btn-warning",
                              "Add a review",
                              disabled <-- addReviewCardActive.signal,
                              onClick.mapTo(true) --> addReviewCardActive.writer
                              )
                case Some(_) => div("You already posted a review")
              }
          )

    }

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

  def renderReview(review: Review) =

    def isReviewFromThisUser(user: Option[UserSession]): Boolean =
      user.map(_.id) == Option(review).map(_.userId)

    div(
      cls := "container",
      div(
        cls := "markdown-body overview-section",
        // TODO add a highlight if this is "your" review
        cls.toggle("review-highlighted") <-- Session.userState.signal
          .map(isReviewFromThisUser),
        div(
          cls := "company-description",
          div(
            cls := "review-summary",
            renderReviewDetail("Would Recommend", review.wouldRecommend),
            renderReviewDetail("Management", review.management),
            renderReviewDetail("Culture", review.culture),
            renderReviewDetail("Salary", review.salary),
            renderReviewDetail("Benefits", review.benefits)
          ),
          div(
            cls := "review-content",
            // Markdown.toHtml(review.review)
            injectMarkdown(review)
          ),
          div( cls := "review-posted", s"Posted ${Time.unix2humanReadable(review.created.toEpochMilli())}"),
          child.maybe <-- Session.userState.signal // n.b  child.MAYBE <-- Signal[OPTION]
            .map(_.filter(_.id == review.userId))
            .map(_.map(_ => div(cls := "review-posted", "Your review")))
        )
      )
    )

  def renderReviewDetail(detail: String, score: Int) =
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


  def injectMarkdown(review: Review) = div(
    cls := "review-content",
      DomApi
        .unsafeParseHtmlStringIntoNodeArray(Markdown.toHtml(review.review))
        .map{
          case node: dom.Text => span(node.data)
          case node: dom.html.Element => foreignHtmlElement(node)
          case _ => emptyNode
        }
    )



}
