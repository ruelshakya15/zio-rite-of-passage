package com.rockthejvm

import zio.json.{DeriveJsonCodec, JsonCodec}

case class Job(
                id: Long,
                title: String,
                url: String,
                company: String
              )
object Job { // import both from ZIO  (ZIO based json serialization)
  given codec: JsonCodec[Job] = DeriveJsonCodec.gen[Job] // macro-based JSON codec (generated)
}

// special request for the HTTP endpoint
case class CreateJobRequest(
                             title: String,
                             url: String,
                             company: String
                           )
object CreateJobRequest {
  given codec: JsonCodec[CreateJobRequest] = DeriveJsonCodec.gen[CreateJobRequest]
}
