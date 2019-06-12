package com.github.barbasa.gatling.git.request

import java.io.File

import com.github.barbasa.gatling.git._
import org.apache.commons.io.FileUtils
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}
import org.eclipse.jgit.api.{Git => JGit}
import org.eclipse.jgit.transport.{RefSpec, URIish}


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
    println("Calling After!")
    testGitRepo.getRepository.close()
    FileUtils.deleteDirectory(new File(s"$tempBase/$testUser"))
  }

  behavior of "Push"

  "without refSpec" should "find refs/heads/master" in {
    val cmd = Push(new URIish(s"file://$tempBase/$testUser/$testRepo"), s"$testUser", None)
    cmd.send
    testGitRepo.getRepository.resolve("refs/heads/master") should not be null
  }

  "with refSpec" should "only push to the desired refSpec" in {
    val testRefSpecSource = "HEAD"
    val testRefSpecDestination = "refs/for/test"
    val cmd = Push(new URIish(s"file://$tempBase/$testUser/$testRepo"), s"$testUser", Some(new RefSpec(s"$testRefSpecSource:$testRefSpecDestination")))
    cmd.send
    testGitRepo.getRepository.resolve(testRefSpecDestination) should not be null
  }

}
