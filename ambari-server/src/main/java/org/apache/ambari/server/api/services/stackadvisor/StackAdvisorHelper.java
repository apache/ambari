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

package org.apache.ambari.server.api.services.stackadvisor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.services.BaseService;
import org.apache.ambari.server.api.services.LocalUriInfo;
import org.apache.ambari.server.api.services.RecommendationService;
import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.StacksService.StackUriInfo;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorRunner.StackAdvisorCommand;
import org.apache.ambari.server.api.services.stackadvisor.recommendations.RecommendationRequest;
import org.apache.ambari.server.api.services.stackadvisor.recommendations.RecommendationResponse;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class StackAdvisorHelper extends BaseService {

  private static Log LOG = LogFactory.getLog(RecommendationService.class);

  private static final String GET_HOSTS_INFO_URI = "/api/v1/hosts"
      + "?fields=*&Hosts/host_name.in(%s)";
  private static final String GET_SERVICES_INFO_URI = "/api/v1/stacks/%s/versions/%s"
      + "?fields=Versions/stack_name,Versions/stack_version,Versions/parent_stack_version"
      + ",services/StackServices/service_name,services/StackServices/service_version"
      + ",services/components/StackServiceComponents,services/components/dependencies,services/components/auto_deploy"
      + "&services/StackServices/service_name.in(%s)";

  private File recommendationsDir;
  private String stackAdvisorScript;

  /* Monotonically increasing requestid */
  private int requestId = 0;
  private File requestDirectory;
  private StackAdvisorRunner saRunner;

  @Inject
  public StackAdvisorHelper(Configuration conf, StackAdvisorRunner saRunner) throws IOException {
    this.recommendationsDir = conf.getRecommendationsDir();
    this.stackAdvisorScript = conf.getStackAdvisorScript();
    this.saRunner = saRunner;
  }

  /**
   * Return component-layout recommendation based on hosts and services
   * information.
   * 
   * @param hosts list of hosts
   * @param services list of services
   * @return {@link String} representation of recommendations
   * @throws StackAdvisorException
   */
  public synchronized RecommendationResponse getComponentLayoutRecommnedation(
      RecommendationRequest request) throws StackAdvisorException {

    validateRecommendationRequest(request);
    String hostsJSON = getHostsInformation(request);
    String servicesJSON = getServicesInformation(request);

    try {
      createRequestDirectory();

      FileUtils.writeStringToFile(new File(requestDirectory, "hosts.json"), hostsJSON);
      FileUtils.writeStringToFile(new File(requestDirectory, "services.json"), servicesJSON);

      boolean success = saRunner.runScript(stackAdvisorScript,
          StackAdvisorCommand.RECOMMEND_COMPONENT_LAYOUT, requestDirectory);
      if (!success) {
        String message = "Stack advisor script finished with errors";
        LOG.warn(message);
        throw new StackAdvisorException(message);
      }

      String result = FileUtils
          .readFileToString(new File(requestDirectory, "component-layout.json"));

      ObjectMapper mapper = new ObjectMapper();
      return mapper.readValue(result, RecommendationResponse.class);
    } catch (Exception e) {
      String message = "Error occured during preparing component-layout recommendations";
      LOG.warn(message, e);
      throw new StackAdvisorException(message, e);
    }
  }

  /**
   * Create request id directory for each call
   */
  private void createRequestDirectory() throws IOException {
    if (!recommendationsDir.exists()) {
      if (!recommendationsDir.mkdirs()) {
        throw new IOException("Cannot create " + recommendationsDir);
      }
    }

    requestId += 1;
    requestDirectory = new File(recommendationsDir, Integer.toString(requestId));

    if (requestDirectory.exists()) {
      FileUtils.deleteDirectory(requestDirectory);
    }
    if (!requestDirectory.mkdirs()) {
      throw new IOException("Cannot create " + requestDirectory);
    }
  }

  private String getHostsInformation(RecommendationRequest request) throws StackAdvisorException {
    String hostsURI = String.format(GET_HOSTS_INFO_URI, request.getHostsCommaSeparated());

    Response response = handleRequest(null, null, new LocalUriInfo(hostsURI), Request.Type.GET,
        createHostResource());

    if (response.getStatus() != Status.OK.getStatusCode()) {
      String message = String.format(
          "Error occured during hosts information retrieving, status=%s, response=%s",
          response.getStatus(), (String) response.getEntity());
      LOG.warn(message);
      throw new StackAdvisorException(message);
    }

    String hostsJSON = (String) response.getEntity();
    if (LOG.isDebugEnabled()) {
      LOG.debug("Hosts information: " + hostsJSON);
    }

    Collection<String> unregistered = getUnregisteredHosts(hostsJSON, request.getHosts());
    if (unregistered.size() > 0) {
      String message = String.format("There are unregistered hosts in the request, %s",
          Arrays.toString(unregistered.toArray()));
      LOG.warn(message);
      throw new StackAdvisorException(message);
    }

    return hostsJSON;
  }

  @SuppressWarnings("unchecked")
  private Collection<String> getUnregisteredHosts(String hostsJSON, List<String> hosts)
      throws StackAdvisorException {
    ObjectMapper mapper = new ObjectMapper();
    List<String> registeredHosts = new ArrayList<String>();

    try {
      JsonNode root = mapper.readTree(hostsJSON);
      Iterator<JsonNode> iterator = root.get("items").getElements();
      while (iterator.hasNext()) {
        JsonNode next = iterator.next();
        String hostName = next.get("Hosts").get("host_name").getTextValue();
        registeredHosts.add(hostName);
      }

      return CollectionUtils.subtract(hosts, registeredHosts);
    } catch (Exception e) {
      throw new StackAdvisorException("Error occured during calculating unregistered hosts", e);
    }
  }

  private String getServicesInformation(RecommendationRequest request) throws StackAdvisorException {
    String stackName = request.getStackName();
    String stackVersion = request.getStackVersion();
    String servicesURI = String.format(GET_SERVICES_INFO_URI, stackName, stackVersion,
        request.getServicesCommaSeparated());

    Response response = handleRequest(null, null, new StackUriInfo(new LocalUriInfo(servicesURI)),
        Request.Type.GET, createStackVersionResource(stackName, stackVersion));

    if (response.getStatus() != Status.OK.getStatusCode()) {
      String message = String.format(
          "Error occured during services information retrieving, status=%s, response=%s",
          response.getStatus(), (String) response.getEntity());
      LOG.warn(message);
      throw new StackAdvisorException(message);
    }

    String servicesJSON = (String) response.getEntity();
    if (LOG.isDebugEnabled()) {
      LOG.debug("Services information: " + servicesJSON);
    }
    return servicesJSON;
  }

  private ResourceInstance createHostResource() {
    Map<Resource.Type, String> mapIds = new HashMap<Resource.Type, String>();
    return createResource(Resource.Type.Host, mapIds);
  }

  private ResourceInstance createStackVersionResource(String stackName, String stackVersion) {
    Map<Resource.Type, String> mapIds = new HashMap<Resource.Type, String>();
    mapIds.put(Resource.Type.Stack, stackName);
    mapIds.put(Resource.Type.StackVersion, stackVersion);

    return createResource(Resource.Type.StackVersion, mapIds);
  }

  private void validateRecommendationRequest(RecommendationRequest request)
      throws StackAdvisorException {
    if (request.getHosts().isEmpty() || request.getServices().isEmpty()) {
      throw new StackAdvisorException("Hosts and services must not be empty");
    }
  }

}
