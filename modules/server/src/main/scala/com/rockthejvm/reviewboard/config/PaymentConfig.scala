package com.rockthejvm.reviewboard.config

final case class PaymentConfig(
    apiKey: String,
    secret: String,
    price: String, // in the case of stripe: the product ID ?????
    successUrl: String,
    cancelUrl: String
)
