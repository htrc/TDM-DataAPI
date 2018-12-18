package v1.dao

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.google.inject.ImplementedBy
import play.api.libs.json.JsObject

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[FeaturesMongoDaoImpl])
trait FeaturesDao {

  def getFeatures(id: String)(implicit ec: ExecutionContext): Future[Option[JsObject]]
  def getFeaturesAsSource(ids: Set[String])(parallelism: Int = 1): Source[JsObject, NotUsed]

}
