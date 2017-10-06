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
 * distributed under the License is distribut
 * ed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.topology;

import org.apache.ambari.server.controller.StackV2;
import org.apache.ambari.server.orm.entities.BlueprintEntity;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Blueprint implementation.
 */
public class BlueprintImplV2 implements BlueprintV2 {


  @Override
  public String getName() {
    return null;
  }

  @Override
  public HostGroupV2 getHostGroup(String name) {
    return null;
  }

  @Override
  public Map<String, HostGroupV2> getHostGroups() {
    return null;
  }

  @Override
  public Collection<StackV2> getStacks() {
    return null;
  }

  @Override
  public Collection<ServiceGroup> getServiceGroups() {
    return null;
  }

  @Override
  public Collection<Service> getAllServices() {
    return null;
  }

  @Override
  public Collection<ComponentV2> getComponents(Service service) {
    return null;
  }

  @Override
  public Collection<HostGroupV2> getHostGroupsForService(Service service) {
    return null;
  }

  @Override
  public Collection<HostGroupV2> getHostGroupsForComponent(ComponentV2 component) {
    return null;
  }

  @Override
  public Configuration getConfiguration() {
    return null;
  }

  @Override
  public Setting getSetting() {
    return null;
  }

  @Override
  public String getRecoveryEnabled(String serviceName, String componentName) {
    return null;
  }

  @Override
  public String getCredentialStoreEnabled(String serviceName) {
    return null;
  }

  @Override
  public boolean shouldSkipFailure() {
    return false;
  }

  @Override
  public SecurityConfiguration getSecurity() {
    return null;
  }

  @Override
  public void validateTopology() throws InvalidTopologyException {

  }

  @Override
  public void validateRequiredProperties() throws InvalidTopologyException {

  }

  @Override
  public boolean isValidConfigType(String configType) {
    return false;
  }

  @Override
  public BlueprintEntity toEntity() {
    return null;
  }

  @Override
  public List<RepositorySetting> getRepositorySettings() {
    return null;
  }
}
