package models

import play.api.db.slick.Config.driver.simple._

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._


case class Celebrity(id: Option[Long] = None,
  source: String,
  name: String,
  givenName: String,
  lastName: String,
  birthDate: String,
  birthPlaceLabel: String,
  abstractText: String,
  thumbnail: String,
  children: Option[String],
  height: Option[String],
  residence: Option[String]) {
  
}


  

/* Table mapping
 */
class CelebritysTable(tag: Tag) extends Table[Celebrity](tag, "CELEBRITY") {

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def source = column[String]("source", O.Nullable)
  def name = column[String]("name", O.Nullable)
  def givenName = column[String]("givenName", O.Nullable)
  def lastName = column[String]("lastName", O.Nullable)
  def birthDate = column[String]("birthDate", O.Nullable)
  def birthPlaceLabel = column[String]("birthPlaceLabel", O.Nullable)
  def abstractText = column[String]("abstractText", O.Nullable)
  def thumbnail = column[String]("thumbnail", O.Nullable)
  def children = column[String]("children", O.Nullable)
  def height = column[String]("height", O.Nullable)
  def residence = column[String]("residence", O.Nullable)

  def * = (id.?, source, name, givenName, lastName, birthDate, birthPlaceLabel, abstractText, thumbnail, children.?, height.?, residence.?) <> (Celebrity.tupled, Celebrity.unapply _)

}

