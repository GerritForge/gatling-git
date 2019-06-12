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

import java.io._

import scala.util.Random

object MockFiles {

  trait MockFile {
    def name: String
    def content: String

    def generateContent(contentLength: Int): String
    def save(workTreeDirectory: String): String
  }

  abstract class AbstractMockFile(contentLength: Int) extends MockFile {
    val alphabet         = ('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9') ++ ("_")
    override def content = generateContent(contentLength)
    override def name    = generateRandomString(10)

    def generateRandomString(length: Int): String = (1 to length).foldLeft("")(
      (acc, _) => acc + alphabet(Random.nextInt(alphabet.size))
    )
  }

  class TextFile(contentLength: Int) extends AbstractMockFile(contentLength) {

    override def generateContent(size: Int): String = {
      generateRandomString(size)
    }

    override def save(workTreeDirectory: String): String = {
      val filePath = "%s/%s".format(workTreeDirectory, name)
      val file     = new File(filePath)
      val writer   = new BufferedWriter(new FileWriter(file))
      writer.write(content)
      writer.close()

      filePath
    }
  }
}

object MockFileFactory {
  import MockFiles._

  def create(fileType: String, contentLength: Int): MockFile = {
    fileType match {
      case "text" => new TextFile(contentLength)
      case _      => throw new Exception("Unknown file type: %s".format(fileType))
    }
  }
}
