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

package com.github.barbasa.gatling.git.request
import java.io.File
import java.time.LocalDateTime

import com.jcraft.jsch.JSch
import com.jcraft.jsch.{Session => SSHSession}
import io.gatling.commons.stats.{OK => GatlingOK}
import io.gatling.commons.stats.{KO => GatlingFail}
import io.gatling.commons.stats.Status
import org.eclipse.jgit.api._
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport._
import org.eclipse.jgit.transport.JschConfigSessionFactory
import org.eclipse.jgit.transport.OpenSshConfig
import org.eclipse.jgit.transport.SshSessionFactory
import org.eclipse.jgit.util.FS
import org.eclipse.jgit.transport.SshTransport

sealed trait Request {

  def tmpPath: String
  def name: String
  def send: Unit
  def url: URIish
  def user: String
  private val repoName = url.getPath.split("/").last
  val workTreeDirectory: File = new File(s"$tmpPath/$user/$repoName")
  private val builder = new FileRepositoryBuilder
  val repository: Repository = builder.setWorkTree(workTreeDirectory).build()

  val sshSessionFactory: SshSessionFactory = new JschConfigSessionFactory() {
    protected def configure(host: OpenSshConfig.Host, session: SSHSession): Unit = {}

    override protected def createDefaultJSch(fs: FS): JSch = {
      val defaultJSch = super.createDefaultJSch(fs)
      defaultJSch.addIdentity("/tmp/ssh-keys/id_rsa")
      defaultJSch
    }
  }

  val cb = new TransportConfigCallback() {
    def configure(transport: Transport): Unit = {
      val sshTransport = transport.asInstanceOf[SshTransport]
      sshTransport.setSshSessionFactory(sshSessionFactory)
    }
  }

}

object Request {
  def gatlingStatusFromGit(response: Response): Status = {
    response.status match {
      case OK   => GatlingOK
      case Fail => GatlingFail
    }
  }
}

class PimpedGitTransportCommand[C <: GitCommand[_],T](val c: TransportCommand[C,T]) {
  def setAuthenticationMethod(url: URIish, cb: TransportConfigCallback): C = {
    url.getScheme match {
      case "ssh" => c.setTransportConfigCallback(cb)
      case "http" => c.setCredentialsProvider(new UsernamePasswordCredentialsProvider("user", "password"))
    }
  }
}

object PimpedGitTransportCommand {
  implicit def toPimpedTransportCommand[C <: GitCommand[_],T](s: TransportCommand[C,T]) = new PimpedGitTransportCommand[C,T](s)
}

case class Clone(url: URIish, user: String, tmpPath: String) extends Request {

  val name = s"Clone: $url"
  def send: Unit = {
    import PimpedGitTransportCommand._
    Git.cloneRepository.setAuthenticationMethod(url, cb).setURI(url.toString).setDirectory(workTreeDirectory).call()
  }
}

case class Pull(url: URIish, user: String, tmpPath: String) extends Request {
  override def name: String = s"Pull: $url"

  override def send: Unit = {
    import PimpedGitTransportCommand._
    new Git(repository).pull().setAuthenticationMethod(url, cb).call()
  }
}

case class Push(url: URIish, user: String, tmpPath: String) extends Request {
  override def name: String = s"Push: $url"
  val uniqueSuffix = s"$user - ${LocalDateTime.now}"

  override def send: Unit = {
    import PimpedGitTransportCommand._
    val git = new Git(repository)
    git.add.addFilepattern(s"testfile-$uniqueSuffix").call
    git
      .commit()
      .setMessage(s"Test commit - $uniqueSuffix")
      .call()
    // XXX Make branch configurable
    // XXX Make credential configurable
    git.push
      .setAuthenticationMethod(url, cb)
      .add("HEAD:refs/for/master")
      .call()
  }
}

case class InvalidRequest(url: URIish, user: String, tmpPath: String) extends Request {
  override def name: String = "Invalid Request"

  override def send: Unit = {
    throw new Exception("Invalid Git command type")
  }
}

case class Response(status: ResponseStatus)

sealed trait ResponseStatus
case object OK extends ResponseStatus
case object Fail extends ResponseStatus
