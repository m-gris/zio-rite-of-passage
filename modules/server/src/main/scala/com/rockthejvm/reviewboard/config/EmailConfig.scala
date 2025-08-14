package com.rockthejvm.reviewboard.config

final case class EmailConfig(
  host: String,
  port: Int,
  user: String,
  pwd: String,
  baseUrl: String,
  )
