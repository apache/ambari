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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorException;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorRequest;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorRunner;
import org.apache.ambari.server.api.services.stackadvisor.validations.ValidationResponse;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

/**
 * {@link StackAdvisorCommand} implementation for component-layout validation.
 */
public class GetComponentLayoutValidationCommand extends StackAdvisorCommand<ValidationResponse> {

  public GetComponentLayoutValidationCommand(File recommendationsDir, String stackAdvisorScript,
      int requestId, StackAdvisorRunner saRunner) {
    super(recommendationsDir, stackAdvisorScript, requestId, saRunner);
  }

  @Override
  protected StackAdvisorCommandType getCommandType() {
    return StackAdvisorCommandType.VALIDATE_COMPONENT_LAYOUT;
  }

  @Override
  protected void validate(StackAdvisorRequest request) throws StackAdvisorException {
    if (request.getHosts().isEmpty() || request.getServices().isEmpty()
        || request.getComponentHostsMap().isEmpty()) {
      throw new StackAdvisorException("Hosts, services and recommendations must not be empty");
    }
  }

  private static final String SERVICES_PROPETRY = "services";
  private static final String SERVICES_COMPONENTS_PROPETRY = "components";
  private static final String COMPONENT_INFO_PROPETRY = "StackServiceComponents";
  private static final String COMPONENT_NAME_PROPERTY = "component_name";
  private static final String COMPONENT_HOSTNAMES_PROPETRY = "hostnames";

  @Override
  protected StackAdvisorData adjust(StackAdvisorData data, StackAdvisorRequest request) {
    // do nothing
    Map<String, Set<String>> componentHostsMap = request.getComponentHostsMap();

    try {
      JsonNode root = this.mapper.readTree(data.servicesJSON);
      ArrayNode services = (ArrayNode) root.get(SERVICES_PROPETRY);
      Iterator<JsonNode> servicesIter = services.getElements();

      while (servicesIter.hasNext()) {
        JsonNode service = servicesIter.next();
        ArrayNode components = (ArrayNode) service.get(SERVICES_COMPONENTS_PROPETRY);
        Iterator<JsonNode> componentsIter = components.getElements();

        while (componentsIter.hasNext()) {
          JsonNode component = componentsIter.next();
          ObjectNode componentInfo = (ObjectNode) component.get(COMPONENT_INFO_PROPETRY);
          String componentName = componentInfo.get(COMPONENT_NAME_PROPERTY).getTextValue();

          Set<String> componentHosts = componentHostsMap.get(componentName);
          ArrayNode hostnames = componentInfo.putArray(COMPONENT_HOSTNAMES_PROPETRY);
          if (null != componentHosts) {
            for (String hostName : componentHosts) {
              hostnames.add(hostName);
            }
          }
        }
      }

      data.servicesJSON = mapper.writeValueAsString(root);
    } catch (Exception e) {
      // should not happen
      String message = "Error parsing services.json file content: " + e.getMessage();
      LOG.warn(message, e);
      throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity(message).build());
    }

    return data;
  }

  @Override
  protected String getResultFileName() {
    return "component-layout-validation.json";
  }

}
