/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.groovy.client

import groovy.util.logging.Slf4j

@Slf4j
class AmbariHostsTest extends AbstractAmbariClientTest {

  private enum Scenario {
    CLUSTERS
  }

  def "test get host components as map when there is no cluster yet"() {
    given:
    ambari.metaClass.getHostComponenets = { return null }

    when:
    def result = ambari.getHostComponentsMap("host")

    then:
    [:] == result
  }

  def "test get host components as map when there is a cluster"() {
    given:
    mockResponses(Scenario.CLUSTERS.name())

    when:
    def result = ambari.getHostComponentsMap("host")

    then:
    ["DATANODE"          : "STARTED",
     "HDFS_CLIENT"       : "INSTALLED",
     "HISTORYSERVER"     : "STARTED",
     "MAPREDUCE2_CLIENT" : "INSTALLED",
     "NAMENODE"          : "STARTED",
     "NODEMANAGER"       : "STARTED",
     "RESOURCEMANAGER"   : "STARTED",
     "SECONDARY_NAMENODE": "STARTED",
     "YARN_CLIENT"       : "INSTALLED",
     "ZOOKEEPER_CLIENT"  : "INSTALLED",
     "ZOOKEEPER_SERVER"  : "STARTED"
    ] == result
  }

  def protected String selectResponseJson(Map resourceRequestMap, String scenarioStr) {
    def thePath = resourceRequestMap.get("path");
    def query = resourceRequestMap.get("query");
    def Scenario scenario = Scenario.valueOf(scenarioStr)
    def json = null
    if (thePath == TestResources.CLUSTERS.uri()) {
      switch (scenario) {
        case Scenario.CLUSTERS: json = "clusters.json"
          break
      }
    } else if (thePath == TestResources.HOST_COMPONENTS.uri()) {
      json = "host-components.json"
    } else {
      log.error("Unsupported resource path: {}", thePath)
    }
    return json
  }

}
