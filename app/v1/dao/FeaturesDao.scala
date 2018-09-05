package v1.dao

import com.google.inject.ImplementedBy
import org.reactivestreams.Publisher
import play.api.libs.json.JsObject

import scala.concurrent.Future

@ImplementedBy(classOf[FeaturesMongoDaoImpl])
trait FeaturesDao {

  def getFeatures(id: String): Future[Option[JsObject]]
  def getFeatures(ids: Set[String]): Publisher[JsObject]

}
