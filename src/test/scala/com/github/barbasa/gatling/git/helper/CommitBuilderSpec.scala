package com.github.barbasa.gatling.git.helper
import java.io.File

import com.github.barbasa.gatling.git.request.GitTestHelpers
import org.apache.commons.io.FileUtils
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}
import org.eclipse.jgit.api.{Git => JGit}
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk

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

  behavior of "CommitBuilder"

  "without prefix parameter" should "create commits without prefix" in {
    val commitBuilder = new CommitBuilder(testGitRepo.getRepository)
    commitBuilder.createCommit(
      fixtures.numberOfFilesPerCommit,
      fixtures.minContentLengthOfCommit,
      fixtures.maxContentLengthOfCommit,
      fixtures.defaultPrefixOfCommit
    )
    getHeadCommit.getFullMessage should startWith("Test commit header - ")
  }

  "with prefix parameter" should "start with the prefix" in {
    val commitBuilder = new CommitBuilder(testGitRepo.getRepository)
    commitBuilder.createCommit(
      fixtures.numberOfFilesPerCommit,
      fixtures.minContentLengthOfCommit,
      fixtures.maxContentLengthOfCommit,
      "testPrefix - "
    )
    getHeadCommit.getFullMessage should startWith("testPrefix - Test commit header - ")
  }

}
