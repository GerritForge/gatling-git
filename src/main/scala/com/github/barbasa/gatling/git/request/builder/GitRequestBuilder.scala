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

package com.github.barbasa.gatling.git.request.builder

import com.github.barbasa.gatling.git.{GatlingGitConfiguration, GitRequestSession}
import com.github.barbasa.gatling.git.action.GitRequestActionBuilder
import com.github.barbasa.gatling.git.request._
import io.gatling.commons.validation.{Failure, Success, Validation}
import io.gatling.core.session.Session
import org.eclipse.jgit.transport.URIish

import scala.util.Try

object GitRequestBuilder {

  implicit def toActionBuilder(requestBuilder: GitRequestBuilder): GitRequestActionBuilder =
    new GitRequestActionBuilder(requestBuilder)

}

case class GitRequestBuilder(request: GitRequestSession)(implicit
    conf: GatlingGitConfiguration,
    @deprecated(
      "Use computeChangeId for generating a Change-Id instead of relying on hooks copied on clone",
      since = "1.0.12"
    )
    val postMsgHook: Option[String] = None
) {

  def buildWithSession(session: Session): Validation[Request] = {

    for {
      command           <- request.commandName(session)
      urlString         <- request.url(session)
      url               <- validateUrl(urlString)
      refSpec           <- request.refSpec(session)
      tag               <- request.tag(session)
      force             <- request.force(session)
      computeChangeId   <- request.computeChangeId(session)
      pushOptions       <- request.pushOptions(session)
      user              <- request.userId(session)
      requestName       <- request.requestName(session)
      repoDirOverride   <- request.repoDirOverride(session)
      createNewPatchset <- request.createNewPatchset(session)
    } yield {
      val userId               = if (user == "") session.userId.toString else user
      val maybeRepoDirOverride = if (repoDirOverride == "") None else Some(repoDirOverride)
      command.toLowerCase match {
        case "clone" => Clone(url, userId, refSpec, requestName)
        case "fetch" => Fetch(url, userId, refSpec, requestName)
        case "pull"  => Pull(url, userId, requestName, maybeRepoDirOverride)
        case "push" =>
          Push(
            url,
            userId,
            refSpec,
            force = force,
            computeChangeId = computeChangeId,
            options = pushOptions.split(",").toList,
            maybeRequestName = requestName,
            repoDirOverride = maybeRepoDirOverride,
            createNewPatchset = createNewPatchset
          )
        case "tag"          => Tag(url, userId, refSpec, tag, requestName)
        case "cleanup-repo" => CleanupRepo(url, userId, requestName)
        case _              => InvalidRequest(url, userId, requestName)
      }
    }
  }

  private def validateUrl(stringUrl: String): Validation[URIish] = {
    Try(Success(new URIish(stringUrl))).recover { case e: Exception =>
      val errorMsg = s"Invalid url: $stringUrl. ${e.getMessage}"
      println(errorMsg)
      Failure(errorMsg)
    }.get
  }
}
