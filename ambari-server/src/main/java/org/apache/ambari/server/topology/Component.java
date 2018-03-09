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

import javax.annotation.Nullable;

import org.apache.ambari.server.controller.internal.ProvisionAction;

public class Component {

  private final String name;
  private final String mpackInstance;
  private final String serviceInstance;
  private final ProvisionAction provisionAction;

  @Deprecated
  public Component(String name) {
    this(name, null, null, null);
  }

  public Component(String name, @Nullable String mpackInstance, @Nullable String serviceInstance, ProvisionAction provisionAction) {
    this.name = name;
    this.mpackInstance = mpackInstance;
    this.serviceInstance = serviceInstance;
    this.provisionAction = provisionAction;
  }

  /**
   * Gets the name of this component
   *
   * @return component name
   */
  public String getName() {
    return this.name;
  }

  /**
   * @return the mpack associated with this component (can be {@code null} if component -> mpack mapping is unambiguous)
   */
  public String getMpackInstance() {
    return mpackInstance;
  }

  /**
   * @return the service instance this component belongs to. Can be {@code null} if component does not belong to a service
   * instance (there is a single service of the component's service type)
   */
  public String getServiceInstance() {
    return serviceInstance;
  }

  /**
   * Gets the provision action associated with this component.
   *
   * @return the provision action for this component, which
   *         may be null if the default action is to be used
   */
  public ProvisionAction getProvisionAction() {
    return this.provisionAction;
  }

  @Override
  public String toString() {
    return com.google.common.base.Objects.toStringHelper(this)
      .add("name", name)
      .add("mpackInstance", mpackInstance)
      .add("serviceInstance", serviceInstance)
      .add("provisionAction", provisionAction)
      .toString();
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Component component = (Component) o;
    return Objects.equals(name, component.name) &&
      Objects.equals(mpackInstance, component.mpackInstance) &&
      Objects.equals(serviceInstance, component.serviceInstance) &&
      provisionAction == component.provisionAction;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, mpackInstance, serviceInstance, provisionAction);
  }
}
