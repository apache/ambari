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
class AmbariServiceConfigurationTest extends AbstractAmbariClientTest {

  private enum Scenario {
    CONFIGURATIONS, MULTIPLE_VERSIONS
  }

  def "test request service configurations map"() {
    given:
    mockResponses(Scenario.CONFIGURATIONS.name())

    when:
    Map<String, Map<String, String>> serviceConfigMap = ambari.getServiceConfigMap();

    then:
    serviceConfigMap != [:]
    serviceConfigMap.get("yarn-site") != [:]
  }

  def "test request service configurations with multiple versions"() {
    given:
    mockResponses(Scenario.MULTIPLE_VERSIONS.name())

    when:
    Map<String, Map<String, String>> serviceConfigMap = ambari.getServiceConfigMap();

    then:
    serviceConfigMap != [:]
    serviceConfigMap.get("yarn-site") != [:]
  }

  // ---- helper method definitions

  def protected String selectResponseJson(Map resourceRequestMap, String scenarioStr) {
    def thePath = resourceRequestMap.get("path")
    def theQuery = resourceRequestMap.get("query")
    def Scenario scenario = Scenario.valueOf(scenarioStr)

    def json = null
    if (thePath == TestResources.CLUSTERS.uri()) {
      json = "clusters.json"
    } else if (thePath == TestResources.CONFIGURATIONS.uri()) {
      if (!theQuery) {
        switch (scenario) {
          case Scenario.MULTIPLE_VERSIONS:
            json = "service-versions-multiple.json"
            break
          case Scenario.CONFIGURATIONS:
            json = "service-versions.json"
            break
        }
      } else {
        json = "service-config.json"
      }

    } else {
      log.error("Unsupported resource path: {}", thePath)
    }
    return json
  }

}
