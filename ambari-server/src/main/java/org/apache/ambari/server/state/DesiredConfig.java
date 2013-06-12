/*
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
package org.apache.ambari.server.state;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;

/**
 * Class that holds information about a desired config and is suitable for output
 * in a web service response.
 */
public class DesiredConfig {

  private String versionTag;
  private String serviceName;
  private String user;
  private List<HostOverride> hostOverrides = new ArrayList<HostOverride>();

  /**
   * Sets the version tag
   * @param version the version tag
   */
  public void setVersion(String version) {
    versionTag = version;
  }

  /**
   * Gets the version tag
   * @return the version tag
   */
  @JsonProperty("tag")
  public String getVersion() {
    return versionTag;
  }

  /**
   * Gets the service name (if any) for the desired config.
   * @return the service name
   */
  @JsonSerialize(include = Inclusion.NON_NULL)
  @JsonProperty("service_name")
  public String getServiceName() {
    return serviceName;
  }

  /**
   * Sets the service name (if any) for the desired config.
   * @param name the service name
   */
  public void setServiceName(String name) {
    serviceName = name;
  }
  
  /**
   * Sets the host overrides for the desired config.  Cluster-based desired configs only.
   * @param overrides the host names
   */
  public void setHostOverrides(List<HostOverride> overrides) {
    hostOverrides = overrides;
  }

  /**
   * Gets the user that set the desired config.
   */
  @JsonSerialize(include = Inclusion.NON_EMPTY)
  public String getUser() {
    return user;
  }
  
  /**
   * Sets the user that set the desired config.
   * @param userName the username
   */
  public void setUser(String userName) {
    user = userName;
  }
  
  
  /**
   * Gets the host overrides for the desired config.  Cluster-based desired configs only.
   * @return the host names that override the desired config
   */
  @JsonSerialize(include = Inclusion.NON_EMPTY)
  @JsonProperty("host_overrides")
  public List<HostOverride> getHostOverrides() {
    return hostOverrides;
  }
  
  /**
   * Used to represent an override on a host.
   */
  public static class HostOverride {
    private String hostName;
    private String versionOverrideTag;

    /**
     * @param name the host name
     * @param tag the config tag
     */
    public HostOverride(String name, String tag) {
      hostName = name;
      versionOverrideTag = tag;
    }

    /**
     * @return the override host name
     */
    @JsonProperty("host_name")
    public String getName() {
      return hostName;
    }

    /**
     * @return the override version tag
     */
    @JsonProperty("tag")
    public String getVersionTag() {
      return versionOverrideTag;
    }

  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    sb.append("version=").append(versionTag);
    if (null != serviceName)
      sb.append(", service=").append(serviceName);
    if (null != hostOverrides && hostOverrides.size() > 0) {
      sb.append(", hosts=[");
      int i = 0;
      for (DesiredConfig.HostOverride h : hostOverrides)
      {
        if (i++ != 0)
          sb.append(",");
        sb.append(h.getName()).append(':').append(h.getVersionTag());
      }

      sb.append(']');
    }
    sb.append("}");

    return sb.toString();
  }

}
