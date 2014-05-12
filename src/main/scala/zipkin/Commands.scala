package zipkin

import scala.sys.process._

object Install extends App {
  "./project/zipkin.sh install".!
}

object Start extends App {
  "./project/zipkin.sh start".run()
}

object Stop extends App {
  "./project/zipkin.sh stop".!
}

object Update extends App {
  "./project/zipkin.sh update".!
}
