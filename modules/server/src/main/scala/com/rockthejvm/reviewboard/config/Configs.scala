package com.rockthejvm.reviewboard.config

import zio.*
import zio.config.*
import zio.config.magnolia.* // magnolia is old library for type class derivations before scala 3
import zio.config.typesafe.TypesafeConfig

import com.typesafe.config.ConfigFactory

object Configs {

  def makeLayer[C](path: String)(using
      desc: Descriptor[C],
      tag: Tag[C] /*this will be created auto by zio*/
  ) /*magnolia will generate this implicit auto*/: ZLayer[Any, Throwable, C] =
    TypesafeConfig.fromTypesafeConfig(
      ZIO.attempt(ConfigFactory.load().getConfig(path) /*from typesafe not zio*/ ),
      descriptor[C] // from magnolia and will auto generate ConfigDescriptor[]
    )

}
