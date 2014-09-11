package controllers

import play.api._
import play.api.mvc._

import models._

import play.api.libs.ws._
import scala.concurrent.Future
import play.api.Play.current

import play.api.db.slick._
import play.api.db.slick.Config.driver.simple._

import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json._

import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import scala.io.Source
import play.api.Logger

object Application extends Controller {

  //create an instance of the table
  val Celebrities = TableQuery[CelebritysTable]

  lazy val iter = Source.fromFile("/Users/apfelbaum24/Downloads/stars.csv").getLines()

  /**
   * Iterates over CSV and trys to extract
   */
  def readInput(startLabel: String = null) = Action.async {
    Logger.debug("startLabel: " + startLabel)

    if (iter.isEmpty) {
      Logger.debug("End of file reached.")
      Ok("End of file reached")
    }

    val line = startLabel match {
      case s: String => iter.dropWhile(s => !s.contains(startLabel)).next()
      case null => iter.next()
    }
    var label = "init"
    Logger.debug("Read line: " + line)

    try {
      val parts = line.split(",")
      label = parts(1).replaceAll("[^A-Za-z0-9 ]", "")
    } catch {
      case e: IndexOutOfBoundsException => {
        Logger.debug("Line skipped: CSV line parsing error")
        Ok(views.html.messageRedirect(line + "\n\n" + "Line skipped: CSV line parsing error", "/read"))
      }
      case e: Exception => {
        Logger.debug("Line skipped: Unkown error!")
        Ok(views.html.messageRedirect(line + "\n\n" + "Line skipped: Unkown error!", "/read"))
      }
    }
    Logger.debug("Attempting query with label: " + label)
    val future = DBpedia.querySingle(label)
    future.map(s => s match {
      case s: Celebrity => {
        DB.withSession { implicit session =>
          Celebrities.insertOrUpdate(s)
          Logger.debug("SUCCESS: Data saved: " + s.label)
          Ok(views.html.messageRedirect("Data saved for label: " + label, "/read"))
        }
      }
      case null => {
        Logger.debug("No Dbpedia dataset found")
        Ok(views.html.messageRedirect("No Dbpedia dataset found", "/read"))
      }

      //        case _ => Ok("wired")
    })

  }

  def index = DBAction { implicit rs =>
    Ok(views.html.index(Celebrities.list, "Alle Datensätze"))
  }

  /**
   * Extracts a single entry
   */
  def getDBPedia(label: String) = Action.async {
    val future = DBpedia.querySingle(label)
    future.map(s => s match {
      case s: Celebrity => {
        play.api.db.slick.DB.withSession { implicit session =>
          Celebrities.insertOrUpdate(s)
          Ok(views.html.index(List(s), "Geparster Datensatz"))
        }
      }
      case null => Ok(views.html.index(List(), "Error while parsing"))
    })
  }

}

object DBpedia {
  def querySingle(label: String): Future[Celebrity] = {
    val query = """
		PREFIX foaf: <http://xmlns.com/foaf/0.1/>
		PREFIX dbo: <http://dbpedia.org/ontology/>
		PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
		PREFIX dbprop: <http://dbpedia.org/property/> 
		
		
		SELECT DISTINCT ?Label ?Person ?Lastname ?GivenName ?BirthDate ?BirthPlaceLabel ?Abstract ?Thumbnail ?Children ?Height ?Residence WHERE {
		?Person a foaf:Person.
		?Person rdfs:label "%s"@en.
    ?Person rdfs:label ?Label.
		
    	OPTIONAL {
    	?Person foaf:surname ?Lastname.
    	?Person foaf:givenName ?GivenName.
    	}
      
    	OPTIONAL {
			?Person dbpedia-owl:abstract ?Abstract.
			?Person dbo:birthDate ?BirthDate.
    	?Person dbpedia-owl:thumbnail ?Thumbnail.
      FILTER (LANG(?Abstract) = 'de') .
    	}
      
    	OPTIONAL {
			?Person dbo:birthPlace ?BirthPlace.
			?BirthPlace rdfs:label ?BirthPlaceLabel.
    		?BirthPlace a dbpedia-owl:Settlement.
	      	FILTER (LANG(?BirthPlaceLabel) = 'de') .
    	}
      
    	OPTIONAL {
    		?Person dbo:height ?Height.
    		}
      
          OPTIONAL {
    		?Person dbpprop:children ?Children.
    		}
		
		OPTIONAL {
	        ?Person dbpprop:residence ?Residence.
			FILTER (LANG(?Residence) = 'en') .
        }
		
		FILTER (LANG(?Label) = 'en') .
		
		} LIMIT 10
      """.format(label)

    WS.url("http://dbpedia.org/sparql")
      .withQueryString("query" -> query)
      .withQueryString("format" -> "application/sparql-results+json")
      //     .withQueryString("format" -> "text")
      .get().map(
        response => {
          //TODO:Handle http errors better
          if (response.status >= 400) return null
          Logger.debug("DBpedia status repsponse: " + response.statusText)
          val json: JsValue = Json.parse(response.body)
          val bindings = json \ "results" \ "bindings" apply (0) //Currently use only first match in case of many results

          val celebrityReads: Reads[Celebrity] = (
            //            (JsPath \ "Person" \ "value").read[Option[Long]] and //Source
            (JsPath \ "Person" \ "value").read[String] and
            (JsPath \ "Label" \ "value").readOpt[String] and
            (JsPath \ "Lastname" \ "value").readOpt[String] and
            (JsPath \ "GivenName" \ "value").readOpt[String] and
            (JsPath \ "BirthDate" \ "value").readOpt[String] and
            (JsPath \ "BirthPlaceLabel" \ "value").readOpt[String] and
            (JsPath \ "Abstract" \ "value").readOpt[String] and
            (JsPath \ "Thumbnail" \ "value").readOpt[String] and
            (JsPath \ "Children" \ "value").readOpt[String] and
            (JsPath \ "Height" \ "value").readOpt[String] and
            (JsPath \ "Residence" \ "value").readOpt[String])(Celebrity.apply _)

          //            val cel = bindings.as[Celebrity]

          celebrityReads.reads(bindings) match {
            case s: JsSuccess[Celebrity] => {
              val p: Celebrity = s.get
              p
            }
            case e: JsError => {
              //              Future.failed(new Exception("DBpedia Json parser error"))
              Logger.debug(e.toString())
              null
            }
          }

        })
  }
}