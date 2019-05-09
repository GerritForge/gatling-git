package com.github.barbasa.gatling.git.action

import com.github.barbasa.gatling.git.request.builder.GitRequestBuilder
import com.github.barbasa.gatling.git.request.{Fail, OK, Request, Response}
import io.gatling.commons.util.Clock
import io.gatling.core.CoreComponents
import io.gatling.core.action.{Action, ExitableAction}
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine
import io.gatling.core.util.NameGen

class GitRequestAction(coreComponents: CoreComponents,
                       reqBuilder: GitRequestBuilder,
                       val next: Action)
    extends ExitableAction
    with NameGen {

  override def statsEngine: StatsEngine = coreComponents.statsEngine

  override def clock: Clock = coreComponents.clock

  override def name: String = genName("GitRequest")

  override def execute(session: Session): Unit = {
    val start = clock.nowMillis

    //XXX Mark session as failed this way if validation fails:
    // statsEngine.reportUnbuildableRequest(...)
    // next ! session.markAsFailed
    val (response, reqName, message) = reqBuilder.buildWithSession(session).map { req =>
      try {
        req.send
        (Response(OK), req.name, None)
      } catch {
        case e: Exception => (Response(Fail), req.name, Some(e.getMessage))
      }
    }.getOrElse((Response(Fail), "Unknown", Some("Cannot build Request")))


    statsEngine.logResponse(session,
                            reqName,
                            start,
                            clock.nowMillis,
                            Request.gatlingStatusFromGit(response),
                            None,
                            message)
    next ! session.markAsSucceeded
  }
}
