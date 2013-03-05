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
package org.apache.ambari.server.state;

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
  
  
  public void setVersion(String version) {
    versionTag = version;
  }
  
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
   * @param name
   */
  public void setServiceName(String name) {
    serviceName = name;
  }
  

  
 
}
