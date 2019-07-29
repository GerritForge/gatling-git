package com.github.barbasa.gatling.git.request

import java.io.File
import org.apache.commons.io.FileUtils
import org.eclipse.jgit.api.{Git => JGit}
import org.eclipse.jgit.transport.URIish
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

import scala.collection.JavaConverters._

class CloneSpec extends FlatSpec with BeforeAndAfter with Matchers with GitTestHelpers {

  before {
    FileUtils.deleteDirectory(originRepoDirectory.getParentFile)
    testGitRepo = JGit.init.setDirectory(originRepoDirectory).call
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
    val response =
      Clone(new URIish(s"file://${originRepoDirectory}"), s"$testUser", s"$testBranchName").send
    response.status shouldBe OK

    val refsList = JGit.open(workTreeDirectory).branchList().call().asScala
    refsList.map(_.getName) should contain(s"refs/heads/$testBranchName")
  }

}
