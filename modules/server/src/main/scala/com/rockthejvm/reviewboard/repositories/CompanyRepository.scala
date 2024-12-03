package com.rockthejvm.reviewboard.repositories

import zio.*
import io.getquill.*
import io.getquill.jdbczio.Quill

import com.rockthejvm.reviewboard.domain.data.Company
import com.rockthejvm.reviewboard.domain.data.CompanyFilter
import cats.syntax.list

trait CompanyRepository {
  def create(company: Company): Task[Company]
  def update(id: Long, op: Company => Company): Task[Company]
  def delete(id: Long): Task[Company]
  def getById(id: Long): Task[Option[Company]]
  def getBySlug(slug: String): Task[Option[Company]]
  def get: Task[List[Company]]
  def uniqueAttributes: Task[CompanyFilter]
  def search(filter: CompanyFilter): Task[List[Company]]
}

class CompanyRepositoryLive(quill: Quill.Postgres[SnakeCase]) extends CompanyRepository { // ***NOTE: SnakeCase error aaucha kaile kai trait ko satta object of SnakeCase basdincha "Postgres[SnakeCase]" yesma so "SnakeCase.type" rakdha huncha
  import quill.*

  inline given schema: SchemaMeta[Company]  = schemaMeta[Company]("companies")
  inline given insMeta: InsertMeta[Company] = insertMeta[Company](_.id)
  inline given upMeta: UpdateMeta[Company]  = updateMeta[Company](_.id)

  override def create(company: Company): Task[Company] =
    run {
      query[Company]
        .insertValue(lift(company)) // lift: It's used to insert a single runtime value into a query
        .returning(r => r)
    }

  override def getById(id: Long): Task[Option[Company]] =
    run {
      query[Company].filter(_.id == lift(id)) // quill expects a List[Company]
    }.map(_.headOption)

  override def getBySlug(slug: String): Task[Option[Company]] =
    run {
      query[Company].filter(_.slug == lift(slug))
    }.map(_.headOption)

  override def get: Task[List[Company]] =
    run(query[Company])

  override def update(id: Long, op: Company => Company): Task[Company] =
    for {
      current <- getById(id).someOrFail(new RuntimeException(s"Could not update: missing id $id"))
      updated <- run {
        query[Company]
          .filter(_.id == lift(id))
          .updateValue(lift(op(current)))
          .returning(r => r)
      }

    } yield updated

  override def delete(id: Long): Task[Company] =
    run {
      query[Company]
        .filter(_.id == lift(id))
        .delete
        .returning(r => r)
    }

  override def uniqueAttributes: Task[CompanyFilter] =
    for {
      locations  <- run(query[Company].map(_.location).distinct).map(_.flatMap(_.toList))
      countries  <- run(query[Company].map(_.country).distinct).map(_.flatMap(_.toList))
      industries <- run(query[Company].map(_.industry).distinct).map(_.flatMap(_.toList))
      tags       <- run(query[Company].map(_.tags)).map(_.flatten.toSet.toList)
    } yield CompanyFilter(locations, countries, industries, tags)

  /** select company from companies where location in filter.locations OR country in .. OR industry
    * in ... OR tags in (select c1.tags from companies where c1.id == company.id)
    */
  override def search(filter: CompanyFilter): Task[List[Company]] =
    if (filter.isEmpty) get
    else
      run {
        query[Company]
          .filter {
            company => // liftQuery: It's used to insert collections or multiple runtime values into a query
              liftQuery(filter.locations.toSet).contains(company.location) ||
              liftQuery(filter.countries.toSet).contains(company.country) ||
              liftQuery(filter.industries.toSet).contains(company.industry) ||
              sql"${company.tags} && ${lift(filter.tags)} ".asCondition // In PostgreSQL, && can be used to check if two arrays have at least one element in common.
          }
      }
      /* ".concatMap"
        This is similar to a flatMap, where we take the list of tags from the selected company and "flatten" them into individual tag values.
        It’s used because a company might have multiple tags, which are stored as a collection.
        This step retrieves each individual tag for the company.
       */
}

object CompanyRepositoryLive {
  val layer = ZLayer {
    ZIO.service[Quill.Postgres[SnakeCase]].map(quill => CompanyRepositoryLive(quill))
  }
}

object CompanyRepositoryDemo extends ZIOAppDefault {

  val program = for {
    repo <- ZIO.service[CompanyRepository]
    _    <- repo.create(Company(-1L, "rock-the-jvm", "Rock the JVM", "rockthejvm.com"))
  } yield ()

  def run = program.provide(
    CompanyRepositoryLive.layer,
    Quill.Postgres.fromNamingStrategy(SnakeCase), //  quill instance
    Quill.DataSource.fromPrefix("rockthejvm.db")
  )
}
