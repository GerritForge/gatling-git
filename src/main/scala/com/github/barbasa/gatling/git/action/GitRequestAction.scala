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

package com.github.barbasa.gatling.git.action

import com.github.barbasa.gatling.git.request.builder.GitRequestBuilder
import com.github.barbasa.gatling.git.request.{Fail, OK, Request, Response}
import io.gatling.commons.util.Clock
import io.gatling.commons.validation.{Failure, Success}
import io.gatling.core.CoreComponents
import io.gatling.core.action.{Action, ExitableAction}
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine
import io.gatling.core.util.NameGen

class GitRequestAction(
    coreComponents: CoreComponents,
    reqBuilder: GitRequestBuilder,
    val next: Action
) extends ExitableAction
    with NameGen {

  override def statsEngine: StatsEngine = coreComponents.statsEngine

  override def clock: Clock = coreComponents.clock

  override def name: String = genName("GitRequest")

  override def execute(session: Session): Unit = {
    val start = clock.nowMillis

    val (response, reqName, message) = reqBuilder.buildWithSession(session) match {
      case Success(req) =>
        try {
          req.send
          (Response(OK), req.name, None)
        } catch {
          case e: Exception => (Response(Fail), req.name, Some(e.getMessage))
        }
      case Failure(message) => (Response(Fail), "Unknown", Some(message))
    }

    statsEngine.logResponse(
      session,
      reqName,
      start,
      clock.nowMillis,
      Request.gatlingStatusFromGit(response),
      None,
      message
    )
    next ! session.markAsSucceeded
  }
}
