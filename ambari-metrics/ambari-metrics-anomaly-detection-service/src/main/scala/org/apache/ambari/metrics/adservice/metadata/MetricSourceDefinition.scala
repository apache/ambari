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

import javax.xml.bind.annotation.XmlRootElement

import org.apache.ambari.metrics.adservice.metadata.MetricSourceDefinitionType.MetricSourceDefinitionType
import org.apache.ambari.metrics.adservice.model.AnomalyType.AnomalyType

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper

/*
{
 "definition-name": "host-memory",
 "app-id" : "HOST",
 "hosts" : [“c6401.ambari.apache.org”],
 "metric-definitions" : [
   {
       "metric-name": "mem_free",
       "metric-description" : "Free memory on a Host.",
       "troubleshooting-info" : "Sudden drop / hike in free memory on a host.",
       "static-threshold" : 10,
       “app-id” : “HOST”
}   ],

 "related-definition-names" : ["host-cpu", “host-network”],
 “anomaly-detection-subsystems” : [“point-in-time”, “trend”]
}
*/

/*

On Startup
Read input definitions directory, parse the JSONs
Create / Update the metric definitions in DB
Convert metric definitions to Map<MetricKey, MetricDefinition>

What to do want to have in memory?
Map of Metric Key -> List<Component Definitions>

What do we use metric definitions for?
Anomaly GET - Associate definition information as well.
Definition CRUD - Get definition given definition name
Get set of metrics that are being tracked
Return definition information for a metric key
Given a metric definition name, return set of metrics.

*/

@XmlRootElement
class MetricSourceDefinition {

  var definitionName: String = _
  var appId: String = _
  var definitionSource: MetricSourceDefinitionType = MetricSourceDefinitionType.CONFIG
  var hosts: List[String] = List.empty[String]
  var relatedDefinitions: List[String] = List.empty[String]
  var associatedAnomalySubsystems: List[AnomalyType] = List.empty[AnomalyType]

  var metricDefinitions: scala.collection.mutable.MutableList[MetricDefinition] =
    scala.collection.mutable.MutableList.empty[MetricDefinition]

  def this(definitionName: String, appId: String, source: MetricSourceDefinitionType) = {
    this
    this.definitionName = definitionName
    this.appId = appId
    this.definitionSource = source
  }

  def addMetricDefinition(metricDefinition: MetricDefinition): Unit = {
    if (!metricDefinitions.contains(metricDefinition)) {
      metricDefinitions.+=(metricDefinition)
    }
  }

  def removeMetricDefinition(metricDefinition: MetricDefinition): Unit = {
    metricDefinitions = metricDefinitions.filter(_ != metricDefinition)
  }

  @Override
  override def equals(obj: scala.Any): Boolean = {

    if (obj == null) {
      return false
    }
    val that = obj.asInstanceOf[MetricSourceDefinition]
    definitionName.equals(that.definitionName)
  }
}

object MetricSourceDefinition {
  val mapper = new ObjectMapper() with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)

  def serialize(definition: MetricSourceDefinition) : String = {
    mapper.writeValueAsString(definition)
  }

  def deserialize(definitionString: String) : MetricSourceDefinition = {
    mapper.readValue[MetricSourceDefinition](definitionString)
  }
}