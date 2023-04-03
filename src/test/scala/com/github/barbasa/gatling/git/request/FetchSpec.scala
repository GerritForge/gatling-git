// Copyright (C) 2019 The Android Open Source Project
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
import com.github.barbasa.gatling.git.request.Request.initRepo

import scala.annotation.nowarn
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

@nowarn("msg=unused value")
class FetchSpec extends AnyFlatSpec with BeforeAndAfter with Matchers with GitTestHelpers {

  before {
    FileUtils.deleteDirectory(originRepoDirectory.getParentFile)
    testGitRepo = initRepo(originRepoDirectory)
  }

  after {
    testGitRepo.getRepository.close()
    FileUtils.deleteDirectory(originRepoDirectory.getParentFile)
  }

  behavior of "Fetch"

  "without any error" should "return OK" in {

    Push(new URIish(s"file://${originRepoDirectory}"), s"$testUser").send

    val cmd      = Fetch(new URIish(s"file://${originRepoDirectory}"), s"$testUser")
    val response = cmd.send
    response.status shouldBe OK
  }

  override def commandName: String = "Fetch"
}
