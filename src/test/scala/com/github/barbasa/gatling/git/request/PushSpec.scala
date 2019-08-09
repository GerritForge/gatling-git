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

import org.apache.commons.io.FileUtils
import org.eclipse.jgit.api.{Git => JGit}
import org.eclipse.jgit.transport.URIish
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

import scala.collection.JavaConverters._

class PushSpec extends FlatSpec with BeforeAndAfter with Matchers with GitTestHelpers {

  before {
    FileUtils.deleteDirectory(originRepoDirectory.getParentFile)
    testGitRepo = JGit.init.setDirectory(originRepoDirectory).call
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

  "with an error" should "return Fail" in {
    // Force non-ff failure:
    // 1. Clone a repo for user-1
    Clone(new URIish(s"file://${originRepoDirectory}"), s"$testUser-1").send
    // 2. Push a change from user-2
    Push(new URIish(s"file://$originRepoDirectory"), s"$testUser-2").send
    // 3. Push a change from user-1 without fetching remote
    val response = Push(new URIish(s"file://$originRepoDirectory"), s"$testUser-1").send
    response.status shouldBe Fail
    response.message shouldBe Some("Status: REJECTED_NONFASTFORWARD - Message: null")
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
}
