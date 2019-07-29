package com.github.barbasa.gatling.git.request

import java.io.File
import java.nio.file.Files

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

  val tempBase: String        = Files.createTempDirectory("gatlinGitTests").toFile.getAbsolutePath
  val testUser: String        = "testUser"
  val testRepo: String        = "testRepo"
  val testRefName: String      = "refs/heads/mybranch"
  val workTreeDirectory: File = new File(s"$tempBase/$testUser/$testRepo")

  val defaultPushConfiguration = PushConfiguration(
    PushConfiguration.DEFAULT_NUM_FILES,
    PushConfiguration.DEFAULT_MIN_CONTENT_LENGTH,
    PushConfiguration.DEFAULT_MAX_CONTENT_LENGTH,
    PushConfiguration.DEFAULT_COMMIT_PREFIX
  )

  implicit val gatlingConfig = GatlingGitConfiguration(
    HttpConfiguration("userName", "password"),
    SshConfiguration("/tmp/keys"),
    tempBase,
    CommandsConfiguration(defaultPushConfiguration)
  )

  object fixtures {
    val numberOfFilesPerCommit   = 2
    val minContentLengthOfCommit = 5
    val maxContentLengthOfCommit = 10
    val defaultPrefixOfCommit    = ""
  }
}
