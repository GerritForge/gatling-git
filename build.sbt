import Dependencies._

enablePlugins(GatlingPlugin)

val ponch = Developer(
  id = "barbasa",
  name = "Fabio Ponciroli",
  email = "ponch78@gmail.com",
  url = url("https://github.com/barbasa")
)

val tony = Developer(
  id = "syntonyze",
  name = "Antonio Barone",
  email = "syntonyze@gmail.com",
  url = url("https://github.com/syntonyze")
)

val thomas = Developer(
  id = "thomasdraebing",
  name = "Thomas Draebing",
  email = "thomas.draebing@sap.com",
  url = url("https://github.com/thomasdraebing")
)

val luca = Developer(
  id = "lucamilanesio",
  name = "Luca Milanesio",
  email = "luca.milanesio@gmail.com",
  url = url("https://github.com/lucamilanesio")
)

val JGitVersion = "5.11.0-SNAPSHOT"
resolvers += "jgit snapshot repo" at "https://repo.eclipse.org/content/groups/jgit/"

lazy val root = (project in file("."))
  .enablePlugins(GitVersioning)
  .settings(
    inThisBuild(List(
      organization := "com.gerritforge",
      organizationName := "GerritForge",
      scalaVersion := "2.12.8",
      assemblyJarName := "gatling-git-extension.jar",
      scmInfo := Some(ScmInfo(url("https://review.gerrithub.io/GerritForge/gatling-git"),
        "scm:https://review.gerrithub.io/GerritForge/gatling-git.git")),
      developers := List(ponch, tony, thomas, luca),
      description := "Gatlin plugin for supporting the Git protocol over SSH and HTTP",
      licenses := List("Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt")),
      homepage := Some(url("https://github.com/GerritForge/gatling-git")),
      pomIncludeRepository := { _ => false },
      publishTo := {
        val nexus = "https://oss.sonatype.org/"
        if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
        else Some("releases" at nexus + "service/local/staging/deploy/maven2")
      },
      publishMavenStyle := true
    )),

    name := "gatling-git",
    libraryDependencies ++=
      gatling ++
        Seq("io.gatling" % "gatling-core" % GatlingVersion) ++
        Seq("io.gatling" % "gatling-app" % GatlingVersion) ++
        Seq("org.eclipse.jgit" % "org.eclipse.jgit" % JGitVersion) ++
        Seq("org.eclipse.jgit" % "org.eclipse.jgit.ssh.apache" % JGitVersion) ++
        Seq("com.google.inject" % "guice" % "3.0") ++
        Seq("commons-io" % "commons-io" % "2.6") ++
        Seq("com.typesafe.scala-logging" %% "scala-logging" % "3.9.2") ++
        Seq("org.scalatest" %% "scalatest" % "3.0.1" % Test),
  )

credentials += Credentials(Path.userHome / ".sbt" / "sonatype_credentials")

git.useGitDescribe := true

useGpg := true
usePgpKeyHex("C54DAC2791F484279F956ED9F53F69D12E935B99")

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs@_*) => MergeStrategy.discard
  case x => MergeStrategy.first
}
