package com.rockthejvm.reviewboard.http.endpoints

import sttp.tapir.*
import com.rockthejvm.reviewboard.domain.errors.HttpError

trait BaseEndpoint {
    val baseEndpoint = endpoint
      .prependIn("api")
      .errorOut(statusCode and plainBody[String]) // error gonna return a (StatusCode, String)
      .mapErrorOut[Throwable](HttpError.decode)(HttpError.encode)  // takes 2 param (/*(StatusCode, String) => MyHttpError*/)(/*MyHttpError => (StatusCode, String) */)
    // ***IMP :  ^^^ Throwable here because server returns Either[Throwable, ..] to catch error and do something (Note: Tapir must iron out the Either datatype kei processing garera and map the Left() side of either as error automatically)         

    val secureBaseEndpoint: Endpoint[String, Unit, Throwable, Unit, Any] = 
      baseEndpoint
        .securityIn(auth.bearer[String]()) // header "Authorization: Bearer ...(JWT token here)"
}
