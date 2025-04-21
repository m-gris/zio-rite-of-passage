package com.rockthejvm.reviewboard.repositories

import zio.*
import io.getquill.*
import io.getquill.jdbczio.Quill

import com.rockthejvm.reviewboard.domain.data.*


trait CompanyRepository {
  def create(company: Company): Task[Company]
  def update(id: Long, op: Company => Company): Task[Company]
  def delete(id: Long): Task[Company]
  def getById(id: Long): Task[Option[Company]]
  def getBySlug(slug: String): Task[Option[Company]]
  def getAll: Task[List[Company]]
  def uniqueAttributes: Task[CompanyFilter]
}


class CompanyRepositoryLive private (quill: Quill.Postgres[SnakeCase]) extends CompanyRepository {

  // import all function from the QUILL INSTANCE !
  import quill.*


  // NB: INLINE GIVEN
  // => allows QUILL MACROS to generate TYPE SAFE QUERIES
  // @@@@@@@@@@ LIVE AT COMPILE TIME @@@@@@@@@@
  // will also respect the column / field ORDERS and prevent accidently 'swapping' values of the same type ...
  inline given schema: SchemaMeta[Company] = schemaMeta[Company]("companies" /* the table name*/)
  inline given upMeta: UpdateMeta[Company] = updateMeta[Company](_.id /*col omitted from the update statement */)
  inline given insMeta: InsertMeta[Company] = insertMeta[Company](_.id /*col omitted from the insert statement*/)

  override def create(company: Company): Task[Company] =
    run {
      query[Company]
        .insertValue(
          /*TRANSLATE a data structure
           *    - from scala
           *    - to the DB Table via macros*/
          lift(company)
        )
        .returning(result => result) // i.e the Company ...

    }

  override def getById(id: Long): Task[Option[Company]] =
    run {
      query[Company]
        .filter(
         // NB: again... we must translate the JVM id to a Postgres id
         _.id == lift(id)
        ) // List[Company]
    }.map(_.headOption) // Option[Company]

  override def getBySlug(slug: String): Task[Option[Company]] =
    run {
      query[Company]
        .filter(_.slug == lift(slug))
    }.map(_.headOption)

  override def getAll: Task[List[Company]] = run(query[Company]) // SELECT * FROM companies

  override def update(id: Long, op: Company => Company): Task[Company] = for {
    current <- getById(id)
                    // would be Task[Option[Company]]
                    // but here we FORCE it to be company
                    .someOrFail(new RuntimeException(s"could not update missing ID $id"))
    updated <- run {
                query[Company]
                  .filter(_.id == lift(id))
                  .updateValue(lift(op(current)))
                  .returning(x => x)
                }
    } yield updated

  override def delete(id: Long): Task[Company] =
    run {
      query[Company]
        .filter(_.id == lift(id))
        .delete
        .returning(x => x)
    }

  override def uniqueAttributes: Task[CompanyFilter] =
    for {
      locations   <- run(query[Company].map(_.location).distinct)
      countries   <- run(query[Company].map(_.country).distinct)
      industries  <- run(query[Company].map(_.industry).distinct)
      // tags        <- run(query[Company].map(_.tags).map(_.flatten.toSet.toList))
    } yield CompanyFilter(
              locations=locations.flatten,
              countries=countries.flatten,
              industries=industries.flatten,
              List() // tags=tags
            )

}

object CompanyRepositoryLive {

  val layer = ZLayer { for {
    quill <- ZIO.service[Quill.Postgres[SnakeCase]]
  } yield CompanyRepositoryLive(quill)
  }
}

object CompanyRepositoryDemo extends ZIOAppDefault {

  val program = for {
    repo <- ZIO.service[CompanyRepository] // the type/trait ... not the 'concrete' versions
    _    <- repo.create(Company(-1L, "Rock the JVM", "rock-the-jvm", "rockthejvm.com"))

  } yield ()
  override def run = program.provide(
    CompanyRepositoryLive.layer,
    Quill.Postgres.fromNamingStrategy(SnakeCase), // the quill instance for the layer above...
    Quill.DataSource.fromPrefix("rockthejvm.db")  // the datasource for the quill instance above...
    )
}
