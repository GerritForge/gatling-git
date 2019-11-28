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

object GitRequestBuilder {

  implicit def toActionBuilder(requestBuilder: GitRequestBuilder): GitRequestActionBuilder =
    new GitRequestActionBuilder(requestBuilder)

}

case class GitRequestBuilder(request: GitRequestSession)(
    implicit conf: GatlingGitConfiguration,
    val postMsgHook: Option[String] = None
) {

  def buildWithSession(session: Session): Validation[Request] = {

    for {
      command   <- request.commandName(session)
      urlString <- request.url(session)
      url       <- validateUrl(urlString)
      refSpec   <- request.refSpec(session)
      tag       <- request.tag(session)
    } yield {
      val user = session.userId.toString
      command.toLowerCase match {
        case "clone" => Clone(url, user, refSpec)
        case "fetch" => Fetch(url, user, refSpec)
        case "pull"  => Pull(url, user)
        case "push"  => Push(url, user, refSpec)
        case "tag"   => Tag(url, user, refSpec, tag)
        case _       => InvalidRequest(url, user)
      }
    }
  }

  private def validateUrl(stringUrl: String): Validation[URIish] = {
    try {
      Success(new URIish(stringUrl))
    } catch {
      case e: Exception => {
        val errorMsg = s"Invalid url: $stringUrl. ${e.getMessage}"
        println(errorMsg)
        Failure(errorMsg)
      }
    }
  }
}
