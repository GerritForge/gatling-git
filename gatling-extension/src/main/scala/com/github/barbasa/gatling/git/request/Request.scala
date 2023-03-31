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
import com.github.barbasa.gatling.git.GatlingGitConfiguration
import com.github.barbasa.gatling.git.GitRequestSession.{
  AllRefs,
  EmptyTag,
  HeadToMasterRefSpec,
  MasterRef
}
import com.github.barbasa.gatling.git.helper.CommitBuilder
import com.github.barbasa.gatling.git.request.Request.{addRemote, initRepo}
import com.typesafe.scalalogging.LazyLogging
import io.gatling.commons.stats.{Status, KO => GatlingFail, OK => GatlingOK}
import org.apache.commons.io.FileUtils
import org.eclipse.jgit.api._
import org.eclipse.jgit.lib.Constants.MASTER
import org.eclipse.jgit.lib.{NullProgressMonitor, Repository, TextProgressMonitor}
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport._
import org.eclipse.jgit.transport.sshd.SshdSessionFactory

import java.io.{File, PrintWriter}
import java.nio.file.{Path, Paths}
import java.time.LocalDateTime
import java.util.{List => JavaList}
import scala.jdk.CollectionConverters._
import scala.reflect.io.Directory

sealed trait Request {

  def conf: GatlingGitConfiguration
  def name: String
  def send: GitCommandResponse
  def url: URIish
  def user: String
  val classLoader: ClassLoader = getClass.getClassLoader
  val repoName                 = url.getPath.split("/").last
  lazy val workTreeDirectory   = new File(conf.tmpBasePath + s"/$user/$repoName-worktree")

  private val builder             = new FileRepositoryBuilder
  lazy val repository: Repository = builder.setWorkTree(workTreeDirectory).build()

  val sshSessionFactory: SshSessionFactory = new SshdSessionFactory {
    override protected def getDefaultIdentities(sshDir: File): JavaList[Path] = {
      val defaultIdentities = super.getDefaultIdentities(sshDir)
      defaultIdentities.add(Paths.get(conf.sshConfiguration.private_key_path))
      defaultIdentities
    }
  }

  def progressMonitor =
    if (conf.gitConfiguration.showProgress) new TextProgressMonitor(new PrintWriter(System.out))
    else NullProgressMonitor.INSTANCE

  def cleanRepo() = {
    val deleteCommandResult = new Directory(workTreeDirectory).deleteRecursively()
    if (deleteCommandResult) {
      addRemote(initRepo(workTreeDirectory), url)
    }
    deleteCommandResult
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
  def initRepo(workTreeDirectory: File, initialBranchName: String = MASTER) = {
    Git.init
      .setInitialBranch(initialBranchName)
      .setDirectory(workTreeDirectory)
      .call()
  }

  def gatlingStatusFromGit(response: GitCommandResponse): Status = {
    response.status match {
      case OK   => GatlingOK
      case Fail => GatlingFail
    }
  }

  def addRemote(repo: Git, url: URIish): RemoteConfig = {
    repo
      .remoteAdd()
      .setName("origin")
      .setUri(url)
      .call()
  }
}

case class Clone(url: URIish, user: String, ref: String = MasterRef)(
    implicit val conf: GatlingGitConfiguration
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

    // Clone doesn't have a Result a return value, hence either it works or
    // it will throw an exception
    GitCommandResponse(OK)
  }
}

case class CleanupRepo(url: URIish, user: String)(
    implicit val conf: GatlingGitConfiguration
) extends Request {
  override def name: String = s"Clean local repository $repoName"

  override def send: GitCommandResponse =
    GitCommandResponse {
      if (cleanRepo())
        OK
      else
        Fail
    }
}

case class Fetch(url: URIish, user: String, refSpec: String = AllRefs)(
    implicit val conf: GatlingGitConfiguration
) extends Request {
  addRemote(initRepo(workTreeDirectory), url)

  val name = s"Fetch: $url"

  def send: GitCommandResponse = {
    import PimpedGitTransportCommand._
    val fetchResult = new Git(repository)
      .fetch()
      .setRemote("origin")
      .setRefSpecs(refSpec)
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
  addRemote(initRepo(workTreeDirectory), url)

  override def name: String = s"Pull: $url"

  override def send: GitCommandResponse = {
    import PimpedGitTransportCommand._
    val pullResult = new Git(repository)
      .pull()
      .setAuthenticationMethod(url, cb)
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

case class Push(
    url: URIish,
    user: String,
    refSpec: String = HeadToMasterRefSpec.value,
    commitBuilder: CommitBuilder = Push.defaultCommitBuilder,
    force: Boolean = false,
    computeChangeId: Boolean = false,
    options: List[String] = List.empty
)(
    implicit val conf: GatlingGitConfiguration
) extends Request {

  override def name: String = s"Push: $url"
  val uniqueSuffix          = s"$user - ${LocalDateTime.now}"

  override def send: GitCommandResponse = {
    import PimpedGitTransportCommand._

    val git = {
      if (!workTreeDirectory.exists()) addRemote(initRepo(workTreeDirectory), url)
      Git.open(workTreeDirectory)
    }
    val isSrcDstRefSpec: String => Boolean = _.contains(":") // e.g. HEAD:refs/for/master

    // TODO: Create multiple commits per push
    commitBuilder.commitToRepository(
      repository,
      Option(refSpec).filterNot(isSrcDstRefSpec),
      computeChangeId
    )

    // XXX Make credential configurable
    val basePushCommand: PushCommand =
      git.push
        .setAuthenticationMethod(url, cb)
        .setRemote(url.toString)
        .add(refSpec)
        .setTimeout(conf.gitConfiguration.commandTimeout)
        .setProgressMonitor(progressMonitor)
        .setForce(force)

    val pushResults =
      if (url.getScheme == "file")
        basePushCommand.call()
      else
        basePushCommand.setPushOptions(options.asJava).call()

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

case class Tag(
    url: URIish,
    user: String,
    refSpec: String = HeadToMasterRefSpec.value,
    tag: String = EmptyTag.value
)(
    implicit val conf: GatlingGitConfiguration
) extends Request
    with LazyLogging {
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

    git.tag().setName(tag).setObjectId(headCommit).call()
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
