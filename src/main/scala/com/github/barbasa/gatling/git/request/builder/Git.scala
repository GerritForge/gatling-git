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

package com.github.barbasa.gatling.git.request.builder

import com.github.barbasa.gatling.git.GatlingGitConfiguration
import io.gatling.core.session.{Expression, StaticStringExpression}

case object Git {

  implicit lazy val conf: GatlingGitConfiguration = GatlingGitConfiguration()

  def clone(repo: Expression[String], schema: Expression[String]): GitRequestBuilder =
    new GitRequestBuilder(
      StaticStringExpression("clone"),
      repo,
      schema,
      StaticStringExpression("anyUser"))

  def fetch(repo: Expression[String], schema: Expression[String]): GitRequestBuilder =
    new GitRequestBuilder(
      StaticStringExpression("clone"),
      repo,
      schema,
      StaticStringExpression("anyUser"))

  def pull(repo: Expression[String], schema: Expression[String]): GitRequestBuilder =
    new GitRequestBuilder(
      StaticStringExpression("clone"),
      repo,
      schema,
      StaticStringExpression("anyUser"))

  def push(repo: Expression[String], schema: Expression[String]): GitRequestBuilder =
    new GitRequestBuilder(
      StaticStringExpression("clone"),
      repo,
      schema,
      StaticStringExpression("anyUser"))
}
