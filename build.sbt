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

ThisBuild / publishTo := {
  val nexus = "https://ossrh-staging-api.central.sonatype.com/"
  if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
  else Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

ThisBuild / organization      := "com.gerritforge"
ThisBuild / organizationName  := "GerritForge"
ThisBuild / scalaVersion      := "2.13.10"
ThisBuild / publishMavenStyle := true
ThisBuild / scalafmtOnCompile := true

val JGitVersion = "5.13.2-20221120.212658-7"

ThisBuild / resolvers +=
  "Eclipse JGit Snapshots" at "https://repo.eclipse.org/content/groups/jgit"

lazy val root = (project in file("."))
  .enablePlugins(GitVersioning)
  .settings(
    scmInfo := Some(
      ScmInfo(
        url("https://review.gerrithub.io/GerritForge/gatling-git"),
        "scm:https://review.gerrithub.io/GerritForge/gatling-git.git"
      )
    ),
    developers  := List(ponch, tony, thomas, luca),
    description := "Gatling plugin for supporting the Git protocol over SSH and HTTP",
    licenses    := List("Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt")),
    homepage    := Some(url("https://github.com/GerritForge/gatling-git")),
    pomIncludeRepository := { _ =>
      false
    },
    name := "gatling-git",
    libraryDependencies ++=
      gatling ++ Seq(
        "io.gatling"                  % "gatling-core"                % GatlingVersion,
        "io.gatling"                  % "gatling-app"                 % GatlingVersion,
        "com.google.inject"           % "guice"                       % "5.1.0",
        "commons-io"                  % "commons-io"                  % "2.11.0",
        "com.typesafe.scala-logging" %% "scala-logging"               % "3.9.5",
        "org.eclipse.jgit"            % "org.eclipse.jgit"            % JGitVersion,
        "org.eclipse.jgit"            % "org.eclipse.jgit.ssh.apache" % JGitVersion,
        "org.scalatest"              %% "scalatest"                   % "3.2.15"       % Test
      )
  )

credentials += Credentials(Path.userHome / ".sbt" / "sonatype_credentials")
scalafmtOnCompile := true

git.useGitDescribe := true

useGpg := true
usePgpKeyHex("C54DAC2791F484279F956ED9F53F69D12E935B99")
