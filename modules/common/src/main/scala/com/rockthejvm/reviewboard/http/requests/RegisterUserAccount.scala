package com.rockthejvm.reviewboard.http.requests

import zio.json.JsonCodec

final case class RegisterUserAccount(
    email: String,
    password: String
) derives JsonCodec // syntax sugar for JSON serialization we have done before
