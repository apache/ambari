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

package org.apache.ambari.view.slider;

import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.Exception;
import java.util.*;

@Singleton
public class SliderAppsAlerts {
  private static final Logger logger = LoggerFactory
     .getLogger(SliderAppsAlerts.class);

  public Map<String, Object> generateComponentsAlerts(Map<String, SliderAppComponent> components, String service){
     HashMap<String, Object> result = new HashMap<String, Object>();
     Set<Map<AlertField,Object>> details = buildAlertsDetails(components, service);

    result.put("detail", details);
    result.put("summary", buildAlertsSummary(details));

    return result;
  }

  private Map<AlertState,Integer> buildAlertsSummary(Set<Map<AlertField,Object>> details){
    Map<AlertState,Integer> result = new HashMap<AlertState, Integer>();

    // Initial filling of map with available states
    for (AlertState state:AlertState.values()){
      result.put(state, 0);
    }

    for(Map<AlertField,Object> item:details){
      AlertState state = (AlertState)item.get(AlertField.status);
      result.put(state,result.get(state)+1);
    }
    return result;
  }

  private Set<Map<AlertField,Object>> buildAlertsDetails(Map<String, SliderAppComponent> components, String service){
    HashSet<Map<AlertField,Object>> resultList = new HashSet<Map<AlertField, Object>>();
    for (String componentKey:components.keySet()){
      resultList.add(buildComponentAlert(components.get(componentKey), service));
    }
    return  resultList;
  }

  private Map<AlertField,Object> buildComponentAlert(SliderAppComponent component, String service){
    HashMap<AlertField,Object> alertItem = new HashMap<AlertField, Object>();
    Date date = Calendar.getInstance().getTime();

    int totalContainerCount = component.getInstanceCount();
    int activeContainerCount = component.getActiveContainers() != null ? component
        .getActiveContainers().size() : 0;
    AlertState state = AlertState.UNKNOWN;
    String message = String.format("%s out of %s active", activeContainerCount,
        totalContainerCount);
    if (totalContainerCount == activeContainerCount || totalContainerCount < 1) {
      // Everything OK
      state = AlertState.OK;
    } else {
      float fraction = (float) activeContainerCount / (float) totalContainerCount;
      if (fraction <= 0.2) { // less than or equal to 20%
        state = AlertState.WARNING;
      } else {
        state = AlertState.CRITICAL;
      }
    }
    alertItem.put(AlertField.description, String.format("%s component",component.getComponentName()));
    alertItem.put(AlertField.host_name, getComponentHostName(component));
    alertItem.put(AlertField.last_status, state);

    alertItem.put(AlertField.last_status_time, new java.sql.Timestamp(date.getTime()));

    alertItem.put(AlertField.service_name, service.toUpperCase());
    alertItem.put(AlertField.component_name, component.getComponentName());
    alertItem.put(AlertField.status, state);
    alertItem.put(AlertField.status_time, new java.sql.Timestamp(date.getTime()));
    alertItem.put(AlertField.output, message);
    alertItem.put(AlertField.actual_status, state);
    return alertItem;
  }

  @SuppressWarnings("unchecked")
  private String getComponentHostName(SliderAppComponent component){
    Map<String,Map<String,String>> containers = null;

    if (component.getActiveContainers().size() > 0){
      containers = component.getActiveContainers();
    }

    if (component.getCompletedContainers().size() > 0 && containers == null) {
      containers =component.getCompletedContainers();
    }


    if (containers != null){
      try {
        // try to obtain host name from any first available container
        return ((Map<String,String>)containers.values().toArray()[0]).get("host");
      } catch (Exception e){
        if (logger.isDebugEnabled()){
          logger.warn("Couldn't obtain host name for the component", e);
        }
      }
    }
    return null;
  }
}
