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
import com.github.barbasa.gatling.git.request.Request.initRepo
import org.apache.commons.io.FileUtils
import org.eclipse.jgit.lib.Constants.R_HEADS
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.transport.URIish
import org.scalatest.BeforeAndAfter

import scala.annotation.nowarn
import scala.jdk.CollectionConverters._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

@nowarn("msg=unused value")
class PushSpec extends AnyFlatSpec with BeforeAndAfter with Matchers with GitTestHelpers {

  before {
    FileUtils.deleteDirectory(originRepoDirectory.getParentFile)
    testGitRepo = initRepo(originRepoDirectory)
  }

  after {
    testGitRepo.getRepository.close()
    FileUtils.deleteDirectory(originRepoDirectory.getParentFile)
  }

  behavior of "Push"

  "with an non-ff rejection error" should "return Fail" in {
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

  "with a forced-push" should "return OK" in {
    // Force non-ff failure:
    // 1. Clone a repo for user-1
    Clone(new URIish(s"file://${originRepoDirectory}"), s"$testUser-1").send
    // 2. Push a change from user-2
    Push(new URIish(s"file://$originRepoDirectory"), s"$testUser-2").send
    // 3. Push a change from user-1 without fetching remote
    val response =
      Push(new URIish(s"file://$originRepoDirectory"), s"$testUser-1", force = true).send
    response.status shouldBe OK
  }

  "without any error" should "return OK" in {
    val response = Push(new URIish(s"file://$originRepoDirectory"), s"$testUser").send
    response.status shouldBe OK
  }

  "with a branch ref-spec" should "push to a new branch" in {
    val pushRef = testBranchName
    val response = Push(
      new URIish(s"file://$originRepoDirectory"),
      s"$testUser",
      s"$pushRef"
    ).send
    response.status shouldBe OK

    testGitRepo.branchList.call.asScala.map(_.getName) should contain(s"$R_HEADS$pushRef")
  }

  "with a branch ref-spec" should "push to an existing branch" in {
    val pushRef = testBranchName
    val push = Push(
      new URIish(s"file://$originRepoDirectory"),
      s"$testUser",
      s"$pushRef"
    )

    push.send.status shouldBe OK
    push.send.status shouldBe OK

    testGitRepo.branchList.call.asScala.map(_.getName) should contain(s"$R_HEADS$pushRef")
  }

  "with a branch and computing a Change-Id" should "create a commit and a new patch-set ready for review" in {
    val basePush =
      Push(new URIish(s"file://$originRepoDirectory"), s"$testUser", refSpec = testBranchName)
    basePush.send.status shouldBe OK
    basePush.copy(computeChangeId = true).send.status shouldBe OK

    def getHead = testGitRepo.getRepository.exactRef(s"$R_HEADS$testBranchName")
    def getChangeIdFooterLine(commit: RevCommit) =
      commit.getFooterLines.asScala.find(_.matches(CommitBuilder.ChangeIdFooterKey))

    val newCommit      = testGitRepo.getRepository.parseCommit(getHead.getObjectId)
    val changeIdFooter = getChangeIdFooterLine(newCommit)
    changeIdFooter should not be (empty)
    val parentCommits = newCommit.getParents
    parentCommits should not be (empty)

    newCommit.getFullMessage should include("Change-Id: I")
    newCommit.getParents should not be empty
    basePush.copy(force = true, createNewPatchset = true).send.status shouldBe OK
    val newPatchSet = testGitRepo.getRepository.parseCommit(getHead.getObjectId)

    newPatchSet.getParents should be(parentCommits)
    newPatchSet.getId should not be newCommit.getId
    getChangeIdFooterLine(newPatchSet).toString should be(changeIdFooter.toString)
  }

  it should "not fail if push options are added over local protocol" in {
    val basePush =
      Push(
        new URIish(s"file://$originRepoDirectory"),
        s"$testUser",
        refSpec = testBranchName,
        options = List("testKey=testValue")
      )
    basePush.send.status shouldBe OK
  }

  override def commandName: String = "Push"
}
