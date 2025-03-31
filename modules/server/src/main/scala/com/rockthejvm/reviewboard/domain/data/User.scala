package com.rockthejvm.reviewboard.domain.data

final case class User(
  id: Long,
  email: String,
  hashedPassword: String
  ){
    def toIdentifiers = Identifiers(id, email)
  }


final case class Identifiers(id: Long, email: String)
