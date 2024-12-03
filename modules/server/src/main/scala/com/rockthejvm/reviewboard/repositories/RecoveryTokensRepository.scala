package com.rockthejvm.reviewboard.repositories

import zio.*

import io.getquill.*
import io.getquill.jdbczio.Quill

import com.rockthejvm.reviewboard.domain.data.PasswordRecoveryToken
import com.rockthejvm.reviewboard.config.RecoveryTokensConfig
import com.rockthejvm.reviewboard.config.Configs

trait RecoveryTokensRepository {
  def getToken(email: String): Task[Option[String]] // option because email might be incorrect
  def checkToken(email: String, token: String): Task[Boolean]
}

class RecoveryTokensRepositoryLive private (
    tokenConfig: RecoveryTokensConfig,
    quill: Quill.Postgres[SnakeCase],
    userRepo: UserRepository
) extends RecoveryTokensRepository {

  import quill.*

  inline given schema: SchemaMeta[PasswordRecoveryToken] =
    schemaMeta[PasswordRecoveryToken]("recovery_tokens")
  inline given insMeta: InsertMeta[PasswordRecoveryToken] = insertMeta[PasswordRecoveryToken]()
  inline given upMeta: UpdateMeta[PasswordRecoveryToken] =
    updateMeta[PasswordRecoveryToken](_.email)

  private def randomUpperCaseString(len: Int): Task[String] = // AB12CD3D
    ZIO.succeed(scala.util.Random.alphanumeric.take(len).mkString.toUpperCase)

  private def findToken(email: String): Task[Option[String]] =
    run(query[PasswordRecoveryToken].filter(_.email == lift(email))).map(_.headOption.map(_.token))
    // select token from recovery_tokens where email = ?

  private def replaceToken(email: String): Task[String] =
    for {
      token <- randomUpperCaseString(8)
      _ <- run(
        query[PasswordRecoveryToken]
          .updateValue( // NOTE: yesma problem cha jasto lairacha
            lift(
              PasswordRecoveryToken(
                email,
                token,
                java.lang.System.currentTimeMillis() + tokenConfig.duration
              )
            )
          )
          .returning(r => r)
      )
    } yield token

  private def generateToken(email: String): Task[String] =
    for {
      token <- randomUpperCaseString(8)
      _ <- run(
        query[PasswordRecoveryToken]
          .insertValue(
            lift(
              PasswordRecoveryToken(
                email,
                token,
                java.lang.System.currentTimeMillis() + tokenConfig.duration
              )
            )
          )
          .returning(r => r)
      )
    } yield token

  private def makeFreshToken(email: String): Task[String] =
    findToken(email).flatMap {
      case Some(_) => replaceToken(email)
      case None    => generateToken(email)
    }
    // find token in the table ,
    // if so, replace
    // if not, create

  override def getToken(email: String): Task[Option[String]] =
    userRepo.getByEmail(email).flatMap {
      case None    => ZIO.none
      case Some(_) => makeFreshToken(email).map(Some(_))
    }
    // check the user in the DB
    // if the user exists, make a fresh a token

  override def checkToken(email: String, token: String): Task[Boolean] =
    for {
      now <- Clock.instant
      checkValid <- run(
        query[PasswordRecoveryToken]
          .filter(r =>
            r.email == lift(email) && r.token == lift(token) && r.expiration > lift(
              now.toEpochMilli
            )
          )
      ).map(_.nonEmpty)
    } yield checkValid
}

object RecoveryTokensRepositoryLive {
  val layer = ZLayer {
    for {
      config   <- ZIO.service[RecoveryTokensConfig]
      quill    <- ZIO.service[Quill.Postgres[SnakeCase]]
      userRepo <- ZIO.service[UserRepository]
    } yield new RecoveryTokensRepositoryLive(config, quill, userRepo)
  }

  val configuredLayer =
    Configs.makeLayer[RecoveryTokensConfig]("rockthejvm.recoverytokens") >>> layer
}
