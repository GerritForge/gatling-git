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

import com.github.barbasa.gatling.git.helper.MockFileFactory
import com.github.barbasa.gatling.git.helper.MockFiles._
import org.eclipse.jgit.api._
import org.eclipse.jgit.lib.Repository
import scala.util.Random

class CommitBuilder(repository: Repository) {

  val git = new Git(repository)
  val random = new Random()

  def createCommit(numFiles: Int, minContentLength: Int, maxContentLength: Int) {
    for (i <- 1 to numFiles) {
      var contentLength: Int = minContentLength + random.nextInt((maxContentLength - minContentLength) +1)
      var file: MockFile = MockFileFactory.create("text", contentLength)
      var fileName: String = file.save(repository.getWorkTree().toString())
      git.add.addFilepattern(fileName).call()
    }

    git
      .commit()
      .setMessage(s"Test commit")
      .call()
  }
}
