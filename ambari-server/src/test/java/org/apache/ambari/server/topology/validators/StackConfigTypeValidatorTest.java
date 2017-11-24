/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import static org.easymock.EasyMock.expect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.ambari.server.controller.StackV2;
import org.apache.ambari.server.topology.BlueprintV2;
import org.apache.ambari.server.topology.ClusterTopology;
import org.apache.ambari.server.topology.Configuration;
import org.apache.ambari.server.topology.InvalidTopologyException;
import org.apache.ambari.server.topology.Service;
import org.easymock.EasyMockRule;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.easymock.TestSubject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class StackConfigTypeValidatorTest extends EasyMockSupport {

  @Rule
  public EasyMockRule mocks = new EasyMockRule(this);

  @Mock
  private Configuration clusterConfigurationMock;

  @Mock
  private Configuration stackConfigurationMock;

  @Mock
  private BlueprintV2 blueprintMock;

  @Mock
  private Service testService;

  @Mock
  private StackV2 stackMock;

  @Mock
  private ClusterTopology clusterTopologyMock;

  private Set<String> clusterRequestConfigTypes;

  @TestSubject
  private StackConfigTypeValidator stackConfigTypeValidator = new StackConfigTypeValidator();

  @Before
  public void before() {
    List<Service> serviceConfigs = new ArrayList<>();
    serviceConfigs.add(testService);
    expect(testService.getConfiguration()).andReturn(clusterConfigurationMock).anyTimes();
    expect(clusterTopologyMock.getBlueprint()).andReturn(blueprintMock).anyTimes();
    expect(clusterTopologyMock.getServiceConfigs()).andReturn(serviceConfigs).anyTimes();
    expect(testService.getStack()).andReturn(stackMock).anyTimes();
  }

  @After
  public void after() {
    resetAll();
  }


  @Test(expected = InvalidTopologyException.class)
  public void testShouldValidationFailWhenUnknownConfigTypeComesIn() throws Exception {
    // GIVEN
    expect(stackMock.getConfiguration()).andReturn(stackConfigurationMock);
    expect(stackConfigurationMock.getAllConfigTypes()).andReturn(new HashSet<>(Arrays.asList("core-site", "yarn-site")));
    expect(clusterConfigurationMock.getAllConfigTypes()).andReturn(new HashSet<>(Arrays.asList("invalid-site")));

    replayAll();

    // WHEN
    stackConfigTypeValidator.validate(clusterTopologyMock);

    // THEN
    // exception is thrown

  }

  @Test
  public void testShouldValidationPassifNoConfigTypesomeIn() throws Exception {
    // GIVEN
    expect(stackMock.getConfiguration()).andReturn(stackConfigurationMock);
    expect(stackConfigurationMock.getAllConfigTypes()).andReturn(new HashSet<>(Arrays.asList("core-site", "yarn-site")));
    expect(clusterConfigurationMock.getAllConfigTypes()).andReturn(new HashSet<>(Collections.emptyList()));

    replayAll();

    // WHEN
    stackConfigTypeValidator.validate(clusterTopologyMock);

    // THEN
    // no exception is thrown

  }

  @Test(expected = InvalidTopologyException.class)
  public void testShouldValidationFailIfMultipleInvalidConfigTypesComeIn() throws Exception {
    // GIVEN
    expect(stackMock.getConfiguration()).andReturn(stackConfigurationMock);
    expect(stackConfigurationMock.getAllConfigTypes()).andReturn(new HashSet<>(Arrays.asList("core-site", "yarn-site")));
    expect(clusterConfigurationMock.getAllConfigTypes()).andReturn(new HashSet<>(Arrays.asList("invalid-site-1", "invalid-default")));

    replayAll();

    // WHEN
    stackConfigTypeValidator.validate(clusterTopologyMock);

    // THEN
    // no exception is thrown

  }
}