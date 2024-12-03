package com.rockthejvm.reviewboard.components

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.codecs.*
import org.scalajs.dom

import zio.*

import com.rockthejvm.reviewboard.domain.data.CompanyFilter
import com.rockthejvm.reviewboard.core.ZJS.*

/**   1. Populate the panel with the right values
  *      a. expose some API that will retrieve the unique value for filtering b. fetch those value
  *         to populate the panel 2. Update the FP when they interact with it 3. When clicking
  *         "apply filters" we should retrieve just those companies
  *      a. make the backend search b. refetch companies when user clicks the filter
  */

class FilterPanel {

  case class CheckValueEvent(groupName: String, value: String, checked: Boolean)

  private val GROUP_LOCATIONS  = "Locations"
  private val GROUP_COUNTRIES  = "Countries"
  private val GROUP_INDUSTRIES = "Insdustries"
  private val GROUP_TAGS       = "Tags"

  private val possibleFilter = EventBus[CompanyFilter]()
  private val checkEvents    = EventBus[CheckValueEvent]()
  private val clicks         = EventBus[Unit]() // clicks on the "apply button"
  private val dirty = clicks.events
    .mapTo(false)
    .mergeWith( // "clicks" trigger vo vane false , "checkEvents" trigger vo vane true return garcha (i.e. when applyfilter/checkboxes clicked)
      checkEvents.events.mapTo(true)
    ) // emits true or false depending on whether to show "apply" or not

  // Map[String, Set[String]] -> CompanyFilter
  private val state: Signal[CompanyFilter] = checkEvents.events
    .scanLeft(Map[String, Set[String]]()) { (currentMap, event) =>
      event match {
        case CheckValueEvent(groupName, value, checked) =>
          // ("getOrElse") part -> checking if Set(values) exists with the group name else add value to empty Set(), or add value to current Set(...)
          if (checked) currentMap + (groupName -> (currentMap.getOrElse(groupName, Set()) + value))
          else currentMap + (groupName         -> (currentMap.getOrElse(groupName, Set()) - value))
      }
    } // .scanLeft returns -> Signal[Map[String, Set[String]]]
    .map { checkMap =>
      CompanyFilter(
        locations = checkMap.getOrElse(GROUP_LOCATIONS, Set()).toList,
        countries = checkMap.getOrElse(GROUP_COUNTRIES, Set()).toList,
        industries = checkMap.getOrElse(GROUP_INDUSTRIES, Set()).toList,
        tags = checkMap.getOrElse(GROUP_TAGS, Set()).toList
      )
    }

  // informs CompanyPage to rerender
  val triggerFilter: EventStream[CompanyFilter] =
    clicks.events.withCurrentValueOf(state) // when new value in click currentValue of state emitted

  def apply() =
    div(
      onMountCallback(_ => useBackend(_.company.allFiltersEndpoint(())).emitTo(possibleFilter)),
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
            renderFilterOptions(GROUP_LOCATIONS, _.locations),
            renderFilterOptions(GROUP_COUNTRIES, _.countries),
            renderFilterOptions(GROUP_INDUSTRIES, _.industries),
            renderFilterOptions(GROUP_TAGS, _.tags),
            renderApplyButton()
          )
        )
      )
    )

  def renderFilterOptions(groupName: String, optionsFn: CompanyFilter => List[String]) =
    div(
      cls := "accordion-item",
      h2(
        cls    := "accordion-header",
        idAttr := s"heading$groupName",
        button(
          cls                                         := "accordion-button collapsed",
          `type`                                      := "button",
          htmlAttr("data-bs-toggle", StringAsIsCodec) := "collapse",
          htmlAttr("data-bs-target", StringAsIsCodec) := s"#collapse$groupName",
          htmlAttr("aria-expanded", StringAsIsCodec)  := "false",
          htmlAttr("aria-controls", StringAsIsCodec)  := s"collapse$groupName",
          groupName
        )
      ),
      div(
        cls                                          := "accordion-collapse collapse",
        idAttr                                       := s"collapse$groupName",
        htmlAttr("aria-labelledby", StringAsIsCodec) := "headingOne",
        htmlAttr("data-bs-parent", StringAsIsCodec)  := "#accordionExample",
        div(
          cls := "accordion-body",
          div(
            cls := "mb-3",
            // stateful Signal, Var
            children <-- possibleFilter.events
              .toSignal(CompanyFilter.empty) // initial value of Signal
              .map(filter => optionsFn(filter).map(value => renderCheckbox(groupName, value)))
          )
        )
      )
    )

  private def renderCheckbox(groupName: String, value: String) =
    div(
      cls := "form-check",
      label(
        cls   := "form-check-label",
        forId := s"filter-$groupName-$value",
        value
      ),
      input(
        cls    := "form-check-input",
        `type` := "checkbox",
        idAttr := s"filter-$groupName-$value",
        onChange.mapToChecked.map(CheckValueEvent(groupName, value, _)) --> checkEvents
      )
    )

  private def renderApplyButton() =
    div(
      cls := "jvm-accordion-search-btn",
      button(
        disabled <-- dirty.toSignal(false).map(v => !v),
        onClick.mapTo(()) --> clicks,
        cls    := "btn btn-primary",
        `type` := "button",
        "Apply Filters"
      )
    )
}
