package v1.dao

import _root_.utils.FutureUtil
import javax.inject.{Inject, Singleton}
import org.reactivestreams.Publisher
import play.api.Logger
import play.api.libs.iteratee._
import play.api.libs.iteratee.streams.IterateeStreams
import play.api.libs.json.{JsObject, Json}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.gridfs.Implicits._
import reactivemongo.api.gridfs.{GridFS, ReadFile}
import reactivemongo.api.indexes.Index
import reactivemongo.api.indexes.IndexType.Ascending
import reactivemongo.api.{BSONSerializationPack, DefaultDB}
import reactivemongo.bson._
import reactivemongo.play.iteratees.cursorProducer
import reactivemongo.play.json._

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FeaturesMongoDaoImpl @Inject()(reactiveMongoApi: ReactiveMongoApi)
                                    (implicit ec: ExecutionContext) extends FeaturesDao {
  def db: Future[DefaultDB] = reactiveMongoApi.database

  type BSONGridFS = GridFS[BSONSerializationPack.type]
  def gridFS: Future[BSONGridFS] = db.map(GridFS[BSONSerializationPack.type](_))

  for {
    gfs <- gridFS
    fi <- gfs.ensureIndex()
    mi <- gfs.files.indexesManager.ensure(
      Index(List("metadata.id" -> Ascending), unique = true)
    )
  } {
    Logger.info(s"Mongo index creation status: GFS: $fi, Meta: $mi")
  }


  implicit object BSONValueStringReader extends BSONReader[BSONValue, String] {
    def read(v: BSONValue): String = v match {
      case oid: BSONObjectID => oid.stringify
      case s: BSONString => s.value
      case other => other.toString
    }
  }

  override def getFeatures(id: String): Future[Option[JsObject]] = {
    getFile(id).flatMap {
      case Some(file) =>
        for {
          fs <- gridFS
          content = fs.enumerate(file) |>>> Iteratee.fold(mutable.ArrayBuilder.make[Byte]()) {
            _ ++= _
          }
          efJsonOpt <- content.map { builder =>
            val bytes = builder.result()
            val featuresJson = Json.parse(bytes).as[JsObject]
            val efJson = featuresJson ++ Json.obj(
              "id" -> file.id.as[String],
              "metadata" -> file.metadata
            )
            Some(efJson)
          }
        } yield efJsonOpt

      case None => Future.successful(None)
    }
  }

  override def getFeatures(ids: Set[String]): Publisher[JsObject] = {
    val futures = ids.toSeq.map(getFeatures)
    val enumerator =
      Enumerator.unfoldM(futures) { remainingFutures =>
        if (remainingFutures.isEmpty) {
          Future(None)
        } else {
          FutureUtil.select(remainingFutures).map {
            case (t, seqFuture) => t.toOption.map {
              a => (seqFuture, a)
            }
          }
        }
      } &> Enumeratee.collect { case Some(features) => features }

    IterateeStreams.enumeratorToPublisher(enumerator)
  }

  protected def getFile(id: String): Future[Option[ReadFile[BSONSerializationPack.type, BSONValue]]] = {
    gridFS.flatMap(_.find(BSONDocument("metadata.id" -> id)).headOption)
  }
}
