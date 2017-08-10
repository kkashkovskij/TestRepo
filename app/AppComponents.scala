import play.api.ApplicationLoader.Context
import play.api.BuiltInComponentsFromContext
import play.api.db.{DBComponents, Database, HikariCPComponents}
import play.api.db.evolutions.EvolutionsComponents
import play.api.mvc.EssentialFilter
import play.api.routing.Router


class AppComponents(cntx: Context)
  extends BuiltInComponentsFromContext(cntx)
    with DBComponents
    with EvolutionsComponents
    with HikariCPComponents
{
  // this will actually run the database migrations on startup
  applicationEvolutions

 // override def router: Router = ???
  override def router: Router = ???

  override def httpFilters: Seq[EssentialFilter] = ???
}