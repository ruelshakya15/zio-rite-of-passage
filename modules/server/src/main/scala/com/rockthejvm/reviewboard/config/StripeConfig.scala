package com.rockthejvm.reviewboard.config

final case class StripeConfig(
    key: String,
    secret: String, // webhook secret
    price: String, // price identifier in stripe
    successUrl: String,
    cancelUrl: String
)
