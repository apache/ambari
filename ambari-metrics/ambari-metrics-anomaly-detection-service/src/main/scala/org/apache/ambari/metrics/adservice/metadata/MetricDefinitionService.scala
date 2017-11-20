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

import org.apache.ambari.metrics.adservice.service.AbstractADService

trait MetricDefinitionService extends AbstractADService{

  /**
    * Given a 'UUID', return the metric key associated with it.
    * @param uuid UUID
    * @return
    */
  def getMetricKeyFromUuid(uuid: Array[Byte]) : MetricKey

  /**
    * Return all the definitions being tracked.
    * @return Map of Metric Source Definition name to Metric Source Definition.
    */
  def getDefinitions: List[MetricSourceDefinition]

  /**
    * Given a component definition name, return the definition associated with it.
    * @param name component definition name
    * @return
    */
  def getDefinitionByName(name: String) : MetricSourceDefinition

  /**
    * Add a new definition.
    * @param definition component definition JSON
    * @return
    */
  def addDefinition(definition: MetricSourceDefinition) : Boolean

  /**
    * Update a component definition by name. Only definitions which were added by API can be modified through API.
    * @param definition component definition name
    * @return
    */
  def updateDefinition(definition: MetricSourceDefinition) : Boolean

  /**
    * Delete a component definition by name. Only definitions which were added by API can be deleted through API.
    * @param name component definition name
    * @return
    */
  def deleteDefinitionByName(name: String) : Boolean

  /**
    * Given an appId, return set of definitions that are tracked for that appId.
    * @param appId component definition appId
    * @return
    */
  def getDefinitionByAppId(appId: String) : List[MetricSourceDefinition]

  /**
    * Return the mapping between definition name to set of metric keys.
    * @return Map of Metric Source Definition to set of metric keys associated with it.
    */
  def getMetricKeys:  Map[String, Set[MetricKey]]

}
