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
package org.apache.ambari.server.topology;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ServiceId {
  private String serviceGroup;
  private String name;

  public ServiceId() { }

  public static ServiceId of(String name, String serviceGroup) {
    ServiceId id = new ServiceId();
    id.name = name;
    id.serviceGroup = serviceGroup;
    return id;
  }

  public String getServiceGroup() {
    return serviceGroup;
  }

  @JsonProperty("service_group")
  public void setServiceGroup(String serviceGroup) {
    this.serviceGroup = serviceGroup;
  }

  public String getName() {
    return name;
  }

  @JsonProperty("service_name")
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ServiceId serviceId = (ServiceId) o;

    if (serviceGroup != null ? !serviceGroup.equals(serviceId.serviceGroup) : serviceId.serviceGroup != null)
      return false;
    return name != null ? name.equals(serviceId.name) : serviceId.name == null;
  }

  @Override
  public int hashCode() {
    int result = serviceGroup != null ? serviceGroup.hashCode() : 0;
    result = 31 * result + (name != null ? name.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "ServiceId{" +
      "serviceGroup='" + serviceGroup + '\'' +
      ", name='" + name + '\'' +
      '}';
  }
}
