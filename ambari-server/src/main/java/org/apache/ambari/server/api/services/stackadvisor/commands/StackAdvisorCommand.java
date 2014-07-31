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

package org.apache.ambari.server.api.services.stackadvisor.commands;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
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
import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.StacksService.StackUriInfo;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorException;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorRequest;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorRunner;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;

/**
 * Parent for all commands.
 */
public abstract class StackAdvisorCommand<T> extends BaseService {

  /**
   * Type of response object provided by extending classes when
   * {@link #invoke(StackAdvisorRequest)} is called.
   */
  private Class<T> type;

  protected static Log LOG = LogFactory.getLog(StackAdvisorCommand.class);

  private static final String GET_HOSTS_INFO_URI = "/api/v1/hosts"
      + "?fields=*&Hosts/host_name.in(%s)";
  private static final String GET_SERVICES_INFO_URI = "/api/v1/stacks/%s/versions/%s"
      + "?fields=Versions/stack_name,Versions/stack_version,Versions/parent_stack_version"
      + ",services/StackServices/service_name,services/StackServices/service_version"
      + ",services/components/StackServiceComponents,services/components/dependencies,services/components/auto_deploy"
      + "&services/StackServices/service_name.in(%s)";

  private File recommendationsDir;
  private String stackAdvisorScript;

  private int requestId;
  private File requestDirectory;
  private StackAdvisorRunner saRunner;

  protected ObjectMapper mapper;

  @SuppressWarnings("unchecked")
  public StackAdvisorCommand(File recommendationsDir, String stackAdvisorScript, int requestId,
      StackAdvisorRunner saRunner) {
    this.type = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass())
        .getActualTypeArguments()[0];

    this.mapper = new ObjectMapper();
    this.mapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);

    this.recommendationsDir = recommendationsDir;
    this.stackAdvisorScript = stackAdvisorScript;
    this.requestId = requestId;
    this.saRunner = saRunner;
  }

  protected abstract StackAdvisorCommandType getCommandType();

  /**
   * Simple holder for 'hosts.json' and 'services.json' data.
   */
  protected class StackAdvisorData {
    protected String hostsJSON;
    protected String servicesJSON;

    public StackAdvisorData(String hostsJSON, String servicesJSON) {
      this.hostsJSON = hostsJSON;
      this.servicesJSON = servicesJSON;
    }
  }

  /**
   * Name with the result JSON, e.g. "component-layout.json" or
   * "validations.json" .
   * 
   * @return the file name
   */
  protected abstract String getResultFileName();

  protected abstract void validate(StackAdvisorRequest request) throws StackAdvisorException;

  protected abstract StackAdvisorData adjust(StackAdvisorData data, StackAdvisorRequest request);

  public synchronized T invoke(StackAdvisorRequest request) throws StackAdvisorException {
    validate(request);
    String hostsJSON = getHostsInformation(request);
    String servicesJSON = getServicesInformation(request);

    StackAdvisorData adjusted = adjust(new StackAdvisorData(hostsJSON, servicesJSON), request);

    try {
      createRequestDirectory();

      FileUtils.writeStringToFile(new File(requestDirectory, "hosts.json"), adjusted.hostsJSON);
      FileUtils.writeStringToFile(new File(requestDirectory, "services.json"),
          adjusted.servicesJSON);

      boolean success = saRunner.runScript(stackAdvisorScript, getCommandType(), requestDirectory);
      if (!success) {
        String message = "Stack advisor script finished with errors";
        LOG.warn(message);
        throw new StackAdvisorException(message);
      }

      String result = FileUtils.readFileToString(new File(requestDirectory, getResultFileName()));

      return this.mapper.readValue(result, this.type);
    } catch (Exception e) {
      String message = "Error occured during stack advisor command invocation";
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

    requestDirectory = new File(recommendationsDir, Integer.toString(requestId));

    if (requestDirectory.exists()) {
      FileUtils.deleteDirectory(requestDirectory);
    }
    if (!requestDirectory.mkdirs()) {
      throw new IOException("Cannot create " + requestDirectory);
    }
  }

  private String getHostsInformation(StackAdvisorRequest request) throws StackAdvisorException {
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

  private String getServicesInformation(StackAdvisorRequest request) throws StackAdvisorException {
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

}
