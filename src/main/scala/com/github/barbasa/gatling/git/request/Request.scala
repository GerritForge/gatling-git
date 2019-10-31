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
import java.io.{File, PrintWriter}
import java.nio.file.{Files, Paths}
import java.time.LocalDateTime

import com.github.barbasa.gatling.git.{GatlingGitConfiguration, GitRequestSession}
import com.github.barbasa.gatling.git.helper.CommitBuilder
import com.jcraft.jsch.JSch
import com.jcraft.jsch.{Session => SSHSession}
import io.gatling.commons.stats.{OK => GatlingOK}
import io.gatling.commons.stats.{KO => GatlingFail}
import io.gatling.commons.stats.Status
import org.apache.commons.io.FileUtils
import org.eclipse.jgit.api._
import org.eclipse.jgit.lib.{NullProgressMonitor, Repository, TextProgressMonitor}
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport._
import org.eclipse.jgit.transport.JschConfigSessionFactory
import org.eclipse.jgit.transport.OpenSshConfig
import org.eclipse.jgit.transport.SshSessionFactory
import org.eclipse.jgit.util.FS
import org.eclipse.jgit.transport.SshTransport
import org.eclipse.jgit.hooks._
import GitRequestSession.{EmptyTag, HeadToMasterRefSpec, MasterRef}
import com.typesafe.scalalogging.LazyLogging
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.eclipse.jgit.revwalk.RevWalk

import collection.JavaConverters._
import scala.util.{Failure, Success, Try}

sealed trait Request {

  def conf: GatlingGitConfiguration
  def name: String
  def send: GitCommandResponse
  def url: URIish
  def user: String
  val classLoader: ClassLoader = getClass.getClassLoader
  private val repoName         = url.getPath.split("/").last
  val workTreeDirectory: File  = new File(conf.tmpBasePath + s"/$user/$repoName-worktree")
  private val builder          = new FileRepositoryBuilder
  workTreeDirectory.mkdirs()
  val repository: Repository   = builder.setWorkTree(workTreeDirectory).build()

  val sshSessionFactory: SshSessionFactory = new JschConfigSessionFactory() {
    protected def configure(host: OpenSshConfig.Host, session: SSHSession): Unit = {}

    override protected def createDefaultJSch(fs: FS): JSch = {
      val defaultJSch = super.createDefaultJSch(fs)
      defaultJSch.addIdentity(conf.sshConfiguration.private_key_path)
      defaultJSch
    }
  }

  def progressMonitor = if(conf.gitConfiguration.showProgress) new TextProgressMonitor(new PrintWriter(System.out)) else  NullProgressMonitor.INSTANCE

  def initRepo() = {
    Git.init
      .setDirectory(workTreeDirectory)
      .call()
      .remoteAdd()
      .setName("origin")
      .setUri(url)
      .call()
  }

  val cb = new TransportConfigCallback() {
    def configure(transport: Transport): Unit = {
      val sshTransport = transport.asInstanceOf[SshTransport]
      sshTransport.setSshSessionFactory(sshSessionFactory)
    }
  }

  class PimpedGitTransportCommand[C <: GitCommand[_], T](val c: TransportCommand[C, T]) {
    def setAuthenticationMethod(url: URIish, cb: TransportConfigCallback): C = {
      url.getScheme match {
        case "ssh" => c.setTransportConfigCallback(cb)
        case "http" | "https" =>
          c.setCredentialsProvider(
            new UsernamePasswordCredentialsProvider(
              conf.httpConfiguration.userName,
              conf.httpConfiguration.password
            )
          )
        case "file" =>
          c.setTransportConfigCallback((transport: Transport) => {
            println("Noop: writing on file")
          })
      }
    }
  }

  object PimpedGitTransportCommand {
    implicit def toPimpedTransportCommand[C <: GitCommand[_], T](s: TransportCommand[C, T]) =
      new PimpedGitTransportCommand[C, T](s)
  }

}

object Request {
  def gatlingStatusFromGit(response: GitCommandResponse): Status = {
    response.status match {
      case OK   => GatlingOK
      case Fail => GatlingFail
    }
  }
}

case class Clone(url: URIish, user: String, ref: String = MasterRef)(
    implicit val conf: GatlingGitConfiguration,
    val postMsgHook: Option[String] = None
) extends Request {

  val name = s"Clone: $url"

  FileUtils.deleteDirectory(workTreeDirectory)

  def send: GitCommandResponse = {
    import PimpedGitTransportCommand._
    Git.cloneRepository
      .setAuthenticationMethod(url, cb)
      .setURI(url.toString)
      .setDirectory(workTreeDirectory)
      .setBranch(ref)
      .setProgressMonitor(progressMonitor)
      .setTimeout(conf.gitConfiguration.commandTimeout)
      .call()

    postMsgHook.foreach { sourceCommitMsgFile =>
      val sourceCommitMsgPath =
        new File(classLoader.getResource(sourceCommitMsgFile).getPath).toPath

      val destinationCommitMsgPath =
        Paths.get(workTreeDirectory.getAbsolutePath, s".git/hooks/${CommitMsgHook.NAME}")
      new File(Files.copy(sourceCommitMsgPath, destinationCommitMsgPath).toString)
        .setExecutable(true)
    }

    // Clone doesn't have a Result a return value, hence either it works or
    // it will throw an exception
    GitCommandResponse(OK)
  }
}

