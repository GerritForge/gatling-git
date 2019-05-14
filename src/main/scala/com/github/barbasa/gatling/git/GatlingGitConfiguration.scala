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
import org.eclipse.jgit.transport.URIish

@Singleton
case class GatlingGitConfiguration private(
  httpConfiguration: HttpConfiguration,
  sshConfiguration: SshConfiguration,
  tmpBasePath: String
)

case class HttpConfiguration(userName: String, password: String, uri: URIish)
case class SshConfiguration(private_key_path: String, uri: URIish)

object GatlingGitConfiguration {
  private val config = ConfigFactory.load()

  val SSH_SCHEME = "ssh"
  val HTTP_SCHEME = "http"
  val HTTPS_SCHEME = "https"

  def apply(): GatlingGitConfiguration = {
    val httpUserName = config.getString("http.username")
    val httpPassword = config.getString("http.password")

    val tmpBasePath = "/%s/gatling-%d".format(
      config.getString("tmpFiles.basePath"),
      System.currentTimeMillis)

    val httpGitURI = new URIish(config.getString("http.server_uri"))

    val sshPrivateKeyPath = config.getString("ssh.private_ssh_key_path")
    val sshGitURI = new URIish(config.getString("ssh.server_uri"))

    if(!Set(HTTP_SCHEME,HTTPS_SCHEME).contains(httpGitURI.getScheme)) {
      throw new IllegalArgumentException(
        s"http scheme should be $HTTP_SCHEME or $HTTPS_SCHEME, but '${httpGitURI.getScheme}' was provided")
    }

    if(SSH_SCHEME != sshGitURI.getScheme) {
      throw new IllegalArgumentException(
        s"ssh scheme should be $SSH_SCHEME but '${sshGitURI.getScheme}' was provided")
    }

    GatlingGitConfiguration(
      HttpConfiguration(httpUserName, httpPassword, httpGitURI),
      SshConfiguration(sshPrivateKeyPath, sshGitURI),
      tmpBasePath
    )
  }
}
