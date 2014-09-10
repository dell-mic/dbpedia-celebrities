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

object Application extends Controller {

  //create an instance of the table
  val Celebrities = TableQuery[CelebritysTable]

  lazy val iter = Source.fromFile("/Users/apfelbaum24/Downloads/stars.csv").getLines()
  
  //    source: String,
  //  name: String,
  //  givenName: String,
  //  lastName: String,
  //  birthDate: String,
  //  birthPlaceLabel: String,
  //  abstractText: String,
  //  thumbnail: String,
  //  children: Int,
  //  height: String,
  //  residence: String)
  
  def readInput = Action {
    val line = iter.next()
    if (line == null) Ok("End of file reached")
    val parts = line.split(",")
    try { Ok(views.html.messageRedirect(line + "\n\n" + "Extracted label: " + parts(1), "/read")) }
    catch {
     case e: Exception => Ok(views.html.messageRedirect(line + "\n\n" +"Line skipped", "/read"))
   }
  }

  def index = DBAction { implicit rs =>
    //    val testDataSets = Seq(
    //      Celebrity(Option(1L), "test", "Halle Berry", "Halle", "Berry", "27-12-78", "USA", "Eine Schauspielerin", "not_found", Option("5"), Option("160m"), Option("Kalifornien")))
    //    Celebrities.insertAll(testDataSets: _*)

    Ok(views.html.index(Celebrities.list, "Alle Datensätze"))
  }

  def getDBPedia(label: String) = Action.async {
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
          val json: JsValue = Json.parse(response.body)
          val bindings = json \ "results" \ "bindings" apply (0) //Currently use only first match in case of many results

          implicit val celebrityReads: Reads[Celebrity] = (
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
              var p: Celebrity = s.get
              // do something with person
              play.api.db.slick.DB.withSession { implicit session =>
                Celebrities.insertOrUpdate(p)
                Ok(views.html.index(List(p), "Geparster Datensatz"))
              }

            }
            case e: JsError => {
              Ok(e.toString())
            }
          }

        })
  }

}