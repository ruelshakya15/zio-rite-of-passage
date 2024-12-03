package com.rockthejvm.reviewboard.repositories

import zio.*

import io.getquill.*
import io.getquill.jdbczio.Quill

import com.rockthejvm.reviewboard.domain.data.InviteNamedRecord
import com.rockthejvm.reviewboard.domain.data.InviteRecord
import com.rockthejvm.reviewboard.domain.data.Company

trait InviteRepository {
  def getByUserName(userName: String): Task[List[InviteNamedRecord]]
  def getInvitePack(userName: String, companyId: Long): Task[Option[InviteRecord]]
  def addInvitePack(userName: String, companyId: Long, nInvites: Int): Task[Long]
  def activatePack(id: Long): Task[Boolean]
  def markInvites(userName: String, companyId: Long, nInvites: Int): Task[Int]

}

class InviteRepositoryLive private (quill: Quill.Postgres[SnakeCase]) extends InviteRepository {
  import quill.*

  inline given schema: SchemaMeta[InviteRecord]  = schemaMeta[InviteRecord]("invites")
  inline given insMeta: InsertMeta[InviteRecord] = insertMeta[InviteRecord](_.id)
  inline given upMeta: UpdateMeta[InviteRecord]  = updateMeta[InviteRecord](_.id)

  // company table
  inline given companySchema: SchemaMeta[Company]  = schemaMeta[Company]("companies")
  inline given companyInsMeta: InsertMeta[Company] = insertMeta[Company](_.id)
  inline given companyUpMeta: UpdateMeta[Company]  = updateMeta[Company](_.id)

  /** select companyId, companyName, nInvites from invites, companies where invites.userName == ?
    * and invite.actives = true and invites.nInvites > 0 and invites.companyId == companies.id
    *
    * @param userName
    * @return
    */
  override def getByUserName(userName: String): Task[List[InviteNamedRecord]] =
    // ***NOTICE: run in the outermost layer i think because so same instance used in both unlike in companyRepo code
    run(
      for {
        record <- query[InviteRecord]
          .filter(_.userName == lift(userName))
          .filter(_.nInvites > 0)
          .filter(_.active)
        company <- query[Company]
        if company.id == record.companyId // join condition using if guards
      } yield InviteNamedRecord(company.id, company.name, record.nInvites)
    )

  override def getInvitePack(userName: String, companyId: Long): Task[Option[InviteRecord]] =
    run(
      query[InviteRecord]
        .filter(_.companyId == lift(companyId))
        .filter(_.userName == lift(userName))
        .filter(_.active)
    ).map(_.headOption)

    // WARNING: adding multiple active packs for the same company may cause unexpected behavior
  override def addInvitePack(userName: String, companyId: Long, nInvites: Int): Task[Long] =
    run(
      query[InviteRecord]
        .insertValue(lift(InviteRecord(-1, userName, companyId, nInvites, false)))
        .returning(_.id)
    )

  override def activatePack(id: Long): Task[Boolean] =
    for {
      current <- run(query[InviteRecord].filter(_.id == lift(id)))
        .map(_.headOption)
        .someOrFail(
          new RuntimeException(s"Unable to activate pack $id: no such pack")
        ) // this someOrfail also safeguard against current.copy being called below as None.copy will result in error
      result <- run(
        query[InviteRecord]
          .filter(_.id == lift(id))
          .updateValue(lift(current.copy(active = true)))
          .returning(_ => true) 
      )
    } yield result

    /* Adding Pack Flow:
       trigger addInvitePack (active false)
       payment process
       activate the pack that was just paid
     */

  override def markInvites(userName: String, companyId: Long, nInvites: Int): Task[Int] =
    for {
      currentRecord <- getInvitePack(userName, companyId)
        .someOrFail(
          new RuntimeException(s"user $userName cannot send invites for company id $companyId")
        )
      nInvitesMarked <- ZIO.succeed(Math.min(nInvites, currentRecord.nInvites))
      _ <- run(
        query[InviteRecord]
          .filter(_.id == lift(currentRecord.id))
          .updateValue(lift(currentRecord.copy(nInvites = currentRecord.nInvites - nInvitesMarked)))
          returning (r => r)
      )
    } yield nInvitesMarked

}

object InviteRepositoryLive {
  val layer = ZLayer {
    for {
      quill <- ZIO.service[Quill.Postgres[SnakeCase]]
    } yield new InviteRepositoryLive(quill)
  }
}

object InviteRepositoryDemo extends ZIOAppDefault {
  override def run: ZIO[Any & (ZIOAppArgs & Scope), Any, Any] = {
    val program = for {
      repo    <- ZIO.service[InviteRepository]
      records <- repo.getByUserName("daniel@rockthejvm.com")
      _       <- Console.printLine(s"Records: ${records}")
    } yield ()

    program.provide(
      InviteRepositoryLive.layer,
      Repository.dataLayer
    )
  }
}
