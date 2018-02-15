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

import java.util.Optional;

import org.apache.ambari.server.state.StackId;
import org.inferred.freebuilder.FreeBuilder;

/**
 * I provide additional information for a component specified in the blueprint,
 * based on values resolved from the stack and sensible defaults.
 */
@FreeBuilder
public interface ResolvedComponent {

  StackId stackId();
  Optional<String> serviceGroupName();
  String serviceType();
  Optional<String> serviceName();
  String componentName();
  boolean masterComponent();

  /**
   * @return the component as specified in the blueprint
   */
  Component component();

  /**
   * @return service group name if it set, otherwise defaults to the stack name
   */
  default String effectiveServiceGroupName() {
    return serviceGroupName().orElse(stackId().getStackName());
  }

  /**
   * @return service name if it set, otherwise defaults to the service type (eg. ZOOKEEPER)
   */
  default String effectiveServiceName() {
    return serviceName().orElse(serviceType());
  }

  /**
   * Starts building a {@code ResolvedComponent} for the given component.
   */
  static Builder builder(Component component) {
    return new Builder()
      .component(component)
      .componentName(component.getName())
      .serviceName(Optional.ofNullable(component.getServiceInstance()))
      .serviceGroupName(Optional.ofNullable(component.getMpackInstance()));
  }

  class Builder extends ResolvedComponent_Builder {
    protected Builder() {
      masterComponent(false);
    }
  }

}
