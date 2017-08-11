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

  // TODO нужно возворащать вставленный артикль
  def insert(chapter: Chapter): Future[Unit] = db.run(Chapters += chapter).map { _ => () }

  def delete(id: Int): Unit ={ db.run(Chapters.filter(_.id === id).delete) }

  //TODO метод должен получать объект Article уже измененный и апдейтить его вбазе целиком. Такой метод лучше назвать update
  def modify(id: Int, shortName: String, fullName: String, text: String): Unit = {

    if (shortName != "") db.run((for { a <- Chapters if a.id === id } yield a.shortName).update(shortName))
    if (fullName != "")db.run((for { a <- Chapters if a.id === id } yield a.fullName).update(fullName))
    if (text != "")db.run((for { a <- Chapters if a.id === id } yield a.text).update(text))

  }

//  def getByParentId(parentId: Int): Seq[Chapter] = {
//    Await.result(db.run(Chapters.filter(_.parentId === parentId).result).map{a => a}, 1.second)
//  }

  private class ChaptersTable(tag: Tag) extends Table[Chapter] (tag, "chapters"){

    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def shortName = column[String]("shortname")
    def fullName = column[String]("fullname")
    def text = column[String]("text")
    def parentId = column[Option[Int]]("parentid")

    def * = (id, shortName, fullName, text, parentId) <> (Chapter.tupled, Chapter.unapply)
  }
}
