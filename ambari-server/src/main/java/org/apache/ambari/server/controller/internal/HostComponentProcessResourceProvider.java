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

package org.apache.ambari.server.controller.internal;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.HostComponentProcessResponse;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;

/**
 * Resource Provider for HostComponent process resources.
 */
public class HostComponentProcessResourceProvider extends ReadOnlyResourceProvider {

  // ----- Property ID constants ---------------------------------------------

  // process
  public static final String HC_PROCESS_NAME_ID = "HostComponentProcess/name";
  public static final String HC_PROCESS_STATUS_ID = "HostComponentProcess/status";
  
  public static final String HC_PROCESS_CLUSTER_NAME_ID = "HostComponentProcess/cluster_name";
  public static final String HC_PROCESS_HOST_NAME_ID = "HostComponentProcess/host_name";
  public static final String HC_PROCESS_COMPONENT_NAME_ID = "HostComponentProcess/component_name";

  // Primary Key Fields
  private static Set<String> pkPropertyIds =
      new HashSet<String>(Arrays.asList(new String[]{
          HC_PROCESS_CLUSTER_NAME_ID, HC_PROCESS_HOST_NAME_ID, HC_PROCESS_COMPONENT_NAME_ID, HC_PROCESS_NAME_ID}));

  // ----- Constructors ----------------------------------------------------

  /**
   * Create a  new resource provider for the given management controller.
   *
   * @param propertyIds     the property ids
   * @param keyPropertyIds  the key property ids
   */
  HostComponentProcessResourceProvider(Set<String> propertyIds,
      Map<Resource.Type, String> keyPropertyIds, AmbariManagementController amc) {
    super(propertyIds, keyPropertyIds, amc);
  }


  // ----- ResourceProvider ------------------------------------------------

  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }


  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
             NoSuchResourceException, NoSuchParentResourceException {

    final Set<Map<String, Object>> requestMaps = getPropertyMaps(predicate);

    Set<String> requestedIds = getRequestPropertyIds(request, predicate);

    Set<HostComponentProcessResponse> responses = getResources(new Command<Set<HostComponentProcessResponse>>() {
      @Override
      public Set<HostComponentProcessResponse> invoke() throws AmbariException {
        return getHostComponentProcesses(requestMaps);
      }
    });
    
    Set<Resource> resources = new HashSet<Resource>();
    
    for (HostComponentProcessResponse response : responses) {
      Resource r = new ResourceImpl(Resource.Type.HostComponentProcess);
      
      setResourceProperty(r, HC_PROCESS_CLUSTER_NAME_ID, response.getCluster(),
          requestedIds);
      setResourceProperty(r, HC_PROCESS_HOST_NAME_ID, response.getHost(),
          requestedIds);
      setResourceProperty(r, HC_PROCESS_COMPONENT_NAME_ID, response.getComponent(), requestedIds);
      
      setResourceProperty(r, HC_PROCESS_NAME_ID, response.getValueMap().get("name"),
          requestedIds);
      setResourceProperty(r, HC_PROCESS_STATUS_ID, response.getValueMap().get("status"),
          requestedIds);
      
      // set the following even if they aren't defined
      for (Entry<String, String> entry : response.getValueMap().entrySet()) {
        // these are already set
        if (entry.getKey().equals("name") || entry.getKey().equals("status"))
          continue;
        
        setResourceProperty(r, "HostComponentProcess/" + entry.getKey(),
            entry.getValue(), requestedIds);
          
      }

      resources.add(r);
    }

    return resources;
  }
  
  // ----- Instance Methods ------------------------------------------------

  private Set<HostComponentProcessResponse> getHostComponentProcesses(
      Set<Map<String, Object>> requestMaps)
    throws AmbariException {
    
    Set<HostComponentProcessResponse> results = new HashSet<HostComponentProcessResponse>();
    
    Clusters clusters = getManagementController().getClusters();

    for (Map<String, Object> requestMap : requestMaps) {
      
      String cluster = (String) requestMap.get(HC_PROCESS_CLUSTER_NAME_ID);
      String component = (String) requestMap.get(HC_PROCESS_COMPONENT_NAME_ID);
      String host = (String) requestMap.get(HC_PROCESS_HOST_NAME_ID);

      Cluster c = clusters.getCluster(cluster);
      
      Collection<ServiceComponentHost> schs = c.getServiceComponentHosts(host);
      
      for (ServiceComponentHost sch : schs) {
        if (!sch.getServiceComponentName().equals(component))
          continue;
        
        for (Map<String, String> proc : sch.getProcesses()) {
          results.add(new HostComponentProcessResponse(cluster, sch.getHostName(),
              component, proc));
        }
      }
      
    }
    return results;
  }
  


}