case class Fetch(url: URIish, user: String)(implicit val conf: GatlingGitConfiguration)
    extends Request {
  initRepo()

  val name = s"Fetch: $url"

  def send: GitCommandResponse = {
    import PimpedGitTransportCommand._
    val fetchResult = new Git(repository)
      .fetch()
      .setRemote("origin")
      .setRefSpecs("+refs/*:refs/*")
      .setAuthenticationMethod(url, cb)
      .setTimeout(conf.gitConfiguration.commandTimeout)
      .setProgressMonitor(progressMonitor)
      .call()

    if (fetchResult.getAdvertisedRefs.size() > 0) {
      GitCommandResponse(OK)
    } else {
      GitCommandResponse(Fail)
    }
  }
}

case class Pull(url: URIish, user: String)(implicit val conf: GatlingGitConfiguration)
    extends Request {
  initRepo()

  override def name: String = s"Pull: $url"

  override def send: GitCommandResponse = {
    import PimpedGitTransportCommand._
    val pullResult = new Git(repository)
      .pull().setAuthenticationMethod(url, cb)
      .setTimeout(conf.gitConfiguration.commandTimeout)
      .setProgressMonitor(progressMonitor)
      .call()

    if (pullResult.isSuccessful) {
      GitCommandResponse(OK)
    } else {
      GitCommandResponse(Fail, Some(pullResult.toString))
    }
  }
}

case class Push(url: URIish,
                user: String,
                refSpec: String = HeadToMasterRefSpec.value,
                commitBuilder: CommitBuilder = Push.defaultCommitBuilder)(
    implicit val conf: GatlingGitConfiguration
) extends Request {
  initRepo()

  override def name: String = s"Push: $url"
  val uniqueSuffix          = s"$user - ${LocalDateTime.now}"

  override def send: GitCommandResponse = {
    import PimpedGitTransportCommand._
    val git = new Git(repository)

    // TODO: Create multiple commits per push
    commitBuilder.commitToRepository(repository)

    // XXX Make branch configurable
    // XXX Make credential configurable
    val pushResults = git.push
      .setAuthenticationMethod(url, cb)
      .setRemote(url.toString)
      .add(refSpec)
      .setTimeout(conf.gitConfiguration.commandTimeout)
      .setProgressMonitor(progressMonitor)
      .call()

    val maybeRemoteRefUpdate = pushResults.asScala
      .flatMap { pushResult =>
        pushResult.getRemoteUpdates.asScala
      }
      .find(
        remoteRefUpdate =>
          Seq(
            RemoteRefUpdate.Status.REJECTED_OTHER_REASON,
            RemoteRefUpdate.Status.REJECTED_NONFASTFORWARD,
            RemoteRefUpdate.Status.REJECTED_NODELETE,
            RemoteRefUpdate.Status.NON_EXISTING,
            RemoteRefUpdate.Status.REJECTED_REMOTE_CHANGED
          ).contains(remoteRefUpdate.getStatus)
      )

    maybeRemoteRefUpdate.fold(GitCommandResponse(OK))(
      remoteRefUpdate =>
        GitCommandResponse(
          Fail,
          Some(
            s"Status: ${remoteRefUpdate.getStatus.toString} - Message: ${remoteRefUpdate.getMessage}"
          )
        )
    )
  }
}

case class Tag(url: URIish,
               user: String,
               refSpec: String = HeadToMasterRefSpec.value,
               tag: String = EmptyTag.value)(
                implicit val conf: GatlingGitConfiguration
              ) extends Request with LazyLogging {
  override def name: String = s"Push: $url"

  val uniqueSuffix = s"$user - ${LocalDateTime.now}"

  override def send: GitCommandResponse = {
    import PimpedGitTransportCommand._
    val git = Git.init().setDirectory(workTreeDirectory).call()
    git.remoteAdd().setName("origin").setUri(url).call()

    val fetchResult = git
      .fetch()
      .setRemote("origin")
      .setRefSpecs(refSpec)
      .setAuthenticationMethod(url, cb)
      .setTimeout(conf.gitConfiguration.commandTimeout)
      .setProgressMonitor(progressMonitor)
      .call()

    val fetchHead = fetchResult.getAdvertisedRef(refSpec)

    val revWalk = new RevWalk(git.getRepository)
    val headCommit = try {
      revWalk.parseAny(fetchHead.getObjectId)
    } finally {
      revWalk.close()
    }

    val tagResult = git.tag().setName(tag).setObjectId(headCommit).call()
    val pushResult = git
      .push()
      .setRemote("origin")
      .setRefSpecs(new RefSpec(s"refs/tags/${tag}"))
      .setAuthenticationMethod(url, cb)
      .setTimeout(conf.gitConfiguration.commandTimeout)
      .setProgressMonitor(progressMonitor)
      .call()
      .asScala

    if (!pushResult.isEmpty) {
      GitCommandResponse(OK)
    } else {
      GitCommandResponse(Fail)
    }
  }
}

object Push {
  val conf = GatlingGitConfiguration()
  val defaultCommitBuilder = new CommitBuilder(
    conf.commands.pushConfig.numFiles,
    conf.commands.pushConfig.minContentLength,
    conf.commands.pushConfig.maxContentLength,
    conf.commands.pushConfig.commitPrefix
  )
}

case class InvalidRequest(url: URIish, user: String)(implicit val conf: GatlingGitConfiguration)
    extends Request {
  override def name: String = "Invalid Request"

  override def send: GitCommandResponse = {
    throw new Exception("Invalid Git command type")
  }
}

case class GitCommandResponse(status: GitCommandResponseStatus, message: Option[String] = None)

sealed trait GitCommandResponseStatus
case object OK   extends GitCommandResponseStatus
case object Fail extends GitCommandResponseStatus
