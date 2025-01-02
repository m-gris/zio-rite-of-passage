package com.rockthejvm.reviewboard.domain.data

import zio.json.JsonCodec
import zio.json.DeriveJsonCodec

final case class Company(

  // NON-NULLABLE FIELDS --- start
  id: Long,
  name: String,
  slug: String,
  url: String,
  // NON-NULLABLE FIELDS --- end


  // OPTIONAL FIELDS
  location: Option[String] = None,
  country: Option[String] = None,
  industry: Option[String] = None,
  image: Option[String] = None, // BASE 64 -- easy to render on the frontend
  tags: List[String] = List()

)


object Company {
  /* Exposing the COMPANY as an HTTP PAYLOAD
   * i.e make the Company instance to be JSON serializable */
  given codec: JsonCodec[Company] = DeriveJsonCodec.gen[Company] // to serialize BACK & FORTH, TO & FROM JSON
  // this given instance will be passed automatically whenever we need to
  //  - return a Company as an HTTP Payload
  //  - or take a Company as input (as an HTTP Payload)

  def makeSlug(name: String): String =
    name
      .replaceAll(" +", " ") // i.e NORMALIZE TO SINGLE WHITESPACE
      .split(" ")
      .map(_.toLowerCase)
      .mkString(sep="-")
}

