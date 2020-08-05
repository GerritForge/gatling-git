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

package com.github.barbasa.gatling.git

import io.gatling.core.session.{Expression, StaticStringExpression, ExpressionSuccessWrapper}
import org.eclipse.jgit.lib.Constants.{HEAD, MASTER, R_HEADS}

import GitRequestSession._

case class GitRequestSession(
    commandName: Expression[String],
    url: Expression[String],
    refSpec: Expression[String] = HeadToMasterRefSpec,
    tag: Expression[String] = EmptyTag,
    force: Expression[Boolean] = False
)

object GitRequestSession {
  val MasterRef           = s"$R_HEADS$MASTER"
  val AllRefs             = s"+refs/*:refs/*"
  val HeadToMasterRefSpec = StaticStringExpression(s"$HEAD:$MasterRef")
  val EmptyTag            = StaticStringExpression("")
  val False               = false.expressionSuccess

  def cmd(
      cmd: String,
      url: Expression[String],
      refSpec: Expression[String] = HeadToMasterRefSpec,
      tag: Expression[String] = EmptyTag
  ): GitRequestSession =
    GitRequestSession(StaticStringExpression(cmd), url, refSpec, tag)
}
