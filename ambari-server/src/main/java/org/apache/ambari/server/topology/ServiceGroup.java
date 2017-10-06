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


import java.util.Collection;
import java.util.Set;

public class ServiceGroup {

  private final String name;

  private final Collection<Service> services;

  private final Configuration configuration;

  private final Set<ServiceGroup> dependencies;

  public ServiceGroup(String name, Collection<Service> services) {
    this(name, services, null, null);
  }

  public ServiceGroup(String name, Collection<Service> services, Configuration configuration, Set<ServiceGroup> dependencies) {
    this.name = name;
    this.services = services;
    this.configuration = configuration;
    this.dependencies = dependencies;
  }

  /**
   * Gets the name of this service group
   *
   * @return component name
   */
  public String getName() {
    return this.name;
  }


  public Collection<Service> getServices() {
    return services;
  }

  public Configuration getConfiguration() {
    return configuration;
  }

  public Set<ServiceGroup> getDependencies() {
    return dependencies;
  }
}
