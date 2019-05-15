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

  def clone(url: Expression[String]): GitRequestBuilder =
    new GitRequestBuilder(StaticStringExpression("clone"), url)

  def fetch(url: Expression[String]): GitRequestBuilder =
    new GitRequestBuilder(StaticStringExpression("fetch"), url)

  def pull(url: Expression[String]): GitRequestBuilder =
    new GitRequestBuilder(StaticStringExpression("pull"), url)

  def push(url: Expression[String]): GitRequestBuilder =
    new GitRequestBuilder(StaticStringExpression("push"), url)
}
