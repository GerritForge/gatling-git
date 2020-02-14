## Gatling protocol manager for Git

gatling-git is a plug-in protocol manager for [Gatling](https://gatling.io), the popular Scala-based
performance testing tool.

### Why yet another Gatling plugin?

[Gatling](https://gatling.io) allows creating simple end-to-end test scenarios for web applications
or REST-API services by leveraging the Akka/Netty client protocol stack. However, when performing
testing against services that have also a Git server component, a more specific protocol handler
built on top of the Git protocol itself is needed.

Gatling-git is a JGit-based implementation of the Gatling protocol manager. You can build higher
level test scenarios using directly the Git verbs in your tests.

### Prerequisites

* [Scala 2.12][scala]
* [Gatling 3.1.1][gatling-3.1.1]

[gatling-3.1.1]: https://mvnrepository.com/artifact/io.gatling/gatling-core/3.1.1
[scala]: https://www.scala-lang.org/download/

### Using the Gatling Git test scenarios and protocol

Add the gatling-git plugin, in addition to the standard Gatling imports.

```scala
import com.github.barbasa.gatling.git._
import com.github.barbasa.gatling.git.protocol._
import com.github.barbasa.gatling.git.request.builder._
```

Add the following dependencies to your Gatling SBT project:

```scala
val GATLING_VER = "3.1.1"

libraryDependencies += "io.gatling.highcharts" % "gatling-charts-highcharts" % GATLING_VER % Test
libraryDependencies += "io.gatling.highcharts" % "gatling-test-framework" % GATLING_VER % Test
libraryDependencies += "com.gerritforge" %% "gatling-git" % "1.0.4" % Test
```

The gatling-git plugin relies on a global configuration defined in the `application.conf` of
your tests and read through a global implicit configuration object that needs to be defined
in your test class.

```scala
implicit val gatlingGitConfiguration = GatlingGitConfiguration()
```

The gatling-git plugin provides the following Git-specific actions for your test scenarios:
* clone
* fetch
* pull
* push

Use any of the above inside your Gatling test scenario, like in the following example:

```scala
  val gitCloneScenario = scenario("Git clone from Gerrit")
                           .exec(new GitRequestBuilder(
                                     GitRequestSession(
                                       "clone", "ssh://github.com/GerritForge/gatling-git", "master")))
```

Inject a number of concurrent users into the scenario using the usual Gatling DSL syntax,
and then setup the Git protocol associated to the simulation.

For instance, for a constant traffic from 2 concurrent users for a 1 minute test:

```scala
val gitProtocol = GitProtocol()

  setUp(
    gitPush.inject(constantConcurrentUsers(2) during (1 minute))
  ).protocols(gitProtocol)
```

### Build from source

See below the instructions on how to build the gatling-git plugin from source.
It requires [Scala 2.12][scala] and [SBT 1.2.8][sbt-1.2.8]

```bash
sbt compile
```

[sbt-1.2.8]: https://www.scala-sbt.org/download.html

### Setup

If you are running SSH commands the private keys of the users used for testing need to go in `/tmp/ssh-keys`.
The keys need to be generated this way (JSch won't validate them [otherwise](https://stackoverflow.com/questions/53134212/invalid-privatekey-when-using-jsch)):

```bash
ssh-keygen -m PEM -t rsa -C "test@mail.com" -f /tmp/ssh-keys/id_rsa
```

NOTE: Don't forget to add the public keys for the testing user(s) to your git server.

#### Using Gatling's feeder input

The ReplayRecordsScenario is expecting the [src/test/resources/data/requests.json](/src/test/resources/data/requests.json) file.
Here below an example:

```json
[
  {
    "url": "ssh://admin@localhost:29418/loadtest-repo.git",
    "cmd": "clone"
  },
  {
    "url": "http://localhost:8080/loadtest-repo.git",
    "cmd": "fetch"
  },
  {
    "url": "http://localhost:8080/loadtest-repo.git",
    "cmd": "push",
    "ref-spec": "HEAD:refs/for/master"
  }
]
```

Valid commands that can be specified in the `cmd` parameter are:

* `clone`: clone the remote repository
* `fetch`: run a git-upload-pack
* `pull`: run a git-upload-pack and then merge the remote fetched head to the local branch
* `push`: push the local ref to the remote Git server

The common parameters are:

* `url`: The HTTP or SSH Git URL of the remote repository.
* `ref-spec`: ref-spec of the `push` operation. Can be specified with a simple branch name or have
  the more general form of `local:remote` refs.

### How to run the tests

All tests:
```
sbt "gatling:test"
```

Single test:
```
sbt "gatling:testOnly com.github.barbasa.gatling.git.ReplayRecordsScenario"
```

Generate report:
```
sbt "gatling:lastReport"
```

### How to use docker

To build the docker container run:

```
docker build -t gatling-git .
```

To execute tests from the docker container run:

```
docker run -it \
  -e GIT_HTTP_PASSWORD="foo" \
  -e GIT_HTTP_USERNAME="bar" \
  -v $DATA_DIR:/data \
  -v $SCENARIO_DIR:/scenarios \
  gatling-git
```

`$DATA_DIR` refers to a directory containing the data-files fed to the feeder
used in a scenario. `$SCENARIO_DIR` is a directory containing all the scenarios
that should be run.
