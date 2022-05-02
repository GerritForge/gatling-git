// Copyright (C) 2022 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at

// http://www.apache.org/licenses/LICENSE-2.0

// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.github.barbasa.gatling.git.request

import org.apache.commons.io.FileUtils
import org.eclipse.jgit.api.{Git => JGit}
import org.eclipse.jgit.transport.URIish
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

import java.io.File

class CleanupRepoSpec extends FlatSpec with BeforeAndAfter with Matchers with GitTestHelpers {

  before {
    FileUtils.deleteDirectory(originRepoDirectory.getParentFile)
    testGitRepo = JGit.init.setDirectory(originRepoDirectory).call
  }

  after {
    testGitRepo.getRepository.close()
    FileUtils.deleteDirectory(originRepoDirectory.getParentFile)
  }

  behavior of "CleanupRepo"

  it should "return OK when the directory exists" in {
    val response = CleanupRepo(new URIish(s"file://$originRepoDirectory"), testUser).send

    response.status shouldBe OK
  }

  it should "return Fail when there directory does not exist" in {
    val cleanupRepoAction = new CleanupRepo(new URIish(s"file://$originRepoDirectory"), testUser) {
      override lazy val workTreeDirectory = new File("/non/existent/directory")
    }

    cleanupRepoAction.send.status shouldBe Fail
  }
}
