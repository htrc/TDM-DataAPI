package v1.filters

import akka.stream.Materializer
import javax.inject.{Inject, Singleton}
import org.joda.time.DateTime
import play.api.Logger
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class LoggingFilter @Inject()(implicit val mat: Materializer,
                              ec: ExecutionContext) extends Filter {
  val accessLogger: Logger = Logger("access")

  def apply(nextFilter: RequestHeader => Future[Result])
           (requestHeader: RequestHeader): Future[Result] = {

    val startTime = System.currentTimeMillis
    val strStartTime = DateTime.now().toString("[dd/MM/yyyy:HH:mm:ss Z]")
    val protocol = if (requestHeader.secure) "HTTPS" else "HTTP"
    val referrer = requestHeader.headers.get("Referer").getOrElse("-")
    val userAgent = requestHeader.headers.get("User-Agent").getOrElse("-")

    nextFilter(requestHeader).map { result =>

      val strRespSize = result.body.contentLength.map(_.toString).getOrElse("-")

      val endTime = System.currentTimeMillis
      val requestTime = endTime - startTime

      accessLogger.info(f"""${requestHeader.remoteAddress} - $strStartTime "${requestHeader.method} ${requestHeader.uri} ${requestHeader.version}" $protocol ${result.header.status} $strRespSize "$referrer" "$userAgent" ($requestTime%,d ms)""")

      result
    }
  }
}