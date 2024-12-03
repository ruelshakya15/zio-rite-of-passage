package com.rockthejvm.reviewboard.http.endpoints

import sttp.tapir.*
import sttp.tapir.json.zio.*
import sttp.tapir.generic.auto.* // imports the type class derivation package  (implicit conversion for json milaucha)

import com.rockthejvm.reviewboard.http.requests.*
import com.rockthejvm.reviewboard.domain.data.*

trait CompanyEndpoints extends BaseEndpoint {
  val createEndpoint =
    secureBaseEndpoint
      .tag("companies")
      .name("create")
      .description("create a listing for a company")
      .in("companies")
      .post
      .in(jsonBody[CreateCompanyRequest])
      .out(jsonBody[Company])

  val getAllEndpoint =
    baseEndpoint
      .tag("companies")
      .name("getAll")
      .description("get all company listings")
      .in("companies")
      .get
      .out(jsonBody[List[Company]])

  val getByIdEndpoint =
    baseEndpoint
      .tag("companies")
      .name("getById")
      .description("get company by its id (or maybe by slug?)") // TODO
      .in("companies" / path[String]("id"))
      .get
      .out(jsonBody[Option[Company]])

      // getById and allFilter endpoint have similar .in req so need to plave allFilter first in route so we match it first
  val allFiltersEndpoint =
    baseEndpoint
      .tag("Companies")
      .name("allFilters")
      .description("Get all possible search filters")
      .in("companies" / "filters")
      .get
      .out(jsonBody[CompanyFilter])
  
  val searchEndpoint =
    baseEndpoint
      .tag("Companies")
      .name("search")
      .description("Get companies based on filters")
      .in("companies" / "search")
      .post
      .in(jsonBody[CompanyFilter])
      .out(jsonBody[List[Company]])
}
