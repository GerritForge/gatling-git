// Copyright (C) 2019 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.github.barbasa.gatling.git

import java.io.File
import java.nio.file.Path
import com.github.barbasa.gatling.git.protocol.GitProtocol
import com.github.barbasa.gatling.git.request.builder.GitRequestBuilder
import io.gatling.core.Predef._
import io.gatling.core.structure.ScenarioBuilder

import scala.concurrent.duration._

class ReplayRecordsScenario extends Simulation {

  val gitProtocol = GitProtocol()

  implicit val postMessageHook: Option[Path] = {
    val classLoader: ClassLoader = getClass.getClassLoader
    Some(new File(classLoader.getResource("hooks/commit-msg").getPath).toPath)
  }
  val feeder = csv("data/requests.csv").circular

  val replayCallsScenario: ScenarioBuilder =
    scenario("Git commands")
      .repeat(10000) {
        feed(feeder)
          .exec(new GitRequestBuilder("${cmd}", "${url}"))
      }
//      .pause("${pause}")

  setUp(
    replayCallsScenario.inject(
      nothingFor(4 seconds),
      atOnceUsers(10),
      rampUsers(10) during (5 seconds),
      constantUsersPerSec(20) during (15 seconds),
      constantUsersPerSec(20) during (15 seconds) randomized
    ))
    .protocols(gitProtocol)
    .maxDuration(60 seconds)
  // XXX Add tear down to cleanup the files created during the simulation
}
