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
package org.apache.ambari.server.topology.addservice;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.apache.ambari.server.controller.internal.HostComponentResourceProvider.CLUSTER_NAME;
import static org.apache.ambari.server.controller.internal.HostComponentResourceProvider.COMPONENT_NAME;
import static org.apache.ambari.server.controller.internal.HostComponentResourceProvider.HOST_NAME;
import static org.apache.ambari.server.controller.internal.HostComponentResourceProvider.SERVICE_NAME;
import static org.apache.ambari.server.controller.predicate.Predicates.and;
import static org.apache.ambari.server.controller.predicate.Predicates.anyOf;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.apache.ambari.server.controller.internal.ProvisionAction;
import org.apache.ambari.server.controller.predicate.EqualsPredicate;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.topology.ProvisionStep;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 * Builds one or more predicates which can be used in the request for updating host component states.
 * {@link ProvisionStep}
 */
public class ProvisionActionPredicateBuilder {

  private final Map<ProvisionStep, Predicate> predicates = new EnumMap<>(ProvisionStep.class);

  public ProvisionActionPredicateBuilder(AddServiceInfo request) {
    String clusterName = request.clusterName();
    ProvisionAction requestAction = request.getRequest().getProvisionAction();
    Map<String, Map<String, Set<String>>> newServices = request.newServices();

    // create some helper maps
    Map<String, String> serviceByComponent =
      mapServicesByComponent(newServices);
    Map<String, ProvisionAction> customServiceActions =
      findServicesWithCustomAction(requestAction, request.getRequest().getServices());
    Map<String, Map<String, Map<ProvisionAction, Set<String>>>> customComponentActions =
      findComponentsWithCustomAction(requestAction, request.getRequest().getComponents(), serviceByComponent, customServiceActions);

    Map<ProvisionStep, List<Predicate>> servicePredicatesByStep = new EnumMap<>(ProvisionStep.class);

    for (Map.Entry<String, Map<String, Set<String>>> serviceEntry : newServices.entrySet()) {
      String serviceName = serviceEntry.getKey();
      ProvisionAction serviceAction = customServiceActions.getOrDefault(serviceName, requestAction);
      Predicate serviceNamePredicate = new EqualsPredicate<>(SERVICE_NAME, serviceName);

      Map<String, Map<ProvisionAction, Set<String>>> customActionByComponent = customComponentActions.get(serviceName);
      if (customActionByComponent == null) {
        classifyItem(serviceAction, serviceNamePredicate, servicePredicatesByStep);
      } else {
        Map<ProvisionStep, List<Predicate>> componentPredicatesByStep = new EnumMap<>(ProvisionStep.class);

        for (Map.Entry<String, Set<String>> componentEntry : serviceEntry.getValue().entrySet()) {
          String componentName = componentEntry.getKey();
          Set<String> allHosts = componentEntry.getValue();
          Map<ProvisionAction, Set<String>> hostsByAction = customActionByComponent.getOrDefault(componentName, ImmutableMap.of());

          if (!hostsByAction.isEmpty()) {
            Set<String> customActionHosts = new HashSet<>();
            for (Map.Entry<ProvisionAction, Set<String>> e : hostsByAction.entrySet()) {
              ProvisionAction componentAction = e.getKey();
              Set<String> hosts = e.getValue();
              Predicate componentPredicate = predicateForComponentHosts(componentName, hosts);
              classifyItem(componentAction, componentPredicate, componentPredicatesByStep);
              customActionHosts.addAll(hosts);
            }

            Set<String> leftoverHosts = ImmutableSet.copyOf(Sets.difference(allHosts, customActionHosts));
            if (!leftoverHosts.isEmpty()) {
              Predicate componentPredicate = predicateForComponentHosts(componentName, leftoverHosts);
              classifyItem(serviceAction, componentPredicate, componentPredicatesByStep);
            }
          } else {
            Predicate componentPredicate = predicateForComponent(componentName);
            classifyItem(serviceAction, componentPredicate, componentPredicatesByStep);
          }
        }

        Function<Predicate, Predicate> andServiceNameMatches = and(serviceNamePredicate);
        for (Map.Entry<ProvisionStep, List<Predicate>> entry : componentPredicatesByStep.entrySet()) {
          ProvisionStep step = entry.getKey();
          List<Predicate> componentPredicates = entry.getValue();
          anyOf(componentPredicates).map(andServiceNameMatches).ifPresent(predicate ->
            servicePredicatesByStep.computeIfAbsent(step, __ -> new LinkedList<>()).add(predicate)
          );
        }
      }
    }

    Function<Predicate, Predicate> andClusterNameMatches = and(new EqualsPredicate<>(CLUSTER_NAME, clusterName));
    for (Map.Entry<ProvisionStep, List<Predicate>> entry : servicePredicatesByStep.entrySet()) {
      ProvisionStep step = entry.getKey();
      List<Predicate> servicePredicates = entry.getValue();
      anyOf(servicePredicates).map(andClusterNameMatches).ifPresent(predicate -> predicates.put(step, predicate));
    }
  }

