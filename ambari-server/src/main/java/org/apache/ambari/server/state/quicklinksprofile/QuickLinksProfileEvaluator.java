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

package org.apache.ambari.server.state.quicklinksprofile;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.ambari.server.state.quicklinks.Link;

import com.google.common.base.Optional;

/**
 * This class can evaluate whether a quicklink has to be shown or hidden based on the received {@link QuickLinksProfile}.
 */
public class QuickLinksProfileEvaluator {
  private final Evaluator globalRules;
  private final Map<String, Evaluator> serviceRules = new HashMap<>();
  private final Map<ServiceComponent, Evaluator> componentRules = new HashMap<>();

  public QuickLinksProfileEvaluator(QuickLinksProfile profile) throws QuickLinksProfileEvaluatorException {
    globalRules = new Evaluator(profile.getFilters());
    for (Service service: nullToEmptyList(profile.getServices())) {
      serviceRules.put(service.getName(), new Evaluator(service.getFilters()));
      for (Component component: nullToEmptyList(service.getComponents())) {
        componentRules.put(ServiceComponent.of(service.getName(), component.getName()),
            new Evaluator(component.getFilters()));
      }
    }
  }

  /**
   * @param service the name of the service
   * @param quickLink the quicklink
   * @return a boolean indicating whether the link in the parameter should be visible
   */
  public boolean isVisible(@Nonnull String service, @Nonnull Link quickLink) {
    // First, component rules are evaluated if exist and applicable
    Optional<Boolean> componentResult = evaluateComponentRules(service, quickLink);
    if (componentResult.isPresent()) {
      return componentResult.get();
    }

    // Secondly, service level rules are applied
    Optional<Boolean> serviceResult = evaluateServiceRules(service, quickLink);
    if (serviceResult.isPresent()) {
      return serviceResult.get();
    }

    // Global rules are evaluated lastly. If no rules apply to the link, it will be hidden.
    return globalRules.isVisible(quickLink).or(false);
  }

  private Optional<Boolean> evaluateComponentRules(@Nonnull String service, @Nonnull Link quickLink) {
    if (null == quickLink.getComponentName()) {
      return Optional.absent();
    }
    else {
      Evaluator componentEvaluator = componentRules.get(ServiceComponent.of(service, quickLink.getComponentName()));
      return componentEvaluator != null ? componentEvaluator.isVisible(quickLink) : Optional.<Boolean>absent();
    }
  }

  private Optional<Boolean> evaluateServiceRules(@Nonnull String service, @Nonnull Link quickLink) {
    return serviceRules.containsKey(service) ?
        serviceRules.get(service).isVisible(quickLink) : Optional.<Boolean>absent();
  }

  static <T> List<T> nullToEmptyList(@Nullable List<T> items) {
    return items != null ? items : Collections.<T>emptyList();
  }
}

/**
 * Groups quicklink filters that are on the same level (e.g. a global evaluator or an evaluator for the "HDFS" service,
 * etc.). The evaluator pick the most applicable filter for a given quick link. If no applicable filter is found, it
 * returns {@link Optional#absent()}.
 * <p>
 *   Filter evaluation order is the following:
 *   <ol>
 *     <li>First, link name filters are evaluated. These match links by name.</li>
 *     <li>If there is no matching link name filter, link attribute filters are evaluated next. "Hide" type filters
 *     take precedence to "show" type filters.</li>
 *     <li>Finally, the match-all filter is evaluated, provided it exists.</li>
 *   </ol>
 * </p>
 */
class Evaluator {
  private final Map<String, Boolean> linkNameFilters = new HashMap<>();
  private final Set<String> showAttributes = new HashSet<>();
  private final Set<String> hideAttributes = new HashSet<>();
  private Optional<Boolean> acceptAllFilter = Optional.absent();

  Evaluator(List<Filter> filters) throws QuickLinksProfileEvaluatorException {
    for (Filter filter: QuickLinksProfileEvaluator.nullToEmptyList(filters)) {
      if (filter instanceof LinkNameFilter) {
        String linkName = ((LinkNameFilter)filter).getLinkName();
        if (linkNameFilters.containsKey(linkName) && linkNameFilters.get(linkName) != filter.isVisible()) {
          throw new QuickLinksProfileEvaluatorException("Contradicting filters for link name [" + linkName + "]");
        }
        linkNameFilters.put(linkName, filter.isVisible());
      }
      else if (filter instanceof LinkAttributeFilter) {
        String linkAttribute = ((LinkAttributeFilter)filter).getLinkAttribute();
        if (filter.isVisible()) {
          showAttributes.add(linkAttribute);
        }
        else {
          hideAttributes.add(linkAttribute);
        }
        if (showAttributes.contains(linkAttribute) && hideAttributes.contains(linkAttribute)) {
          throw new QuickLinksProfileEvaluatorException("Contradicting filters for link attribute [" + linkAttribute + "]");
        }
      }
      // If none of the above, it is an accept-all filter. We expect only one for an Evaluator
      else {
        if (acceptAllFilter.isPresent() && !acceptAllFilter.get().equals(filter.isVisible())) {
          throw new QuickLinksProfileEvaluatorException("Contradicting accept-all filters.");
        }
        acceptAllFilter = Optional.of(filter.isVisible());
      }
    }
  }

  /**
   * @param quickLink the link to evaluate
   * @return Three way evaluation result, which can be one of these:
   *    show: Optional.of(true), hide: Optional.of(false), don't know: absent optional
   */
  Optional<Boolean> isVisible(Link quickLink) {
    // process first priority filters based on link name
    if (linkNameFilters.containsKey(quickLink.getName())) {
      return Optional.of(linkNameFilters.get(quickLink.getName()));
    }

    // process second priority filters based on link attributes
    // 'hide' rules take precedence over 'show' rules
    for (String attribute: QuickLinksProfileEvaluator.nullToEmptyList(quickLink.getAttributes())) {
      if (hideAttributes.contains(attribute)) return Optional.of(false);
    }
    for (String attribute: QuickLinksProfileEvaluator.nullToEmptyList(quickLink.getAttributes())) {
      if (showAttributes.contains(attribute)) return Optional.of(true);
    }

    // accept all filter (if exists) is the last priority
    return acceptAllFilter;
  }
}

/**
 * Simple value class encapsulating a link name an component name.
 */
class ServiceComponent {
  private final String service;
  private final String component;

  ServiceComponent(String service, String component) {
    this.service = service;
    this.component = component;
  }

  static ServiceComponent of(String service, String component) {
    return new ServiceComponent(service, component);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ServiceComponent that = (ServiceComponent) o;
    return Objects.equals(service, that.service) &&
        Objects.equals(component, that.component);
  }

  @Override
  public int hashCode() {
    return Objects.hash(service, component);
  }
}