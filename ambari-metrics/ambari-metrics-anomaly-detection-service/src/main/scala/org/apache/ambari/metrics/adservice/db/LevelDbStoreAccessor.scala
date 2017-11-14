package org.apache.ambari.metrics.adservice.db

import org.apache.ambari.metrics.adservice.metadata.MetricSourceDefinition

import com.google.inject.Inject

class LevelDbStoreAccessor extends AdMetadataStoreAccessor{

  @Inject
  var levelDbDataSource : MetadataDatasource = _

  @Inject
  def this(levelDbDataSource: MetadataDatasource) = {
    this
    this.levelDbDataSource = levelDbDataSource
  }

  /**
    * Return all saved component definitions from DB.
    *
    * @return
    */
  override def getSavedInputDefinitions: List[MetricSourceDefinition] = {
    List.empty[MetricSourceDefinition]
  }

  /**
    * Save a set of component definitions
    *
    * @param metricSourceDefinitions Set of component definitions
    * @return Success / Failure
    */
override def saveInputDefinitions(metricSourceDefinitions: List[MetricSourceDefinition]): Boolean = {
  true
}

  /**
    * Save a component definition
    *
    * @param metricSourceDefinition component definition
    * @return Success / Failure
    */
  override def saveInputDefinition(metricSourceDefinition: MetricSourceDefinition): Boolean = {
    true
  }

  /**
    * Delete a component definition
    *
    * @param definitionName component definition
    * @return
    */
  override def removeInputDefinition(definitionName: String): Boolean = {
    true
  }
}
