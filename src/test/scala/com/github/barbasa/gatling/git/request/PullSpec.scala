package com.github.barbasa.gatling.git.request

import java.io.File

import org.apache.commons.io.FileUtils
import org.eclipse.jgit.api.{Git => JGit}
import org.eclipse.jgit.transport.URIish
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

class PullSpec extends FlatSpec with BeforeAndAfter with Matchers with GitTestHelpers {

  before {
    FileUtils.deleteDirectory(new File(s"$tempBase/$testUser"))
    testGitRepo = JGit.init.setDirectory(workTreeDirectory).call
  }

  after {
    testGitRepo.getRepository.close()
    FileUtils.deleteDirectory(new File(s"$tempBase/$testUser"))
  }

  behavior of "Pull"

  "without any error" should "return OK" in {

    Push(new URIish(s"file://$tempBase/$testUser/$testRepo"), s"$testUser").send

    val cmd      = Pull(new URIish(s"file://$tempBase/$testUser/$testRepo"), s"$testUser")
    val response = cmd.send
    response.status shouldBe OK
  }

}
