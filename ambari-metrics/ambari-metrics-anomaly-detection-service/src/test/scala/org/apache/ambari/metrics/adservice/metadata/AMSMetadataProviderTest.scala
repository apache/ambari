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

import java.util

import org.apache.ambari.metrics.adservice.configuration.MetricCollectorConfiguration
import org.scalatest.FunSuite

class AMSMetadataProviderTest extends FunSuite {

  test("testFromTimelineMetricKey") {
    val timelineMetricKeys: java.util.Set[java.util.Map[String, String]] = new java.util.HashSet[java.util.Map[String, String]]()

    val uuid: Array[Byte] = Array.empty[Byte]

    for (i <- 1 to 3) {
      val keyMap: java.util.Map[String, String] = new util.HashMap[String, String]()
      keyMap.put("metricName", "M" + i)
      keyMap.put("appId", "App")
      keyMap.put("hostname", "H")
      keyMap.put("uuid", new String(uuid))
      timelineMetricKeys.add(keyMap)
    }

    val aMSMetadataProvider : ADMetadataProvider = new ADMetadataProvider(new MetricCollectorConfiguration)

    val metricKeys : Set[MetricKey] = aMSMetadataProvider.getMetricKeys(timelineMetricKeys)
    assert(metricKeys.size == 3)
  }


}
