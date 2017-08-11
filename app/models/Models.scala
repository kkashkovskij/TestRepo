package models

import scala.collection.mutable
trait TextItem{
  def getId: Int
  def getShortName: String
  def getFullName: String
  def getText: String
  def getChildrenList: mutable.MutableList[String]
  def getType: String

}

case class Article (id: Int, shortName: String, fullName: String, text: String, chapterId: Int) extends TextItem{
  override def toString: String = shortName
  def getId: Int = id
  def getShortName: String = shortName
  def getFullName: String = fullName
  def getText: String = text
  def getChildrenList: mutable.MutableList[String] = null
  def getType: String = "article"
}

case class Chapter (id: Int, shortName: String, fullName: String, text: String, parentId: Option[Int]) extends TextItem{
  override def toString: String = shortName
  def getId: Int = id
  def getShortName: String = shortName
  def getFullName: String = fullName
  def getText: String = text
  def getChildrenList: mutable.MutableList[String] = null
  def getType: String = "chapter"



}

