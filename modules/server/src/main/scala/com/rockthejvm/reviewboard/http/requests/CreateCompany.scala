package com.rockthejvm.reviewboard.http.requests

import zio.json.JsonCodec
import zio.json.DeriveJsonCodec
import sttp.tapir.generic.auto._
import com.rockthejvm.reviewboard.domain.data.Company

final case class CreateCompany(

  // NON-NULLABLE FIELDS --- start
  name: String,
  url: String,
  // NON-NULLABLE FIELDS --- end

  // OPTIONAL FIELDS
  location: Option[String] = None,
  country: Option[String] = None,
  industry: Option[String] = None,
  image: Option[String] = None, // BASE 64 -- easy to render on the frontend
  tags: Option[List[String]] = None

) {
  def toCompany(id: Long): Company =
    Company(
      id=id,
      slug=Company.makeSlug(name),
      name=name,
      url=url,
      location=location,
      country=country,
      industry=industry,
      image=image,
      tags=tags.getOrElse(List())
    )
}

object CreateCompany {
  given codec: JsonCodec[CreateCompany] = DeriveJsonCodec.gen[CreateCompany]
}
