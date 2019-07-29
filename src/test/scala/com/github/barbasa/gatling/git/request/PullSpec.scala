package com.github.barbasa.gatling.git.request

import java.io.File

import org.apache.commons.io.FileUtils
import org.eclipse.jgit.api.{Git => JGit}
import org.eclipse.jgit.transport.URIish
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

class PullSpec extends FlatSpec with BeforeAndAfter with Matchers with GitTestHelpers {

  before {
    FileUtils.deleteDirectory(originRepoDirectory.getParentFile)
    testGitRepo = JGit.init.setDirectory(originRepoDirectory).call
  }

  after {
    testGitRepo.getRepository.close()
    FileUtils.deleteDirectory(originRepoDirectory.getParentFile)
  }

  behavior of "Pull"

  "without any error" should "return OK" in {

    Push(new URIish(s"file://$originRepoDirectory"), s"$testUser").send

    val cmd      = Pull(new URIish(s"file://$originRepoDirectory"), s"$testUser")
    val response = cmd.send
    response.status shouldBe OK
  }

}
