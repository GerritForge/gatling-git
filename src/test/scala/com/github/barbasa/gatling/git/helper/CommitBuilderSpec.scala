package com.github.barbasa.gatling.git.helper
import java.io.File

import com.github.barbasa.gatling.git.request.GitTestHelpers
import org.apache.commons.io.FileUtils
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}
import org.eclipse.jgit.api.{Git => JGit}
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.revwalk.{RevCommit, RevTree, RevWalk}
import org.eclipse.jgit.treewalk.TreeWalk

import scala.collection.mutable.ListBuffer

class CommitBuilderSpec extends FlatSpec with BeforeAndAfter with Matchers with GitTestHelpers {
  before {
    FileUtils.deleteDirectory(new File(s"$tempBase/$testUser"))
    testGitRepo = JGit.init.setDirectory(workTreeDirectory).call
  }

  after {
    testGitRepo.getRepository.close()
    FileUtils.deleteDirectory(new File(s"$tempBase/$testUser"))
  }

  def getHeadCommit: RevCommit = {
    try {
      val repository = testGitRepo.getRepository
      try {
        val head = repository.findRef(Constants.HEAD)
        try {
          val walk = new RevWalk(repository)
          try {
            walk.parseCommit(head.getObjectId)
          }
        }
      }
    } catch {
      case e: Exception => fail(e.getCause)
    }
  }

  def getPathsInCommit(headCommit: RevTree): List[String] = {
    var committedDirectories = new ListBuffer[String]()
    try {
      val treeWalk = new TreeWalk(testGitRepo.getRepository)
      try {
        treeWalk.reset(headCommit)
        while (treeWalk.next) {
          committedDirectories += treeWalk.getPathString
        }
      } finally if (treeWalk != null) treeWalk.close()
    }
    committedDirectories.toList
  }

  behavior of "CommitBuilder"

  "without prefix parameter" should "create commits without prefix" in {
    val commitBuilder = new CommitBuilder(
      testGitRepo.getRepository,
      fixtures.numberOfFilesPerCommit,
      fixtures.minContentLengthOfCommit,
      fixtures.maxContentLengthOfCommit,
      fixtures.defaultPrefixOfCommit,
      Seq(testGitRepo.getRepository.getWorkTree)
    )
    commitBuilder.createCommit()
    getHeadCommit.getFullMessage should startWith("Test commit header - ")
  }

  "with prefix parameter" should "start with the prefix" in {
    val commitBuilder = new CommitBuilder(testGitRepo.getRepository,
                                          fixtures.numberOfFilesPerCommit,
                                          fixtures.minContentLengthOfCommit,
                                          fixtures.maxContentLengthOfCommit,
                                          "testPrefix - ",
                                          Seq(testGitRepo.getRepository.getWorkTree))
    commitBuilder.createCommit()
    getHeadCommit.getFullMessage should startWith("testPrefix - Test commit header - ")

  }

  "with different destination directories" should "start with the prefix" in {
    val directoryA = new File(s"${testGitRepo.getRepository.getWorkTree}/directoryA")
    val directoryB = new File(s"${testGitRepo.getRepository.getWorkTree}/directoryB")
    directoryA.mkdir
    directoryB.mkdir
    val commitBuilder = new CommitBuilder(
      testGitRepo.getRepository,
      fixtures.numberOfFilesPerCommit,
      fixtures.minContentLengthOfCommit,
      fixtures.maxContentLengthOfCommit,
      fixtures.defaultPrefixOfCommit,
      Seq(directoryA, directoryB)
    )
    commitBuilder.createCommit()
    getPathsInCommit(getHeadCommit.getTree) should be(List("directoryA", "directoryB"))
  }

}
