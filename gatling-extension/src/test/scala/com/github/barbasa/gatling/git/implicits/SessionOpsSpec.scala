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

package com.github.barbasa.gatling.git.implicits

import com.github.barbasa.gatling.git.implicits.SessionOps._
import io.gatling.core.session.{Session, StaticStringExpression}
import org.scalatest.{FlatSpec, Matchers}

class SessionOpsSpec extends FlatSpec with Matchers {
  behavior of "ensureOrElse"

  it should "set session attribute when not present" in {
    val aUserID        = 1L
    val attributeValue = "foo"
    val attributeKey   = "bar"

    Session("TestScenario", aUserID, System.currentTimeMillis())
      .ensureOrElse(attributeValue, StaticStringExpression(attributeKey))
      .attributes(attributeValue)
      .asInstanceOf[StaticStringExpression]
      .value shouldBe attributeKey
  }

  it should "maintain the attribute when already set" in {
    val aUserID                = 1L
    val attributeValue         = "foo"
    val originalAttributeValue = "bar"

    Session("TestScenario", aUserID, System.currentTimeMillis())
      .set(attributeValue, originalAttributeValue)
      .ensureOrElse(attributeValue, StaticStringExpression("some other value"))
      .attributes(attributeValue)
      .asInstanceOf[String] shouldBe originalAttributeValue
  }
}
