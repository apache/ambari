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
import org.iq80.leveldb.util.FileUtils
import org.mockito.Mockito.when
import org.scalatest.{BeforeAndAfter, FunSuite, Matchers}
import org.scalatest.mockito.MockitoSugar

class LevelDBDataSourceTest extends FunSuite with BeforeAndAfter with Matchers with MockitoSugar {

  var db: LevelDBDataSource = _
  var file : File = FileUtils.createTempDir("adservice-leveldb-test")

  before {
    val appConfig: AnomalyDetectionAppConfig = mock[AnomalyDetectionAppConfig]
    val mdConfig : MetricDefinitionDBConfiguration = mock[MetricDefinitionDBConfiguration]

    when(appConfig.getMetricDefinitionDBConfiguration).thenReturn(mdConfig)
    when(mdConfig.verifyChecksums).thenReturn(true)
    when(mdConfig.performParanoidChecks).thenReturn(false)
    when(mdConfig.getDbDirPath).thenReturn(file.getAbsolutePath)

    db = new LevelDBDataSource(appConfig)
    db.initialize()
  }

  test("testOperations") {
    db.put("Hello".getBytes(), "World".getBytes())
    assert(db.get("Hello".getBytes()).get.sameElements("World".getBytes()))
    db.update(Seq("Hello".getBytes()), Seq(("Hello".getBytes(), "Mars".getBytes())))
    assert(db.get("Hello".getBytes()).get.sameElements("Mars".getBytes()))
  }

  after {
    FileUtils.deleteRecursively(file)
  }
}
