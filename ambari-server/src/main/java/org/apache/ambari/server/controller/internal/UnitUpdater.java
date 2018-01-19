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
package org.apache.ambari.server.controller.internal;

import static org.apache.commons.lang.StringUtils.isBlank;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.apache.ambari.server.topology.ClusterTopology;
import org.apache.ambari.server.topology.validators.UnitValidatedProperty;

/**
 * I append the stack defined unit to the original property value.
 * For example, "1024" would be updated to "1024m" if the stack unit is MB
 * Properties with any other unit than the stack defined unit are rejected.
 */
public class UnitUpdater implements BlueprintConfigurationProcessor.PropertyUpdater {
  private final String serviceName;
  private final String configType;

  public UnitUpdater(String serviceName, String configType) {
    this.serviceName = serviceName;
    this.configType = configType;
  }

  /**
   * @return property value with updated unit
   */
  @Override
  public String updateForClusterCreate(String propertyName,
                                       String origValue,
                                       Map<String, Map<String, String>> properties,
                                       ClusterTopology topology) {
      PropertyUnit stackUnit = PropertyUnit.of(topology.getBlueprint().getStack(), serviceName, configType, propertyName);
      PropertyValue value = PropertyValue.of(propertyName, origValue);
      if (value.hasUnit(stackUnit)) {
        return value.toString();
      } else if (!value.hasAnyUnit()) {
        return value.withUnit(stackUnit);
      } else { // should not happen because of prevalidation in UnitValidator
        throw new IllegalArgumentException("Property " + propertyName + "=" + origValue + " has an unsupported unit. Stack supported unit is: " + stackUnit + " or no unit");
      }
  }

  @Override
  public Collection<String> getRequiredHostGroups(String propertyName, String origValue, Map<String, Map<String, String>> properties, ClusterTopology topology) {
    return Collections.emptySet();
  }

  public static class PropertyUnit {
    private static final String DEFAULT_UNIT = "m";
    private final String unit;

    public static PropertyUnit of(Stack stack, UnitValidatedProperty property) {
      return PropertyUnit.of(stack, property.getServiceName(), property.getConfigType(), property.getPropertyName());
    }

    public static PropertyUnit of(Stack stack, String serviceName, String configType, String propertyName) {
      return new PropertyUnit(
        stackUnit(stack, serviceName, configType, propertyName)
          .map(PropertyUnit::toJvmUnit)
          .orElse(DEFAULT_UNIT));
    }

    private static Optional<String> stackUnit(Stack stack, String serviceName, String configType, String propertyName) {
      try {
        return Optional.ofNullable(
          stack.getConfigurationPropertiesWithMetadata(serviceName, configType)
            .get(propertyName)
            .getPropertyValueAttributes()
            .getUnit());
      } catch (NullPointerException e) {
        return Optional.empty();
      }
    }

    private static String toJvmUnit(String stackUnit) {
      switch (stackUnit.toLowerCase()) {
        case "mb" : return "m";
        case "gb" : return "g";
        case "b"  :
        case "bytes" : return "";
        default: throw new IllegalArgumentException("Unsupported stack unit: " + stackUnit);
      }
    }

    private PropertyUnit(String unit) {
      this.unit = unit;
    }

    @Override
    public String toString() {
      return unit;
    }
  }

  public static class PropertyValue {
    private final String value;

    public static PropertyValue of(String name, String value) {
      return new PropertyValue(normalized(name, value));
    }

    private static String normalized(String name, String value) {
      if (isBlank(value)) {
        throw new IllegalArgumentException("Missing property value " + name);
      }
      return value.trim().toLowerCase();
    }

    private PropertyValue(String value) {
      this.value = value;
    }

    public boolean hasUnit(PropertyUnit unit) {
      return value.endsWith(unit.toString());
    }

    public boolean hasAnyUnit() {
      return !Character.isDigit(value.charAt(value.length() -1));
    }

    public String withUnit(PropertyUnit unit) {
      return value + unit;
    }

    @Override
    public String toString() {
      return value;
    }
  }
}