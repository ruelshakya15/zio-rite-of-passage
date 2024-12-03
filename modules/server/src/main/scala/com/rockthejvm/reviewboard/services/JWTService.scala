package com.rockthejvm.reviewboard.services

import zio.*
import com.typesafe.config.ConfigFactory // TO READ CONF FILES

import com.auth0.jwt.* // FOR JWT tokens
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.JWTVerifier.BaseVerification

import java.time.Instant

import com.rockthejvm.reviewboard.config.*
import com.rockthejvm.reviewboard.domain.data.{User, UserID, UserToken}

trait JWTService {
  def createToken(user: User): Task[UserToken]
  def verifyToken(token: String): Task[UserID]
}
// we are going to use same clock to build & verify tokens
class JWTServiceLive(jwtConfig: JWTConfig, clock: java.time.Clock) extends JWTService {
  private val ISSUER         = "rockthejvm.com"
  private val CLAIM_USERNAME = "username"
  // same hash we used in pass words
  private val algorithm = Algorithm.HMAC512(jwtConfig.secret) // acts as a salt
  val verifier: JWTVerifier =
    JWT
      .require(algorithm)
      .withIssuer(ISSUER)
      .asInstanceOf[BaseVerification]
      .build(clock)

  override def createToken(user: User): Task[UserToken] =
    for {
      now <- ZIO.attempt(
        clock.instant()
      ) // (returns ZIO with java instant) use this to avoid java.instant side effect and make it an effectful operation
      expiration <- ZIO.succeed(now.plusSeconds(jwtConfig.ttl))
      token <- ZIO.attempt(
        JWT
          .create()
          .withIssuer(ISSUER)
          .withIssuedAt(now)
          .withExpiresAt(expiration)
          .withSubject(user.id.toString) // user identifier
          .withClaim(CLAIM_USERNAME, user.email)
          .sign(algorithm)
      )
    } yield UserToken(
      user.id,
      user.email,
      token,
      expiration.getEpochSecond() /*convert Instant() -> Long*/
    )

  override def verifyToken(token: String): Task[UserID] =
    for {
      decoded <- ZIO.attempt(verifier.verify(token))
      userId <- ZIO.attempt(
        UserID(
          decoded.getSubject().toLong,
          decoded.getClaim(CLAIM_USERNAME).asString()
        )
      )
    } yield userId
}

object JWTServiceLive {
  val layer = ZLayer {
    for {
      jwtConfig <- ZIO.service[JWTConfig]
      clock     <- Clock.javaClock
    } yield new JWTServiceLive(jwtConfig, clock)
  }

  val configuredLayer =
    Configs.makeLayer[JWTConfig]("rockthejvm.jwt") >>> layer
}

object JWTServiceDemo extends ZIOAppDefault {
  val program = for {
    service   <- ZIO.service[JWTService]
    userToken <- service.createToken(User(1L, "daniel@rockthejvm.com", "unimportant"))
    _         <- Console.printLine(userToken)
    userId    <- service.verifyToken(userToken.token)
    _         <- Console.printLine(userId.toString)
  } yield ()

  override def run: ZIO[Any & (ZIOAppArgs & Scope), Any, Any] =
    program.provide(
      JWTServiceLive.layer,
      Configs.makeLayer[JWTConfig]("rockthejvm.jwt")
    )
}
/* 3 parts (JWT token)
  1) header (what type of token(jwt,..), and type of algo used)
  eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.

  2) payload (contains claims + issuedAt,withSubject ...)
  eyJzdWIiOiIxIiwiaXNzIjoicm9ja3RoZWp2bS5jb20iLCJleHAiOjE3Mjg3NDUzOTYsImlhdCI6MTcyNjE1MzM5NiwidXNlcm5hbWUiOiJkYW5pZWxAcm9ja3RoZWp2bS5jb20ifQ.

  3) signature - uses algo to hash 1) and 2)
  Wi97b1naXMN2ysui-o8mF47bop9Vt_oKPnG0Y6YIIy82UakZhXDkXG8fSwE9PQjge9XIq9uH8hl1c5APB_CWog
 */
