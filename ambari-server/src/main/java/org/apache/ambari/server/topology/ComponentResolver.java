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

import static java.util.stream.Collectors.toSet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.ambari.server.controller.internal.StackDefinition;
import org.apache.ambari.server.state.StackId;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;

/**
 * Resolves all incompletely specified host group components in the topology:
 * finds stack and/or service type that each component is defined in.
 */
public class ComponentResolver {

  private static final Logger LOG = LoggerFactory.getLogger(ComponentResolver.class);

  private final BlueprintBasedClusterProvisionRequest request;
  private final Map<String, ServiceInstance> uniqueServices;
  private final Map<String, Map<String, ServiceInstance>> mpackServices;

  public ComponentResolver(BlueprintBasedClusterProvisionRequest request) {
    this.request = request;
    uniqueServices = request.getUniqueServices();
    mpackServices = request.getServicesByMpack();
  }

  public Map<String, Set<ResolvedComponent>> resolve() {
    Map<String, Set<ResolvedComponent>> result = new HashMap<>();
    List<String> problems = new LinkedList<>();

    StackDefinition stack = request.getStack();
    for (HostGroup hg : request.getHostGroups().values()) {
      result.put(hg.getName(), new HashSet<>());

      for (Component comp : hg.getComponents()) {
        Stream<Pair<StackId, String>> servicesForComponent = stack.getServicesForComponent(comp.getName());
        servicesForComponent = filterByMpackName(comp, servicesForComponent);
        servicesForComponent = filterByServiceName(comp, servicesForComponent);

        Set<Pair<StackId, String>> serviceMatches = servicesForComponent.collect(toSet());

        if (serviceMatches.size() != 1) {
          String msg = formatResolutionProblemMessage(hg, comp, serviceMatches);
          LOG.warn("Component resolution failure:" + msg);
          problems.add(msg);
        } else {
          Pair<StackId, String> stackService = serviceMatches.iterator().next();
          StackId stackId = stackService.getLeft();
          String serviceType = stackService.getRight();
          String serviceName = comp.getServiceInstance();
          if (Strings.isNullOrEmpty(serviceName)) {
            serviceName = serviceType;
          }

          ResolvedComponent resolved = new ResolvedComponent(stackId, serviceName, serviceType, comp);
          LOG.debug("Component resolved: " + resolved);
          result.get(hg.getName()).add(resolved);
        }
      }
    }

    if (!problems.isEmpty()) {
      throw new IllegalArgumentException("Component resolution failure:\n" + Joiner.on("\n").join(problems));
    }

    return result;
  }

  private static String formatResolutionProblemMessage(HostGroup hg, Component comp, Set<Pair<StackId, String>> serviceMatches) {
    boolean multipleMatches = !serviceMatches.isEmpty();
    String problem = multipleMatches ? "Multiple services" : "No service";

    StringBuilder sb = new StringBuilder(problem)
      .append(" found for component ").append(comp.getName())
      .append(" in host group " ).append(hg.getName());

    if (!Strings.isNullOrEmpty(comp.getMpackInstance())) {
      sb.append(" mpack: ").append(comp.getMpackInstance());
    }
    if (!Strings.isNullOrEmpty(comp.getServiceInstance())) {
      sb.append(" service: ").append(comp.getServiceInstance());
    }
    if (multipleMatches) {
      sb.append(": ").append(serviceMatches);
    }

    return sb.toString();
  }

  // if component references a specific service instance, filter the stream by the type of that service
  private Stream<Pair<StackId, String>> filterByServiceName(Component comp, Stream<Pair<StackId, String>> stream) {
    if (!Strings.isNullOrEmpty(comp.getServiceInstance())) {
      String mpackName = comp.getMpackInstance();
      Map<String, ServiceInstance> services = !Strings.isNullOrEmpty(mpackName)
        ? mpackServices.get(mpackName)
        : uniqueServices;

      ServiceInstance service = services.get(comp.getServiceInstance());
      if (service != null) {
        String serviceType = service.getType();

        return stream.filter(pair -> pair.getRight().equals(serviceType));
      }
    }

    return stream;
  }

  // if component references a specific mpack instance, filter the stream by the name of that mpack
  private Stream<Pair<StackId, String>> filterByMpackName(Component comp, Stream<Pair<StackId, String>> stream) {
    if (!Strings.isNullOrEmpty(comp.getMpackInstance())) {
      return stream.filter(pair -> pair.getLeft().getStackName().equals(comp.getMpackInstance()));
    }

    return stream;
  }

}
