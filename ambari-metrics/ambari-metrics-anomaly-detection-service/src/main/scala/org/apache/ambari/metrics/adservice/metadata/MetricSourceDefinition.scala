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

@SerialVersionUID(10001L)
@XmlRootElement
class MetricSourceDefinition extends Serializable{

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