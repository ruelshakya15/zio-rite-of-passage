package com.rockthejvm.reviewboard.repositories

import io.getquill.jdbczio.Quill
import io.getquill.SnakeCase

object Repository {
  def quillLayer =
    Quill.Postgres.fromNamingStrategy(SnakeCase) //  quill instance

  def dataSourceLayer =
    Quill.DataSource.fromPrefix("rockthejvm.db") // Note: Quill uses "javax.sql.DataSource"

  val dataLayer =
    dataSourceLayer >>> quillLayer

}
