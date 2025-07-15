package com.rockthejvm.reviewboard.components
import zio.*
import org.scalajs.dom
import com.raquo.laminar.api.L.{*, given}
import com.rockthejvm.reviewboard.domain.data.Review
import com.rockthejvm.reviewboard.http.requests.ReviewCreationRequest
import com.rockthejvm.reviewboard.core.ZJS.*
import com.rockthejvm.reviewboard.core.ZJS.useBackend

class AddReviewCard(companyId: Long, onCancel: () => Unit, triggerBus: EventBus[Unit]) {

  case class ReviewingState(
    review: Review = Review.empty(companyId),
    showErrors: Boolean = false,
    upstreamError: Option[String] = None,
    )

  val state = Var(ReviewingState())

  val submiter = Observer[ReviewingState] { thisState =>
    dom.console.log("Button clicked")
    if thisState.upstreamError.nonEmpty then {
      state.update(_.copy(showErrors=true))
    } else {
      useBackend(_.reviewEndpoints.createEndpoint(ReviewCreationRequest.fromReview(thisState.review)))
        // IF SUCCESSFUL BACKEND-CALL
        .map { resp =>
          // FIX: Must call onCancel() to hide the form after successful submission
          // Without this, the form stays open with filled data, making it appear
          // as if nothing happened even though the review was posted successfully
          onCancel()
        }
        // IF FAILURE IN BACKEND-CALL
        .tapError { (error: Throwable) =>
          ZIO.succeed {
            state.update(_.copy(showErrors = true, upstreamError = Some(error.getMessage)))
          }
        }
        // ZIO EFFECT DESCRIBED .. Now must execute it !
        // emitTo(triggerBus) does two things:
        // 1. Executes the ZIO effect (posts the review)
        // 2. Emits () to triggerBus, which triggers CompanyPage to refresh the review list
        .emitTo(triggerBus)
    }
  }

  def apply() =
    div(
      cls := "container",
      div(
        cls := "markdown-body overview-section",
        div(
          cls := "company-description add-review",
          div(
            // score dropdowns
            div(
              cls := "add-review-scores",
              renderDropDown("Would recommend", (s, v) => s.copy(wouldRecommend = v)),
              renderDropDown("Management", (s, v) => s.copy(management = v)),
              renderDropDown("Salary", (s, v) => s.copy(salary = v)),
              renderDropDown("Culture", (s, v) => s.copy(culture = v)),
              renderDropDown("Benefits", (s, v) => s.copy(benefits = v)),
              ),
            // text area for the text review
            div(
              cls := "add-review-text",
              label(forId := "add-review-text", "Your review - supports Markdown"),
              textArea(
                idAttr      := "add-review-text",
                cls         := "add-review-text-input",
                placeholder := "Write your review here",
                onInput.mapToValue --> state.updater { (s: ReviewingState, value: String) =>
                  s.copy(review = s.review.copy(review=value))
                }
              )
            ),
            button(
              `type` := "button",
              cls    := "btn btn-warning rock-action-btn",
              "Post review",
              onClick.preventDefault.mapTo(state.now()) --> submiter
            ),
            a(
              cls  := "add-review-cancel",
              href := "#",
              "Cancel",
              onClick --> (_ => onCancel() )
              ),
            children <--  state.signal
              .map(s => s.upstreamError.filter(_ => s.showErrors))
              .map(maybeRenderError)
              .map(_.toList)
          )
        )
      )
    )

  private def maybeRenderError(maybeError: Option[String]) = maybeError.map {
    message => div(cls := "page-status-errors", message)
  }

  private def renderDropDown(name: String, updateFn: (Review, Int) => Review ) =
    val selectId = name.split(" ").map(_.toLowerCase).mkString("-")
      div(
        cls := "add-review-score",
        label(forId := selectId, s"$name:"),
        select(
          idAttr := "would-recommend-selector",
          (1 to 5).reverse.map { v =>
            option(
              v.toString,
              )
          },
          onInput.mapToValue --> state.updater {
            (s: ReviewingState, value: String) => (s.copy(review=updateFn(s.review, value.toInt)))
          }
          )
        )
      // TODO do the same for all score fields

}
