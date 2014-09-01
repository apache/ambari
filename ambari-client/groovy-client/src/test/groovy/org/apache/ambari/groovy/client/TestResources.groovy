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

enum TestResources {
  CLUSTERS("http://localhost:8080/api/v1/clusters"),
  CLUSTER("http://localhost:8080/api/v1/clusters/MySingleNodeCluster"),
  CONFIGURATIONS("http://localhost:8080/api/v1/clusters/MySingleNodeCluster/configurations"),
  BLUEPRINTS("http://localhost:8080/api/v1/blueprints"),
  BLUEPRINT("http://localhost:8080/api/v1/blueprints/single-node-hdfs-yarn"),
  BLUEPRINT_MULTI("http://localhost:8080/api/v1/blueprints/hdp-multinode-default"),
  BLUEPRINT_MULTI2("http://localhost:8080/api/v1/blueprints/hdp-multinode-default2"),
  INEXISTENT_BLUEPRINT("http://localhost:8080/api/v1/blueprints/inexistent-blueprint"),
  HOSTS("http://localhost:8080/api/v1/hosts"),
  TASKS("http://localhost:8080/api/v1/clusters/MySingleNodeCluster/requests/1"),
  SERVICES("http://localhost:8080/api/v1/clusters/MySingleNodeCluster/services"),
  SERVICE_COMPONENTS("http://localhost:8080/api/v1/clusters/MySingleNodeCluster/services/HDFS/components"),
  HOST_COMPONENTS("http://localhost:8080/api/v1/clusters/MySingleNodeCluster/hosts/host/host_components")

  String uri;

  TestResources(String uri) {
    this.uri = uri
  }

  public uri() {
    return this.uri
  }

}
