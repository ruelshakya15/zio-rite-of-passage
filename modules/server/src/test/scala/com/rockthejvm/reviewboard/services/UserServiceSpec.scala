package com.rockthejvm.reviewboard.services

import zio.*
import zio.test.*

import com.rockthejvm.reviewboard.repositories.*
import com.rockthejvm.reviewboard.services.*
import com.rockthejvm.reviewboard.services.CompanyServiceSpec.service
import com.rockthejvm.reviewboard.domain.data.{User, UserID, UserToken}

object UserServiceSpec extends ZIOSpecDefault {

   // OWN
  val baseUrl = "http://localhost:4041"

  val daniel = User(
    1L,
    "daniel@rockthejvm.com",
    "1000:A5BA81F5750E120CE22F244C870B6C616DF2F7DA90D17F13:66A65DF77BF719324FBCBEF76B2F829B571137763B2838E3" // from UserService hashPassDemo for("rockthejvm") password
  )

  val stubRepoLayer = ZLayer.succeed {
    new UserRepository {
      val db = collection.mutable.Map[Long, User](1L -> daniel)
      def create(user: User): Task[User] = ZIO.succeed {
        db += (user.id -> user)
        user
      }

      def getById(id: Long): Task[Option[User]] =
        ZIO.succeed(db.get(id))

      def getByEmail(email: String): Task[Option[User]] =
        ZIO.succeed(db.values.find(_.email == email))

      def update(id: Long, op: User => User): Task[User] =
        ZIO.attempt { // using attempt as this can crash
          val newUser = op(db(id))
          db += (newUser.id -> newUser)
          newUser
        }

      def delete(id: Long): Task[User] = ZIO.attempt {
        val user = db(id)
        db -= id
        user
      }
    }
  }

  val stubJWTLayer = ZLayer.succeed {
    new JWTService {
      override def createToken(user: User): Task[UserToken] =
        ZIO.succeed(UserToken(user.id, user.email, "BIG ACCESS", Long.MaxValue))
      override def verifyToken(token: String): Task[UserID] =
        ZIO.succeed(UserID(daniel.id, daniel.email))
    }
  }

  val stubEmailsLayer = ZLayer.succeed { // stubEmail & stubTokenRepo not used
    new EmailService(baseUrl) { // OWN
      override def sendEmail(to: String, subject: String, content: String): Task[Unit] = ???
      override def sendPasswordRecoveryEmail(to: String, token: String): Task[Unit]    = ???
    }
  }

  val stubTokenRepoLayer = ZLayer.succeed {
    new RecoveryTokensRepository {
      val db = collection.mutable.Map[String, String]()
      override def checkToken(email: String, token: String): Task[Boolean] =
        ZIO.succeed(db.get(email).filter(_ == token).nonEmpty)
      override def getToken(email: String): Task[Option[String]] = ZIO.attempt {
        val token = util.Random.alphanumeric.take(8).mkString.toUpperCase()
        db += (email -> token)
        Some(token)
      }
    }
  }

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("UserServiceSpec")(
      test("create and validate a user") {
        for {
          service <- ZIO.service[UserService]
          user <- service.registerUser(
            daniel.email,
            "rockthejvm"
          ) // the password that generates above Hash stored in daniel
          valid <- service.verifyPassword(daniel.email, "rockthejvm")
        } yield assertTrue(valid && user.email == daniel.email)
      },
      test("validate correct credentials") {
        for {
          service <- ZIO.service[UserService]
          valid   <- service.verifyPassword(daniel.email, "rockthejvm")
        } yield assertTrue(valid)
      },
      test("invalidate incorrect credentials") {
        for {
          service <- ZIO.service[UserService]
          valid   <- service.verifyPassword(daniel.email, "somethingelse") // wrong password
        } yield assertTrue(!valid)
      },
      test("invalidate non-existent user") {
        for {
          service <- ZIO.service[UserService]
          valid   <- service.verifyPassword("someon@gmail.com", "somethingelse") // wrong email
        } yield assertTrue(!valid)
      },
      test("update password") {
        for {
          service  <- ZIO.service[UserService]
          valid    <- service.updatePassword(daniel.email, "rockthejvm", "scalarulez")
          oldValid <- service.verifyPassword(daniel.email, "rockthejvm")
          newValid <- service.verifyPassword(daniel.email, "scalarulez")
        } yield assertTrue(newValid && !oldValid)
      },
      test("delete non-existent user should fail") {
        for {
          service <- ZIO.service[UserService]
          err <- service
            .deleteUser("someone@gmail.com", "something")
            .flip // shift error to value channel
        } yield assertTrue(err.isInstanceOf[RuntimeException])
      },
      test("delete with incorrect credentials fail") {
        for {
          service <- ZIO.service[UserService]
          err <- service.deleteUser(daniel.email, "something").flip // shift error to value channel
        } yield assertTrue(err.isInstanceOf[RuntimeException])
      },
      test("delete user") {
        for {
          service <- ZIO.service[UserService]
          user    <- service.deleteUser(daniel.email, "rockthejvm")
        } yield assertTrue(user.email == daniel.email)
      }
    ).provide(
      UserServiceLive.layer,
      stubJWTLayer,
      stubRepoLayer,
      stubEmailsLayer,
      stubTokenRepoLayer // Email & TokenRepo layer test not written
    )

}
