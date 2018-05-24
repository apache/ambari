/*
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
package org.apache.ambari.server.controller;

import java.util.Objects;

import org.apache.commons.lang.builder.EqualsBuilder;

import com.google.common.base.MoreObjects;

public class ServiceGroupRequest {

  private String clusterName; // REF
  private String serviceGroupName; // GET/CREATE/UPDATE/DELETE

  /**
   * ServiceGroups should be addressed by their StackID and/or Mpack ID
   */
  @Deprecated
  private String stack; // Associated stack version info

  public ServiceGroupRequest(String clusterName, String serviceGroupName, String stack) {
    this.clusterName = clusterName;
    this.serviceGroupName = serviceGroupName;
    this.stack = stack;
  }

  /**
   * @return the clusterName
   */
  public String getClusterName() {
    return clusterName;
  }

  /**
   * @param clusterName the clusterName to set
   */
  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  /**
   * @return the serviceGroupName
   */
  public String getServiceGroupName() {
    return serviceGroupName;
  }

  /**
   * @param serviceGroupName the service group name to set
   */
  public void setServiceGroupName(String serviceGroupName) {
    this.serviceGroupName = serviceGroupName;
  }

  /**
   * ServiceGroups should be addressed by their StackID and/or Mpack ID
   *
   * @return the servicegroup version
   */
  @Deprecated
  public String getStack() {
    return stack;
  }

  /**
   * ServiceGroups should be addressed by their StackID and/or Mpack ID
   *
   * @param stack
   *          the servicegroup stack to set
   */
  @Deprecated
  public void setStack(String stack) {
    this.stack = stack;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("clusterName", clusterName)
        .add("serviceGroupName",serviceGroupName)
        .add("stackId", stack).toString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }

    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    ServiceGroupRequest that = (ServiceGroupRequest) obj;
    EqualsBuilder equalsBuilder = new EqualsBuilder();

    equalsBuilder.append(clusterName, that.clusterName);
    equalsBuilder.append(serviceGroupName, that.serviceGroupName);
    equalsBuilder.append(stack, that.stack);

    return equalsBuilder.isEquals();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return Objects.hash(clusterName, serviceGroupName, stack);
  }
}