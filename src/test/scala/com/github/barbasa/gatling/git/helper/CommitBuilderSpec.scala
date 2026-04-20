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
import com.github.barbasa.gatling.git.request.GitTestHelpers
import org.apache.commons.io.FileUtils
import org.scalatest.BeforeAndAfterEach
import org.eclipse.jgit.api.{Git => JGit}
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.treewalk.TreeWalk

import scala.util.{Try, Using}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.TryValues._

import scala.annotation.{nowarn, tailrec}

@nowarn("msg=unused value")
class CommitBuilderSpec
    extends AnyFlatSpec
    with BeforeAndAfterEach
    with Matchers
    with GitTestHelpers {
  override def beforeEach(): Unit = {
    val worktreeFile = workTreeDirectory()
    FileUtils.deleteDirectory(worktreeFile)
    testGitRepo = JGit.init.setDirectory(worktreeFile).call
  }

  override def afterEach(): Unit = {
    testGitRepo.getRepository.close()
    FileUtils.deleteDirectory(workTreeDirectory())
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
      fixtures.defaultPrefixOfCommit,
      fixtures.filenamePrefix,
      fixtures.filenameExt,
      fixtures.totalNumFiles
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
      "testPrefix - ",
      fixtures.filenamePrefix,
      fixtures.filenameExt,
      fixtures.totalNumFiles
    )
    commitBuilder.commitToRepository(testGitRepo.getRepository)
    getHeadCommit.getFullMessage should startWith("testPrefix - Test commit header - ")
  }

  "with number of files parameter" should "create a commit with the requested number of files" in {
    val numFilesPerCommit = 20
    val commitBuilder     = newCommitBuilder().copy(numFiles = numFilesPerCommit)

    commitBuilder.commitToRepository(testGitRepo.getRepository)
    getCommitTreeContent should have size numFilesPerCommit.toLong
  }

  it should "create different set of files per commit" in {
    val commitBuilder = newCommitBuilder()

    commitBuilder.commitToRepository(testGitRepo.getRepository)
    val firstCommitPaths = getCommitTreeContent
    commitBuilder.commitToRepository(testGitRepo.getRepository)
    val secondCommitPaths = getCommitTreeContent

    firstCommitPaths should not contain allElementsOf(secondCommitPaths)
  }

  it should "create files with different content length" in {
    newCommitBuilder().commitToRepository(testGitRepo.getRepository)
    val contentSizes = getCommitTreeContent.map(_.contentLen)
    contentSizes.distinct.size should be > 1
  }

  it should "not exceed the limit of total number of generated files" in {
    val numCommits    = 10
    val totalNumFiles = (fixtures.numberOfFilesPerCommit * numCommits) / 2
    val commitBuilder = newCommitBuilder().copy(totalNumFiles = totalNumFiles)

    (1 to 10).foreach(_ => commitBuilder.commitToRepository(testGitRepo.getRepository))

    getCommitTreeContent.size should be > fixtures.numberOfFilesPerCommit
    getCommitTreeContent.size should be <= totalNumFiles
  }

  def newCommitBuilder() = new CommitBuilder(
    fixtures.numberOfFilesPerCommit,
    fixtures.minContentLengthOfCommit,
    fixtures.maxContentLengthOfCommit,
    fixtures.defaultPrefixOfCommit,
    fixtures.filenamePrefix,
    fixtures.filenameExt,
    fixtures.totalNumFiles
  )

  private def getCommitTreeContent: Seq[FileContent] =
    Using(new TreeWalk(testGitRepo.getRepository)) { t =>
      t.addTree(getHeadCommit.getTree)
      t.setRecursive(true)
      extractPathFromTree(t)
    }.success.value

  @tailrec
  final def extractPathFromTree(
      tree: TreeWalk,
      files: Seq[FileContent] = Seq.empty[FileContent]
  ): Seq[FileContent] =
    if (tree.next()) {
      extractPathFromTree(tree, files :+ FileContent.readFromTree(tree))
    } else {
      files
    }

  override def commandName: String = "CommitBuilder"
}

case class FileContent(filename: String, contentLen: Int)

object FileContent {

  def readFromTree(tree: TreeWalk): FileContent = {
    val objectId = tree.getObjectId(0)
    val loader   = tree.getObjectReader.open(objectId)
    FileContent(tree.getPathString, loader.getBytes().length)
  }
}
