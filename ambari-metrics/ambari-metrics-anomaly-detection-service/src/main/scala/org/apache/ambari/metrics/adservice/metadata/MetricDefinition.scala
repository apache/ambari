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

import org.apache.commons.lang3.StringUtils
/*
   {
       "metric-name": "mem_free",
       "appId" : "HOST",
       "hosts" : ["h1","h2"],
       "metric-description" : "Free memory on a Host.",
       "troubleshooting-info" : "Sudden drop / hike in free memory on a host.",
       "static-threshold" : 10,
       “app-id” : “HOST”
}
 */

@SerialVersionUID(1002L)
class MetricDefinition extends Serializable {

  var metricName: String = _
  var appId: String = _
  var hosts: List[String] = List.empty[String]
  var metricDescription: String = ""
  var troubleshootingInfo: String = ""
  var staticThreshold: Double = _

  //A Metric definition is valid if we can resolve a metricName and appId (defined or inherited) at runtime)
  private var valid : Boolean = true

  def this(metricName: String,
           appId: String,
           hosts: List[String],
           metricDescription: String,
           troubleshootingInfo: String,
           staticThreshold: Double) = {
    this
    this.metricName = metricName
    this.appId = appId
    this.hosts = hosts
    this.metricDescription = metricDescription
    this.troubleshootingInfo = troubleshootingInfo
    this.staticThreshold = staticThreshold
  }

  @Override
  override def equals(obj: scala.Any): Boolean = {

    if (obj == null || (getClass ne obj.getClass))
      return false

    val that = obj.asInstanceOf[MetricDefinition]

    if (!(metricName == that.metricName))
      return false

    if (StringUtils.isNotEmpty(appId)) {
      appId == that.appId
    }
    else {
      StringUtils.isEmpty(that.appId)
    }
  }

  def isValid: Boolean = {
    valid
  }

  def makeInvalid() : Unit = {
    valid = false
  }
}

object MetricDefinition {

  def apply(metricName: String,
            appId: String,
            hosts: List[String],
            metricDescription: String,
            troubleshootingInfo: String,
            staticThreshold: Double): MetricDefinition =
    new MetricDefinition(metricName, appId, hosts, metricDescription, troubleshootingInfo, staticThreshold)

  def apply(metricName: String, appId: String, hosts: List[String]): MetricDefinition =
    new MetricDefinition(metricName, appId, hosts, null, null, -1)

}