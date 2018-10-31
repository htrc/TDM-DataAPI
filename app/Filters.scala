import javax.inject.{Inject, Singleton}
import play.api.http.DefaultHttpFilters
import play.filters.gzip.GzipFilter
import v1.filters.{InfoFilter, LoggingFilter}

@Singleton
class Filters @Inject()(gzipFilter: GzipFilter,
                        buildInfoFilter: InfoFilter,
                        accessLoggerFilter: LoggingFilter)
  extends DefaultHttpFilters(gzipFilter, buildInfoFilter, accessLoggerFilter)
