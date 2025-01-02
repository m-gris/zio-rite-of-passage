package com.rockthejvm

import scala.collection.mutable

import zio.*
import sttp.tapir.*
import zio.http.Server
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import sttp.tapir.server.ziohttp.ZioHttpServerOptions
import zio.json.JsonCodec
import zio.json.DeriveJsonCodec
import sttp.tapir.json.zio.jsonBody
import org.checkerframework.checker.units.qual.g
import cats.instances.byte
import scala.annotation.meta.getter
import scala.runtime.stdLibPatches.language.experimental.into
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.generic.auto.* // contains JSON codec generators (type class derivation functionality)

object TapirRecap extends  ZIOAppDefault {
  //
  // backend agnostic
  // exposes HTTP Endpoints & Services

  /*

   Tapir creates a clear separation between:
      - HTTP endpoints DEFINITION
      - their actual IMPLEMENTATION as a SERVICE.

   1st define endpoints as TYPE-SAFE SPECIFICATIONS:
    - inputs
    - outputs
    - error

   then define the "HTTP Service" IMPLEMENTATION:
      - provide the actual BUSINESS LOGIC that HANDLES REQUESTS made to those endpoint definitions.
        i.e connecting endpoint specs to real functions that process the data and return responses.

   */

  // We can
  // DESCRIBE AN ENDPOINT
  // &
  // SERVE IT AS AN HTTP SERVER

  val simplestEndpoint = endpoint // uses a 'builder' pattern
          .tag("simple")
          .name("simple")
          .description("simplest endpoint possible")
          // ^^ this is for documentation (swager etc...)
          .get // the http method
          .in("simple") // the path / url
          .out(plainBody[String]) // simple string output... nothing to parse or serialize
          .serverLogicSuccess[Task](_ /* the request */ => ZIO.succeed("All good"))

  val httpApp = ZioHttpInterpreter
                      (ZioHttpServerOptions.default) // can add configs e.g. CORS for security etc...
                      .toHttp(simplestEndpoint) // the route that this server is going to serve


  val simpleServerProgram = Server.serve(httpApp)

  /* side note
   * CORS (Cross-Origin Resource Sharing) 
   * a security mechanism that lets web servers specify which origins:
   *    - domains
   *    - schemes
   *    - ports 
   * are allowed to access their resources
   * preventing unauthorized cross-origin requests while enabling legitimate ones.
   */

  // SIMULATING A JOB BOARD
  val db: mutable.Map[Long, Job] = mutable.Map(
      1L -> Job(1L, "Instructor", "rocktheivm.com" , "Rock the JVM")
    )

  // create
  val createEndpoint: ServerEndpoint[
                            Any, // no requirements
                            Task // the 'effect type' -- remember: Task is a ZIO type alias for ZIO[Any, Throwable, A]
                            ] = endpoint
        .tags("jobs" :: Nil)
        .name("create")
        .description("create a job")
        .in("jobs")
        .in(jsonBody[CreateJobRequest]) // PAYLOAD TYPE -- defined in Job.scala but direct access because same package
        .post
        .out(jsonBody[Job])
        .serverLogicSuccess( request => ZIO.succeed {
            // insert a new job in the "db"
            val newId = db.keys.max + 1
            val newJob = new Job(newId, request.title, request.url, request.company)
            db += (newId -> newJob)
            newJob
            })


  // get by id
  val getByIDEndpoint: ServerEndpoint[Any, Task] = 
      endpoint
        .tags("jobs" :: Nil)
        .name("getByID")
        .description("Get a job from its ID")
        .in("jobs" / path[Long]("id")) // as a PATH ARGUMENT (instead of a payload like in createEndpoint)
        .get
        .out(jsonBody[Option[Job]])
        .serverLogicSuccess( id => ZIO.succeed(db.get(id)))

  // get all
  val getAllEndpoint: ServerEndpoint[Any, Task] = endpoint
        .tags("jobs" :: Nil)
        .name("getAll")
        .description("Get all jobs")
        .in("jobs")
        .get
        .out(jsonBody[List[Job]])
        .serverLogicSuccess(_ => ZIO.succeed(db.values.toList))

  val biggerServerProgram: ZIO[
                            Server, // provided / injected below
                            Nothing,
                            Nothing
                            ] = Server.serve(
    ZioHttpInterpreter(
      ZioHttpServerOptions.default
    ).toHttp(List(createEndpoint, getByIDEndpoint, getAllEndpoint)))

  def run = biggerServerProgram.provide(
    Server.default // without other configs, should start at 0.0.0.0:8080
    ) // we can therefore try it out:
        // - in the browser by going to http://localhost:8080/simple
        // - in the terminal with `http get locahost:8080/simple`


}

