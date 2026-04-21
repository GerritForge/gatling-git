// Copyright (C) 2019 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.github.barbasa.gatling.git.helper

import com.github.barbasa.gatling.git.helper.MockFiles._
import org.eclipse.jgit.api._
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.{FooterKey, FooterLine}

import java.time.LocalDateTime

import scala.util.Random
import scala.jdk.CollectionConverters._
import org.eclipse.jgit.lib.Constants.R_HEADS
import org.eclipse.jgit.util.ChangeIdUtil

case class CommitBuilder(
    numFiles: Int,
    minContentLength: Int,
    maxContentLength: Int,
    prefix: String,
    filenamePrefix: String,
    filenameExt: String,
    totalNumFiles: Int,
    modifyExisting: Boolean = false
) {

  import CommitBuilder._

  val random            = new Random()
  val EmptySetOfStrings = Set.empty[String]

  def commitToRepository(
      repository: Repository,
      branch: Option[String] = Option.empty,
      computeChangeId: Boolean = false,
      amend: Boolean = false
  ) = {
    val git = new Git(repository)

    val existingBranches = git.branchList.call.asScala
      .map(_.getName.drop(R_HEADS.length))
      .toSet

    val existingBranch = branch.filter(existingBranches.contains)
    existingBranch.foreach(git.checkout.setName(_).call)

    val contentLength: Int = minContentLength + random
      .nextInt((maxContentLength - minContentLength) + 1)
    val adjustedTotalNumFiles = math.max(numFiles, totalNumFiles)
    val fileNames: Set[String] =
      (1 to numFiles).map(_ => EmptySetOfStrings).fold(EmptySetOfStrings) { (names, _) =>
        if (modifyExisting) modifyExistingFile(repository, names)
        else addNewRandomFile(contentLength, adjustedTotalNumFiles, repository, names)
      }

    val gitAdd = git.add()
    fileNames.foreach(fileName => gitAdd.addFilepattern(fileName))
    if (fileNames.nonEmpty) {
      gitAdd.call()
    }

    val uniqueSuffix  = s"${LocalDateTime.now}"
    val commitCommand = git.commit()
    val existingChangeId: Option[FooterLine] = if (amend) {
      git
        .log()
        .setMaxCount(1)
        .call()
        .asScala
        .flatMap(_.getFooterLines.asScala)
        .find(_.matches(ChangeIdFooterKey))
    } else {
      Option.empty
    }
    val revCommit = if (amend) {
      val changeIdFooterLines = existingChangeId.map("\n" + _).getOrElse("")
      commitCommand
        .setAmend(amend)
        .setMessage(
          s"${prefix}Amended test commit header - $uniqueSuffix\n\nTest commit body - $uniqueSuffix\n$changeIdFooterLines"
        )
        .call()
    } else {
      commitCommand
        .setMessage(
          s"${prefix}Test commit header - $uniqueSuffix\n\nTest commit body - $uniqueSuffix\n"
        )
        .call()
    }

    if (!amend && computeChangeId) {
      Option(
        ChangeIdUtil.computeChangeId(
          revCommit.getTree.getId,
          revCommit.getId,
          revCommit.getAuthorIdent,
          revCommit.getCommitterIdent,
          revCommit.getFullMessage
        )
      ).foreach(changeId =>
        git
          .commit()
          .setAmend(true)
          .setMessage(ChangeIdUtil.insertId(revCommit.getFullMessage, changeId, true))
          .call()
      )
    }

    branch
      .filterNot(existingBranch.contains)
      .foreach(git.branchCreate.setName(_).call)
  }

  private def modifyExistingFile(
      repository: Repository,
      filenamesToExclude: Set[String]
  ): Set[String] = {
    val workTree = repository.getWorkTree
    val candidates = getCandidates(workTree)
      .filterNot(f => filenamesToExclude.contains(workTree.toPath.relativize(f.toPath).toString))

    if (candidates.isEmpty) {
      addNewRandomFile(
        minContentLength,
        math.max(numFiles, totalNumFiles),
        repository,
        filenamesToExclude
      )
    } else {
      val file   = candidates(random.nextInt(candidates.length))
      val source = scala.io.Source.fromFile(file)
      val lines =
        try source.getLines().toIndexedSeq
        finally source.close()

      if (lines.isEmpty) {
        addNewRandomFile(
          minContentLength,
          math.max(numFiles, totalNumFiles),
          repository,
          filenamesToExclude
        )
      } else {
        val buf         = scala.collection.mutable.ArrayBuffer(lines: _*)
        val numToChange = math.max(1, lines.length / 20)
        (0 until numToChange).foreach { _ =>
          val i = random.nextInt(buf.length)
          buf(i) = MockFiles.generateRandomString(lines(i).length)
        }
        val writer = new java.io.BufferedWriter(new java.io.FileWriter(file))
        try writer.write(buf.mkString("\n"))
        finally writer.close()

        filenamesToExclude + workTree.toPath.relativize(file.toPath).toString
      }
    }
  }

  private def getCandidates(workTree: java.io.File): IndexedSeq[java.io.File] =
    CommitBuilder.fileCache.getOrElseUpdate(workTree.getAbsolutePath, listTextFiles(workTree))

  private def listTextFiles(dir: java.io.File): IndexedSeq[java.io.File] =
    dir.listFiles().toIndexedSeq.flatMap {
      case d if d.isDirectory && d.getName != ".git" => listTextFiles(d)
      case f if f.isFile && isTextFile(f)            => IndexedSeq(f)
      case _                                         => IndexedSeq.empty
    }

  private def isTextFile(file: java.io.File): Boolean =
    CommitBuilder.textExtensions.exists(file.getName.endsWith)

  private def addNewRandomFile(
      contentLength: Int,
      totalNumFiles: Int,
      repository: Repository,
      filenamesToExclude: Set[String]
  ): Set[String] = {
    val file: MockFile = MockFileFactory
      .create(
        TextFileType,
        contentLength,
        filenamePrefix,
        filenameExt,
        totalNumFiles,
        filenamesToExclude
      )
    file.save(repository.getWorkTree.toString): Unit
    (filenamesToExclude.toSeq :+ file.name).toSet[String]
  }
}

object CommitBuilder {
  val ChangeIdFooterKey = new FooterKey("Change-Id")

  private[helper] val fileCache =
    scala.collection.concurrent.TrieMap.empty[String, IndexedSeq[java.io.File]]

  private[helper] val textExtensions = Set(
    ".java",
    ".scala",
    ".kt",
    ".py",
    ".js",
    ".ts",
    ".go",
    ".rb",
    ".c",
    ".cpp",
    ".h",
    ".txt",
    ".md",
    ".xml",
    ".json",
    ".yaml",
    ".yml",
    ".properties",
    ".conf",
    ".sh"
  )
}
