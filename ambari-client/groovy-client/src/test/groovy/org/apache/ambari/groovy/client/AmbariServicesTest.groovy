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
class AmbariServicesTest extends AbstractAmbariClientTest {

  def slurper = new JsonSlurper()

  private enum Scenario {
    SERVICES, NO_SERVICES, NO_SERVICE_COMPONENTS
  }

  def "test get service components map"() {
    given:
    // get the name of the cluster
    super.mockResponses(Scenario.SERVICES.name())

    when:
    def Map result = ambari.getServiceComponentsMap()

    then:
    [HDFS: [
      DATANODE          : "STARTED",
      HDFS_CLIENT       : "INSTALLED",
      NAMENODE          : "STARTED",
      SECONDARY_NAMENODE: "STARTED"]
    ] == result
  }


  def "test get service components map for empty services"() {
    given:
    mockResponses(Scenario.NO_SERVICES.name())

    when:
    def Map result = ambari.getServiceComponentsMap()

    then:
    [:] == result
  }

  def "test get service components map for empty components"() {
    given:
    // get the name of the cluster
    mockResponses(Scenario.NO_SERVICE_COMPONENTS.name())

    when:
    def Map result = ambari.getServiceComponentsMap()

    then:
    [HDFS: [:]] == result
  }

  def "test get services as map"() {
    given:
    super.mockResponses(Scenario.SERVICES.name())

    when:
    def result = ambari.getServicesMap()

    then:
    [HDFS: "STARTED"] == result
  }

  def "test get services as map for empty result"() {
    given:
    mockResponses(Scenario.NO_SERVICES.name())

    when:
    def result = ambari.getServicesMap()

    then:
    [:] == result
  }


  def "test services started"() {
    given:
    // get the name of the cluster
    super.mockResponses(Scenario.SERVICES.name())

    when:
    def boolean result = ambari.servicesStarted()

    then:
    result
  }

  def "test services stopped"() {
    given:
    // get the name of the cluster
    super.mockResponses(Scenario.SERVICES.name())

    when:
    def boolean result = ambari.servicesStopped()

    then:
    !result
  }

  def "test stop all services"() {
    given:
    def context
    ambari.metaClass.getClusterName = { return "cluster" }
    ambari.getAmbari().metaClass.put = { Map request ->
      context = request
    }
    ambari.getSlurper().metaClass.parseText { String text -> return ["Requests": ["id": 1]] }

    when:
    def id = ambari.stopAllServices()

    then:
    1 == id
    context.path == "http://localhost:8080/api/v1/clusters/cluster/services"
    def body = slurper.parseText(context.body)
    body.RequestInfo.context == "Stop All Services"
    body.ServiceInfo.state == "INSTALLED"
  }

  def "test start service ZOOKEEPER"() {
    given:
    def context
    ambari.metaClass.getClusterName = { return "cluster" }
    ambari.getAmbari().metaClass.put = { Map request ->
      context = request
    }
    ambari.getSlurper().metaClass.parseText { String text -> return ["Requests": ["id": 1]] }

    when:
    def id = ambari.startService("ZOOKEEPER")

    then:
    1 == id
    context.path == "http://localhost:8080/api/v1/clusters/cluster/services/ZOOKEEPER"
    def body = slurper.parseText(context.body)
    body.RequestInfo.context == "Starting ZOOKEEPER"
    body.ServiceInfo.state == "STARTED"
  }

  def "test stop service ZOOKEEPER"() {
    given:
    def context
    ambari.metaClass.getClusterName = { return "cluster" }
    ambari.getAmbari().metaClass.put = { Map request ->
      context = request
    }
    ambari.getSlurper().metaClass.parseText { String text -> return ["Requests": ["id": 1]] }

    when:
    def id = ambari.stopService("ZOOKEEPER")

    then:
    1 == id
    context.path == "http://localhost:8080/api/v1/clusters/cluster/services/ZOOKEEPER"
    def body = slurper.parseText(context.body)
    body.RequestInfo.context == "Stopping ZOOKEEPER"
    body.ServiceInfo.state == "INSTALLED"
  }

  def private String selectResponseJson(Map resourceRequestMap, String scenarioStr) {
    def thePath = resourceRequestMap.get("path");
    def Scenario scenario = Scenario.valueOf(scenarioStr)

    def json = null
    if (thePath == TestResources.CLUSTERS.uri()) {
      json = "clusters.json"
    } else if (thePath == TestResources.SERVICES.uri()) {
      switch (scenario) {
        case Scenario.SERVICES: json = "services.json"
          break
        case Scenario.NO_SERVICES: json = "no-services.json"
          break
        case Scenario.NO_SERVICE_COMPONENTS:
          json = "services.json"
          break
      }
    } else if (thePath == TestResources.SERVICE_COMPONENTS.uri()) {
      switch (scenario) {
        case Scenario.NO_SERVICE_COMPONENTS: json = "no-service-components-hdfs.json"
          break
        default:
          json = "service-components-hdfs.json"
          break
      }
    } else {
      log.error("Unsupported resource path: {}", thePath)
    }
    return json
  }
}
