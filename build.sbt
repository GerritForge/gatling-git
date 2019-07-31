import Dependencies._

enablePlugins(GatlingPlugin)

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
      developers := List(
        Developer(
          id    = "barbasa",
          name  = "Fabio Ponciroli",
          email = "ponch78@gmail.com",
          url   = url("https://github.com/barbasa")
        ),
        Developer(
          id    = "syntonyze",
          name  = "Antonio Barone",
          email = "syntonyze@gmail.com",
          url   = url("https://github.com/syntonyze")
        )),
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
        Seq("io.gatling" % "gatling-core" % "3.1.1" ) ++
        Seq("io.gatling" % "gatling-app" % "3.1.1" ) ++
        Seq("org.eclipse.jgit" % "org.eclipse.jgit" % "5.3.0.201903130848-r") ++
        Seq("com.google.inject" % "guice" % "3.0") ++
        Seq("commons-io" % "commons-io" % "2.6") ++
        Seq("org.scalatest" %% "scalatest" % "3.0.1" % Test ),
  )

credentials += Credentials(Path.userHome / ".sbt" / "sonatype_credentials")

git.useGitDescribe := true

useGpg := true
usePgpKeyHex("C01506A0")

assemblyMergeStrategy in assembly := {
 case PathList("META-INF", xs @ _*) => MergeStrategy.discard
 case x => MergeStrategy.first
}
