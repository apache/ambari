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

package org.apache.ambari.server.view.configuration;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import java.util.Arrays;
import java.util.List;

/**
 * View auto instance configuration.
 * </p>
 * Used by Ambari to automatically create an instance of a view and associate it with
 * a cluster if the cluster's stack and services match those given in this configuration.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class AutoInstanceConfig extends InstanceConfig {
  /**
   * The stack id.
   */
  @XmlElement(name="stack-id")
  private String stackId;

  /**
   * The list of view instances.
   */
  @XmlElementWrapper
  @XmlElement(name="service")
  private List<String> services;

  /**
   * Cluster Inherited permissions. Comma separated strings for multiple values
   * Possible values: ALL.CLUSTER.ADMINISTRATOR, ALL.CLUSTER.OPERATOR, ALL.CLUSTER.USER,
   * ALL.SERVICE.OPERATOR, ALL.SERVICE.ADMINISTRATOR
   */
  private String permissions;

  /**
   * Get the stack id used for auto instance creation.
   *
   * @return the stack id
   */
  public String getStackId() {
    return stackId;
  }

  /**
   * Get the services used for auto instance creation.
   *
   * @return the services
   */
  public List<String> getServices() {
    return services;
  }

  /**
   * @return the list of configured cluster inherited permissions
   */
  public List<String> getPermissions() {
    if(permissions == null) {
      return Lists.newArrayList();
    }
    return FluentIterable.from(Arrays.asList(permissions.split(","))).transform(new Function<String, String>() {
      @Override
      public String apply(String permission) {
        return permission.trim();
      }
    }).toList();
  }
}
