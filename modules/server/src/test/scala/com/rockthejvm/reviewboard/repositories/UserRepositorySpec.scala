package com.rockthejvm.reviewboard.repositories

import zio.*
import zio.test.*

import com.rockthejvm.reviewboard.domain.data.User
import com.rockthejvm.reviewboard.syntax.*
import com.rockthejvm.reviewboard.repositories.*

object UserRepositorySpec extends ZIOSpecDefault with RepositorySpec {

  val aUser = User(
    id = 1L,
    email = "rockthejvm.com",
    hashedPassword = "password"
  )

  override val initScript: String = "sql/users.sql"
  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("UserRepositorySpec")(
      test("create user") {
        val program = for {
          repo <- ZIO.service[UserRepository]
          user <- repo.create(aUser)
        } yield user

        program.assert { user =>
          user.id == aUser.id &&
          user.email == aUser.email &&
          user.hashedPassword == aUser.hashedPassword
        }
      },
      test("get user by ids(id, email)") {
        for {
          repo         <- ZIO.service[UserRepository]
          user         <- repo.create(aUser)
          fetchedUser  <- repo.getById(user.id)
          fetchedUser2 <- repo.getByEmail(user.email)
        } yield assertTrue(
          fetchedUser.contains(user) &&
            fetchedUser2.contains(user)
        )
      },
      test("edit user details") {
        for {
          repo        <- ZIO.service[UserRepository]
          user        <- repo.create(aUser)
          fetchedUser <- repo.update(user.id, _.copy(email = "changedEmail.com"))
        } yield assertTrue(
          fetchedUser.id == user.id &&
            fetchedUser.email == "changedEmail.com" &&
            fetchedUser.hashedPassword == user.hashedPassword
        )
      },
      test("delete user") {
        for {
          repo      <- ZIO.service[UserRepository]
          user      <- repo.create(aUser)
          _         <- repo.delete(user.id)
          maybeUser <- repo.getById(user.id)
        } yield assertTrue(
          maybeUser.isEmpty
        )
      }
    ).provide(
      UserRepositoryLive.layer,
      dataSourceLayer,
      Repository.quillLayer,
      Scope.default
    )
}
