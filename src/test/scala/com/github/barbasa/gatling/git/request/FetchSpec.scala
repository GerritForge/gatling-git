package com.github.barbasa.gatling.git.request

import java.io.File

import com.github.barbasa.gatling.git._
import org.apache.commons.io.FileUtils
import org.eclipse.jgit.api.{Git => JGit}
import org.eclipse.jgit.transport.URIish
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

class FetchSpec extends FlatSpec with BeforeAndAfter with Matchers {

  var testGitRepo: JGit = _

  val tempBase: String        = "/tmp"
  val testUser: String        = "testUser"
  val testRepo: String        = "testRepo"
  val workTreeDirectory: File = new File(s"$tempBase/$testUser/$testRepo")

  implicit val gatlingConfig = GatlingGitConfiguration(
    HttpConfiguration("userName", "password"),
    SshConfiguration("/tmp/keys"),
    tempBase,
    CommandsConfiguration(PushConfiguration(1, 2, 3))
  )

  before {
    FileUtils.deleteDirectory(new File(s"$tempBase/$testUser"))
    testGitRepo = JGit.init.setDirectory(workTreeDirectory).call
  }

  after {
    println("Calling After!")
    testGitRepo.getRepository.close()
    FileUtils.deleteDirectory(new File(s"$tempBase/$testUser"))
  }

  behavior of "Fetch"

  "without any error" should "return OK" in {

    Push(new URIish(s"file://$tempBase/$testUser/$testRepo"), s"$testUser").send

    val cmd      = Fetch(new URIish(s"file://$tempBase/$testUser/$testRepo"), s"$testUser")
    val response = cmd.send
    response.status shouldBe OK
  }

}
