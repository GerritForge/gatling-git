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
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.transport.URIish
import org.scalatest.BeforeAndAfter

import scala.annotation.nowarn
import scala.jdk.CollectionConverters._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.nio.file.Files

@nowarn("msg=unused value")
class CloneSpec extends AnyFlatSpec with BeforeAndAfter with Matchers with GitTestHelpers {

  before {
    FileUtils.deleteDirectory(originRepoDirectory.getParentFile)
    testGitRepo = initRepo(originRepoDirectory)
    testGitRepo.commit().setMessage("Initial Commit").call()
    testGitRepo.branchCreate().setName(testBranchName).call()
    testGitRepo.branchCreate().setName(secondTestBranchName).call()
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

  it should "cleanup the working directory on exit" in {
    val suffix = System.nanoTime().toString
    val response =
      Clone(
        new URIish(s"file://${originRepoDirectory}"),
        s"$testUser",
        s"$testBranchName",
        suffix,
        deleteWorkdirOnExit = true
      ).send
    response.status shouldBe OK

    val workTreePath = workTreeDirectory(s"-$suffix")
    workTreePath.exists() shouldBe false
  }

  it should "should specify target working directory" in {
    val workDir = Files.createTempDirectory("clonespec")
    val response = Clone(
      new URIish(s"file://${originRepoDirectory}"),
      s"$testUser",
      s"$testBranchName",
      repoDirOverride = Some(workDir.toString)
    ).send
    response.status shouldBe OK

    val gitWorkDir = JGit.open(workDir.toFile)
    gitWorkDir.status().call().isClean shouldBe true
  }

  "clone mirror" should "clone all the references from the origin" in {
    val workDirFirstClone = Files.createTempDirectory("firstClone")
    val firstClone = Clone(
      new URIish(s"file://${originRepoDirectory}"),
      s"$testUser",
      s"$testBranchName",
      repoDirOverride = Some(workDirFirstClone.toString)
    ).send
    firstClone.status shouldBe OK

    val firstChange =
      Push(
        url = new URIish(s"file://$originRepoDirectory"),
        user = s"$testUser",
        refSpec = s"HEAD:refs/some-namespace/$testBranchName",
        computeChangeId = true,
        maybeResetTo = s"${Constants.DEFAULT_REMOTE_NAME}/$testBranchName",
        repoDirOverride = Some(workDirFirstClone.toString)
      ).send
    firstChange.status shouldBe OK

    val workDirSecondClone = Files.createTempDirectory("secondClone")
    val secondCloneWithMirror = Clone(
      url = new URIish(s"file://${originRepoDirectory}"),
      user = s"$testUser",
      ref = Constants.MASTER,
      repoDirOverride = Some(workDirSecondClone.toString),
      mirror = true
    ).send
    secondCloneWithMirror.status shouldBe OK

    val git = JGit.open(workDirSecondClone.toFile)
    val localRefs =
      git.getRepository.getRefDatabase.getRefs.asScala.map(ref => (ref.getName, ref.getObjectId))
    val remoteRefs = git.lsRemote().call().asScala.map(ref => (ref.getName, ref.getObjectId))
    localRefs should contain theSameElementsAs (remoteRefs)
    FileUtils.deleteDirectory(workDirFirstClone.toFile)
    FileUtils.deleteDirectory(workDirSecondClone.toFile)
  }

  "clone without mirror" should "clone only refs/heads/*" in {
    val workDirFirstClone = Files.createTempDirectory("firstClone")
    val firstClone = Clone(
      url = new URIish(s"file://${originRepoDirectory}"),
      user = s"$testUser",
      ref = Constants.MASTER,
      repoDirOverride = Some(workDirFirstClone.toString)
    ).send
    firstClone.status shouldBe OK

    val git = JGit.open(workDirFirstClone.toFile)
    val localRefs =
      git.getRepository.getRefDatabase.getRefs.asScala.map(ref => (ref.getName, ref.getObjectId))
    val remoteRefs = git.lsRemote().call().asScala.map(ref => (ref.getName, ref.getObjectId))
    localRefs should not contain theSameElementsAs(remoteRefs)
    FileUtils.deleteDirectory(workDirFirstClone.toFile)
  }

  it should "clone just one branch" in {
    val workDir = Files.createTempDirectory("oneBranch")
    val cloneOneBranch = Clone(
      url = new URIish(s"file://${originRepoDirectory}"),
      user = s"$testUser",
      ref = s"$testBranchName",
      repoDirOverride = Some(workDir.toString),
      refsToClone = Set(s"refs/heads/$testBranchName", s"refs/heads/$secondTestBranchName")
    ).send
    cloneOneBranch.status shouldBe OK

    val git       = JGit.open(workDir.toFile)
    val localRefs = git.getRepository.getRefDatabase.getRefs.asScala.map(_.getName)
    localRefs should contain theSameElementsAs
      Set(
        "HEAD",
        s"refs/heads/$testBranchName",
        s"refs/remotes/origin/$testBranchName",
        s"refs/remotes/origin/$secondTestBranchName"
      )
    FileUtils.deleteDirectory(workDir.toFile)
  }

  it should "clone all the branches" in {
    val workDir = Files.createTempDirectory("allBranch")
    val cloneOneBranch = Clone(
      url = new URIish(s"file://${originRepoDirectory}"),
      user = s"$testUser",
      ref = s"$testBranchName",
      repoDirOverride = Some(workDir.toString),
      refsToClone = Set.empty
    ).send
    cloneOneBranch.status shouldBe OK

    val git       = JGit.open(workDir.toFile)
    val localRefs = git.getRepository.getRefDatabase.getRefs.asScala.map(_.getName)
    localRefs should contain theSameElementsAs
      Set(
        "HEAD",
        s"refs/heads/$testBranchName",
        s"refs/remotes/origin/${Constants.MASTER}",
        s"refs/remotes/origin/$testBranchName",
        s"refs/remotes/origin/$secondTestBranchName"
      )
    FileUtils.deleteDirectory(workDir.toFile)
  }

  override def commandName: String = "Clone"
}
