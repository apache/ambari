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
class AmbariRecommendTest extends AbstractAmbariClientTest {

  private enum Scenario {
    SINGLE_NODE_BLUEPRINT, MULTI_NODE_BLUEPRINT, MULTI_NODE_BLUEPRINT2
  }

  def "test recommend for single node"() {
    given:
    mockResponses(Scenario.SINGLE_NODE_BLUEPRINT.name())
    ambari.metaClass.getHostNames = { return ["amb0": "HEALTHY"] }

    when:
    def result = ambari.recommendAssignments("single-node-hdfs-yarn")

    then:
    [host_group_1: ["amb0"]] == result
  }

  def "test recommend for invalid host number"() {
    given:
    mockResponses(Scenario.MULTI_NODE_BLUEPRINT.name())
    ambari.metaClass.getHostNames = { return ["amb0": "HEALTHY"] }

    when:
    def result
    try {
      result = ambari.recommendAssignments("hdp-multinode-default")
    } catch (InvalidHostGroupHostAssociation e) {
      result = e.getMinRequiredHost()
    }

    then:
    result == 7
  }

  def "test recommend for no slave group"() {
    given:
    mockResponses(Scenario.MULTI_NODE_BLUEPRINT2.name())
    ambari.metaClass.getHostNames = {
      return [
        "amb0": "HEALTHY",
        "amb1": "HEALTHY",
        "amb2": "HEALTHY",
        "amb3": "HEALTHY",
        "amb4": "HEALTHY",
        "amb5": "HEALTHY",
        "amb6": "HEALTHY",
        "amb7": "HEALTHY",
        "amb8": "HEALTHY",
        "amb9": "HEALTHY",
        "am10": "HEALTHY",
        "am10": "HEALTHY",
        "am20": "HEALTHY",
        "am30": "HEALTHY",
        "am40": "HEALTHY",
      ]
    }

    when:
    def result
    def msg
    try {
      result = ambari.recommendAssignments("hdp-multinode-default2")
    } catch (InvalidHostGroupHostAssociation e) {
      msg = e.getMessage()
      result = e.getMinRequiredHost()
    }

    then:
    result == 5
    msg == "At least one 'slave_' is required"
  }

  def "test recommend for multi node"() {
    given:
    mockResponses(Scenario.MULTI_NODE_BLUEPRINT.name())
    ambari.metaClass.getHostNames = {
      return [
        "amb0": "HEALTHY",
        "amb1": "HEALTHY",
        "amb2": "HEALTHY",
        "amb3": "HEALTHY",
        "amb4": "HEALTHY",
        "amb5": "HEALTHY",
        "amb6": "HEALTHY",
        "amb7": "HEALTHY",
        "amb8": "HEALTHY",
        "amb9": "HEALTHY",
        "am10": "HEALTHY",
        "am10": "HEALTHY",
        "am20": "HEALTHY",
        "am30": "HEALTHY",
        "am40": "HEALTHY",
      ]
    }

    when:
    def result = ambari.recommendAssignments("hdp-multinode-default")

    then:
    [master_1: ["amb0"],
     master_2: ["amb1"],
     master_3: ["amb2"],
     master_4: ["amb3"],
     gateway : ["amb4"],
     slave_1 : ["amb5", "amb7", "amb9", "am20", "am40"],
     SLAVE_2 : ["amb6", "amb8", "am10", "am30"]
    ] == result
  }

  def protected String selectResponseJson(Map resourceRequestMap, String scenarioStr) {
    def thePath = resourceRequestMap.get("path");
    def query = resourceRequestMap.get("query");
    def Scenario scenario = Scenario.valueOf(scenarioStr)
    def json = null
    if (thePath == TestResources.BLUEPRINT.uri()) {
      json = "blueprint.json"
    } else if (thePath == TestResources.BLUEPRINT_MULTI.uri()) {
      json = "hdp-multinode-default.json"
    } else if (thePath == TestResources.BLUEPRINT_MULTI2.uri()) {
      json = "hdp-multinode-default2.json"
    } else {
      log.error("Unsupported resource path: {}", thePath)
    }
    return json
  }

}
