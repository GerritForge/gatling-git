## Gerrit - performance test suite

### Prerequisites

* [Scala 2.12][scala]

[scala]: https://www.scala-lang.org/download/

### How to build

```bash
sbt compile
```

### Setup

If you are running SSH commands the private keys of the users used for testing need to go in `/tmp/ssh-keys`.
The keys need to be generated this way (JSch won't validate them [otherwise](https://stackoverflow.com/questions/53134212/invalid-privatekey-when-using-jsch):

```bash
ssh-keygen -m PEM -t rsa -C "test@mail.com" -f /tmp/ssh-keys/id_rsa
```

NOTE: Don't forget to add the public keys for the testing user(s) to your git server

#### Input

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
  }
]
```

Valid commands are:
* fetch
* pull
* push
* clone

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
