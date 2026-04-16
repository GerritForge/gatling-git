// Copyright (C) 2024 The Android Open Source Project
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

package com.github.barbasa.gatling.git.helper

import com.github.barbasa.gatling.git.helper.MockFiles.{AbstractMockFile, fileNamePool}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.annotation.nowarn

@nowarn("msg=unused value")
class MockFileFactorySpec extends AnyFlatSpec with Matchers {

  behavior of "MockFileFactory"

  "file name" should "contain only digits and letters" in {
    val fileNameWithExtension: String = new TestFile(10, fileNamePool(0)).save("anyWorkTree")

    val fileNameParts = fileNameWithExtension.split("\\.")

    fileNameParts(0).filterNot(_.isLetterOrDigit) should be("")
    fileNameParts(1) should be("java")

  }

  class TestFile(contentLength: Int, name: String) extends AbstractMockFile(contentLength, name) {

    override def generateContent(contentLength: Int): String = "fileContent"
    override def save(workTreeDirectory: String): String = {
      name
    }
  }
}
