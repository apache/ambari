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

package org.apache.ambari.metrics.adservice.leveldb

import java.io.File

import org.apache.ambari.metrics.adservice.app.AnomalyDetectionAppConfig
import org.apache.ambari.metrics.adservice.configuration.MetricDefinitionDBConfiguration
import org.apache.ambari.metrics.adservice.db.MetadataDatasource
import org.iq80.leveldb.{DB, Options, WriteOptions}
import org.iq80.leveldb.impl.Iq80DBFactory

import com.google.inject.Singleton

@Singleton
class LevelDBDataSource(appConfig: AnomalyDetectionAppConfig) extends MetadataDatasource {

  private var db: DB = _
  @volatile var isInitialized: Boolean = false

  override def initialize(): Unit = {
    if (isInitialized) return 

    val configuration: MetricDefinitionDBConfiguration = appConfig.getMetricDefinitionDBConfiguration

    db = createDB(new LevelDbConfig {
      override val createIfMissing: Boolean = true
      override val verifyChecksums: Boolean = configuration.verifyChecksums
      override val paranoidChecks: Boolean = configuration.performParanoidChecks
      override val path: String = configuration.getDbDirPath
    })
    isInitialized = true
  }

  private def createDB(levelDbConfig: LevelDbConfig): DB = {
    import levelDbConfig._

    val options = new Options()
      .createIfMissing(createIfMissing)
      .paranoidChecks(paranoidChecks) // raise an error as soon as it detects an internal corruption
      .verifyChecksums(verifyChecksums) // force checksum verification of all data that is read from the file system on behalf of a particular read

    Iq80DBFactory.factory.open(new File(path), options)
  }

  override def close(): Unit = {
    db.close()
  }

  /**
    * This function obtains the associated value to a key, if there exists one.
    *
    * @param key
    * @return the value associated with the passed key.
    */
  override def get(key: Key): Option[Value] = Option(db.get(key))

  /**
    * This function updates the DataSource by deleting, updating and inserting new (key-value) pairs.
    *
    * @param toRemove which includes all the keys to be removed from the DataSource.
    * @param toUpsert which includes all the (key-value) pairs to be inserted into the DataSource.
    *                 If a key is already in the DataSource its value will be updated.
    */
  override def update(toRemove: Seq[Key], toUpsert: Seq[(Key, Value)]): Unit = {
    val batch = db.createWriteBatch()
    toRemove.foreach { key => batch.delete(key) }
    toUpsert.foreach { item => batch.put(item._1, item._2) }
    db.write(batch, new WriteOptions())
  }

  override def put(key: Key, value: Value): Unit = {
    db.put(key, value)
  }

  override def delete(key: Key): Unit = {
    db.delete(key)
  }
}

trait LevelDbConfig {
  val createIfMissing: Boolean
  val paranoidChecks: Boolean
  val verifyChecksums: Boolean
  val path: String
}