package com.github.barbasa.gatling.git.request

import java.io.File
import java.nio.file.Files

import com.github.barbasa.gatling.git.{
  CommandsConfiguration,
  GatlingGitConfiguration,
  HttpConfiguration,
  PushConfiguration,
  SshConfiguration
}
import org.eclipse.jgit.api.{Git => JGit}
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.revwalk.{RevCommit, RevTree, RevWalk}
import org.eclipse.jgit.treewalk.TreeWalk
import org.scalatest.FlatSpec

import scala.collection.mutable.ListBuffer
import scala.util.Try

trait GitTestHelpers extends FlatSpec {
  var testGitRepo: JGit = _

  val tempBase: String          = Files.createTempDirectory("gatlinGitTests").toFile.getAbsolutePath
  val testUser: String          = "testUser"
  val testRepo: String          = "testRepo"
  val testBranchName: String    = "mybranch"
  val originRepoDirectory: File = new File(s"$tempBase/$testUser/$testRepo")
  val workTreeDirectory: File   = new File(s"$tempBase/$testUser/$testRepo-worktree")

  val defaultPushConfiguration = PushConfiguration(
    PushConfiguration.DEFAULT_NUM_FILES,
    PushConfiguration.DEFAULT_MIN_CONTENT_LENGTH,
    PushConfiguration.DEFAULT_MAX_CONTENT_LENGTH,
    PushConfiguration.DEFAULT_COMMIT_PREFIX
  )

  implicit val gatlingConfig = GatlingGitConfiguration(
    HttpConfiguration("userName", "password"),
    SshConfiguration("/tmp/keys"),
    tempBase,
    CommandsConfiguration(defaultPushConfiguration)
  )

  object fixtures {
    val numberOfFilesPerCommit   = 2
    val minContentLengthOfCommit = 5
    val maxContentLengthOfCommit = 10
    val defaultPrefixOfCommit    = ""
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
    val treeWalk             = new TreeWalk(testGitRepo.getRepository)
    val resultTry = Try {
      treeWalk.reset(headCommit)
      while (treeWalk.next) {
        committedDirectories += treeWalk.getPathString
      }
      committedDirectories.toList
    }
    if (treeWalk != null) treeWalk.close()
    resultTry.get
  }
}
