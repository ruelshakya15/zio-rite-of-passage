package com.rockthejvm.reviewboard.http.controllers

import com.rockthejvm.reviewboard.domain.data.Company
import com.rockthejvm.reviewboard.http.endpoints.CompanyEndpoints

import zio.*
import scala.collection.mutable
import sttp.tapir.Endpoint
import sttp.tapir.server.ServerEndpoint
import com.rockthejvm.reviewboard.services.CompanyService
import com.rockthejvm.reviewboard.services.JWTService
import com.rockthejvm.reviewboard.services.JWTServiceLive
import com.rockthejvm.reviewboard.domain.data.UserID

class CompanyController private (service: CompanyService, jwtService: JWTService)
    extends BaseController
    with CompanyEndpoints {

  // in-memory "database"
  val db = mutable.Map[Long, Company]()

  // create   type: ServerEndpoint[Any, Task] defined here or else tapir will create it as an Endpoint[....] with 7,8 args
  val create: ServerEndpoint[Any, Task] = createEndpoint
    .serverSecurityLogic[UserID, Task](token => jwtService.verifyToken(token).either)
    .serverLogic { _ => req => service.create(req).either }

  // getAll
  val getAll: ServerEndpoint[Any, Task] =
    getAllEndpoint.serverLogic { _ =>
      service.getAll.either
    }

  // getById  -> should work with both (http post /companies/1  &  /companies/rock-the-jvm)
  val getById: ServerEndpoint[Any, Task] =
    getByIdEndpoint.serverLogic { id =>
      ZIO
        .attempt(id.toLong)
        .flatMap(service.getById)
        .catchSome { case _: NumberFormatException =>
          service.getBySlug(id)
        }
        .either
    }

  val allFilters: ServerEndpoint[Any, Task] =
    allFiltersEndpoint.serverLogic { _ =>
      service.allFilters.either

    }

  val search: ServerEndpoint[Any, Task] =
    searchEndpoint.serverLogic { filter =>
      service.search(filter).either
    }
  override val routes: List[ServerEndpoint[Any, Task]] = List(
    create,
    getAll,
    allFilters,
    search,
    getById
  ) // allFilter, search ahead of getbyId because its has similar route so it matches first
}

object CompanyController {
  val makeZIO = for {
    service <- ZIO.service[
      CompanyService
    ] // Service dependency passed here so need to create object with ZLayer in CompanyService
    jwtService <- ZIO.service[JWTService]
  } yield new CompanyController(service, jwtService)
}
