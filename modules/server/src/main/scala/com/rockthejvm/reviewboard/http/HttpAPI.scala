package com.rockthejvm.reviewboard.http

import sttp.tapir.*
import com.rockthejvm.reviewboard.http.controllers.*

object HttpAPI {
  // PURPOSE
  // to make the server program AGNOSTIC with regard to the different controllers
  // by COMBINING all the endpoints into a SINGLE COMPREHENSIVE LIST

  def gatherRoutes(controllers: List[BaseController]) =
    controllers.flatMap(_.routes) // routes being lists, we must flatten those into a single list...

  def makeControllers = for {
    health    <- HealthController.makeZIO
    companies <- CompanyController.makeZIO
    reviews   <- ReviewController.makeZIO
    users     <- UserController.makeZIO
    // TO DO - add more controllers
    } yield  List(health, companies, reviews, users)

  val endpointsZIO = makeControllers.map(gatherRoutes)
}
