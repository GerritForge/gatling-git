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
import org.eclipse.jgit.transport.URIish
import org.scalatest.BeforeAndAfter

import java.io.File
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class CleanupRepoSpec extends AnyFlatSpec with BeforeAndAfter with Matchers with GitTestHelpers {

  before {
    FileUtils.deleteDirectory(originRepoDirectory.getParentFile)
    testGitRepo = Request.initRepo(originRepoDirectory)
  }

  after {
    testGitRepo.getRepository.close()
    FileUtils.deleteDirectory(originRepoDirectory.getParentFile)
  }

  behavior of "CleanupRepo"

  it should "return OK when the directory exists asdf" in {
    val cleanupRepo: CleanupRepo = CleanupRepo(new URIish(s"file://$originRepoDirectory"), testUser)
    cleanupRepo.workTreeDirectory().mkdirs()

    val response = cleanupRepo.send

    response.status shouldBe OK
  }

  it should "return Fail when there directory does not exist" in {
    val cleanupRepoAction = new CleanupRepo(new URIish(s"file://$originRepoDirectory"), testUser) {
      override def workTreeDirectory(suffix: Option[String] = None) =
        new File("/non/existent/directory")
    }

    cleanupRepoAction.send.status shouldBe Fail
  }

  override def commandName: String = ???
}
