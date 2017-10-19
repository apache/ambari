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
package org.apache.ambari.server.topology.validators;

import static org.apache.ambari.server.controller.internal.UnitUpdater.PropertyValue;

import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.StackV2;
import org.apache.ambari.server.controller.internal.UnitUpdater.PropertyUnit;
import org.apache.ambari.server.topology.ClusterTopology;
import org.apache.ambari.server.topology.InvalidTopologyException;
import org.apache.ambari.server.topology.TopologyValidator;

/**
 * I validate the unit of properties by checking if it matches to the stack defined unit.
 * Properties with different unit than the stack defined unit are rejected.
 */
public class UnitValidator implements TopologyValidator {
  private final Set<UnitValidatedProperty> relevantProps;

  public UnitValidator(Set<UnitValidatedProperty> propertiesToBeValidated) {
    this.relevantProps = propertiesToBeValidated;
  }

  @Override
  public void validate(ClusterTopology topology) throws InvalidTopologyException {
    topology.getServiceConfigs().forEach(service -> {
      String stackId = service.getStackId();
      validateConfig(service.getConfiguration().getFullProperties(), topology.getBlueprint().getStackById(stackId));
      topology.getHostGroupInfo().values().forEach(hostGroup -> {
        String stackId2 = service.getStackId();
        validateConfig(hostGroup.getConfiguration().getFullProperties(), topology.getBlueprint().getStackById(stackId2));
      });
    });
  }

  private void validateConfig(Map<String, Map<String, String>> configuration, StackV2 stack) {
    configuration.entrySet().forEach(each ->
      validateConfigType(each.getKey(), each.getValue(), stack)
    );
  }

  private void validateConfigType(String configType, Map<String, String> config, StackV2 stack) {
    config.keySet().forEach(propertyName -> validateProperty(configType, config, propertyName, stack));
  }

  private void validateProperty(String configType, Map<String, String> config, String propertyName, StackV2 stack) {
    relevantProps.stream()
      .filter(each -> each.hasTypeAndName(configType, propertyName))
      .findFirst()
      .ifPresent(relevantProperty -> checkUnit(config, stack, relevantProperty));
  }

  private void checkUnit(Map<String, String> configToBeValidated, StackV2 stack, UnitValidatedProperty prop) {
    PropertyUnit stackUnit = PropertyUnit.of(stack, prop);
    PropertyValue value = PropertyValue.of(prop.getPropertyName(), configToBeValidated.get(prop.getPropertyName()));
    if (value.hasAnyUnit() && !value.hasUnit(stackUnit)) {
      throw new IllegalArgumentException("Property " + prop.getPropertyName() + "=" + value + " has an unsupported unit. Stack supported unit is: " + stackUnit + " or no unit");
    }
  }

}
