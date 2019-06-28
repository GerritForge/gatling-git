package com.github.barbasa.gatling.git.request

import java.io.File

import com.github.barbasa.gatling.git.{
  CommandsConfiguration,
  GatlingGitConfiguration,
  HttpConfiguration,
  PushConfiguration,
  SshConfiguration
}
import org.eclipse.jgit.api.{Git => JGit}

trait GitTestHelpers {
  var testGitRepo: JGit = _

  val tempBase: String        = "/tmp"
  val testUser: String        = "testUser"
  val testRepo: String        = "testRepo"
  val workTreeDirectory: File = new File(s"$tempBase/$testUser/$testRepo")

  implicit val gatlingConfig = GatlingGitConfiguration(
    HttpConfiguration("userName", "password"),
    SshConfiguration("/tmp/keys"),
    tempBase,
    CommandsConfiguration(PushConfiguration(1, 2, 3))
  )
}
