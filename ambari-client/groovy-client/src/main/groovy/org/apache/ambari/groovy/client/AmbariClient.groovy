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

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovyx.net.http.HttpResponseException
import groovyx.net.http.RESTClient
import org.apache.http.NoHttpResponseException
import org.apache.http.client.ClientProtocolException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Basic client to send requests to the Ambari server.
 */
class AmbariClient {

  private static final Logger LOGGER = LoggerFactory.getLogger(AmbariClient.class)
  private static final int PAD = 30
  private static final int OK_RESPONSE = 200
  boolean debugEnabled = false;
  def RESTClient ambari
  def slurper = new JsonSlurper()
  def clusterName

  /**
   * Connects to the ambari server.
   *
   * @param host host of the Ambari server; default value is localhost
   * @param port port of the Ambari server; default value is 8080
   * @param user username of the Ambari server; default is admin
   * @param password password fom the Ambari server; default is admin
   */
  AmbariClient(host = 'localhost', port = '8080', user = 'admin', password = 'admin') {
    ambari = new RESTClient("http://${host}:${port}/api/v1/" as String)
    ambari.headers['Authorization'] = 'Basic ' + "$user:$password".getBytes('iso-8859-1').encodeBase64()
    ambari.headers['X-Requested-By'] = 'ambari'
  }

  /**
   * Connects to the ambari server.
   *
   * @param restClient underlying client
   * @param slurper slurper to parse responses
   */
  AmbariClient(RESTClient restClient, JsonSlurper slurper) {
    this.ambari = restClient
    this.slurper = slurper
  }

  /**
   * Sets the debug variable. Used by printing the API calls for the Ambari Shell.
   *
   * @param enabled enable or disable
   */
  def setDebugEnabled(boolean enabled) {
    debugEnabled = enabled;
  }

  /**
   * Returns the name of the cluster.
   *
   * @return the name of the cluster of null if no cluster yet
   */
  def String getClusterName() {
    if (!clusterName) {
      def clusters = getClusters();
      if (clusters) {
        clusterName = clusters.items[0]?.Clusters?.cluster_name
      }
    }
    return clusterName
  }

  /**
   * Checks whether the blueprint exists or not.
   *
   * @param id id of the blueprint
   * @return true if exists false otherwise
   */
  def boolean doesBlueprintExists(String id) {
    def result = false
    try {
      result = ambari.get(path: "blueprints/$id", query: ['fields': "Blueprints"]).status == OK_RESPONSE
    } catch (e) {
      LOGGER.info("Blueprint does not exist", e)
    }
    return result
  }

  /**
   * Checks whether there are available blueprints or not.
   *
   * @return true if blueprints are available false otherwise
   */
  def boolean isBlueprintAvailable() {
    return getBlueprints().items?.size > 0
  }

  /**
   * Returns a pre-formatted String of the blueprint.
   *
   * @param id id of the blueprint
   * @return formatted String
   */
  def String showBlueprint(String id) {
    def resp = getBlueprint(id)
    if (resp) {
      def groups = resp.host_groups.collect {
        def name = it.name
        def comps = it.components.collect { it.name.padRight(PAD).padLeft(PAD + 10) }.join("\n")
        return "HOSTGROUP: $name\n$comps"
      }.join("\n")
      return "[${resp.Blueprints.stack_name}:${resp.Blueprints.stack_version}]\n$groups"
    }
    return "Not found"
  }

  /**
   * Returns the host group - components mapping of the blueprint.
   *
   * @param id id of the blueprint
   * @return Map where the key is the host group and the value is the list of components
   */
  def Map<String, List<String>> getBlueprintMap(String id) {
    def result = getBlueprint(id).host_groups?.collectEntries { [(it.name): it.components.collect { it.name }] }
    result ?: new HashMap()
  }

  /**
   * Returns a pre-formatted String of the blueprints.
   *
   * @return formatted blueprint list
   */
  def String showBlueprints() {
    getBlueprints().items.collect { "${it.Blueprints.blueprint_name.padRight(PAD)} [${it.Blueprints.stack_name}:${it.Blueprints.stack_version}]" }.join("\n")
  }

  /**
   * Returns a Map containing the blueprint name - stack association.
   *
   * @return Map where the key is the blueprint's name value is the used stack
   */
  def Map<String, String> getBlueprintsMap() {
    def result = getBlueprints().items?.collectEntries { [(it.Blueprints.blueprint_name): it.Blueprints.stack_name + ":" + it.Blueprints.stack_version] }
    result ?: new HashMap()
  }

  /**
   * Returns the name of the host groups for a given blueprint.
   *
   * @param blueprint id of the blueprint
   * @return host group list or empty list
   */
  def List<String> getHostGroups(String blueprint) {
    def result = getBlueprint(blueprint)
    result ? result.host_groups.collect { it.name } : new ArrayList<String>()
  }

