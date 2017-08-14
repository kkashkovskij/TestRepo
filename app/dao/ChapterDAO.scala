package dao

import javax.inject.Inject

import scala.concurrent.{Await, ExecutionContext, Future}
import models.Chapter
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import play.api.db.slick.HasDatabaseConfigProvider

class ChapterDAO @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._
  private val Chapters = TableQuery[ChaptersTable]

  def all(): Future[Seq[Chapter]] = db.run(Chapters.result)

  def insert(chapter: Chapter): Future[Unit] = {
    db.run(Chapters += chapter).map { _ => () }
  }

  def delete(id: Int): Unit ={ db.run(Chapters.filter(_.id === id).delete) }

  def edit(chapter: Chapter): Unit = {
    db.run(Chapters.filter(_.id === chapter.id).update(chapter))
  }

  private class ChaptersTable(tag: Tag) extends Table[Chapter] (tag, "chapters"){

    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def shortName = column[String]("shortname")
    def fullName = column[String]("fullname")
    def text = column[String]("text")
    def parentId = column[Option[Int]]("parentid")

    def * = (id, shortName, fullName, text, parentId) <> (Chapter.tupled, Chapter.unapply)
  }
}
