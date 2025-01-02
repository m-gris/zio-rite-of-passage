package com.rockthejvm

import zio.*
import io.getquill.*
import io.getquill.jdbczio.Quill

object QuillRecap extends ZIOAppDefault {

  // QUILL is a lib. that will allow interactions with a Database

  /* side note: here the term 'REPOSITORY'
   * refers to the "REPOSITORY PATTERN"
   * an abstraction layer between business logic and data persistence
   * that provides a collection-like interface for accessing domain objects
   * while hiding the complexities of database operations, mapping, and query construction.
   *
   * REPOSITORY => a higher-level INTERFACE for data STORAGE OPERATIONS.
   *
   */
  val program: ZIO[
                JobRepository, // we will need a ZLayer to create / provide this JobRepository
                Throwable,
                Unit
                ] =
    for
      jobRepo <- ZIO.service[JobRepository]
      /*
      * ZIO.service is a method used to access services from the ZIO environment,
      * enabling dependency injection by fetching the service type specified by the user,
      * which must be part of the environment's type.
      *
      * This method simplifies the process of obtaining
      * services that are required for executing ZIO effects,
      * promoting a clean and modular architecture by separating
      *    - service definitions
      *    - from their implementations
      */
      _ <- jobRepo.create(Job(-1, "SWE", "rockthejvm.com", "Rock the JVM"))
      _ <- jobRepo.create(Job(-1, "Instructor", "rockthejvm.com", "Rock the JVM"))
    yield ()

  def run = program.provide(
    JobRepositoryLive.layer,
    Quill.Postgres.fromNamingStrategy(SnakeCase), // the quill instance for the layer above...
    Quill.DataSource.fromPrefix("mydbconf") // the datasource for the quill instance above...
    // will read the config section in `application.conf`
    // & spin-up a data source, ie. essentially a CONNECTION POOL
    )
}


// 'repository' => what Quill uses to interact with a single table
trait JobRepository {
  // will 'declare' all the methods that we'd like to use
  def create(job: Job): Task[Job]
  def update(id: Long, job: Job => Job): Task[Job]
  def delete(id: Long): Task[Job]
  def getById(id: Long): Task[Option[Job]]
  def get: Task[List[Job]]
}

class JobRepositoryLive(
  // USE A CONSTRUCTOR TO REQUIRE A DEPENDENCY
  quill: Quill.Postgres[SnakeCase]) extends JobRepository:
//
  // STEP 1 - import quill methods
  import quill.* // e.g how to RUN A QUERY etc...

  // STEP 2 - 'given' schemas for create, update etc...
  /*
   INLINE GIVENS
   feature that allows to define given instances that are INLINED AT THE POINT OF USE.
   i.e => the compiler will replace the usage of these givens with their actual implementation,
   rather than referring to them by reference.
   purpose: performance improvements
    how ? : by eliminating the overhead of a method call
   */
  inline given schema: SchemaMeta[Job] = schemaMeta[Job]("jobs") // specify the table name
  inline given insMeta: InsertMeta[Job] = insertMeta[Job](exclude= t => t.id) // colums to be 'excluded' (i.e postgres will handle the id on its own)
  inline given upMeta: UpdateMeta[Job] = updateMeta[Job](_.id) // colums to be 'excluded' (i.e postgres will handle the id on its own)

  // STEP 3 - the actual ... logic ???
  def create(job: Job): Task[Job] =
    run {
      // a MACRO-BASED method from Quill
      // which expects the Query to be formatted as a DataStructure
      query[Job] // the start of a 'builder like pattern'
        .insertValue(lift(job)) // macro-based magic, turning Job into a proper representation of the jobs table
        .returning(j => j) // returning the job that was created
    }
    /*
     * QUILL MACROS provide the capability to INSPECT AND ANALYZE SQL QUERIES AT COMPILE TIME.
     * allowing developers to VERIFY THE CORRECTNESS AND EFFICIENCY of queries while being written,
     * WITHOUT HAVING TO EXECUTE them against a database.
     * This compile-time analysis helps in catching potential errors early in the development process,
     * such as syntax mistakes or type mismatches in queries.

      i.e QUILL MACROS allow to inspect query at compile time, while we are writing the code,
      without having to run the application or connect to a database.
      This leads to safer and more reliable database interactions,
      as errors can be detected and resolved early in the development cycle.

      Quill achieves this by:
        - translating database queries written in Scala
        - into plain SQL at compile time,
      and any issues in the query can be flagged immediately.

     */

  def update(id: Long, op: Job => Job): Task[Job] = for {

      // A FOR-COMPREHENSION is used here instead of a simple code block
      // to LEVERAGE THE MONADIC PROPERTIES OF ZIO'S TASK.
      // While a simple code block can execute steps in sequence,
      // a for-comprehension in ZIO provides additional benefits:
      //
      //    AUTOMATIC ERROR HANDLING:
      //       Each step in the for-comp can fail with an error,
      //       automatically handled by ZIO
      //       by propagating those through the monad without executing subsequent steps.
      //       This avoids clutter of explicit error checking after each operation.
      //
      //    INTEGRATION WITH ZIO'S ENVIRONMENT AND RESOURCE MANAGEMENT:
      //       ZIO Tasks can require context from ZIO's environment,
      //       and using for-comprehensions helps in seamlessly injecting those dependencies.

      // Step 1: Fetch the Job by its ID, fail with an exception if not found.
      getCurrent <- getById(id).someOrFail(new RuntimeException(s"Could not update: missing key $id"))

      // Step 2: Apply the operation `op` to the fetched Job and update it in the database.
      updated <- run {
        query[Job]
          .filter(_.id == lift(id)) // Convert Scala filter into a SQL WHERE clause.
          .updateValue(lift(op(getCurrent))) // Apply the operation and lift the result to the query context.
          .returning(j => j) // Return the updated job.
      }

    } yield updated

  def delete(id: Long): Task[Job] =
    run {
      query[Job]
        .filter(_.id == lift(id) )
        .delete
        .returning(j => j) // return the job that has been deleted
    }

  def getById(id: Long): Task[Option[Job]] = 
    run {
      query[Job]
        .filter(_.id == lift(id)) // SELECT * FROM jobs WHERE id == id
    }.map(_.headOption) // LIMIT 1 -- at most 1 -- Hence an Option...

  def get: Task[List[Job]] =
    run(query[Job]) // SELECT * FROM jobs


object JobRepositoryLive:
  val layer = ZLayer {
    // this layer require a Quill Service & returns a JobRepositoryLive if successful
    ZIO.service[Quill.Postgres[SnakeCase]].map(quill => JobRepositoryLive(quill))
  }
