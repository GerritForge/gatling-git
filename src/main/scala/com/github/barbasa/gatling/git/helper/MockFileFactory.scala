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

  val loremIpsumText =
    "Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium, totam rem aperiam, eaque ipsa quae ab illo inventore veritatis et quasi architecto beatae vitae dicta sunt explicabo. Nemo enim ipsam voluptatem quia voluptas sit aspernatur aut odit aut fugit, sed quia consequuntur magni dolores eos qui ratione voluptatem sequi nesciunt. Neque porro quisquam est, qui dolorem ipsum quia dolor sit amet, consectetur, adipisci velit, sed quia non numquam eius modi tempora incidunt ut labore et dolore magnam aliquam quaerat voluptatem. Ut enim ad minima veniam, quis nostrum exercitationem ullam corporis suscipit laboriosam, nisi ut aliquid ex ea commodi consequatur? Quis autem vel eum iure reprehenderit qui in ea voluptate velit esse quam nihil molestiae consequatur, vel illum qui dolorem eum fugiat quo voluptas nulla pariatur?"

  val loremIpsumTextLen = loremIpsumText.length

  abstract class AbstractMockFile(contentLength: Int) extends MockFile {
    override def content = generateContent(contentLength)
    override def name    = generateRandomString(10) + ".java"

    def generateRandomString(length: Int): String =
      (1 to length)
        .grouped(120)
        .map(line =>
          loremIpsumText
            .drop(Random.nextInt(loremIpsumTextLen - line.length))
            .take(line.length)
        )
        .mkString("\n")
  }

  sealed trait FileType
  case object TextFileType extends FileType

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

  def create(fileType: FileType, contentLength: Int): MockFile = {
    fileType match {
      case TextFileType => new TextFile(contentLength)
    }
  }
}
