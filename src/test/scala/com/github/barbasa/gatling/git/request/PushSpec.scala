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

import com.github.barbasa.gatling.git.helper.CommitBuilder
import org.apache.commons.io.FileUtils
import org.eclipse.jgit.api.{Git => JGit}
import org.eclipse.jgit.transport.URIish
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

import scala.collection.JavaConverters._

class PushSpec extends FlatSpec with BeforeAndAfter with Matchers with GitTestHelpers {

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

  behavior of "Push"

  "without any error" should "return OK" in {
    val response = Push(new URIish(s"file://$originRepoDirectory"), s"$testUser").send
    response.status shouldBe OK
  }

  it should "push to a new branch" in {
    val pushRef = s"refs/heads/$testBranchName"
    val response = Push(
      new URIish(s"file://$originRepoDirectory"),
      s"$testUser",
      s"HEAD:$pushRef"
    ).send
    response.status shouldBe OK

    val refsList = testGitRepo.branchList().call().asScala
    refsList.map(_.getName) should contain(pushRef)
  }

  it should "push to a the specified directory" in {
    val directoryA = new File(s"${workTreeDirectory.toString}/directoryA")
    directoryA.mkdir
    val commitBuilder = new CommitBuilder(
      fixtures.numberOfFilesPerCommit,
      fixtures.minContentLengthOfCommit,
      fixtures.maxContentLengthOfCommit,
      fixtures.defaultPrefixOfCommit,
      Seq(directoryA)
    )
    val response = Push(
      url = new URIish(s"file://$originRepoDirectory"),
      user = s"$testUser",
      maybeCommitBuilder = Some(commitBuilder)
    ).send

    Thread.sleep(1000)
    response.status shouldBe OK
    getPathsInCommit(getHeadCommit.getTree) should be(List("directoryA"))
  }
}
