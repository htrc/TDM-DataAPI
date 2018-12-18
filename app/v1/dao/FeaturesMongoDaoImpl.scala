package v1.dao

import java.io.ByteArrayInputStream
import java.util.zip.GZIPInputStream

import _root_.utils.Using.using
import akka.NotUsed
import akka.stream.scaladsl.Source
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.iteratee._
import play.api.libs.json.{JsObject, Json}
import play.modules.reactivemongo.MongoController._
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.DefaultDB
import reactivemongo.api.indexes.Index
import reactivemongo.api.indexes.IndexType.Ascending
import reactivemongo.play.json._

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

@Singleton
class FeaturesMongoDaoImpl @Inject()(reactiveMongoApi: ReactiveMongoApi)
                                    (implicit ec: ExecutionContext) extends FeaturesDao {
  def db: Future[DefaultDB] = reactiveMongoApi.database
  def gridFS: Future[JsGridFS] = reactiveMongoApi.asyncGridFS

  for {
    gfs <- gridFS
    fi <- gfs.ensureIndex()
    mi <- gfs.files.indexesManager.ensure(
      Index(List("metadata.id" -> Ascending), unique = true)
    )
  } {
    Logger.info(s"Mongo index creation status: GFS: $fi, Meta: $mi")
  }

  override def getFeatures(id: String)(implicit ec: ExecutionContext): Future[Option[JsObject]] = {
    getFile(id).flatMap {
      case Some(file) => readFile(file).map(Some(_))
      case None => Future.successful(None)
    }
  }

  override def getFeaturesAsSource(ids: Set[String])(parallelism: Int = 1): Source[JsObject, NotUsed] = {
    Source(ids).mapAsyncUnordered(parallelism)(getFeatures).collect {
      case Some(ef) => ef
    }
  }

  protected def readFile(file: JsReadFile[JsObject])(implicit ec: ExecutionContext): Future[JsObject] = {
    for {
      gfs <- gridFS
      content = gfs.enumerate(file) |>>> Iteratee.fold(arrayBuilderOfKnownSize[Byte](file.length))(_ ++= _)
      efJson <- content.map { builder =>
        val bytes = builder.result()
        val featuresJson = using(new GZIPInputStream(new ByteArrayInputStream(bytes), bytes.length))(Json.parse(_).as[JsObject])
        val json = featuresJson ++ Json.obj(
          "id" -> (file.id \ "$oid").as[String],
          "metadata" -> file.metadata
        )
        json
      }
    } yield efJson
  }

  protected def getFile(id: String)(implicit ec: ExecutionContext): Future[Option[JsReadFile[JsObject]]] = {
    gridFS.flatMap(_.find(Json.obj("metadata.id" -> id)).headOption)
  }

  private def arrayBuilderOfKnownSize[T: ClassTag](size: Long): mutable.ArrayBuilder[T] = {
    val builder = mutable.ArrayBuilder.make[T]()
    builder.sizeHint(size.toInt)
    builder
  }
}
