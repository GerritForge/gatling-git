package com.github.barbasa.gatling.git.request

import java.io.File

import com.github.barbasa.gatling.git._
import org.apache.commons.io.FileUtils
import org.eclipse.jgit.api.{PullResult, Git => JGit}
import org.eclipse.jgit.transport.URIish
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

class PushSpec extends FlatSpec with BeforeAndAfter with Matchers {

  var testGitRepo: JGit = _

  val tempBase: String = "/tmp"
  val testUser: String = "testUser"
  val testRepo: String = "testRepo"
  val workTreeDirectory: File  = new File(s"$tempBase/$testUser/$testRepo")

  implicit val gatlingConfig = GatlingGitConfiguration(
    HttpConfiguration("userName", "password"),
    SshConfiguration("/tmp/keys"),
    tempBase,
    CommandsConfiguration(PushConfiguration(1,2,3)))

  before {
    FileUtils.deleteDirectory(new File(s"$tempBase/$testUser"))
    testGitRepo = JGit.init.setDirectory(workTreeDirectory).call
  }

  after {
    testGitRepo.getRepository.close()
    FileUtils.deleteDirectory(new File(s"$tempBase/$testUser"))
  }

  behavior of "Push"

  "without any error" should "return OK" in {
    val response = Push(new URIish(s"file://$tempBase/$testUser/$testRepo/1"), s"$testUser").send
    response.status shouldBe OK
  }

}

