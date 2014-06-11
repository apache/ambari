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
package org.apache.ambari.server.controller.nagios;

import org.apache.ambari.server.state.Alert;

/**
 * Represents a Nagios alert as represented by the JSON returning from the HTTP
 * call.
 */
public class NagiosAlert {
  private String service_description = null;
  private String host_name = null;
//  private String current_attempt = null;
  private String current_state = null;
  private String plugin_output = null;
  private String last_hard_state_change = null;
  private String last_hard_state = null;
  private String last_time_ok = null;
  private String last_time_warning = null;
  private String last_time_unknown = null;
  private String last_time_critical = null;
//  private String is_flapping = null;
//  private String last_check = null;
  private String service_type = null;
  private String long_plugin_output = null;

  
  /**
   * Use a cluster alert as the basis for this alert.  This bridge can be
   * removed when Nagios is not longer the Source For Alerts.
   */
  public NagiosAlert(Alert alert) {
    service_type = alert.getService();
    host_name = alert.getHost();
    switch (alert.getState()) {
    case CRITICAL:
      current_state = "2";
      break;
    case OK:
      current_state = "0";
      break;
    case WARNING:
      current_state = "1";
      break;
    default:
      current_state = "3";
      break;
    }
    
    service_description = alert.getLabel();
    plugin_output = alert.getText();
    
  }
  
  public NagiosAlert() {
  }

  /**
   * @return the description
   */
  public String getDescription() {
    return service_description;
  }
  
  /**
   * @return the host
   */
  public String getHost() {
    return host_name;
  }
  
  /**
   * @return the service
   */
  public String getService() {
    return service_type;
  }
  
  /**
   * @return the status
   */
  public int getStatus() {
    int i = 3;
    try {
      i = Integer.parseInt(current_state);
    } catch (Exception e) {
      // don't ruin someone's day
    }
    return i;
  }
  
  
  /**
   * @return the last status
   */
  public int getLastStatus() {
    int i = 3;
    try {
      i = Integer.parseInt(last_hard_state);
    } catch (Exception e) {
      // don't ruin someone's day      
    }
    
    return i;
  }  
  
  /**
   * @return the output
   */
  public String getOutput() {
    return plugin_output;
  }
  
  /**
   * @param status the status
   * @return a string indicating what the status means
   */
  public static String getStatusString(int status) {
    switch (status) {
      case 0:
        return "OK";
      case 1:
        return "WARNING";
      case 2:
        return "CRITICAL";
      default:
        return "UNKNOWN";
    }
  }
  
  
  /**
   * @return the status timestamp
   */
  public long getStatusTime() {
    long l = -1L;
    
    try {
      switch (getStatus()) {
        case 0:
          l = getLong (last_time_ok);
          break;
        case 1:
          l = getLong(last_time_warning);
          break;
        case 2:
          l = getLong(last_time_critical);
          break;
        default:
          l = getLong(last_time_unknown);
          break;
      }
    } catch (Exception e) {
      // don't ruin someone's day
    }
    
    return l;
  }

  /**
   * @return the last status timestamp
   */
  public long getLastStatusTime() {
    return getLong(last_hard_state_change);
  }
  
  private long getLong(String str) {
    try {
      return Long.parseLong(str);
    } catch (Exception e) {
      return 0L;
    }
  }
  
  /**
   * @return the long output, if any
   */
  public String getLongPluginOutput() {
    return long_plugin_output;
  }
  
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append('{');
    sb.append(this.service_type).append(',');
    sb.append(this.host_name).append(',');
    sb.append(this.current_state);
    sb.append('}');
    return sb.toString();
  }

}
