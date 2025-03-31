package com.rockthejvm.reviewboard.domain.data

import zio.json.JsonCodec
import zio.json.DeriveJsonCodec

import java.time.Instant

final case class Review(
  id: Long, // PK
  companyId: Long,
  userId: Long, // FK
  // scores
  management: Int, // 1-5
  culture: Int,
  salary: Int,
  benefits: Int,
  wouldRecommend: Int,
  review: String,
  created: Instant,
  updated: Instant
)


object Review {
  // Exposing the Review as an JSON serializable HTTP PAYLOAD
  given codec: JsonCodec[Review] = DeriveJsonCodec.gen[Review] // to serialize BACK & FORTH, TO & FROM JSON
  // this given instance will be passed automatically whenever we need to
  //  - return a Review as an HTTP Payload
  //  - or take a Review as input (as an HTTP Payload)
}

