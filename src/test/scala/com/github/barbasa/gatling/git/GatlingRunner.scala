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

import io.gatling.app.Gatling
import io.gatling.core.config.GatlingPropertiesBuilder

import scala.annotation.nowarn

@nowarn("msg=unused value")
object GatlingRunner {

  def main(args: Array[String]): Unit = {

    val simClass = classOf[ReplayRecordsScenario].getName

    val props = new GatlingPropertiesBuilder
    props.simulationClass(simClass)

    Gatling.fromMap(props.build): Unit

  }

}
