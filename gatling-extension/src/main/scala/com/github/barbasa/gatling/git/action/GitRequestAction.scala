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
import com.github.barbasa.gatling.git.request.{Fail, OK, Request, GitCommandResponse}
import io.gatling.commons.util.Clock
import io.gatling.commons.validation.{Failure, Success}
import io.gatling.core.CoreComponents
import io.gatling.core.action.{Action, ExitableAction}
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine
import io.gatling.core.util.NameGen

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import io.gatling.commons.stats.{KO => GatlingFail, OK => GatlingOK, Status}

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
    Future {
      val start = clock.nowMillis

      val (response, reqName, message) = reqBuilder.buildWithSession(session) match {
        case Success(req) =>
          try {
            val commandResponse = req.send

            commandResponse match {
              case GitCommandResponse(Fail, Some(message)) =>
                mayOverrideFailure(session, commandResponse, req.name, message)
              case _ =>
                (commandResponse, req.name, commandResponse.message)
            }
          } catch {
            case e: Exception =>
              mayOverrideFailure(
                session,
                GitCommandResponse(Fail),
                req.name,
                s"${e.getMessage} - ${e.getCause}"
              )
          }
        case Failure(message) => (GitCommandResponse(Fail), "Unknown", Some(message))
      }

      val gatlingStatus = Request.gatlingStatusFromGit(response)
      statsEngine.logResponse(
        session.scenario,
        session.groups,
        reqName,
        start,
        clock.nowMillis,
        gatlingStatus,
        None,
        message
      )
      coreComponents.throttler match {
        case Some(throttler) =>
          throttler.throttle(
            session.scenario,
            () => next ! setSessionStatus(session, gatlingStatus)
          )
        case None => next ! setSessionStatus(session, gatlingStatus)
      }
    }: Unit
  }

  private def mayOverrideFailure(
      session: Session,
      origResponse: GitCommandResponse,
      reqName: String,
      originalMessage: String
  ): (GitCommandResponse, String, Option[String]) =
    reqBuilder.request.ignoreFailureRegexps(session) match {
      case Success(regexList) if regexList.exists(originalMessage.matches) =>
        val newMessage = s"[Ignore failure] Request '$reqName' - $originalMessage"
        logger.warn(newMessage)
        (GitCommandResponse(OK, Some(newMessage)), reqName, Some(newMessage))
      case _ => (origResponse, reqName, Some(originalMessage))
    }

  private def setSessionStatus(session: Session, status: Status): Session = {
    status match {
      case GatlingOK   => session.markAsSucceeded
      case GatlingFail => session.markAsFailed
    }
  }
}
