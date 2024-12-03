package com.rockthejvm.reviewboard.domain.data

import zio.json.JsonCodec
import zio.json.DeriveJsonCodec

final case class Company(
    id: Long,
    slug: String,
    name: String, // "My Company Inc" -> companies.rockthejvm.com/company/my-company-inc   (Route can be derived from name)
    url: String,
    location: Option[String] = None,
    country: Option[String] = None,
    industry: Option[String] = None,
    image: Option[String] = None,
    tags: List[String] = List()
)

object Company {
    given codec: JsonCodec[Company] = DeriveJsonCodec.gen[Company]

    def makeSlug(name: String): String =
      name
        .replaceAll(" +"," ") // replaces multiple spaces with a single space.
        .split(" ")
        .map(_.toLowerCase())
        .mkString("-") // "My Company  Inc" -> "my-company-inc"
}
