package com.rockthejvm.reviewboard.repositories

import zio.*
import io.getquill.*
import io.getquill.jdbczio.Quill

import com.rockthejvm.reviewboard.domain.data.User

trait UserRepository {
  def create(user: User): Task[User]
  def getById(id: Long): Task[Option[User]]
  def getByEmail(email: String): Task[Option[User]]
  def update(id: Long, op: User => User): Task[User]
  def delete(id: Long): Task[User]
}


class UserRepositoryLive private (quill: Quill.Postgres[SnakeCase]) extends UserRepository {

  import quill.*

  // the "mapping rules"
  inline given userSchema: SchemaMeta[User]   = schemaMeta[User]("users")
  // the "insertion rules"
  inline given userInsertSchema: InsertMeta[User] =
    insertMeta[User](/*excluding*/_.id)
  // the "update rules"
  inline given userUpdateSchema: UpdateMeta[User] =
    updateMeta[User](/*excluding*/_.id)

  def create(user: User): Task[User] =
    run(query[User].insertValue(lift(user)).returning(r => r))

  def getById(id: Long): Task[Option[User]] =
    run(query[User].filter(_.id == lift(id))).map(_.headOption)

  def getByEmail(email: String): Task[Option[User]] =
    run(query[User].filter(_.email == lift(email))).map(_.headOption)

  def update(id: Long, op: User => User): Task[User] = for {
    current <- getById(id)
                    // would be Task[Option[Company]]
                    // but here we FORCE it to be Company
                    .someOrFail(new RuntimeException(s"could not update missing ID $id"))
    updated <- run(
                query[User]
                  .filter(_.id == lift(id))
                  .updateValue(lift(op(current)))
                  .returning(r => r)
                )
  } yield updated

  def delete(id: Long): Task[User] =
    run(
      query[User]
        .filter(_.id == lift(id))
        .delete
        .returning(r => r)
      )
}



object UserRepositoryLive {
  val layer = ZLayer { for {
    quill <- ZIO.service[Quill.Postgres[SnakeCase]]
  } yield UserRepositoryLive(quill)
  }
}
