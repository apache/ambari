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

package org.apache.ambari.server.controller;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ObjectNotFoundException;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.ArrayUtils;

import com.google.inject.Inject;

public class RootServiceResponseFactory extends
    AbstractRootServiceResponseFactory {

  private static final String RUNNING_STATE = "RUNNING";
  @Inject
  private Configuration configs;
  
  @Override
  public Set<RootServiceResponse> getRootServices(RootServiceRequest request) throws ObjectNotFoundException {
    
    Set<RootServiceResponse> response;
    
    String serviceName = null;
    
    if (request != null)
      serviceName = request.getServiceName();

    if (serviceName != null) {
      Services service;
      try {
        service = Services.valueOf(serviceName);
      }
      catch (IllegalArgumentException ex) {
        throw new ObjectNotFoundException("Root service name: " + serviceName);
      }
      
      response = Collections.singleton(new RootServiceResponse(service.toString()));
    } else {
      response = new HashSet<RootServiceResponse>();
      
      for (Services service: Services.values())    
        response.add(new RootServiceResponse(service.toString()));
    }    
    return response;
  }
  
  @Override
  public Set<RootServiceComponentResponse> getRootServiceComponents(
      RootServiceComponentRequest request) throws ObjectNotFoundException {
    Set<RootServiceComponentResponse> response = new HashSet<RootServiceComponentResponse>();
    
    String serviceName = request.getServiceName();
    String componentName = request.getComponentName();
    Services service;

    try {
      service = Services.valueOf(serviceName);
    }
    catch (IllegalArgumentException ex) {
      throw new ObjectNotFoundException("Root service name: " + serviceName);
    }
    catch (NullPointerException np) {
      throw new ObjectNotFoundException("Root service name: null");
    }
    
    if (componentName != null) {
      Components component;
      try {
        component = Components.valueOf(componentName);
        if (!ArrayUtils.contains(service.getComponents(), component))
          throw new ObjectNotFoundException("No component name: " + componentName + "in service: " + serviceName);
      }
      catch (IllegalArgumentException ex) {
        throw new ObjectNotFoundException("Component name: " + componentName);
      }
      response = Collections.singleton(new RootServiceComponentResponse(component.toString(),
                                       getComponentProperties(componentName)));
    } else {
    
      for (Components component: service.getComponents())    
        response.add(new RootServiceComponentResponse(component.toString(),
                     getComponentProperties(component.name())));
      }
    return response;
  }

  private Map<String, String> getComponentProperties(String componentName){
    
    Map<String, String> response;
    Components component = null;

    if (componentName != null) {
      component = Components.valueOf(componentName);
      
      switch (component) {
      case AMBARI_SERVER:
        response = configs.getConfigsMap();
        break;

      default:
        response = Collections.emptyMap();
      }
    }
    else
      response = Collections.emptyMap();

    return response;
  }

  
  public enum Services {
    AMBARI(Components.values());
    private Components[] components;

    Services(Components[] components) {
      this.components = components;
    }

    public Components[] getComponents() {
      return components;
    }
  }
  
  public enum Components {
    AMBARI_SERVER, AMBARI_AGENT
  }

  @Override
  public Set<RootServiceHostComponentResponse> getRootServiceHostComponent(RootServiceHostComponentRequest request, Set<HostResponse> hosts) throws AmbariException {
    Set<RootServiceHostComponentResponse> response = new HashSet<RootServiceHostComponentResponse>();

    Set<RootServiceComponentResponse> rootServiceComponents = 
        getRootServiceComponents(new RootServiceComponentRequest(request.getServiceName(), request.getComponentName()));

    //Cartesian product with hosts and components
    for (RootServiceComponentResponse component : rootServiceComponents) {
      
      Set<HostResponse> filteredHosts = new HashSet<HostResponse>(hosts);      
      
      //Make some filtering of hosts if need
      if (component.getComponentName().equals(Components.AMBARI_SERVER.name()))
        CollectionUtils.filter(filteredHosts, new Predicate() {
          @Override
          public boolean evaluate(Object arg0) {
            HostResponse hostResponse = (HostResponse) arg0;
            return hostResponse.getHostname().equals(StageUtils.getHostName());
          }
        });
      
      for (HostResponse host : filteredHosts) {
        
        if (component.getComponentName().equals(Components.AMBARI_SERVER.name()))
          response.add(new RootServiceHostComponentResponse(host.getHostname(), component.getComponentName(),
            RUNNING_STATE, component.getProperties()));
        else
          response.add(new RootServiceHostComponentResponse(host.getHostname(), component.getComponentName(),
            host.getHostState(), component.getProperties()));
      }
    }
    
    return response;
  }
}
