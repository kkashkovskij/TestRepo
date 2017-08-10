package models

import controllers.TreeNode

import scala.collection.mutable
trait Entity{
  def getFullName: String
  def getText: String
  def getChildrenList: mutable.MutableList[String]
  def getId: Int
  def getType: String
}

case class Article (id: Int, shortName: String, fullName: String, text: String, chapterId: Int) extends Entity{
  override def toString: String = shortName
  def getFullName: String = fullName
  def getText: String = text
  def getChildrenList: mutable.MutableList[String] = null
  def getId: Int = id
  def getType: String = "article"
}

case class Chapter (id :Int, shortName: String, fullName: String, text: String, parentId: Option[Int]) extends Entity{
  override def toString: String = shortName
  def getFullName: String = fullName
  def getText: String = text
  def getChildrenList: mutable.MutableList[String] = null
  def getId: Int = id
  def getType: String = "chapter"
}

