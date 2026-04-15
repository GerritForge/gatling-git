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

package com.github.barbasa.gatling.git.request

import com.github.barbasa.gatling.git.request.Request.initRepo
import org.apache.commons.io.FileUtils
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Constants.{MASTER, R_HEADS}
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.transport.URIish
import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.annotation.nowarn

@nowarn("msg=unused value")
class MergeSpec extends AnyFlatSpec with BeforeAndAfter with Matchers with GitTestHelpers {

  before {
    FileUtils.deleteDirectory(originRepoDirectory.getParentFile)
    testGitRepo = initRepo(originRepoDirectory)
  }

  after {
    testGitRepo.getRepository.close()
    FileUtils.deleteDirectory(originRepoDirectory.getParentFile)
  }

  behavior of "Merge"

  "with an existing local source branch" should "create a merge commit on the target branch" in {
    val repoDir = Option(workTreeDirectory().toString)
    val origin  = new URIish(s"file://$originRepoDirectory")

    Push(origin, testUser, repoDirOverride = repoDir).send.status shouldBe OK
    Git
      .open(workTreeDirectory())
      .checkout()
      .setCreateBranch(true)
      .setName(testBranchName)
      .setStartPoint(MASTER)
      .call()
    Push(
      origin,
      testUser,
      refSpec = testBranchName,
      repoDirOverride = repoDir
    ).send.status shouldBe OK

    val response = Merge(
      origin,
      testUser,
      sourceRef = testBranchName,
      targetRef = MASTER,
      repoDirOverride = repoDir
    ).send

    response.status shouldBe OK

    val masterHead = testGitRepo.getRepository.exactRef(s"$R_HEADS$MASTER").getObjectId
    val revWalk    = new RevWalk(testGitRepo.getRepository)
    val mergeCommit =
      try {
        revWalk.parseCommit(masterHead)
      } finally {
        revWalk.close()
      }

    mergeCommit.getParentCount shouldBe 2
  }

  override def commandName: String = "Merge"
}
