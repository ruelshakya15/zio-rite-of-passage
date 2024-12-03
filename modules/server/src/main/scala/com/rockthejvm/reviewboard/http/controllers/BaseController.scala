package com.rockthejvm.reviewboard.http.controllers

import zio.*
import sttp.tapir.server.ServerEndpoint

trait BaseController {
  val routes: List[ServerEndpoint[Any, Task]] // because our endpoints can fail
}
