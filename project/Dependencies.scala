import sbt._

object Dependencies {
  val GatlingVersion = "3.9.0"

  lazy val gatling = Seq(
    "io.gatling.highcharts" % "gatling-charts-highcharts",
    "io.gatling"            % "gatling-test-framework"
  ).map(_ % GatlingVersion % Test)
}
