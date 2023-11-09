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

class CommitBuilder(numFiles: Int, minContentLength: Int, maxContentLength: Int, prefix: String) {

  import CommitBuilder._

  val random = new Random()

  def commitToRepository(
      repository: Repository,
      branch: Option[String] = Option.empty,
      computeChangeId: Boolean = false,
      amend: Boolean = false
  ) = {
    val git = new Git(repository)
    Vector.range(0, numFiles).foreach { _ =>
      val contentLength: Int = minContentLength + random
        .nextInt((maxContentLength - minContentLength) + 1)
      val file: MockFile = MockFileFactory.create(TextFileType, contentLength)
      file.save(repository.getWorkTree.toString)
    }

    val existingBranches = git.branchList.call.asScala
      .map(_.getName.drop(R_HEADS.length))
      .toSet

    val existingBranch = branch.filter(existingBranches.contains)
    existingBranch.foreach(git.checkout.setName(_).call)

    git.add.addFilepattern(".").call()

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
}

object CommitBuilder {
  val ChangeIdFooterKey = new FooterKey("Change-Id")
}
