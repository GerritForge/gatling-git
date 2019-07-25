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

package com.github.barbasa.gatling.git.helper
import java.io.File

import com.github.barbasa.gatling.git.request.{Clone, GitTestHelpers, OK}
import org.apache.commons.io.FileUtils
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}
import org.eclipse.jgit.api.{Git => JGit}
import org.eclipse.jgit.transport.URIish

class CommitBuilderSpec extends FlatSpec with BeforeAndAfter with Matchers with GitTestHelpers {
  before {
    FileUtils.deleteDirectory(originRepoDirectory.getParentFile)
    testGitRepo = JGit.init.setDirectory(originRepoDirectory).call
    val response = Clone(new URIish(s"file://${originRepoDirectory}"), s"$testUser").send
    response.status shouldBe OK
  }

  after {
    testGitRepo.getRepository.close()
    FileUtils.deleteDirectory(originRepoDirectory.getParentFile)
  }

  behavior of "CommitBuilder"

  "without prefix parameter" should "create commits without prefix" in {

    val commitBuilder = new CommitBuilder(
      fixtures.numberOfFilesPerCommit,
      fixtures.minContentLengthOfCommit,
      fixtures.maxContentLengthOfCommit,
      fixtures.defaultPrefixOfCommit,
      Seq(testGitRepo.getRepository.getWorkTree)
    )

    commitBuilder.commitToRepository(testGitRepo.getRepository)
    getHeadCommit.getFullMessage should startWith("Test commit header - ")
  }

  "with prefix parameter" should "start with the prefix" in {

    val commitBuilder = new CommitBuilder(
      fixtures.numberOfFilesPerCommit,
      fixtures.minContentLengthOfCommit,
      fixtures.maxContentLengthOfCommit,
      "testPrefix - ",
      Seq(testGitRepo.getRepository.getWorkTree)
    )
    commitBuilder.commitToRepository(testGitRepo.getRepository)
    getHeadCommit.getFullMessage should startWith("testPrefix - Test commit header - ")

  }

  "with different destination directories" should "place files in different directories - files == directories" in {
    val directoryA = new File(s"${testGitRepo.getRepository.getWorkTree}/directoryA")
    val directoryB = new File(s"${testGitRepo.getRepository.getWorkTree}/directoryB")
    directoryA.mkdir
    directoryB.mkdir
    val directories = Seq(directoryA, directoryB)
    val numFiles    = directories.size
    val commitBuilder = new CommitBuilder(
      numFiles,
      fixtures.minContentLengthOfCommit,
      fixtures.maxContentLengthOfCommit,
      fixtures.defaultPrefixOfCommit,
      directories
    )
    commitBuilder.commitToRepository(testGitRepo.getRepository)
    getPathsInCommit(getHeadCommit.getTree) should be(List("directoryA", "directoryB"))
  }

  "with different destination directories" should "place files in different directories - files < directories" in {
    val directoryA = new File(s"${testGitRepo.getRepository.getWorkTree}/directoryA")
    val directoryB = new File(s"${testGitRepo.getRepository.getWorkTree}/directoryB")
    directoryA.mkdir
    directoryB.mkdir
    val directories = Seq(directoryA, directoryB)
    val numFiles    = directories.size - 1
    val commitBuilder = new CommitBuilder(
      numFiles,
      fixtures.minContentLengthOfCommit,
      fixtures.maxContentLengthOfCommit,
      fixtures.defaultPrefixOfCommit,
      directories
    )
    commitBuilder.commitToRepository(testGitRepo.getRepository)
    getPathsInCommit(getHeadCommit.getTree) should be(List("directoryA"))
  }

  "with different destination directories" should "place files in different directories - files > directories" in {
    val directoryA = new File(s"${testGitRepo.getRepository.getWorkTree}/directoryA")
    val directoryB = new File(s"${testGitRepo.getRepository.getWorkTree}/directoryB")
    directoryA.mkdir
    directoryB.mkdir
    val directories = Seq(directoryA, directoryB)
    val numFiles    = directories.size + 1
    val commitBuilder = new CommitBuilder(
      numFiles,
      fixtures.minContentLengthOfCommit,
      fixtures.maxContentLengthOfCommit,
      fixtures.defaultPrefixOfCommit,
      directories
    )
    commitBuilder.commitToRepository(testGitRepo.getRepository)
    getPathsInCommit(getHeadCommit.getTree) should be(List("directoryA", "directoryB"))
  }

}
