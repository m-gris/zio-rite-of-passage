package com.rockthejvm.reviewboard.http.requests

import java.time.Instant

import zio.json.JsonCodec
import zio.json.DeriveJsonCodec
import sttp.tapir.generic.auto._

import com.rockthejvm.reviewboard.domain.data.Review

final case class ReviewCreationRequest(
  companyId: Long,
  management: Int,
  culture: Int,
  salary: Int,
  benefits: Int,
  wouldRecommend: Int,
  review: String,
) {
  // def toReview(id: Long, created: Instant, updated: Instant): Review =
  //   Review(
  //   id=id,
  //   companyId=companyId,
  //   userId=userId,
  //   management=management,
  //   culture=culture,
  //   salary=salary,
  //   benefits=benefits,
  //   wouldRecommend=wouldRecommend,
  //   review=review,
  //   created=created,
  //   updated=updated,
  //   )
}

object ReviewCreationRequest {
  given codec: JsonCodec[ReviewCreationRequest] = DeriveJsonCodec.gen[ReviewCreationRequest]
  def fromReview(review: Review) = ReviewCreationRequest(
  companyId=review.companyId,
  management=review.management,
  culture=review.culture,
  salary=review.salary,
  benefits=review.benefits,
  wouldRecommend=review.wouldRecommend,
  review=review.review,
  )
}
