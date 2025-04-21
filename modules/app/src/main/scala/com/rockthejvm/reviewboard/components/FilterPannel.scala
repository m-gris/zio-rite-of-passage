package com.rockthejvm.reviewboard.components

import frontroute.*
import org.scalajs.dom
import com.raquo.laminar.codecs.*
import com.raquo.laminar.api.L.{*, given}

import com.rockthejvm.reviewboard.core.ZJS.*
import com.rockthejvm.reviewboard.domain.data.CompanyFilter
import com.rockthejvm.reviewboard.http.endpoints.CompanyEndpoints

/*
* 2. Update the FP when they interact with it
* 3. When clicking "apply filters" we should retrieve just those companies
* a. make the backend search
* b. refetch companies when user clicks the filter
*/


object FilterPannel {

  enum Filter:
    case LOCATIONS, COUNTRIES, INDUSTRIES, TAGS

  case class FilterCheck(filter: Filter, value: String, checked: Boolean)

  // "Reactive Variable" to surface the filters to the FrontEnd
  // We will emit a backend call to /companies/filters
  // which will emit a value on that bus
  val possibleFilter = Var[CompanyFilter](CompanyFilter.empty /* initial value */)

  val filterChecks = EventBus[FilterCheck]() // "fed" by renderCheckBox
  val clicksOnApplyButton = EventBus[Unit]() // clicks on Apply buton

  // the `apply` button, once clicked should only be clickable again
  // after a checkbox have been clicked
  val applyButtonIsClickable: EventStream[Boolean] = clicksOnApplyButton
                                                      .events
                                                      .mapTo(false)
                                                      .mergeWith(filterChecks
                                                                  .events
                                                                  .mapTo(true))

  // STATE - Done in 2 steps
  // 1. Map[FilterGroup, Set[String]] => 2. CompanyFilter
  // i.e mapping from FilterGroup to a Set of boxes that have been checked
  // this mapping will then be transformed into a CompanyFilter
  val state: Signal[CompanyFilter] =
    filterChecks
      .events // event-stream
      .scanLeft(Map[Filter, Set[String]]()) { (current_map, event) =>

        event match {
          case FilterCheck(groupName, value, checked) =>
            // add or remove the value, depending on checked status
            val starting_set = current_map.getOrElse(groupName, Set())
            val ending_set = if checked then starting_set + value
                             else starting_set - value
            current_map.updated(groupName, ending_set)
            }

        }.map { new_map =>

            CompanyFilter(
              locations=new_map.getOrElse(Filter.LOCATIONS, Set()).toList,
              countries=new_map.getOrElse(Filter.COUNTRIES, Set()).toList,
              industries=new_map.getOrElse(Filter.INDUSTRIES, Set()).toList,
              tags=new_map.getOrElse(Filter.TAGS, Set()).toList,
              )



      }

  def apply() =

    div(

      onMountCallback{
        val unit = () // just to reduce "parentheses-noise"
        _ => useBackend(_.companyEndpoints.allFiltersEndpoint(unit))
              .map(possibleFilter.set)
              .runJs
      },

      // DEBUG TRICKS...
      // child.text <-- possibleFilter.events.map(_.toString),
      // child.text <-- checkEvents.events.map(_.toString),
      // child.text <-- state.map(_.toString),
      // child.text <-- applyButtonIsClickable.map(_.toString),

      // Side node about "REACTIVITY"...
      // i.e... the div will be refreshed whenever there is a change
      // i.e... when there a new event in possibleFilter

      cls    := "accordion accordion-flush",
      idAttr := "accordionFlushExample",
      div(
        cls := "accordion-item",
        h2(
          cls    := "accordion-header",
          idAttr := "flush-headingOne",
          button(
            cls                                         := "accordion-button",
            idAttr                                      := "accordion-search-filter",
            `type`                                      := "button",
            htmlAttr("data-bs-toggle", StringAsIsCodec) := "collapse",
            htmlAttr("data-bs-target", StringAsIsCodec) := "#flush-collapseOne",
            htmlAttr("aria-expanded", StringAsIsCodec)  := "true",
            htmlAttr("aria-controls", StringAsIsCodec)  := "flush-collapseOne",
            div(
              cls := "jvm-recent-companies-accordion-body-heading",
              h3(
                span("Search"),
                " Filters"
              )
            )
          )
        ),
        div(
          cls                                          := "accordion-collapse collapse show",
          idAttr                                       := "flush-collapseOne",
          htmlAttr("aria-labelledby", StringAsIsCodec) := "flush-headingOne",
          htmlAttr("data-bs-parent", StringAsIsCodec)  := "#accordionFlushExample",
          div(
            cls := "accordion-body p-0",
            renderFilterOptions(Filter.LOCATIONS, _.locations),
            renderFilterOptions(Filter.COUNTRIES, _.countries),
            renderFilterOptions(Filter.INDUSTRIES,_.industries),
            renderFilterOptions(Filter.TAGS, _.tags),
            renderApplyButton()
          )
        )
      )
    )

  def renderFilterOptions(groupName: Filter, optionsFn: CompanyFilter => List[String]) =

    div(
      cls := "accordion-item",
      h2(
        cls    := "accordion-header",
        idAttr := s"heading${groupName.toString}",
        button(
          cls                                         := "accordion-button collapsed",
          `type`                                      := "button",
          htmlAttr("data-bs-toggle", StringAsIsCodec) := "collapse",
          htmlAttr("data-bs-target", StringAsIsCodec) := s"#collapse${groupName.toString}",
          htmlAttr("aria-expanded", StringAsIsCodec)  := "false",
          htmlAttr("aria-controls", StringAsIsCodec)  := s"collapse${groupName.toString}",
          groupName.toString
        )
      ),
      div(
        cls                                          := "accordion-collapse collapse",
        idAttr                                       := s"collapse${groupName.toString}",
        htmlAttr("aria-labelledby", StringAsIsCodec) := "headingOne",
        htmlAttr("data-bs-parent", StringAsIsCodec)  := "#accordionExample",
        div(
          cls := "accordion-body",
          div(
            cls := "mb-3",
            // Laminar Reminder: Signal & Var are STATEFUL
            children <-- possibleFilter
                          .signal
                          .map(filter => optionsFn(filter)
                          .map(value => renderCheckBox(groupName, value)))
          )
        )
      )
    )

  private def renderCheckBox(groupName: Filter, value: String) =
    div(
      cls := "form-check",
      label(
        cls   := "form-check-label",
        forId := s"filter-${groupName.toString}-$value",
        value
      ),
    input(
      cls    := "form-check-input",
      `type` := "checkbox",
      idAttr := s"filter-${groupName.toString}-$value",
      onChange // a "listener" / "modifier"
        .mapToChecked
        .map(checked =>
            FilterCheck(groupName, value, checked)
            // emiter of CheckedValueEvents
            // which we feed into the event bus...
            ) --> filterChecks

    )
  )

  private def renderApplyButton() =
    div(
      cls := "jvm-accordion-search-btn",
      button(
        disabled <-- applyButtonIsClickable.toSignal(false).map(bool => !bool),
        onClick.mapTo(()) --> clicksOnApplyButton,
        cls    := "btn btn-primary",
        `type` := "button",
        "Apply Filters"
      )
    )

}
