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

import java.io.File

import com.github.barbasa.gatling.git.helper.MockFiles._
import org.eclipse.jgit.api._
import org.eclipse.jgit.lib.Repository
import java.time.LocalDateTime

import scala.util.Random

class CommitBuilder(repository: Repository,
                      numFiles: Int,
                      minContentLength: Int,
                      maxContentLength: Int,
                      prefix: String,
                      destinationDirectories: Seq[File]) {

  val git    = new Git(repository)
  val random = new Random()

  def createCommit() {
    val directories = Iterator.continually(destinationDirectories).flatten.take(numFiles)
    Vector.range(0, numFiles).foreach { e =>
        val contentLength: Int = minContentLength + random
          .nextInt((maxContentLength - minContentLength) + 1)
        val file: MockFile = MockFileFactory.create("text", contentLength)
        file.save(directories.next.toString)
      }

    git.add.addFilepattern(".").call()

    val uniqueSuffix = s"${LocalDateTime.now}"
    git
      .commit()
      .setMessage(s"${prefix}Test commit header - $uniqueSuffix\n\nTest commit body - $uniqueSuffix\n")
      .call()
  }
}
