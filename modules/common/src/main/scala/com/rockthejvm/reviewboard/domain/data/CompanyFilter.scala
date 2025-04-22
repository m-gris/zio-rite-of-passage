package com.rockthejvm.reviewboard.domain.data

import zio.json.JsonCodec

final case class CompanyFilter(
  locations: List[String] = List(),
  countries: List[String] = List(),
  industries: List[String] = List(),
  tags: List[String] = List()
  )
derives JsonCodec: // because this must be passed back-and-forth between frontend and backend

  // Dynamically checks if all List fields are empty
  // Works automatically with any list fields added in the future
  val isEmpty: Boolean = productIterator      // Get all fields of the case class
    .collect { case list: List[_] => list }   // Filter to keep only List fields
    .forall(_.isEmpty)                        // Check if all lists are empty

object CompanyFilter {
  val empty = CompanyFilter()
}
