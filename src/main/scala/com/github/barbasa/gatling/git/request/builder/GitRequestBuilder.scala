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

import com.github.barbasa.gatling.git.action.GitRequestActionBuilder
import com.github.barbasa.gatling.git.request._
import io.gatling.core.session.{Expression, Session}
import org.eclipse.jgit.transport.URIish

object GitRequestBuilder {

  implicit def toActionBuilder(
      requestBuilder: GitRequestBuilder): GitRequestActionBuilder =
    new GitRequestActionBuilder(requestBuilder)

}

case class GitRequestBuilder(commandName: Expression[String],
                             url: Expression[String]) {

  def buildWithSession(session: Session): Option[Request] = {
    val command = commandName(session).toOption.get.toLowerCase

    val user = session.userId.toString

    validateUrl(url(session).toOption.get).map { u =>
      command match {
        case "clone" => Clone(u, user)
        case "fetch" => Fetch(u, user)
        case "pull" => Pull(u, user)
        case "push" => Push(u, user)
        case _ => InvalidRequest(u, user)
      }
    }
  }

  private def validateUrl(stringUrl: String): Option[URIish] = {
    try {
      Some(new URIish(stringUrl))
    } catch {
        case e: Exception => {
          println(s"Invalid url: $stringUrl. ${e.getMessage}")
          None
        }
      }
  }
}
