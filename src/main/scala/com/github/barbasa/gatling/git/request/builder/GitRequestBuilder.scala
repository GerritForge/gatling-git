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
                             url: Expression[String],
                             userExpr: Expression[String]) {

  // XXX This should probably return an Either if validation of the
  // parameters (i.e.: url and cmd) fails
  def buildWithSession(session: Session): Option[Request] = {
    val command = commandName(session).toOption.get.toLowerCase
    val user = userExpr(session).toOption.get.toLowerCase

    validateUrl(url(session).toOption.get).map { u =>
      command match {
        case "clone" => Clone(u, user)
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
