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

package com.github.barbasa.gatling.git

import com.google.inject.Singleton
import com.typesafe.config.{Config, ConfigFactory}

@Singleton
case class GatlingGitConfiguration private (
    gitConfiguration: GitConfiguration,
    httpConfiguration: HttpConfiguration,
    sshConfiguration: SshConfiguration,
    tmpBasePath: String,
    commands: CommandsConfiguration
)
case class GitConfiguration(commandTimeout: Int, showProgress: Boolean)

object GitConfiguration {
  val DEFAULT_TIMEOUT       = 30
  val DEFAULT_SHOW_PROGRESS = true
}

case class HttpConfiguration(userName: String, password: String)
case class SshConfiguration(private_key_path: String)
case class PushConfiguration(
    numFiles: Int,
    minContentLength: Int,
    maxContentLength: Int,
    commitPrefix: String
)
object PushConfiguration {
  val DEFAULT_NUM_FILES          = 4
  val DEFAULT_MIN_CONTENT_LENGTH = 100
  val DEFAULT_MAX_CONTENT_LENGTH = 10000
  val DEFAULT_COMMIT_PREFIX      = ""
}

case class CommandsConfiguration(pushConfig: PushConfiguration)

object GatlingGitConfiguration {
  private val config = ConfigFactory.load()

  implicit class RichConfig(val config: Config) extends AnyVal {
    def optionalInt(path: String): Option[Int] =
      if (config.hasPath(path)) {
        Some(config.getInt(path))
      } else {
        None
      }

    def optionalString(path: String): Option[String] =
      if (config.hasPath(path)) {
        Some(config.getString(path))
      } else {
        None
      }

    def optionalBoolean(path: String): Option[Boolean] =
      if (config.hasPath(path)) {
        Some(config.getBoolean(path))
      } else {
        None
      }

  }

  def apply(): GatlingGitConfiguration = {
    val gitCommandTimeout =
      if (config.hasPath("git.commandTimeout")) config.getInt("git.commandTimeout")
      else GitConfiguration.DEFAULT_TIMEOUT

    val gitShowProgress = config
      .optionalBoolean("git.showProgress")
      .getOrElse(GitConfiguration.DEFAULT_SHOW_PROGRESS)

    val httpUserName = config.getString("http.username")
    val httpPassword = config.getString("http.password")
    val testDataDirectory: String =
      if (config.hasPath("tmpFiles.testDataDirectory")) {
        config.getString("tmpFiles.testDataDirectory")
      } else {
        System.currentTimeMillis.toString
      }
    val tmpBasePath =
      "/%s/gatling-%s".format(config.getString("tmpFiles.basePath"), testDataDirectory)

    val sshPrivateKeyPath = config.getString("ssh.private_key_path")

    val numFiles = config
      .optionalInt("commands.push.numFiles")
      .getOrElse(PushConfiguration.DEFAULT_NUM_FILES)
    val minContentLength = config
      .optionalInt("commands.push.minContentLength")
      .getOrElse(PushConfiguration.DEFAULT_MIN_CONTENT_LENGTH)
    val maxContentLength = config
      .optionalInt("commands.push.maxContentLength")
      .getOrElse(PushConfiguration.DEFAULT_MAX_CONTENT_LENGTH)
    //XXX: Missing validation on parameters, i.e.: values >0. max > min
    val commitPrefix = config
      .optionalString("commands.push.commitPrefix")
      .getOrElse(PushConfiguration.DEFAULT_COMMIT_PREFIX)

    GatlingGitConfiguration(
      GitConfiguration(commandTimeout = gitCommandTimeout, showProgress = gitShowProgress),
      HttpConfiguration(httpUserName, httpPassword),
      SshConfiguration(sshPrivateKeyPath),
      tmpBasePath,
      CommandsConfiguration(
        PushConfiguration(numFiles, minContentLength, maxContentLength, commitPrefix)
      )
    )
  }
}
