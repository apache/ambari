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
class AmbariTasksTest extends AbstractAmbariClientTest {

  private enum Scenario {
    TASKS, NO_TASKS
  }

  def "test get task as map"() {
    given:
    mockResponses(Scenario.TASKS.name())

    when:
    def result = ambari.getTaskMap()

    then:
    [
      'DATANODE INSTALL'          : 'COMPLETED',
      'GANGLIA_MONITOR INSTALL'   : 'COMPLETED',
      'GANGLIA_SERVER INSTALL'    : 'COMPLETED',
      'HDFS_CLIENT INSTALL'       : 'COMPLETED',
      'HISTORYSERVER INSTALL'     : 'COMPLETED',
      'MAPREDUCE2_CLIENT INSTALL' : 'COMPLETED',
      'NAMENODE INSTALL'          : 'COMPLETED',
      'NODEMANAGER INSTALL'       : 'COMPLETED',
      'RESOURCEMANAGER INSTALL'   : 'COMPLETED',
      'SECONDARY_NAMENODE INSTALL': 'COMPLETED',
      'YARN_CLIENT INSTALL'       : 'COMPLETED',
      'ZOOKEEPER_CLIENT INSTALL'  : 'COMPLETED',
      'ZOOKEEPER_SERVER INSTALL'  : 'COMPLETED',
      'DATANODE START'            : 'COMPLETED',
      'GANGLIA_MONITOR START'     : 'COMPLETED',
      'GANGLIA_SERVER START'      : 'COMPLETED',
      'NAMENODE START'            : 'COMPLETED',
      'ZOOKEEPER_SERVER START'    : 'COMPLETED',
      'HISTORYSERVER START'       : 'COMPLETED',
      'RESOURCEMANAGER START'     : 'COMPLETED',
      'SECONDARY_NAMENODE START'  : 'COMPLETED',
      'NODEMANAGER START'         : 'COMPLETED'] == result
  }

  def "test get task as map for no tasks"() {
    given:
    mockResponses(Scenario.NO_TASKS.name())

    when:
    def result = ambari.getTaskMap()

    then:
    [:] == result
  }

  def protected String selectResponseJson(Map resourceRequestMap, String scenarioStr) {
    def thePath = resourceRequestMap.get("path");
    def Scenario scenario = Scenario.valueOf(scenarioStr)

    def json = null
    if (thePath == TestResources.CLUSTERS.uri()) {
      json = "clusters.json"
    } else if (thePath == TestResources.TASKS.uri()) {
      switch (scenario) {
        case Scenario.TASKS: json = "request-tasks.json"
          break
        case Scenario.NO_TASKS: json = "no-request-tasks.json"
          break
      }
    } else {
      log.error("Unsupported resource path: {}", thePath)
    }
    return json
  }

}
