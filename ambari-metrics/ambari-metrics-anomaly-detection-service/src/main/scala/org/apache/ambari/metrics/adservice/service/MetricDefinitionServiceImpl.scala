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

package org.apache.ambari.metrics.adservice.service

import org.apache.ambari.metrics.adservice.app.AnomalyDetectionAppConfig
import org.apache.ambari.metrics.adservice.db.AdMetadataStoreAccessor
import org.apache.ambari.metrics.adservice.metadata._
import org.slf4j.{Logger, LoggerFactory}

import com.google.inject.{Inject, Singleton}

@Singleton
class MetricDefinitionServiceImpl extends MetricDefinitionService {

  val LOG : Logger = LoggerFactory.getLogger(classOf[MetricDefinitionServiceImpl])

  var adMetadataStoreAccessor: AdMetadataStoreAccessor = _
  var configuration: AnomalyDetectionAppConfig = _
  var metricMetadataProvider: MetricMetadataProvider = _

  val metricSourceDefinitionMap: scala.collection.mutable.Map[String, MetricSourceDefinition] = scala.collection.mutable.Map()
  val metricDefinitionMetricKeyMap: scala.collection.mutable.Map[MetricSourceDefinition, Set[MetricKey]] = scala.collection.mutable.Map()
  val metricKeys: scala.collection.mutable.Set[MetricKey] = scala.collection.mutable.Set.empty[MetricKey]

  @Inject
  def this (anomalyDetectionAppConfig: AnomalyDetectionAppConfig, metadataStoreAccessor: AdMetadataStoreAccessor) = {
    this ()
    adMetadataStoreAccessor = metadataStoreAccessor
    configuration = anomalyDetectionAppConfig
  }

  @Override
  def initialize() : Unit = {
    LOG.info("Initializing Metric Definition Service...")

    //Initialize Metric Metadata Provider
    metricMetadataProvider = new ADMetadataProvider(configuration.getMetricCollectorConfiguration)

    //Load definitions from metadata store
    val definitionsFromStore: List[MetricSourceDefinition] = adMetadataStoreAccessor.getSavedInputDefinitions
    for (definition <- definitionsFromStore) {
      sanitizeMetricSourceDefinition(definition)
    }

    //Load definitions from configs
    val definitionsFromConfig: List[MetricSourceDefinition] = getInputDefinitionsFromConfig
    for (definition <- definitionsFromConfig) {
      sanitizeMetricSourceDefinition(definition)
    }

    //Union the 2 sources, with DB taking precedence.
    //Save new definition list to DB.
    metricSourceDefinitionMap.++=(combineDefinitionSources(definitionsFromConfig, definitionsFromStore))

    //Reach out to AMS Metadata and get Metric Keys. Pass in MSD and get back Set<MK>
    for (definition <- metricSourceDefinitionMap.values) {
      val keys: Set[MetricKey] = metricMetadataProvider.getMetricKeysForDefinitions(definition)
      metricDefinitionMetricKeyMap(definition) = keys
      metricKeys.++=(keys)
    }

    LOG.info("Successfully initialized Metric Definition Service.")
  }

  def getMetricKeyFromUuid(uuid: Array[Byte]): MetricKey = {
    var key: MetricKey = null
    for (metricKey <- metricKeys) {
      if (metricKey.uuid.sameElements(uuid)) {
        key = metricKey
      }
    }
    key
  }

  @Override
  def getDefinitions: List[MetricSourceDefinition] = {
    metricSourceDefinitionMap.values.toList
  }

  @Override
  def getDefinitionByName(name: String): MetricSourceDefinition = {
    if (!metricSourceDefinitionMap.contains(name)) {
      LOG.warn("Metric Source Definition with name " + name + " not found")
      null
    } else {
      metricSourceDefinitionMap.apply(name)
    }
  }

  @Override
  def addDefinition(definition: MetricSourceDefinition): Boolean = {
    if (metricSourceDefinitionMap.contains(definition.definitionName)) {
      LOG.info("Definition with name " + definition.definitionName + " already present.")
      return false
    }
    definition.definitionSource = MetricSourceDefinitionType.API

    val success: Boolean = adMetadataStoreAccessor.saveInputDefinition(definition)
    if (success) {
      metricSourceDefinitionMap += definition.definitionName -> definition
      val keys: Set[MetricKey] = metricMetadataProvider.getMetricKeysForDefinitions(definition)
      metricDefinitionMetricKeyMap(definition) = keys
      metricKeys.++=(keys)
      LOG.info("Successfully created metric source definition : " + definition.definitionName)
    }
    success
  }

