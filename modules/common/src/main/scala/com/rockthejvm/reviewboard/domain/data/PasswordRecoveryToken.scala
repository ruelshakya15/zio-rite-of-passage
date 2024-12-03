package com.rockthejvm.reviewboard.domain.data

import zio.json.JsonCodec


final case class PasswordRecoveryToken(
    email: String,
    token: String,
    expiration: Long
) derives JsonCodec
