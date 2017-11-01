/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.metrics.adservice.metadata

import org.scalatest.FunSuite

class MetricSourceDefinitionTest extends FunSuite {

  test("createNewMetricSourceDefinition") {
    val msd : MetricSourceDefinition = new MetricSourceDefinition("testDefinition", "testAppId", MetricSourceDefinitionType.API)

    assert(msd.definitionName == "testDefinition")
    assert(msd.appId == "testAppId")
    assert(msd.definitionSource == MetricSourceDefinitionType.API)

    assert(msd.hosts.isEmpty)
    assert(msd.metricDefinitions.isEmpty)
    assert(msd.associatedAnomalySubsystems.isEmpty)
    assert(msd.relatedDefinitions.isEmpty)
  }

  test("testAddMetricDefinition") {
    val msd : MetricSourceDefinition = new MetricSourceDefinition("testDefinition", "testAppId", MetricSourceDefinitionType.API)
    assert(msd.metricDefinitions.isEmpty)

    msd.addMetricDefinition(MetricDefinition("TestMetric", "TestApp", List.empty[String]))
    assert(msd.metricDefinitions.nonEmpty)
  }

  test("testEquals") {
    val msd1 : MetricSourceDefinition = new MetricSourceDefinition("testDefinition", "testAppId", MetricSourceDefinitionType.API)
    val msd2 : MetricSourceDefinition = new MetricSourceDefinition("testDefinition", "testAppId2", MetricSourceDefinitionType.API)
    assert(msd1 == msd2)
  }

  test("testRemoveMetricDefinition") {
    val msd : MetricSourceDefinition = new MetricSourceDefinition("testDefinition", "testAppId", MetricSourceDefinitionType.API)
    assert(msd.metricDefinitions.isEmpty)

    msd.addMetricDefinition(MetricDefinition("TestMetric", "TestApp", List.empty[String]))
    assert(msd.metricDefinitions.nonEmpty)

    msd.removeMetricDefinition(MetricDefinition("TestMetric", "TestApp", List.empty[String]))
    assert(msd.metricDefinitions.isEmpty)
  }

  test("serializeDeserialize") {
    val msd : MetricSourceDefinition = new MetricSourceDefinition("testDefinition", "testAppId", MetricSourceDefinitionType.API)
    val msdString: String = MetricSourceDefinition.serialize(msd)
    assert(msdString.nonEmpty)

    val msd2: MetricSourceDefinition = MetricSourceDefinition.deserialize(msdString)
    assert(msd2 != null)
    assert(msd == msd2)

  }
}
