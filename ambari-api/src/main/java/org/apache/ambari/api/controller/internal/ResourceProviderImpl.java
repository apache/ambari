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

package org.apache.ambari.api.controller.internal;

import org.apache.ambari.api.controller.spi.ManagementController;
import org.apache.ambari.api.controller.spi.Predicate;
import org.apache.ambari.api.controller.spi.PropertyId;
import org.apache.ambari.api.controller.spi.Request;
import org.apache.ambari.api.controller.spi.Resource;
import org.apache.ambari.api.controller.spi.ResourceProvider;

import java.util.HashSet;
import java.util.Set;

/**
 */
public abstract class ResourceProviderImpl implements ResourceProvider {

  protected final Set<PropertyId> propertyIds;

  private final ManagementController managementController;

  private ResourceProviderImpl(Set<PropertyId> propertyIds, ManagementController managementController) {
    this.propertyIds = propertyIds;
    this.managementController = managementController;
  }

  @Override
  public Set<PropertyId> getPropertyIds() {
    return propertyIds;
  }

  public ManagementController getManagementController() {
    return managementController;
  }

  public static ResourceProvider getResourceProvider(Resource.Type type, Set<PropertyId> propertyIds, ManagementController managementController)  {

    switch (type) {
      case Cluster:
        return new ClusterResourceProvider(propertyIds, managementController);
      case Service:
        return new ServiceResourceProvider(propertyIds, managementController);
      case Component:
        return new ComponentResourceProvider(propertyIds, managementController);
      case Host:
        return new HostResourceProvider(propertyIds, managementController);
      case HostComponent:
        return new HostComponentResourceProvider(propertyIds, managementController);
    }
    throw new IllegalArgumentException("Unknown type " + type);
  }

  protected Request getRequest(Request request) {
    Set<PropertyId> propertyIds = new HashSet<PropertyId>(request.getPropertyIds());
    if (propertyIds.size() == 0) {
      request = new RequestImpl(this.propertyIds, null);
    }
    return request;
  }

  private static class ClusterResourceProvider extends ResourceProviderImpl{

    private ClusterResourceProvider(Set<PropertyId> propertyIds, ManagementController managementController) {
      super(propertyIds, managementController);
    }

    @Override
    public void createResources(Request request) {
      getManagementController().createClusters(request);
    }

    @Override
    public Set<Resource> getResources(Request request, Predicate predicate) {
      request = getRequest(request);
      return getManagementController().getClusters(request, predicate);
    }

    @Override
    public void updateResources(Request request, Predicate predicate) {
      getManagementController().updateClusters(request, predicate);
    }

    @Override
    public void deleteResources(Predicate predicate) {
      getManagementController().deleteClusters(predicate);
    }
  }

  private static class ServiceResourceProvider extends ResourceProviderImpl{

    private ServiceResourceProvider(Set<PropertyId> propertyIds, ManagementController managementController) {
      super(propertyIds, managementController);
    }

    @Override
    public void createResources(Request request) {
      getManagementController().createServices(request);
    }

    @Override
    public Set<Resource> getResources(Request request, Predicate predicate) {
      request = getRequest(request);
      return getManagementController().getServices(request, predicate);
    }

    @Override
    public void updateResources(Request request, Predicate predicate) {
      getManagementController().updateServices(request, predicate);
    }

    @Override
    public void deleteResources(Predicate predicate) {
      getManagementController().deleteServices(predicate);
    }
  }

  private static class ComponentResourceProvider extends ResourceProviderImpl{

    private ComponentResourceProvider(Set<PropertyId> propertyIds, ManagementController managementController) {
      super(propertyIds, managementController);
    }

    @Override
    public void createResources(Request request) {
      getManagementController().createComponents(request);
    }

    @Override
    public Set<Resource> getResources(Request request, Predicate predicate) {
      request = getRequest(request);
      return getManagementController().getComponents(request, predicate);
    }

    @Override
    public void updateResources(Request request, Predicate predicate) {
      getManagementController().updateComponents(request, predicate);
    }

    @Override
    public void deleteResources(Predicate predicate) {
      getManagementController().deleteComponents(predicate);
    }
  }

  private static class HostResourceProvider extends ResourceProviderImpl{

    private HostResourceProvider(Set<PropertyId> propertyIds, ManagementController managementController) {
      super(propertyIds, managementController);
    }

    @Override
    public void createResources(Request request) {
      getManagementController().createHosts(request);
    }

    @Override
    public Set<Resource> getResources(Request request, Predicate predicate) {
      request = getRequest(request);
      return getManagementController().getHosts(request, predicate);
    }

    @Override
    public void updateResources(Request request, Predicate predicate) {
      getManagementController().updateHosts(request, predicate);
    }

    @Override
    public void deleteResources(Predicate predicate) {
      getManagementController().deleteHosts(predicate);
    }
  }

  private static class HostComponentResourceProvider extends ResourceProviderImpl{

    private HostComponentResourceProvider(Set<PropertyId> propertyIds, ManagementController managementController) {
      super(propertyIds, managementController);
    }

    @Override
    public void createResources(Request request) {
      getManagementController().createHostComponents(request);
    }

    @Override
    public Set<Resource> getResources(Request request, Predicate predicate) {
      request = getRequest(request);
      return getManagementController().getHostComponents(request, predicate);
    }

    @Override
    public void updateResources(Request request, Predicate predicate) {
      getManagementController().updateHostComponents(request, predicate);
    }

    @Override
    public void deleteResources(Predicate predicate) {
      getManagementController().deleteHostComponents(predicate);
    }
  }
}