  public Optional<Predicate> getPredicate(ProvisionStep action) {
    return Optional.ofNullable(predicates.get(action));
  }

  /**
   * @param newServices service -> component -> hosts map
   * @return component -> service map
   */
  private static Map<String, String> mapServicesByComponent(Map<String, Map<String, Set<String>>> newServices) {
    Map<String, String> serviceByComponent = new HashMap<>();
    for (Map.Entry<String, Map<String, Set<String>>> e : newServices.entrySet()) {
      String service = e.getKey();
      for (String component : e.getValue().keySet()) {
        serviceByComponent.put(component, service);
      }
    }
    return serviceByComponent;
  }

  /**
   * @param requestAction default provision action of the request
   * @param services set of services explicitly listed in the request
   * @return service -> action map; only contains services whose action does not match the request-level action
   */
  private static Map<String, ProvisionAction> findServicesWithCustomAction(ProvisionAction requestAction, Set<Service> services) {
    return services.stream()
      .filter(service -> service.getProvisionAction().isPresent())
      .filter(service -> !Objects.equals(requestAction, service.getProvisionAction().get()))
      .collect(toMap(Service::getName, service -> service.getProvisionAction().get()));
  }

  /**
   * @param requestAction default provision action of the request
   * @param components set of components explicitly listed in the request
   * @param serviceByComponent component -> service map
   * @param actionByService service -> action map, only contains custom provision actions, which do not match the request's action
   * @return service -> component -> action -> hosts mapping; only contains components whose action does not match the upper-level (service or request) action
   */
  private static Map<String, Map<String, Map<ProvisionAction, Set<String>>>> findComponentsWithCustomAction(
    ProvisionAction requestAction,
    Set<Component> components,
    Map<String, String> serviceByComponent,
    Map<String, ProvisionAction> actionByService
  ) {
    Map<String, Map<String, Map<ProvisionAction, Set<String>>>> result = new HashMap<>();
    for (Component component : components) {
      component.getProvisionAction().ifPresent(componentAction -> {
        String componentName = component.getName();
        String serviceName = serviceByComponent.get(componentName);
        ProvisionAction serviceAction = actionByService.getOrDefault(serviceName, requestAction);
        if (!Objects.equals(serviceAction, componentAction)) {
          result
            .computeIfAbsent(serviceName, __ -> new HashMap<>())
            .computeIfAbsent(componentName, __ -> new EnumMap<>(ProvisionAction.class))
            .computeIfAbsent(componentAction, __ -> new HashSet<>())
            .addAll(component.getHosts().stream().map(Host::getFqdn).collect(toSet()));
        }
      });
    }
    return result;
  }

  /**
   * Adds {@code item} to the list(s) it belongs to depending on {@code action}.
   * @param action provision action
   * @param item the item to add
   * @param itemsByStep step -> list of items
   */
  private static <T> void classifyItem(ProvisionAction action, T item, Map<ProvisionStep, List<T>> itemsByStep) {
    for (ProvisionStep step : action.getSteps()) {
      itemsByStep.computeIfAbsent(step, __ -> new LinkedList<>()).add(item);
    }
  }

  private static Predicate predicateForComponentHosts(String componentName, Set<String> hosts) {
    Preconditions.checkNotNull(hosts);
    Preconditions.checkArgument(!hosts.isEmpty());
    Set<Predicate> hostPredicates = hosts.stream().map(ProvisionActionPredicateBuilder::predicateForHostname).collect(toSet());
    return anyOf(hostPredicates).map(and(predicateForComponent(componentName))).get();
  }

  private static Predicate predicateForComponent(String componentName) {
    return new EqualsPredicate<>(COMPONENT_NAME, componentName);
  }

  private static Predicate predicateForHostname(String hostname) {
    return new EqualsPredicate<>(HOST_NAME, hostname);
  }
}
