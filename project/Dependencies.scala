import sbt._

object Dependencies {
  val GatlingVersion = "3.4.2"

  lazy val gatling = Seq(
    "io.gatling.highcharts" % "gatling-charts-highcharts",
    "io.gatling" % "gatling-test-framework",
  ).map(_ % GatlingVersion % Test)
}
