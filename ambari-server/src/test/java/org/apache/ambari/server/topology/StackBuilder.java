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

import org.apache.ambari.server.state.AutoDeployInfo;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.DependencyInfo;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;

import com.google.common.base.Preconditions;

/**
 * Helps build stack definitions for testing.
 */
public class StackBuilder {

  private final StackInfo stackInfo = new StackInfo();
  private final StackId stackId;
  private ServiceInfo currentService;
  private ComponentInfo currentComponent;
  private DependencyInfo currentDependency;

  public StackBuilder(StackId stackId) {
    this.stackId = stackId;
    stackInfo.setName(stackId.getStackName());
    stackInfo.setVersion(stackId.getStackVersion());
  }

  public StackBuilder addService(String service) {
    ServiceInfo previousService = currentService;
    currentService = getOrCreateService(service);
    if (currentService != previousService) {
      currentComponent = null;
      currentDependency = null;
    }
    return this;
  }

  public StackBuilder addComponent(String component) {
    Preconditions.checkNotNull(currentService);
    ComponentInfo previousComponent = currentComponent;
    currentComponent = getOrCreateComponent(component, currentService);
    if (currentComponent != previousComponent) {
      currentDependency = null;
    }
    return this;
  }

  public StackBuilder withCardinality(String cardinality) {
    Preconditions.checkNotNull(currentComponent);
    currentComponent.setCardinality(cardinality);
    return this;
  }

  public StackBuilder dependsOn(String component) {
    Preconditions.checkNotNull(currentService);
    Preconditions.checkNotNull(currentComponent);
    currentDependency = createDependency(currentComponent, currentService.getName(), component);
    return this;
  }

  public StackBuilder dependsOn(String service, String component) {
    Preconditions.checkNotNull(currentComponent);
    currentDependency = createDependency(currentComponent, service, component);
    return this;
  }

  public StackBuilder withScope(String scope, boolean autoDeployEnabled) {
    Preconditions.checkNotNull(currentDependency);
    currentDependency.setScope(scope);
    if (currentDependency.getAutoDeploy() == null) {
      currentDependency.setAutoDeploy(new AutoDeployInfo());
    }
    currentDependency.getAutoDeploy().setEnabled(autoDeployEnabled);
    return this;
  }

  public StackBuilder autoDeploy(boolean enabled) {
    Preconditions.checkNotNull(currentComponent);
    getOrCreateAutoDeployInfo(currentComponent).setEnabled(enabled);
    return this;
  }

  public StackBuilder coLocateWith(String component) {
    Preconditions.checkNotNull(currentService);
    coLocateWith(currentService.getName(), component);
    return this;
  }

  public StackBuilder coLocateWith(String service, String component) {
    Preconditions.checkNotNull(currentComponent);
    getOrCreateAutoDeployInfo(currentComponent).setCoLocate(String.format("%s/%s", service, component));
    getOrCreateComponent(component, getOrCreateService(service));
    return this;
  }

  public StackInfo stackInfo() {
    return stackInfo;
  }

  public ResolvedComponent lastAddedComponent() {
    Preconditions.checkNotNull(currentService);
    Preconditions.checkNotNull(currentComponent);
    return resolveComponent(currentComponent, currentService, stackId);
  }

  public ResolvedComponent componentToBeCoLocatedWith() {
    Preconditions.checkNotNull(currentComponent);
    Preconditions.checkNotNull(currentComponent.getAutoDeploy());
    Preconditions.checkNotNull(currentComponent.getAutoDeploy().getCoLocate());
    String[] parts = currentComponent.getAutoDeploy().getCoLocate().split("/");
    return componentOfType(parts[0], parts[1]);
  }

  public ResolvedComponent componentOfType(String component) {
    Preconditions.checkNotNull(currentService);
    ComponentInfo componentInfo = currentService.getComponentByName(component);
    return resolveComponent(componentInfo, currentService, stackId);
  }

  public ResolvedComponent componentOfType(String service, String component) {
    ServiceInfo serviceInfo = stackInfo.getService(service);
    ComponentInfo componentInfo = serviceInfo.getComponentByName(component);
    return resolveComponent(componentInfo, serviceInfo, stackId);
  }

  private static ResolvedComponent resolveComponent(ComponentInfo componentInfo, ServiceInfo serviceInfo, StackId stackId) {
    return ResolvedComponent.builder(new Component(componentInfo.getName()))
      .componentInfo(componentInfo)
      .serviceInfo(serviceInfo)
      .stackId(stackId)
      .build();
  }

  private DependencyInfo createDependency(ComponentInfo component, String dependencyServiceName, String dependencyComponentName) {
    getOrCreateComponent(dependencyComponentName, getOrCreateService(dependencyServiceName));

    DependencyInfo dependency = new DependencyInfo();
    dependency.setName(String.format("%s/%s", dependencyServiceName, dependencyComponentName));
    component.getDependencies().add(dependency);

    return dependency;
  }

  private ServiceInfo getOrCreateService(String stackServiceName) {
    ServiceInfo serviceInfo = stackInfo.getService(stackServiceName);
    if (serviceInfo == null) {
      serviceInfo = new ServiceInfo();
      serviceInfo.setName(stackServiceName);
      stackInfo.getServices().add(serviceInfo);
    }
    return serviceInfo;
  }

  private static ComponentInfo getOrCreateComponent(String componentName, ServiceInfo serviceInfo) {
    ComponentInfo componentInfo = serviceInfo.getComponentByName(componentName);
    if (componentInfo == null) {
      componentInfo = new ComponentInfo();
      componentInfo.setName(componentName);
      componentInfo.setCardinality("0+");
      serviceInfo.getComponents().add(componentInfo);
    }
    return componentInfo;
  }

  private static AutoDeployInfo getOrCreateAutoDeployInfo(ComponentInfo componentInfo) {
    if (componentInfo.getAutoDeploy() == null) {
      componentInfo.setAutoDeploy(new AutoDeployInfo());
    }
    return componentInfo.getAutoDeploy();
  }

}
