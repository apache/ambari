/**
  * Licensed to the Apache Software Foundation (ASF) under one
  * or more contributor license agreements.  See the NOTICE file
  * distributed with this work for additional information
  * regarding copyright ownership.  The ASF licenses this file
  * to you under the Apache License, Version 2.0 (the
  * "License"); you may not use this file except in compliance
  * with the License.  You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
package org.apache.ambari.metrics.adservice.app

import org.junit.runner.Description
import org.junit.runners.model.Statement

import io.dropwizard.Configuration
import io.dropwizard.testing.ConfigOverride
import io.dropwizard.testing.junit.DropwizardAppRule

import scala.collection.mutable

object DropwizardAppRuleHelper {

  def withAppRunning[C <: Configuration](serviceClass: Class[_ <: io.dropwizard.Application[C]],
                                         configPath: String, configOverrides: ConfigOverride*)
                                        (fn: (DropwizardAppRule[C]) => Statement) {
    val overrides = new mutable.ListBuffer[ConfigOverride]
    configOverrides.foreach { o => overrides += o }
    val rule = new DropwizardAppRule(serviceClass, configPath, overrides.toList: _*)
    rule.apply(fn(rule), Description.EMPTY).evaluate()
  }

}
