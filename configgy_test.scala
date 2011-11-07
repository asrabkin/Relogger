import net.lag.configgy.Configgy
import net.lag.logging.Logger

object ConfiggyTest {

def main(args:Array[String]):Unit = {
  System.out.println("Hello, world")
  val log = Logger.get
  log.info("I am a configgy info")
  log.debug("And I am debug")
}


}
