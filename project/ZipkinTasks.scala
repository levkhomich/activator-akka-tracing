import sbt._
import sbt.Keys._

object ZipkinTasks extends Plugin {

  val zipkinInstall = TaskKey[Unit]("zipkin-install", "Installs zipkin to project's dir")
  val zipkinUpdate = TaskKey[Unit]("zipkin-update", "Updates zipkin")
  val zipkinStart = TaskKey[Unit]("zipkin-start", "Starts zipkin")
  val zipkinStop = TaskKey[Unit]("zipkin-stop", "Stops zipkin")

  def zipkinScript(param: String): String = {
    s"./project/zipkin.sh $param"
  }

  override val settings = Seq(
    zipkinInstall := zipkinScript("install").!,
    zipkinUpdate := zipkinScript("update").!,
    zipkinStart := zipkinScript("start").run(),
    zipkinStop := zipkinScript("stop").!

    // doesn't work with Activator
    // runMain <<= (runMain in Runtime).dependsOn(zipkinStart).andFinally { zipkinScript("stop").run() },
  )
}