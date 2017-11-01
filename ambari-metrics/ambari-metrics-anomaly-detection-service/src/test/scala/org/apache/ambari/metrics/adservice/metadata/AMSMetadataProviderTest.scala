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

import org.apache.ambari.metrics.adservice.configuration.MetricCollectorConfiguration
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetricKey
import org.scalatest.FunSuite

class AMSMetadataProviderTest extends FunSuite {

  test("testFromTimelineMetricKey") {
    val timelineMetricKeys: java.util.Set[TimelineMetricKey] = new java.util.HashSet[TimelineMetricKey]()

    val uuid: Array[Byte] = Array.empty[Byte]

    for (i <- 1 to 3) {
      val key: TimelineMetricKey = new TimelineMetricKey("M" + i, "App", null, "H", uuid)
      timelineMetricKeys.add(key)
    }

    val aMSMetadataProvider : ADMetadataProvider = new ADMetadataProvider(new MetricCollectorConfiguration)

    val metricKeys : Set[MetricKey] = aMSMetadataProvider.fromTimelineMetricKey(timelineMetricKeys)
    assert(metricKeys.size == 3)
  }

}
