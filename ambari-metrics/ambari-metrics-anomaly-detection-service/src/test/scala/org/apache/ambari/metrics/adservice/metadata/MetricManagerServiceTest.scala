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

package org.apache.ambari.metrics.adservice.metadata

import org.apache.ambari.metrics.adservice.app.AnomalyDetectionAppConfig
import org.apache.ambari.metrics.adservice.db.AdMetadataStoreAccessor
import org.easymock.EasyMock.{anyObject, expect, expectLastCall, replay}
import org.scalatest.FunSuite
import org.scalatest.easymock.EasyMockSugar

class MetricManagerServiceTest extends FunSuite {

  test("testAddDefinition") {

    val definitions : scala.collection.mutable.MutableList[MetricSourceDefinition] = scala.collection.mutable.MutableList.empty[MetricSourceDefinition]

    for (i <- 1 to 3) {
      val msd1 : MetricSourceDefinition = new MetricSourceDefinition("TestDefinition" + i, "testAppId", MetricSourceDefinitionType.API)
      definitions.+=(msd1)
    }

    val newDef : MetricSourceDefinition = new MetricSourceDefinition("NewDefinition", "testAppId", MetricSourceDefinitionType.API)

    val adMetadataStoreAccessor: AdMetadataStoreAccessor = EasyMockSugar.niceMock[AdMetadataStoreAccessor]
    expect(adMetadataStoreAccessor.getSavedInputDefinitions).andReturn(definitions.toList).once()
    expect(adMetadataStoreAccessor.saveInputDefinition(newDef)).andReturn(true).once()
    replay(adMetadataStoreAccessor)

    val metricManagerService: MetricManagerServiceImpl = new MetricManagerServiceImpl(new AnomalyDetectionAppConfig, adMetadataStoreAccessor)

    metricManagerService.setAdMetadataStoreAccessor(adMetadataStoreAccessor)

    metricManagerService.addDefinition(newDef)

    assert(metricManagerService.metricSourceDefinitionMap.size == 4)
    assert(metricManagerService.metricSourceDefinitionMap.get("testDefinition") != null)
  }

  test("testGetDefinitionByName") {
    val definitions : scala.collection.mutable.MutableList[MetricSourceDefinition] = scala.collection.mutable.MutableList.empty[MetricSourceDefinition]

    for (i <- 1 to 3) {
      val msd1 : MetricSourceDefinition = new MetricSourceDefinition("TestDefinition" + i, "testAppId", MetricSourceDefinitionType.API)
      definitions.+=(msd1)
    }

    val adMetadataStoreAccessor: AdMetadataStoreAccessor = EasyMockSugar.niceMock[AdMetadataStoreAccessor]
    expect(adMetadataStoreAccessor.getSavedInputDefinitions).andReturn(definitions.toList).once()
    replay(adMetadataStoreAccessor)

    val metricManagerService: MetricManagerServiceImpl = new MetricManagerServiceImpl(new AnomalyDetectionAppConfig, adMetadataStoreAccessor)

    metricManagerService.setAdMetadataStoreAccessor(adMetadataStoreAccessor)
    for (i <- 1 to 3) {
      val definition: MetricSourceDefinition = metricManagerService.getDefinitionByName("TestDefinition" + i)
      assert(definition != null)
    }
  }

  test("testGetDefinitionByAppId") {
    val definitions : scala.collection.mutable.MutableList[MetricSourceDefinition] = scala.collection.mutable.MutableList.empty[MetricSourceDefinition]

    for (i <- 1 to 3) {
      var msd1 : MetricSourceDefinition = null
      if (i == 2) {
        msd1 = new MetricSourceDefinition("TestDefinition" + i, null, MetricSourceDefinitionType.API)
      } else {
        msd1 = new MetricSourceDefinition("TestDefinition" + i, "testAppId", MetricSourceDefinitionType.API)
      }
      definitions.+=(msd1)
    }

    val adMetadataStoreAccessor: AdMetadataStoreAccessor = EasyMockSugar.niceMock[AdMetadataStoreAccessor]
    expect(adMetadataStoreAccessor.getSavedInputDefinitions).andReturn(definitions.toList).once()
    replay(adMetadataStoreAccessor)

    val metricManagerService: MetricManagerServiceImpl = new MetricManagerServiceImpl(new AnomalyDetectionAppConfig, adMetadataStoreAccessor)

    metricManagerService.setAdMetadataStoreAccessor(adMetadataStoreAccessor)
    val definitionsByAppId: List[MetricSourceDefinition] = metricManagerService.getDefinitionByAppId("testAppId")
    assert(definitionsByAppId.size == 2)
  }

  test("testDeleteDefinitionByName") {
    val definitions : scala.collection.mutable.MutableList[MetricSourceDefinition] = scala.collection.mutable.MutableList.empty[MetricSourceDefinition]

    for (i <- 1 to 3) {
      var msd1 : MetricSourceDefinition = null
      if (i == 2) {
        msd1 = new MetricSourceDefinition("TestDefinition" + i, null, MetricSourceDefinitionType.CONFIG)
      } else {
        msd1 = new MetricSourceDefinition("TestDefinition" + i, "testAppId", MetricSourceDefinitionType.API)
      }
      definitions.+=(msd1)
    }

    val adMetadataStoreAccessor: AdMetadataStoreAccessor = EasyMockSugar.niceMock[AdMetadataStoreAccessor]
    expect(adMetadataStoreAccessor.getSavedInputDefinitions).andReturn(definitions.toList).once()
    expect(adMetadataStoreAccessor.removeInputDefinition(anyObject[String])).andReturn(true).times(2)
    replay(adMetadataStoreAccessor)

    val metricManagerService: MetricManagerServiceImpl = new MetricManagerServiceImpl(new AnomalyDetectionAppConfig, adMetadataStoreAccessor)

    metricManagerService.setAdMetadataStoreAccessor(adMetadataStoreAccessor)

    var success: Boolean = metricManagerService.deleteDefinitionByName("TestDefinition1")
    assert(success)
    success = metricManagerService.deleteDefinitionByName("TestDefinition2")
    assert(!success)
    success = metricManagerService.deleteDefinitionByName("TestDefinition3")
    assert(success)
  }

}