  @Override
  def updateDefinition(definition: MetricSourceDefinition): Boolean = {
    if (!metricSourceDefinitionMap.contains(definition.definitionName)) {
      LOG.warn("Metric Source Definition with name " + definition.definitionName + " not found")
      return false
    }

    if (metricSourceDefinitionMap.apply(definition.definitionName).definitionSource != MetricSourceDefinitionType.API) {
      return false
    }
    definition.definitionSource = MetricSourceDefinitionType.API

    val success: Boolean = adMetadataStoreAccessor.saveInputDefinition(definition)
    if (success) {
      metricSourceDefinitionMap += definition.definitionName -> definition
      val keys: Set[MetricKey] = metricMetadataProvider.getMetricKeysForDefinitions(definition)
      metricDefinitionMetricKeyMap(definition) = keys
      metricKeys.++=(keys)
      LOG.info("Successfully updated metric source definition : " + definition.definitionName)
    }
    success
  }

  @Override
  def deleteDefinitionByName(name: String): Boolean = {
    if (!metricSourceDefinitionMap.contains(name)) {
      LOG.warn("Metric Source Definition with name " + name + " not found")
      return false
    }

    val definition : MetricSourceDefinition = metricSourceDefinitionMap.apply(name)
    if (definition.definitionSource != MetricSourceDefinitionType.API) {
      LOG.warn("Cannot delete metric source definition which was not created through API.")
      return false
    }

    val success: Boolean = adMetadataStoreAccessor.removeInputDefinition(name)
    if (success) {
      metricSourceDefinitionMap -= definition.definitionName
      metricKeys.--=(metricDefinitionMetricKeyMap.apply(definition))
      metricDefinitionMetricKeyMap -= definition
      LOG.info("Successfully deleted metric source definition : " + name)
    }
    success
  }

  @Override
  def getDefinitionByAppId(appId: String): List[MetricSourceDefinition] = {

    val defList : List[MetricSourceDefinition] = metricSourceDefinitionMap.values.toList
    defList.filter(_.appId == appId)
  }

  def combineDefinitionSources(configDefinitions: List[MetricSourceDefinition], dbDefinitions: List[MetricSourceDefinition])
  : Map[String, MetricSourceDefinition] = {

    var combinedDefinitionMap: scala.collection.mutable.Map[String, MetricSourceDefinition] =
      scala.collection.mutable.Map.empty[String, MetricSourceDefinition]

    for (definitionFromDb <- dbDefinitions) {
      combinedDefinitionMap(definitionFromDb.definitionName) = definitionFromDb
    }

    for (definition <- configDefinitions) {
      if (!dbDefinitions.contains(definition)) {
        adMetadataStoreAccessor.saveInputDefinition(definition)
        combinedDefinitionMap(definition.definitionName) = definition
      }
    }
    combinedDefinitionMap.toMap
  }

  def getInputDefinitionsFromConfig: List[MetricSourceDefinition] = {
    val configDirectory = configuration.getMetricDefinitionServiceConfiguration.getInputDefinitionDirectory
    InputMetricDefinitionParser.parseInputDefinitionsFromDirectory(configDirectory)
  }

  def setAdMetadataStoreAccessor (adMetadataStoreAccessor: AdMetadataStoreAccessor) : Unit = {
    this.adMetadataStoreAccessor = adMetadataStoreAccessor
  }

  /**
    * Look into the Metric Definitions inside a Metric Source definition, and push down source level appId &
    * hosts to Metric definition if they do not have an override.
    * @param metricSourceDefinition Input Metric Source Definition
    */
  def sanitizeMetricSourceDefinition(metricSourceDefinition: MetricSourceDefinition): Unit = {
    val sourceLevelAppId: String = metricSourceDefinition.appId
    val sourceLevelHostList: List[String] = metricSourceDefinition.hosts

    for (metricDef <- metricSourceDefinition.metricDefinitions.toList) {
      if (metricDef.appId == null) {
        if (sourceLevelAppId == null || sourceLevelAppId.isEmpty) {
          metricDef.makeInvalid()
        } else {
          metricDef.appId = sourceLevelAppId
        }
      }

      if (metricDef.isValid && (metricDef.hosts == null || metricDef.hosts.isEmpty)) {
        if (sourceLevelHostList != null && sourceLevelHostList.nonEmpty) {
          metricDef.hosts = sourceLevelHostList
        }
      }
    }
  }

  /**
    * Return the mapping between definition name to set of metric keys.
    *
    * @return Map of Metric Source Definition to set of metric keys associated with it.
    */
  override def getMetricKeys: Map[String, Set[MetricKey]] = {
    val metricKeyMap: scala.collection.mutable.Map[String, Set[MetricKey]] = scala.collection.mutable.Map()
    for (definition <- metricSourceDefinitionMap.values) {
      metricKeyMap(definition.definitionName) = metricDefinitionMetricKeyMap.apply(definition)
    }
    metricKeyMap.toMap
  }

  /**
    * Return the set of metric keys.
    *
    * @return Set of metric keys.
    */
  override def getMetricKeyList: Set[MetricKey] = {
    metricKeys.toSet
  }
}
