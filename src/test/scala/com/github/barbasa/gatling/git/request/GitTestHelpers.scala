// Copyright (C) 2019 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at

// http://www.apache.org/licenses/LICENSE-2.0

// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

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
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.revwalk.{RevCommit, RevTree, RevWalk}
import org.eclipse.jgit.treewalk.{FileTreeIterator, TreeWalk}
import org.scalatest.FlatSpec

import scala.collection.mutable.ListBuffer
import scala.util.Try
import collection.JavaConverters._

trait GitTestHelpers extends FlatSpec {
  var testGitRepo: JGit = _

  val tempBase: String          = Files.createTempDirectory("gatlingGitTests").toFile.getAbsolutePath
  val testUser: String          = "testUser"
  val testRepo: String          = "testRepo"
  val testBranchName: String    = "mybranch"
  val originRepoDirectory: File = new File(s"$tempBase/$testUser/$testRepo")
  val workTreeDirectory: File   = new File(s"$tempBase/$testUser/$testRepo-worktree")

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

  def getHeadCommit: RevCommit = {
    val repository = testGitRepo.getRepository
    val head       = repository.findRef(Constants.HEAD)
    val walk       = new RevWalk(repository)
    walk.parseCommit(head.getObjectId)
  }

  def getFilePathsInCommit(headCommit: RevTree): List[String] = {
    var committedDirectories = new ListBuffer[String]()
    val treeWalk             = new TreeWalk(testGitRepo.getRepository)
    treeWalk.reset(headCommit)
    treeWalk.setRecursive(true)
    while (treeWalk.next) {
      committedDirectories += treeWalk.getPathString
    }
    committedDirectories.toList
  }
}
