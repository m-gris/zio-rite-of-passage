package com.rockthejvm.reviewboard.http.controllers

import zio.*
import sttp.tapir.server.ServerEndpoint

trait BaseController {
  val routes: List[ServerEndpoint[Any, Task]] // 2 Type Parameters: [-R , F[_] ] --- the Requirements & the Effect
                                  // Any  -R   => No Reqs
                                  // Task F[_] => A Faillible ZIO

}
