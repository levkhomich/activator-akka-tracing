package zipkin

import java.io.File

import scala.sys.process._

trait Zipkin {
  private[this] val script = new File("./project/zipkin.sh")

  def zipkin(cmd: String): ProcessBuilder = {
    // activator drops execution flag
    if (!script.canExecute) {
      if (script.setExecutable(true))
        throw new RuntimeException("can not set executable flag on " + script.getPath)
    }
    stringSeqToProcess(script.getPath :: cmd :: Nil)
  }

}

object Start extends App with Zipkin {
  zipkin("start").run()
}

object Stop extends App with Zipkin {
  zipkin("stop").!
}
