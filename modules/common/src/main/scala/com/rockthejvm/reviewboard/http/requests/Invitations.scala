package com.rockthejvm.reviewboard.http.requests

import zio.json.JsonCodec

final case class Invitations(companyId: Long, companyName: String, nInvites: Int) derives JsonCodec
