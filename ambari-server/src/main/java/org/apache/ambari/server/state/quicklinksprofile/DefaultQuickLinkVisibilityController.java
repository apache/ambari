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

import static java.util.stream.Collectors.toMap;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.ambari.server.state.quicklinks.Link;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

/**
 * This class can evaluate whether a quicklink has to be shown or hidden based on the received {@link QuickLinksProfile}.
 */
public class DefaultQuickLinkVisibilityController implements QuickLinkVisibilityController {
  private static final Logger LOG = LoggerFactory.getLogger(DefaultQuickLinkVisibilityController.class);

  private final FilterEvaluator globalRules;
  private final Map<String, FilterEvaluator> serviceRules = new HashMap<>();
  private final Map<ServiceComponent, FilterEvaluator> componentRules = new HashMap<>();
  /**
   * Map of (service name, link name) -> url
   */
  private final Map<Pair<String, String>, String> urlOverrides = new HashMap<>();


  public DefaultQuickLinkVisibilityController(QuickLinksProfile profile) throws QuickLinksProfileEvaluationException {
    int filterCount = size(profile.getFilters());
    globalRules = new FilterEvaluator(profile.getFilters());
    for (Service service: profile.getServices()) {
      filterCount += size(service.getFilters());
      serviceRules.put(service.getName(), new FilterEvaluator(service.getFilters()));
      for (Component component: service.getComponents()) {
        filterCount += size(component.getFilters());
        componentRules.put(ServiceComponent.of(service.getName(), component.getName()),
            new FilterEvaluator(component.getFilters()));
      }
    }
    if (filterCount == 0) {
      throw new QuickLinksProfileEvaluationException("At least one filter must be defined.");
    }

    // compute url overrides
    profile.getFilters().stream()
      .filter(f -> f instanceof LinkNameFilter && ((LinkNameFilter)f).getLinkUrl() != null)
      .findAny()
      .ifPresent(f -> LOG.warn("Link url overrides only work on service and component levels. Global filter: {}", f));
    for (Service service : profile.getServices()) {
      urlOverrides.putAll(getUrlOverrides(service.getName(), service.getFilters()));

      for (Component component : service.getComponents()) {
        Map<Pair<String, String>, String> componentUrlOverrides = getUrlOverrides(service.getName(), component.getFilters());
        Sets.SetView<Pair<String, String>> duplicateOverrides = Sets.difference(urlOverrides.keySet(), componentUrlOverrides.keySet());
        if (!duplicateOverrides.isEmpty()) {
          LOG.warn("Duplicate url overrides in quick links profile: {}", duplicateOverrides);
        }
        urlOverrides.putAll(componentUrlOverrides);
      }
    }
  }

  private Map<Pair<String, String>, String> getUrlOverrides(String serviceName, Collection<Filter> filters) {
    return filters.stream()
      .filter( f -> f instanceof LinkNameFilter && null != ((LinkNameFilter)f).getLinkUrl() )
      .map( f -> {
        LinkNameFilter lnf = (LinkNameFilter)f;
        return Pair.of(Pair.of(serviceName, lnf.getLinkName()), lnf.getLinkUrl());
      })
      .collect( toMap(Pair::getKey, Pair::getValue) );
  }

  @Override
  public java.util.Optional<String> getUrlOverride(@Nonnull String service, @Nonnull Link quickLink) {
    return Optional.ofNullable( urlOverrides.get(Pair.of(service, quickLink.getName())) );
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
    return globalRules.isVisible(quickLink).orElse(false);
  }

  private int size(@Nullable Collection<?> collection) {
    return null == collection ? 0 : collection.size();
  }

  private Optional<Boolean> evaluateComponentRules(@Nonnull String service, @Nonnull Link quickLink) {
    if (null == quickLink.getComponentName()) {
      return Optional.empty();
    }
    else {
      FilterEvaluator componentEvaluator = componentRules.get(ServiceComponent.of(service, quickLink.getComponentName()));
      return componentEvaluator != null ? componentEvaluator.isVisible(quickLink) : Optional.empty();
    }
  }

  private Optional<Boolean> evaluateServiceRules(@Nonnull String service, @Nonnull Link quickLink) {
    return serviceRules.containsKey(service) ?
        serviceRules.get(service).isVisible(quickLink) : Optional.empty();
  }

  static <T> List<T> nullToEmptyList(@Nullable List<T> items) {
    return items != null ? items : Collections.emptyList();
  }

  /**
   * Simple value class encapsulating a link name an component name.
   */
  static class ServiceComponent {
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
}

