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

import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j

@Slf4j
class AmbariBlueprintsTest extends AbstractAmbariClientTest {

  def slurper = new JsonSlurper()

  private enum Scenario {
    CLUSTERS, NO_CLUSTERS, BLUEPRINT_EXISTS, NO_BLUEPRINT, HOSTS, NO_HOSTS
  }

  def "test get the name of the cluster"() {
    given:
    mockResponses(Scenario.CLUSTERS.name())

    when:
    def result = ambari.getClusterName()

    then:
    "MySingleNodeCluster" == result
  }

  def "test get the name when there is no cluster"() {
    given:
    mockResponses(Scenario.NO_CLUSTERS.name())

    when:
    def result = ambari.getClusterName()

    then:
    null == result
  }

  def "test blueprint doesn't exist"() {
    given:
    mockResponses(Scenario.NO_BLUEPRINT.name())

    when:
    def result = ambari.doesBlueprintExist("inexistent-blueprint")

    then:
    !result
  }

  def "test blueprint exists"() {
    given:
    mockResponses(Scenario.BLUEPRINT_EXISTS.name())

    when:
    def result = ambari.doesBlueprintExist("single-node-hdfs-yarn")

    then:
    result
  }

  def "test get blueprint as map"() {
    given:
    mockResponses(Scenario.BLUEPRINT_EXISTS.name())

    when:
    def response = ambari.getBlueprintMap("single-node-hdfs-yarn")

    then:
    response.keySet().size() == 1
    response.containsKey('host_group_1')
  }


  def "test get blueprint as map when there's no blueprint"() {
    given:
    mockResponses(Scenario.NO_BLUEPRINT.name())
    when:
    def response = ambari.getBlueprintMap("inexistent-blueprint")

    then:
    [:] == response
  }

  def "test get host groups"() {
    given:
    mockResponses(Scenario.BLUEPRINT_EXISTS.name())

    when:
    def result = ambari.getHostGroups("single-node-hdfs-yarn")

    then:
    ["host_group_1"] == result
  }

  def "test get host groups for no groups"() {
    given:
    mockResponses(Scenario.NO_BLUEPRINT.name())

    when:
    def result = ambari.getHostGroups("inexistent-blueprint")

    then:
    [] == result
  }

  def "test get host names"() {
    given:
    mockResponses(Scenario.HOSTS.name())

    when:
    def result = ambari.getHostNames()

    then:
    "UNHEALTHY" == result["server.ambari.com"]
    1 == result.size()
  }

  def "test get host names for empty result"() {
    given:
    mockResponses(Scenario.NO_HOSTS.name())

    when:
    def result = ambari.getHostNames()

    then:
    [:] == result
  }

  def "test validate blueprint"() {
    given:
    def json = getClass().getClassLoader().getResourceAsStream("blueprint.json").text

    when:
    ambari.validateBlueprint(json)

    then:
    noExceptionThrown()
  }

  def "test validate blueprint no slaves_"() {
    given:
    def json = getClass().getClassLoader().getResourceAsStream("hdp-multinode-default2.json").text

    when:
    ambari.validateBlueprint(json)

    then:
    thrown(InvalidBlueprintException)
  }

  def "test validate blueprint with uppercase SLAVE_"() {
    given:
    def json = getClass().getClassLoader().getResourceAsStream("hdp-multinode-default.json").text

    when:
    ambari.validateBlueprint(json)

    then:
    notThrown(InvalidBlueprintException)
  }

  def "test validate blueprint for null json"() {
    when:
    ambari.validateBlueprint(null)

    then:
    thrown(InvalidBlueprintException)
  }

  def "test add blueprint with configuration"() {
    given:
    def json = getClass().getClassLoader().getResourceAsStream("blueprint.json").text
    ambari.metaClass.postBlueprint = { String blueprint -> return }

    when:
    def config = [
      "yarn-site": ["property-key": "property-value", "yarn.nodemanager.local-dirs": "/mnt/fs1/,/mnt/fs2/"],
      "hdfs-site": ["dfs.datanode.data.dir": "/mnt/fs1/,/mnt/fs2/"]
    ]
    def blueprint = ambari.addBlueprint(json, config)

    then:
    def expected = slurper.parseText(getClass().getClassLoader().getResourceAsStream("blueprint-config.json").text)
    def actual = slurper.parseText(blueprint)
    actual == expected
  }

  def "test add blueprint with existing configuration"() {
    given:
    def json = getClass().getClassLoader().getResourceAsStream("multi-node-hdfs-yarn.json").text
    ambari.metaClass.postBlueprint = { String blueprint -> return }

    when:
    def config = [
      "yarn-site": ["property-key": "property-value", "yarn.nodemanager.local-dirs": "apple"],
      "hdfs-site": ["dfs.datanode.data.dir": "/mnt/fs1/,/mnt/fs2/"],
      "core-site": ["fs.defaultFS": "localhost:9000"]
    ]
    def blueprint = ambari.addBlueprint(json, config)

    then:
    def expected = slurper.parseText(getClass().getClassLoader().getResourceAsStream("multi-node-hdfs-yarn-config.json").text)
    def actual = slurper.parseText(blueprint)
    actual == expected
  }

  def "test add blueprint with empty configuration"() {
    given:
    def json = getClass().getClassLoader().getResourceAsStream("blueprint.json").text
    ambari.metaClass.postBlueprint = { String blueprint -> return }

    when:
    def blueprint = ambari.addBlueprint(json, [:])

    then:
    def expected = slurper.parseText(json)
    def actual = slurper.parseText(blueprint)
    actual == expected
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
        case Scenario.NO_CLUSTERS: json = "no-clusters.json"
          break
      }
    } else if (thePath == TestResources.BLUEPRINTS.uri) {
      switch (scenario) {
        case Scenario.BLUEPRINT_EXISTS: json = "blueprints.json"
          break
        case Scenario.NO_BLUEPRINT: json = "no-blueprints.json"
          break
      }
    } else if (thePath == TestResources.BLUEPRINT.uri) {
      switch (scenario) {
        case Scenario.BLUEPRINT_EXISTS: json = "blueprint.json"
          break
        case Scenario.NO_BLUEPRINT: json = "no-blueprint.json"
          break
      }
    } else if (thePath == TestResources.INEXISTENT_BLUEPRINT.uri) {
      switch (scenario) {
        case Scenario.NO_BLUEPRINT: json = "no-blueprint.json"
          break
      }
    } else if (thePath == TestResources.CONFIGURATIONS.uri()) {
      if (query) {
        json = "service-config.json"
      } else {
        json = "service-versions.json"
      }
    } else if (thePath == TestResources.HOSTS.uri()) {
      switch (scenario) {
        case Scenario.HOSTS: json = "hosts.json"
          break
        case Scenario.NO_HOSTS: json = "no-hosts.json"
          break
      }
    } else {
      log.error("Unsupported resource path: {}", thePath)
    }
    return json
  }

}
