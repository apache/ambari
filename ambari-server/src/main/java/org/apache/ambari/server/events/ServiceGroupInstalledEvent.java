/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.events;

/**
 * The {@link ServiceGroupInstalledEvent} class is fired when a service group is
 * successfully installed.
 */
public class ServiceGroupInstalledEvent extends ServiceGroupEvent {
  /**
   * Constructor.
   *
   * @param clusterId
   * @param serviceGroupName
   */
  public ServiceGroupInstalledEvent(long clusterId, String serviceGroupName) {
    super(AmbariEventType.SERVICE_GROUP_INSTALL_SUCCESS, clusterId, serviceGroupName);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder("ServiceGroupInstalledEvent{");
    buffer.append("cluserId=").append(m_clusterId);
    buffer.append(", serviceGroupName=").append(m_serviceGroupName);
    buffer.append("}");
    return buffer.toString();
  }
}