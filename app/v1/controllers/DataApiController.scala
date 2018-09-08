package v1.controllers

import java.nio.charset.StandardCharsets
import java.util.Base64

import akka.stream.scaladsl.Source
import akka.util.ByteString
import javax.inject.Inject
import play.api.http.{ContentTypes, Writeable}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc._
import v1.dao.FeaturesDao

import scala.concurrent.ExecutionContext

class DataApiController @Inject()(featuresDao: FeaturesDao,
                                  components: ControllerComponents)
                                 (implicit val ec: ExecutionContext) extends AbstractController(components) {
  private val jsonWritable: Writeable[JsObject] =
    Writeable(
      obj => ByteString(Json.stringify(obj) + "\n", StandardCharsets.UTF_8),
      contentType = Some(ContentTypes.JSON)
    )

  def getFeatures(base64Id: String): Action[AnyContent] =
    Action.async { implicit req =>
      render.async {
        case Accepts.Json() =>
          val id = new String(Base64.getDecoder.decode(base64Id), StandardCharsets.UTF_8)
          featuresDao.getFeatures(id)
            .map {
              case Some(json) => Ok(json)
              case None => NotFound
            }
      }
    }

  def getBulkFeatures: Action[String] =
    Action(parse.text) { implicit req =>
      render {
        case Accepts.Json() =>
          if (req.body == null || req.body.isEmpty)
            BadRequest
          else {
            val ids = req.body.split("""[\|\n]""").toSet
            val publisher = featuresDao.getFeatures(ids)
            Ok.chunked(Source.fromPublisher(publisher))(jsonWritable)
          }
      }
    }
}
