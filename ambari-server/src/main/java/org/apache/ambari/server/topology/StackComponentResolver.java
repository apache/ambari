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

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.ambari.server.controller.internal.StackDefinition;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

public class StackComponentResolver implements ComponentResolver {

  private static final Logger LOG = LoggerFactory.getLogger(StackComponentResolver.class);

  @Override
  public Map<String, Set<ResolvedComponent>> resolveComponents(BlueprintBasedClusterProvisionRequest request) {
    Collection<MpackInstance> mpacks = request.getMpacks();
    Map<String, StackId> stackIdByMpackName = getMpackStackIds(mpacks);

    Map<String, Set<ResolvedComponent>> result = new HashMap<>();
    List<String> problems = new LinkedList<>();

    StackDefinition stack = request.getStack();
    for (HostGroup hg : request.getHostGroups().values()) {
      Set<ResolvedComponent> hostGroupComponents = new HashSet<>();
      result.put(hg.getName(), hostGroupComponents);

      for (Component comp : hg.getComponents()) {
        String mpackInstanceName = comp.getMpackInstance();
        Stream<Pair<StackId, ServiceInfo>> servicesForComponent = stack.getServicesForComponent(comp.getName());
        servicesForComponent = filterByMpackName(mpackInstanceName, servicesForComponent, stackIdByMpackName);

        try {
          ResolvedComponent resolved = getComponent(comp, servicesForComponent);
          LOG.debug("Component resolved: " + resolved);
          hostGroupComponents.add(resolved);
        } catch (AmbiguousComponentException e) {
          String msg = formatResolutionProblemMessage(hg, comp, e.getMessage());
          LOG.warn("Component resolution failure:" + msg);
          problems.add(msg);
        }
      }
    }

    if (!problems.isEmpty()) {
      throw new IllegalArgumentException("Component resolution failure:\n" + String.join("\n", problems));
    }

    return result;
  }

  public static ResolvedComponent getComponent(Component comp, Stream<Pair<StackId, ServiceInfo>> servicesForComponent) throws AmbiguousComponentException {
    Set<Pair<StackId, ServiceInfo>> serviceMatches = servicesForComponent.collect(toSet());

    if (serviceMatches.size() != 1) {
      throw new AmbiguousComponentException(serviceMatches);
    }

    Pair<StackId, ServiceInfo> stackService = serviceMatches.iterator().next();
    StackId stackId = stackService.getLeft();
    ServiceInfo serviceInfo = stackService.getRight();
    ComponentInfo componentInfo = serviceInfo.getComponentByName(comp.getName());

    return ResolvedComponent.builder(comp)
      .stackId(stackId)
      .serviceInfo(serviceInfo)
      .componentInfo(componentInfo)
      .build();
  }

  private static String formatResolutionProblemMessage(HostGroup hg, Component comp, String message) {
    StringBuilder sb = new StringBuilder(message)
      .append(" for component ").append(comp.getName())
      .append(" in host group " ).append(hg.getName());

    if (!Strings.isNullOrEmpty(comp.getMpackInstance())) {
      sb.append(" mpack: ").append(comp.getMpackInstance());
    }
    if (!Strings.isNullOrEmpty(comp.getServiceInstance())) {
      sb.append(" service: ").append(comp.getServiceInstance());
    }

    return sb.toString();
  }

  // if component references a specific mpack instance, filter the stream by the name of that mpack
  private static Stream<Pair<StackId, ServiceInfo>> filterByMpackName(
    String mpackInstanceName,
    Stream<Pair<StackId, ServiceInfo>> stream,
    Map<String, StackId> stackIdByMpackInstanceName
  ) {
    if (mpackInstanceName != null) {
      StackId mpackStackId = stackIdByMpackInstanceName.get(mpackInstanceName);
      return stream.filter(pair -> Objects.equals(pair.getLeft(), mpackStackId));
    }
    return stream;
  }

  private static Map<String, StackId> getMpackStackIds(Collection<MpackInstance> mpacks) {
    return mpacks.stream()
      .collect(toMap(MpackInstance::getMpackName, MpackInstance::getStackId));
  }

}
