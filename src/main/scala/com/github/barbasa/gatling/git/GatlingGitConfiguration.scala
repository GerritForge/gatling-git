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
import com.typesafe.config.ConfigFactory

@Singleton
case class GatlingGitConfiguration private(
  httpConfiguration: HttpConfiguration,
  sshConfiguration: SshConfiguration,
  tmpBasePath: String
)

case class HttpConfiguration(userName: String, password: String)
case class SshConfiguration(private_key_path: String)

object GatlingGitConfiguration {
  private val config = ConfigFactory.load()

  def apply(): GatlingGitConfiguration = {
    val httpUserName = config.getString("http.username")
    val httpPassword = config.getString("http.password")
    val testDataDirectory: String =
      if (config.hasPath("tmpFiles.testDataDirectory")) {
        config.getString("tmpFiles.testDataDirectory")
      } else {
        System.currentTimeMillis.toString
      }
    val tmpBasePath = "/%s/gatling-%s".format(
      config.getString("tmpFiles.basePath"),
      testDataDirectory)

    val sshPrivateKeyPath = config.getString("ssh.private_key_path")

    GatlingGitConfiguration(
      HttpConfiguration(httpUserName, httpPassword),
      SshConfiguration(sshPrivateKeyPath),
      tmpBasePath
    )
  }
}
