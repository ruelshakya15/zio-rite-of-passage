package com.rockthejvm.reviewboard.config

final case class JWTConfig(
    secret: String,
    ttl: Long
) // since appplication.conf ma jwt{..} ma same structure with (secret, ttl) cha this will be populated accordingly
