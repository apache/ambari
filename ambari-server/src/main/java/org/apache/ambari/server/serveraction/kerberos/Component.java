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
package org.apache.ambari.server.serveraction.kerberos;

import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public class Component {
  private final String hostName;
  private final String serviceName;
  private final String serviceComponentName;

  public static Component fromServiceComponentHost(ServiceComponentHost serviceComponentHost) {
    return new Component(
      serviceComponentHost.getHostName(),
      serviceComponentHost.getServiceName(),
      serviceComponentHost.getServiceComponentName());
  }

  public Component(String hostName, String serviceName, String serviceComponentName) {
    this.hostName = hostName;
    this.serviceName = serviceName;
    this.serviceComponentName = serviceComponentName;
  }

  public String getHostName() {
    return hostName;
  }

  public String getServiceName() {
    return serviceName;
  }

  public String getServiceComponentName() {
    return serviceComponentName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Component component = (Component) o;
    return new EqualsBuilder()
      .append(hostName, component.hostName)
      .append(serviceName, component.serviceName)
      .append(serviceComponentName, component.serviceComponentName)
      .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
      .append(hostName)
      .append(serviceName)
      .append(serviceComponentName)
      .toHashCode();
  }
}
