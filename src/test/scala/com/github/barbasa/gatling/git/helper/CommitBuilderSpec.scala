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

import com.github.barbasa.gatling.git.request.GitTestHelpers
import org.apache.commons.io.FileUtils
import org.scalatest.BeforeAndAfter
import org.eclipse.jgit.api.{Git => JGit}
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import scala.util.Try
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.annotation.nowarn

@nowarn("msg=unused value")
class CommitBuilderSpec extends AnyFlatSpec with BeforeAndAfter with Matchers with GitTestHelpers {
  before {
    FileUtils.deleteDirectory(new File(s"$tempBase/$testUser"))
    testGitRepo = JGit.init.setDirectory(workTreeDirectory()).call
  }

  after {
    testGitRepo.getRepository.close()
    FileUtils.deleteDirectory(new File(s"$tempBase/$testUser"))
  }

  def getHeadCommit: RevCommit = {
    val revCommitT = for {
      repository <- Try(testGitRepo.getRepository)
      head       <- Try(repository.findRef(Constants.HEAD))
      walk       <- Try(new RevWalk(repository))
      revCommit  <- Try(walk.parseCommit(head.getObjectId))
    } yield revCommit
    revCommitT.recover { case e =>
      fail(e.getCause)
    }.get
  }

  behavior of "CommitBuilder"

  "without prefix parameter" should "create and amend commits with Change-Id without prefix" in {

    val commitBuilder = new CommitBuilder(
      fixtures.numberOfFilesPerCommit,
      fixtures.minContentLengthOfCommit,
      fixtures.maxContentLengthOfCommit,
      fixtures.defaultPrefixOfCommit
    )

    commitBuilder.commitToRepository(testGitRepo.getRepository, computeChangeId = true)
    val originalCommit = getHeadCommit

    getHeadCommit.getFullMessage should startWith("Test commit header - ")
    val originalChangeId = getHeadCommit.getFooterLines(CommitBuilder.ChangeIdFooterKey)

    commitBuilder.commitToRepository(testGitRepo.getRepository, amend = true)
    val amendedCommit = getHeadCommit
    getHeadCommit.getFullMessage should startWith("Amended test commit header - ")

    getHeadCommit.getFooterLines(CommitBuilder.ChangeIdFooterKey) should be(originalChangeId)

    amendedCommit.getId should not be (originalCommit.getId)
  }

  "with prefix parameter" should "start with the prefix" in {

    val commitBuilder = new CommitBuilder(
      fixtures.numberOfFilesPerCommit,
      fixtures.minContentLengthOfCommit,
      fixtures.maxContentLengthOfCommit,
      "testPrefix - "
    )
    commitBuilder.commitToRepository(testGitRepo.getRepository)
    getHeadCommit.getFullMessage should startWith("testPrefix - Test commit header - ")
  }

  override def commandName: String = "CommitBuilder"
}
