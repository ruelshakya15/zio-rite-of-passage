package com.rockthejvm

import io.getquill.{jdbczio, *}
import io.getquill.jdbczio.Quill
import zio.*
import zio.schema.validation.Predicate.Str

object QuillDemo extends ZIOAppDefault {

  val program = for {
    repo <- ZIO.service[JobRepository]
    _ <- repo.create(
      Job(-1, "Software engineer", "rockthejvm.com", "Rock the JVM")
    ) // -1 in id because it will be ignored
    _ <- repo.create(Job(-1, "Instructor", "rockthejvm.com", "Rock the JVM"))
  } yield ();

//  *** IMP NOTE: Need to CLOSE connection of pgadmin running on same port 8432 by stopping the service or it wont run
  override def run = program.provide(
    JobRepositoryLive.layer,
    Quill.Postgres.fromNamingStrategy(SnakeCase), //  quill instance
    Quill.DataSource.fromPrefix("mydbconf") // reads the config section in application.conf and spins up a data source
  )
}

// repository
trait JobRepository {
  def create(job: Job): Task[Job]
  def update(id: Long, op: Job => Job): Task[Job]
  def delete(id: Long): Task[Job]
  def getById(id: Long): Task[Option[Job]]
  def get: Task[List[Job]]
}
// Naming Strategy
class JobRepositoryLive(quill: Quill.Postgres[SnakeCase]) extends JobRepository {
  // step 1
  import quill.* // some methods e.g. run a query
  // step 2 - schemas for create, update ...       (These will be passed automatically to our run methods)  ("inline" added to auto generate type safe queries done by macros by quill)
  inline given schema: SchemaMeta[Job] = schemaMeta[Job]("jobs") // describes our Table
  inline given insMeta: InsertMeta[Job] = insertMeta[Job]( _.id) // columns to be excluded in insert statements (postgres generate id by itself)
  inline given upMeta: UpdateMeta[Job] = updateMeta[Job](_.id) // same for update statements

  def create(job: Job): Task[Job] =
    run {
      query[Job]
        .insertValue(
          lift(job)
        ) // careful with lift() - uses macro to convert to table representation   (used when update,delete,insert)
        .returning(j => j)
    }

  def update(id: Long, op: Job => Job): Task[Job] = for {
    current <- getById(id).someOrFail(new RuntimeException(s"Could not update: missing key $id"))
    updated <- run {
      query[Job]
        .filter(_.id == lift(id)) // comparing table id with lifted argument id    (where clause)
        .updateValue(lift(op(current))) //***IMP understand this
        .returning(j => j)

    }
  } yield updated

  def delete(id: Long): Task[Job] =
    run {
      query[Job]
        .filter(_.id == lift(id))
        .delete
        .returning(j => j)
    }

  def getById(id: Long): Task[Option[Job]] =
    run {
      query[Job]
        .filter(_.id == lift(id)) // select * from jobs where id = ? limit 1
    }.map(_.headOption)           // run returns a Seq[ZIO] in this case

  def get: Task[List[Job]] =
    run(query[Job])
}

object JobRepositoryLive {
  val layer = ZLayer {
    ZIO.service[Quill.Postgres[SnakeCase]].map(quill => JobRepositoryLive(quill))
  }
}
