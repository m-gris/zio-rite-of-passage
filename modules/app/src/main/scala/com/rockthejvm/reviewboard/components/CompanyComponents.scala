package com.rockthejvm.reviewboard.components

import org.scalajs.dom
import com.raquo.laminar.codecs.*
import com.raquo.laminar.api.L.{*, given}

import com.rockthejvm.reviewboard.domain.data.Company

object CompanyComponents {

  // private def renderCompanyPicture(company: Company) =
  //   img(
  //     cls := "img-fluid",
  //     src := company.image.getOrElse(Constants.logoPlaceholder),
  //     alt := company.name
  //   )
  //
  // private def renderDetail(icon: String, value: String) =
  //   div(
  //     cls := "company-detail",
  //     i(cls := s"fa fa-$icon company-detail-icon"),
  //     p(
  //       cls := "company-detail-value",
  //       value
  //     )
  //   )
  //
  // private def fullLocationString(company: Company): String =
  //   (company.location, company.country) match {
  //     case (Some(location), Some(country)) => s"$location, $country"
  //     case (Some(location), None)          => location
  //     case (None, Some(country))           => country
  //     case (None, None)                    => "N/A"
  //   }
  //
  // private def renderOverview(company: Company) =
  //   div(
  //     cls := "company-summary",
  //     renderDetail("location-dot", fullLocationString(company)),
  //     renderDetail("tags", company.tags.mkString(", "))
  //   )
  //

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

}
