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

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ServiceId {

  public static final ServiceId NULL = new ServiceId(null, null);

  private final String serviceGroup;
  private final String name;

  @JsonCreator
  public ServiceId(@JsonProperty("service_name") String name, @JsonProperty("service_group") String serviceGroup) {
    this.name = name;
    this.serviceGroup = serviceGroup;
  }

  public static ServiceId of(String name, String serviceGroup) {
    return new ServiceId(name, serviceGroup);
  }

  @JsonProperty("service_group")
  public String getServiceGroup() {
    return serviceGroup;
  }

  @JsonProperty("service_name")
  public String getName() {
    return name;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ServiceId other = (ServiceId) o;

    return Objects.equals(serviceGroup, other.serviceGroup) &&
      Objects.equals(name, other.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(serviceGroup, name);
  }

  @Override
  public String toString() {
    return "ServiceId{" +
      "serviceGroup='" + serviceGroup + '\'' +
      ", name='" + name + '\'' +
      '}';
  }
}