  /**
   * Returns a pre-formatted String of a blueprint used by the cluster.
   *
   * @return formatted String
   */
  def String showClusterBlueprint() {
    ambari.get(path: "clusters/${getClusterName()}", query: ['format': "blueprint"]).data.text
  }

  /**
   * Adds a blueprint to the Ambari server. Exception is thrown if fails.
   *
   * @param json blueprint as json
   * @throws HttpResponseException in case of error
   */
  def void addBlueprint(String json) throws HttpResponseException {
    if (json) {
      postBlueprint(json)
    }
  }

  /**
   * Adds 2 default blueprints.
   *
   * @throws HttpResponseException in case of error
   */
  def void addDefaultBlueprints() throws HttpResponseException {
    addBlueprint(getResourceContent("blueprints/multi-node-hdfs-yarn"))
    addBlueprint(getResourceContent("blueprints/single-node-hdfs-yarn"))
  }

  /**
   * Creates a cluster with the given blueprint and host group - host association.
   *
   * @param clusterName name of the cluster
   * @param blueprintName blueprint id used to create this cluster
   * @param hostGroups Map<String, List<String> key - host group, value - host list
   * @return true if the creation was successful false otherwise
   * @throws HttpResponseException in case of error
   */
  def void createCluster(String clusterName, String blueprintName, Map<String, List<String>> hostGroups) throws HttpResponseException {
    ambari.post(path: "clusters/$clusterName", body: createClusterJson(blueprintName, hostGroups), { it })
  }

  /**
   * Deletes the cluster.
   *
   * @param clusterName name of the cluster
   * @throws HttpResponseException in case of error
   */
  def void deleteCluster(String clusterName) throws HttpResponseException {
    ambari.delete(path: "clusters/$clusterName")
  }

  /**
   * Returns the active cluster as json
   *
   * @return cluster as json String
   * @throws HttpResponseException in case of error
   */
  def String getClusterAsJson() throws HttpResponseException {
    getRequest("clusters/${getClusterName()}")
  }

  /**
   * Returns all clusters as json
   *
   * @return json String
   * @throws HttpResponseException in case of error
   */
  def getClustersAsJson() throws HttpResponseException {
    getRequest("clusters")
  }

  /**
   * Returns a pre-formatted String of the clusters.
   *
   * @return pre-formatted cluster list
   */
  def String showClusterList() {
    getClusters().items.collect { "[$it.Clusters.cluster_id] $it.Clusters.cluster_name:$it.Clusters.version" }.join("\n")
  }

  /**
   * Returns the task properties as Map.
   *
   * @param request request id; default is 1
   * @return property Map or empty Map
   */
  def getTasks(request = 1) {
    getAllResources("requests/$request", "tasks/Tasks")
  }

  /**
   * Returns a pre-formatted task list.
   *
   * @param request request id; default is 1
   * @return pre-formatted task list
   */
  def String showTaskList(request = 1) {
    getTasks(request)?.tasks.collect { "${it.Tasks.command_detail.padRight(PAD)} [${it.Tasks.status}]" }.join("\n")
  }

  /**
   * Returns a Map containing the task's command detail as key and the task's status as value.
   *
   * @param request request id; default is 1
   * @return key task command detail; task value status
   */
  def Map<String, String> getTaskMap(request = 1) {
    def result = getTasks(request).tasks?.collectEntries { [(it.Tasks.command_detail): it.Tasks.status] }
    result ?: new HashMap()
  }

  /**
   * Returns the available host names and its states.
   *
   * @return hostname state association
   */
  def Map<String, String> getHostNames() {
    getHosts().items.collectEntries { [(it.Hosts.host_name): it.Hosts.host_status] }
  }

  /**
   * Returns a pre-formatted list of the hosts.
   *
   * @return pre-formatted String
   */
  def String showHostList() {
    getHosts().items.collect { "$it.Hosts.host_name [$it.Hosts.host_status] $it.Hosts.ip $it.Hosts.os_type:$it.Hosts.os_arch" }.join("\n")
  }

  /**
   * Returns a pre-formatted list of the service components.
   *
   * @return pre-formatted String
   */
  def String showServiceComponents() {
    getServices().items.collect {
      def name = it.ServiceInfo.service_name
      def state = it.ServiceInfo.state
      def componentList = getServiceComponents(name).items.collect {
        "    ${it.ServiceComponentInfo.component_name.padRight(PAD)}  [$it.ServiceComponentInfo.state]"
      }.join("\n")
      "${name.padRight(PAD)} [$state]\n$componentList"
    }.join("\n")
  }

  /**
   * Returns the service components properties as Map where the key is the service name and
   * value is a component - state association.
   *
   * @return service name - [component name - status]
   */
  def Map<String, Map<String, String>> getServiceComponentsMap() {
    def result = getServices().items.collectEntries {
      def name = it.ServiceInfo.service_name
      def componentList = getServiceComponents(name).items.collectEntries { [(it.ServiceComponentInfo.component_name): it.ServiceComponentInfo.state] }
      [(name): componentList]
    }
    result ?: new HashMap()
  }

