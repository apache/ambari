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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;

public class ServiceGroup {

  private String name;
  private Map<String, Service> servicesByName;
  private ListMultimap<String, Service> servicesByType;
  private Configuration configuration;
  private Set<String> dependencies = new HashSet<>();

  public ServiceGroup() { }

  /**
   * Gets the name of this service group
   *
   * @return component name
   */
  public String getName() {
    return this.name;
  }

  public Collection<Service> getServices() {
    return servicesByName.values();
  }

  public Service getServiceByName(String name) {
    return servicesByName.get(name);
  }

  public List<Service> getServiceByType(String name) {
    return servicesByType.get(name);
  }

  public Configuration getConfiguration() {
    return configuration;
  }

  public Set<String> getDependencies() {
    return dependencies;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setServices(Collection<Service> services) {
    services.forEach(s -> s.setServiceGroup(this));
    this.servicesByName = services.stream().collect(Collectors.toMap(Service::getName, Function.identity()));
    this.servicesByType = Multimaps.index(services, Service::getType);
  }

  public void setConfiguration(Configuration configuration) {
    this.configuration = configuration;
  }

  public void setDependencies(Set<String> dependencies) {
    this.dependencies = dependencies;
  }

}
