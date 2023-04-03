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

import com.github.barbasa.gatling.git.request.Request.initRepo
import org.apache.commons.io.FileUtils
import org.eclipse.jgit.api.{Git => JGit}
import org.eclipse.jgit.transport.URIish
import org.scalatest.BeforeAndAfter

import scala.annotation.nowarn
import scala.jdk.CollectionConverters._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

@nowarn("msg=unused value")
class CloneSpec extends AnyFlatSpec with BeforeAndAfter with Matchers with GitTestHelpers {

  before {
    FileUtils.deleteDirectory(originRepoDirectory.getParentFile)
    testGitRepo = initRepo(originRepoDirectory)
    testGitRepo.commit().setMessage("Initial Commit").call()
    testGitRepo.branchCreate().setName(testBranchName).call()
  }

  after {
    testGitRepo.getRepository.close()
    FileUtils.deleteDirectory(originRepoDirectory.getParentFile)
  }

  behavior of "Clone"

  "without any error" should "return OK" in {
    val cmd      = Clone(new URIish(s"file://${originRepoDirectory}"), s"$testUser")
    val response = cmd.send
    response.status shouldBe OK
  }

  it should "clone a specific ref" in {
    val suffix = System.nanoTime().toString
    val response =
      Clone(
        new URIish(s"file://${originRepoDirectory}"),
        s"$testUser",
        s"$testBranchName",
        suffix
      ).send
    response.status shouldBe OK

    val refsList = JGit.open(workTreeDirectory(s"-$suffix")).branchList().call().asScala
    refsList.map(_.getName) should contain(s"refs/heads/$testBranchName")
  }

  override def commandName: String = "Clone"
}
