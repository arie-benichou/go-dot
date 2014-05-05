import org.scalatra.LifeCycle
import javax.servlet.ServletContext
import services._

class ScalatraBootstrap extends LifeCycle {

  override def init(context: ServletContext) {
    //context.mount(new BlokusContextContainer(), "/*")
    //context.mount(new GoContextContainer(), "/*")
  }

}