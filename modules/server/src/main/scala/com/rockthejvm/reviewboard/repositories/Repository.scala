package com.rockthejvm.reviewboard.repositories

import io.getquill.*
import io.getquill.jdbczio.Quill

object Repository {

  def configLayer =
    Quill.DataSource.fromPrefix("rockthejvm.db")

  def dbLayer =
    Quill.Postgres.fromNamingStrategy(SnakeCase)

  def dataLayer =
    configLayer >>> dbLayer

}

