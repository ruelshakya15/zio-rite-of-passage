package com.rockthejvm.reviewboard.pages

import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom

import zio.*

import com.rockthejvm.reviewboard.common.*
import com.rockthejvm.reviewboard.components.*
import com.rockthejvm.reviewboard.domain.data.*
import com.rockthejvm.reviewboard.http.*
import com.rockthejvm.reviewboard.http.endpoints.CompanyEndpoints
import com.rockthejvm.reviewboard.core.ZJS.*

object CompaniesPage {

  // components
  val filterPanel = new FilterPanel

  val firstBatch =  EventBus[List[Company]]() // SELF (because company backend req not made when we switch pages)
  val companyEvents: EventStream[List[Company]] =
      firstBatch.events.mergeWith {
      filterPanel.triggerFilter.flatMap { newFilter =>
        useBackend(_.company.searchEndpoint(newFilter)).toEventStream
      } // Task[O] , Endpoint apply fxn is being called through extension method
    }

  def apply() =
    sectionTag(
      onMountCallback(_ => useBackend(_.company.getAllEndpoint(())).emitTo(firstBatch)), // SELF (same reason as above)
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
            children <-- companyEvents.map(_.map(renderCompany))
          )
        )
      )
    )

 
  private def renderAction(company: Company) =
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

  private def renderCompany(company: Company) =
    div(
      cls := "jvm-recent-companies-cards",
      div(
        cls := "jvm-recent-companies-card-img",
        CompanyComponents.renderCompanyPicture(company)
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
        CompanyComponents.renderOverview(company)
      ),
      renderAction(company)
    )
}
