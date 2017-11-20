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

package org.apache.ambari.metrics.adservice.db

import org.apache.ambari.metrics.adservice.metadata.MetricSourceDefinition
import org.apache.commons.lang.SerializationUtils

import com.google.inject.Inject

/**
  * Implementation of the AdMetadataStoreAccessor.
  * Serves as the adaptor between metric definition service and LevelDB worlds.
  */
class AdMetadataStoreAccessorImpl extends AdMetadataStoreAccessor {

  @Inject
  var metadataDataSource: MetadataDatasource = _

  @Inject
  def this(metadataDataSource: MetadataDatasource) = {
    this
    this.metadataDataSource = metadataDataSource
  }

  /**
    * Return all saved component definitions from DB.
    *
    * @return
    */
  override def getSavedInputDefinitions: List[MetricSourceDefinition] = {
    val valuesFromStore : List[MetadataDatasource#Value] = metadataDataSource.getAll
    val definitions = scala.collection.mutable.MutableList.empty[MetricSourceDefinition]

    for (value : Array[Byte] <- valuesFromStore) {
      val definition : MetricSourceDefinition = SerializationUtils.deserialize(value).asInstanceOf[MetricSourceDefinition]
      if (definition != null) {
        definitions.+=(definition)
      }
    }
    definitions.toList
  }

  /**
    * Save a set of component definitions
    *
    * @param metricSourceDefinitions Set of component definitions
    * @return Success / Failure
    */
  override def saveInputDefinitions(metricSourceDefinitions: List[MetricSourceDefinition]): Boolean = {
    for (definition <- metricSourceDefinitions) {
      saveInputDefinition(definition)
    }
    true
  }

  /**
    * Save a component definition
    *
    * @param metricSourceDefinition component definition
    * @return Success / Failure
    */
  override def saveInputDefinition(metricSourceDefinition: MetricSourceDefinition): Boolean = {
    val storeValue : MetadataDatasource#Value = SerializationUtils.serialize(metricSourceDefinition)
    val storeKey : MetadataDatasource#Key = metricSourceDefinition.definitionName.getBytes()
    metadataDataSource.put(storeKey, storeValue)
    true
  }

  /**
    * Delete a component definition
    *
    * @param definitionName component definition
    * @return
    */
  override def removeInputDefinition(definitionName: String): Boolean = {
    val storeKey : MetadataDatasource#Key = definitionName.getBytes()
    metadataDataSource.delete(storeKey)
    true
  }
}
