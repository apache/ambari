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

package org.apache.ambari.metrics.adservice.db

import org.apache.ambari.metrics.adservice.metadata.MetricSourceDefinition

/**
  * Trait used to talk to the AD Metadata Store.
  */
trait AdMetadataStoreAccessor {

  /**
    * Return all saved component definitions from DB.
    * @return
    */
  def getSavedInputDefinitions: List[MetricSourceDefinition]

  /**
    * Save a set of component definitions
    * @param metricSourceDefinitions Set of component definitions
    * @return Success / Failure
    */
  def saveInputDefinitions(metricSourceDefinitions: List[MetricSourceDefinition]) : Boolean

  /**
    * Save a component definition
    * @param metricSourceDefinition component definition
    * @return Success / Failure
    */
  def saveInputDefinition(metricSourceDefinition: MetricSourceDefinition) : Boolean

  /**
    * Delete a component definition
    * @param definitionName component definition
    * @return
    */
  def removeInputDefinition(definitionName: String) : Boolean
}
