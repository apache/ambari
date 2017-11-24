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

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.emptyMap;
import static org.easymock.EasyMock.expect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.controller.StackLevelConfigurationResponse;
import org.apache.ambari.server.controller.StackV2;
import org.apache.ambari.server.controller.internal.ConfigurationTopologyException;
import org.apache.ambari.server.state.ValueAttributesInfo;
import org.apache.ambari.server.topology.BlueprintV2;
import org.apache.ambari.server.topology.ClusterTopology;
import org.apache.ambari.server.topology.Configuration;
import org.apache.ambari.server.topology.HostGroupInfo;
import org.apache.ambari.server.topology.InvalidTopologyException;
import org.apache.ambari.server.topology.Service;
import org.easymock.EasyMockRule;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;


public class UnitValidatorTest extends EasyMockSupport {
  private static final String CONFIG_TYPE = "config-type";
  private static final String SERVICE = "service";
  @Rule public EasyMockRule mocks = new EasyMockRule(this);
  private Map<String, StackV2.ConfigProperty> stackConfigWithMetadata = new HashMap<>();
  private UnitValidator validator;
  private @Mock ClusterTopology clusterTopology;
  private @Mock Service testService;
  private @Mock BlueprintV2 blueprint;
  private @Mock StackV2 stack;

  @Test(expected = IllegalArgumentException.class)
  public void rejectsPropertyWithDifferentUnitThanStackUnit() throws Exception {
    stackUnitIs("property1", "MB");
    propertyToBeValidatedIs("property1", "12G");
    validate("property1");
  }

  @Test
  public void acceptsPropertyWithSameUnitThanStackUnit() throws Exception {
    stackUnitIs("property1", "MB");
    propertyToBeValidatedIs("property1", "12m");
    validate("property1");
  }

  @Test
  public void skipsValidatingIrrelevantProperty() throws Exception {
    stackUnitIs("property1", "MB");
    propertyToBeValidatedIs("property1", "12g");
    validate("property2");
  }

  @Before
  public void setUp() throws Exception {
    List<Service> serviceConfigs = new ArrayList<>();
    serviceConfigs.add(testService);
    expect(clusterTopology.getBlueprint()).andReturn(blueprint).anyTimes();
    expect(clusterTopology.getHostGroupInfo()).andReturn(new HashMap<String, HostGroupInfo>()).anyTimes();
    expect(clusterTopology.getServiceConfigs()).andReturn(serviceConfigs).anyTimes();
    expect(testService.getStackId()).andReturn("1");
    expect(blueprint.getStackById("1")).andReturn(stack).anyTimes();
    expect(stack.getConfigurationPropertiesWithMetadata(SERVICE, CONFIG_TYPE)).andReturn(stackConfigWithMetadata).anyTimes();
  }

  private void propertyToBeValidatedIs(String propertyName, String propertyValue) throws InvalidTopologyException, ConfigurationTopologyException {
    Map<String, Map<String, String>> propertiesToBeValidated = new HashMap<String, Map<String, String>>() {{
      put(CONFIG_TYPE, new HashMap<String, String>(){{
        put(propertyName, propertyValue);
      }});
    }};
    expect(testService.getConfiguration()).andReturn(new Configuration(propertiesToBeValidated, emptyMap())).anyTimes();
    replayAll();
  }

  private void validate(String propertyName) throws InvalidTopologyException {
    validator = new UnitValidator(newHashSet(new UnitValidatedProperty(SERVICE, CONFIG_TYPE, propertyName)));
    validator.validate(clusterTopology);
  }

  private void stackUnitIs(String name, String unit) {
    ValueAttributesInfo propertyValueAttributes = new ValueAttributesInfo();
    propertyValueAttributes.setUnit(unit);
    stackConfigWithMetadata.put(name, new StackV2.ConfigProperty(new StackLevelConfigurationResponse(
      name,
      "any",
      "any",
      "any",
      "any",
      true,
      Collections.emptySet(),
      Collections.emptyMap(),
      propertyValueAttributes,
      Collections.emptySet()
    )));
  }
}