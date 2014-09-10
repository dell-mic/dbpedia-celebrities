package models

import play.api.db.slick.Config.driver.simple._

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._


case class Celebrity(
  source: String,
  label: Option[String],
  givenName: Option[String],
  lastName: Option[String],
  birthDate: Option[String],
  birthPlaceLabel: Option[String],
  abstractText: Option[String],
  thumbnail: Option[String],
  children: Option[String],
  height: Option[String],
  residence: Option[String]) {
  
}


  

/* Table mapping
 */
class CelebritysTable(tag: Tag) extends Table[Celebrity](tag, "CELEBRITY") {

//  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def source = column[String]("source", O.PrimaryKey)
  def label = column[String]("label", O.Nullable)
  def givenName = column[String]("givenName", O.Nullable)
  def lastName = column[String]("lastName", O.Nullable)
  def birthDate = column[String]("birthDate", O.Nullable)
  def birthPlaceLabel = column[String]("birthPlaceLabel", O.Nullable)
  def abstractText = column[String]("abstractText", O.Nullable, O.DBType("text"))
  def thumbnail = column[String]("thumbnail", O.Nullable)
  def children = column[String]("children", O.Nullable)
  def height = column[String]("height", O.Nullable)
  def residence = column[String]("residence", O.Nullable)

  def * = (source, label.?, givenName.?, lastName.?, birthDate.?, birthPlaceLabel.?, abstractText.?, thumbnail.?, children.?, height.?, residence.?) <> (Celebrity.tupled, Celebrity.unapply _)

}

