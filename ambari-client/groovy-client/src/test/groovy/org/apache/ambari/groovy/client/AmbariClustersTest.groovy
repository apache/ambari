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
class AmbariClustersTest extends AbstractAmbariClientTest {

  private enum Scenario {
    CLUSTERS, CLUSTER
  }

  def "test get cluster as JSON"() {
    given:
    mockResponses(Scenario.CLUSTER.name())

    expect:
    String json = ambari.getClusterAsJson();
    log.debug("JSON: {}", json)

  }

  def "test get clusters as JSON"() {
    given:
    mockResponses(Scenario.CLUSTERS.name())

    expect:
    String json = ambari.getClustersAsJson();
    log.debug("JSON: {}", json)
  }

  def protected String selectResponseJson(Map resourceRequestMap, String scenarioStr) {
    def thePath = resourceRequestMap.get("path");
    def query = resourceRequestMap.get("query");
    def Scenario scenario = Scenario.valueOf(scenarioStr)
    def json = null
    if (thePath == TestResources.CLUSTERS.uri()) {
      json = "clusters.json"
    } else if (thePath == TestResources.CLUSTER.uri()) {
      switch (scenario) {
        case Scenario.CLUSTER: json = "clusterAll.json"
          break
      }
    } else {
      log.error("Unsupported resource path: {}", thePath)
    }
    return json
  }
}
