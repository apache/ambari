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
package org.apache.ambari.server.checks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.apache.ambari.server.state.stack.UpgradePack.PrerequisiteCheckConfig;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Singleton;

/**
 * Checks if Atlas service is present. Upgrade to stack HDP 2.5 can't pursuit
 * with existed on the cluster Atlas service.
 */
@Singleton
@UpgradeCheck(group = UpgradeCheckGroup.DEFAULT)
public class ServicePresenceCheck extends AbstractCheckDescriptor{

  private static final Logger LOG = LoggerFactory.getLogger(ServicePresenceCheck.class);

  static final String KEY_SERVICE_REMOVED = "service_removed";
  /*
   * List of services that do not support upgrade
   * services must be removed before the stack upgrade
   * */
  static final String NO_UPGRADE_SUPPORT_SERVICES_PROPERTY_NAME = "no-upgrade-support-service-names";

  /*
   * List of services removed from the new release
   * */
  static final String REMOVED_SERVICES_PROPERTY_NAME = "removed-service-names";

  /*
   * Such as Spark to Spark2
   */
  static final String NEW_SERVICES_PROPERTY_NAME = "new-service-names";

  public ServicePresenceCheck(){
    super(CheckDescription.SERVICE_PRESENCE_CHECK);
  }

  @Override
  public void perform(PrerequisiteCheck prerequisiteCheck, PrereqCheckRequest request) throws AmbariException {
    final Cluster cluster = clustersProvider.get().getCluster(request.getClusterName());
    Set<String> installedServices = cluster.getServices().keySet();

    List<String> noUpgradeSupportServices = getNoUpgradeSupportServices(request);
    Map<String, String> removedServices = getRemovedServices(request);
    List<String> failReasons = new ArrayList<>();

    String reason = getFailReason(prerequisiteCheck, request);
    for(String service: noUpgradeSupportServices){
      if (installedServices.contains(service.toUpperCase())){
        prerequisiteCheck.getFailedOn().add(service);
        String msg = String.format(reason, service, service);
        failReasons.add(msg);
      }
    }

    reason = getFailReason(KEY_SERVICE_REMOVED, prerequisiteCheck, request);
    for (Map.Entry<String, String> entry : removedServices.entrySet()) {
      String removedService = entry.getKey();
      if(installedServices.contains(removedService.toUpperCase())){
        prerequisiteCheck.getFailedOn().add(removedService);
        String newService = entry.getValue();
        String msg = String.format(reason, removedService, newService);
        failReasons.add(msg);
      }
    }

    if(!failReasons.isEmpty()){
      prerequisiteCheck.setStatus(PrereqCheckStatus.FAIL);
      prerequisiteCheck.setFailReason(StringUtils.join(failReasons, '\n'));
    }
  }

  /**
   * Obtain property value specified in the upgrade XML
   * @return service name
   * */
  private String getPropertyValue(PrereqCheckRequest request, String propertyKey){
    String value = null;
    PrerequisiteCheckConfig prerequisiteCheckConfig = request.getPrerequisiteCheckConfig();
    Map<String, String> checkProperties = null;
    if(prerequisiteCheckConfig != null) {
      checkProperties = prerequisiteCheckConfig.getCheckProperties(this.getClass().getName());
    }
    if(checkProperties != null && checkProperties.containsKey(propertyKey)) {
      value = checkProperties.get(propertyKey);
    }
    return value;
  }

  /**
   * @return service names
   * */
  private List<String> getNoUpgradeSupportServices(PrereqCheckRequest request){
    List<String> result = new ArrayList<String>();
    String value = getPropertyValue(request, NO_UPGRADE_SUPPORT_SERVICES_PROPERTY_NAME);
    if (null != value){
      String[] services = value.split(",");
      for(String service: services){
        service = service.trim();
        if (!service.isEmpty()){
          result.add(service);
        }
      }
    }
    return result;
  }

  /**
   * @return service names and new service names map
   * */
  private Map<String, String> getRemovedServices(PrereqCheckRequest request) throws AmbariException{
    Map<String, String> result = new LinkedHashMap<String, String>();
    String value = getPropertyValue(request, REMOVED_SERVICES_PROPERTY_NAME);
    String newValue = getPropertyValue(request, NEW_SERVICES_PROPERTY_NAME);
    if(value == null && newValue == null){
      return result; //no need to check removed services as they are not specified in the upgrade xml file.
    } else {
      if (value == null || newValue == null){
        throw new AmbariException(String.format("Both %s and %s list must be specified in the upgrade XML file.", REMOVED_SERVICES_PROPERTY_NAME, NEW_SERVICES_PROPERTY_NAME));
      } else {
        List<String> oldServices = Arrays.asList(value.split(","));
        List<String> newServices = Arrays.asList(newValue.split(","));
        if (oldServices.size() != newServices.size()){
          throw new AmbariException(String.format("%s must have the same number of services as the %s list.", NEW_SERVICES_PROPERTY_NAME, REMOVED_SERVICES_PROPERTY_NAME));
        } else {
          for (int i = 0; i < oldServices.size(); i++){
            String oldService = oldServices.get(i).trim();
            String newService = newServices.get(i).trim();
            if (oldService.isEmpty() || newService.isEmpty()) {
              throw new AmbariException(String.format("Make sure both %s and %s list only contain comma separated list of services.", NEW_SERVICES_PROPERTY_NAME, REMOVED_SERVICES_PROPERTY_NAME));
            } else {
              result.put(oldService, newService);
            }
          }
        }
      }
    }
    return result;
  }
}