  /**
   * Returns a pre-formatted service list.
   *
   * @return formatted String
   */
  def String showServiceList() {
    getServices().items.collect { "${it.ServiceInfo.service_name.padRight(PAD)} [$it.ServiceInfo.state]" }.join("\n")
  }

  /**
   * Returns the services properties as Map where the key is the service's name and the values is the service's state.
   *
   * @return service name - service state association as Map
   */
  def Map<String, String> getServicesMap() {
    def result = getServices().items.collectEntries { [(it.ServiceInfo.service_name): it.ServiceInfo.state] }
    result ?: new HashMap()
  }

  /**
   * Returns a pre-formatted component list of a host.
   *
   * @param host which host's components are requested
   * @return formatted String
   */
  def String showHostComponentList(host) {
    getHostComponents(host).items.collect { "${it.HostRoles.component_name.padRight(PAD)} [$it.HostRoles.state]" }.join("\n")
  }

  /**
   * Returns the host's components as Map where the key is the component's name and values is its state.
   *
   * @param host which host's components are requested
   * @return component name - state association
   */
  def Map<String, String> getHostComponentsMap(host) {
    def result = getHostComponents(host).items?.collectEntries { [(it.HostRoles.component_name): it.HostRoles.state] }
    result ?: new HashMap()
  }

  /**
   * Returns the blueprint json as String.
   *
   * @param id id of the blueprint
   * @return json as String, exception if thrown is it fails
   */
  def String getBlueprintAsJson(id) {
    return getRequest("blueprints/$id", "host_groups,Blueprints")
  }

  private def getAllResources(resourceName, fields) {
    slurp("clusters/${getClusterName()}/$resourceName", "$fields/*")
  }

  /**
   * Posts the blueprint JSON to Ambari with name 'bp' in the URL
   * because it does not matter here. The blueprint's name is
   * provided in the JSON.
   *
   * @param blueprint json
   * @return response message
   */
  private void postBlueprint(String blueprint) {
    ambari.post(path: "blueprints/bp", body: blueprint, { it })
  }

  private def createClusterJson(String name, Map hostGroups) {
    def builder = new JsonBuilder()
    def groups = hostGroups.collect {
      def hostList = it.value.collect { ['fqdn': it] }
      [name: it.key, hosts: hostList]
    }
    builder { "blueprint" name; "host-groups" groups }
    builder.toPrettyString()
  }

  private def slurp(path, fields = "") {
    def baseUri = ambari.getUri();
    if (debugEnabled) {
      println "[DEBUG] ${baseUri}${path}?fields=$fields"
    }
    def result = new HashMap()
    try {
      result = slurper.parseText(getRequest(path, fields))
    } catch (e) {
      if (e instanceof NoHttpResponseException || e instanceof ConnectException || e instanceof ClientProtocolException ||
        e instanceof UnknownHostException || (e instanceof HttpResponseException && e.message == "Bad credentials")) {
        throw new AmbariConnectionException("Cannot connect to Ambari $baseUri")
      }
      LOGGER.error("Error occurred during GET request to $baseUri/$path", e)
    }
    return result
  }

  /**
   * Return the blueprint's properties as a Map.
   *
   * @param id id of the blueprint
   * @return properties as Map
   */
  private def getBlueprint(id) {
    slurp("blueprints/$id", "host_groups,Blueprints")
  }

  /**
   * Returns a Map containing the blueprint's properties parsed from the Ambari response json.
   *
   * @return blueprint's properties as Map or empty Map
   */
  private def getBlueprints() {
    slurp("blueprints", "Blueprints")
  }

  /**
   * Returns a Map containing the cluster's properties parsed from the Ambari response json.
   *
   * @return cluster's properties as Map or empty Map
   */
  private def getClusters() {
    slurp("clusters", "Clusters")
  }

  /**
   * Returns the available hosts properties as a Map.
   *
   * @return Map containing the hosts properties
   */
  private def getHosts() {
    slurp("hosts", "Hosts")
  }

  /**
   * Returns the service components properties as Map.
   *
   * @param service id of the service
   * @return service component properties as Map
   */
  private def getServiceComponents(service) {
    getAllResources("services/$service/components", "ServiceComponentInfo")
  }

  /**
   * Returns the services properties as Map parsed from Ambari response json.
   *
   * @return service properties as Map
   */
  private def getServices() {
    getAllResources("services", "ServiceInfo")
  }

  /**
   * Returns the properties of the host components as a Map parsed from the Ambari response json.
   *
   * @param host which host's components are requested
   * @return component properties as Map
   */
  private def getHostComponents(host) {
    getAllResources("hosts/$host/host_components", "HostRoles")
  }

  private String getRequest(path, fields = "") {
    if (fields) {
      ambari.get(path: "$path", query: ['fields': "$fields"]).data.text
    } else {
      ambari.get(path: "$path").data.text
    }
  }

  private String getResourceContent(name) {
    getClass().getClassLoader().getResourceAsStream(name)?.text
  }
}
