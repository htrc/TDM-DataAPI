package v1.controllers

import java.nio.charset.StandardCharsets
import java.util.Base64

import akka.http.scaladsl.model.headers.HttpEncodings
import akka.stream.scaladsl.Compression
import akka.util.ByteString
import javax.inject.{Inject, Singleton}
import play.api.http.{HttpChunk, HttpEntity, Writeable}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc._
import play.mvc.Http.HeaderNames
import v1.dao.FeaturesDao

import scala.concurrent.ExecutionContext

@Singleton
class DataApiController @Inject()(featuresDao: FeaturesDao,
                                  components: ControllerComponents)
                                 (implicit val ec: ExecutionContext) extends AbstractController(components) {
  private val JsonLinesMimeType = "application/x-ndjson"

  private val jsonWriteable: Writeable[JsObject] =
    Writeable(
      obj => ByteString(Json.stringify(obj) + "\n", StandardCharsets.UTF_8),
      contentType = Some(JsonLinesMimeType)
    )
  private val AcceptJsonLines: Accepting = Accepting(JsonLinesMimeType)

  def getFeatures(base64Id: String): Action[AnyContent] =
    Action.async { implicit req =>
      render.async {
        case Accepts.Json() =>
          val id = new String(Base64.getDecoder.decode(base64Id), StandardCharsets.UTF_8).trim()
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
        case AcceptJsonLines() =>
          if (req.body == null || req.body.isEmpty)
            BadRequest
          else {
            val ids = req.body.split("""[\|\n]""").toSet
            val source = featuresDao.getFeaturesAsSource(ids)(2)

            if (req.headers.get(HeaderNames.ACCEPT_ENCODING).getOrElse("").contains("gzip"))
              Result(
                header = ResponseHeader(OK),
                body = HttpEntity.Chunked(
                  source
                    .map(jsonWriteable.transform)
                    .via(Compression.gzip(4))
                    .map(HttpChunk.Chunk),
                  jsonWriteable.contentType
                )
              ).withHeaders(HeaderNames.CONTENT_ENCODING -> HttpEncodings.gzip.value)
            else
              Ok.chunked(source)(jsonWriteable)
          }
      }
    }
}
