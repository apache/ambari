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

import org.apache.ambari.metrics.adservice.app.AnomalyDetectionAppConfig
import org.apache.ambari.metrics.adservice.db.AdMetadataStoreAccessor

import com.google.inject.{Inject, Singleton}

@Singleton
class MetricManagerServiceImpl extends MetricManagerService {

  @Inject
  var adMetadataStoreAccessor: AdMetadataStoreAccessor = _

  var configuration: AnomalyDetectionAppConfig = _
  var metricMetadataProvider: MetricMetadataProvider = _

  var metricSourceDefinitionMap: Map[String, MetricSourceDefinition] = Map()
  var metricKeys: Set[MetricKey] = Set.empty[MetricKey]
  var metricDefinitionMetricKeyMap: Map[MetricDefinition, Set[MetricKey]] = Map()

  @Inject
  def this (anomalyDetectionAppConfig: AnomalyDetectionAppConfig) = {
    this ()
    //TODO : Create AD Metadata instance here (or inject)
    configuration = anomalyDetectionAppConfig
    initializeService()
  }

  def this (anomalyDetectionAppConfig: AnomalyDetectionAppConfig, adMetadataStoreAccessor: AdMetadataStoreAccessor) = {
    this ()
    //TODO : Create AD Metadata instance here (or inject). Pass in Schema information.
    configuration = anomalyDetectionAppConfig
    this.adMetadataStoreAccessor = adMetadataStoreAccessor
    initializeService()
  }

  def initializeService() : Unit = {

    //Create AD Metadata Schema
    //TODO Make sure AD Metadata DB is initialized here.

    //Initialize Metric Metadata Provider
    metricMetadataProvider = new ADMetadataProvider(configuration.getMetricCollectorConfiguration)

    loadMetricSourceDefinitions()
  }

  def loadMetricSourceDefinitions() : Unit = {

    //Load definitions from metadata store
    val definitionsFromStore: List[MetricSourceDefinition] = adMetadataStoreAccessor.getSavedInputDefinitions

    //Load definitions from configs
    val definitionsFromConfig: List[MetricSourceDefinition] = getInputDefinitionsFromConfig

    //Union the 2 sources, with DB taking precedence.
    //Save new definition list to DB.
    metricSourceDefinitionMap = metricSourceDefinitionMap.++(combineDefinitionSources(definitionsFromConfig, definitionsFromStore))

      //Reach out to AMS Metadata and get Metric Keys. Pass in List<CD> and get back Map<MD,Set<MK>>
    for (definition <- metricSourceDefinitionMap.values) {
      val (definitionKeyMap: Map[MetricDefinition, Set[MetricKey]], keys: Set[MetricKey])= metricMetadataProvider.getMetricKeysForDefinitions(definition)
      metricDefinitionMetricKeyMap = metricDefinitionMetricKeyMap.++(definitionKeyMap)
      metricKeys = metricKeys.++(keys)
    }
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
  def getDefinitionByName(name: String): MetricSourceDefinition = {
    metricSourceDefinitionMap.apply(name)
  }

  @Override
  def addDefinition(definition: MetricSourceDefinition): Boolean = {
    if (metricSourceDefinitionMap.contains(definition.definitionName)) {
      return false
    }
    definition.definitionSource = MetricSourceDefinitionType.API

    val success: Boolean = adMetadataStoreAccessor.saveInputDefinition(definition)
    if (success) {
      metricSourceDefinitionMap += definition.definitionName -> definition
    }
    success
  }

  @Override
  def updateDefinition(definition: MetricSourceDefinition): Boolean = {
    if (!metricSourceDefinitionMap.contains(definition.definitionName)) {
      return false
    }

    if (metricSourceDefinitionMap.apply(definition.definitionName).definitionSource != MetricSourceDefinitionType.API) {
      return false
    }

    val success: Boolean = adMetadataStoreAccessor.saveInputDefinition(definition)
    if (success) {
      metricSourceDefinitionMap += definition.definitionName -> definition
    }
    success
  }

  @Override
  def deleteDefinitionByName(name: String): Boolean = {
    if (!metricSourceDefinitionMap.contains(name)) {
      return false
    }

    val definition : MetricSourceDefinition = metricSourceDefinitionMap.apply(name)
    if (definition.definitionSource != MetricSourceDefinitionType.API) {
      return false
    }

    val success: Boolean = adMetadataStoreAccessor.removeInputDefinition(name)
    if (success) {
      metricSourceDefinitionMap += definition.definitionName -> definition
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
    val configDirectory = configuration.getMetricManagerServiceConfiguration.getInputDefinitionDirectory
    InputMetricDefinitionParser.parseInputDefinitionsFromDirectory(configDirectory)
  }

  def setAdMetadataStoreAccessor (adMetadataStoreAccessor: AdMetadataStoreAccessor) : Unit = {
    this.adMetadataStoreAccessor = adMetadataStoreAccessor
  }
}
