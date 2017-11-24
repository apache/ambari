package org.apache.ambari.server.topology.validators;

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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.ambari.server.controller.StackV2;
import org.apache.ambari.server.topology.BlueprintV2;
import org.apache.ambari.server.topology.ClusterTopology;
import org.apache.ambari.server.topology.Configuration;
import org.apache.ambari.server.topology.InvalidTopologyException;
import org.apache.ambari.server.topology.Service;
import org.easymock.EasyMock;
import org.easymock.EasyMockRule;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.easymock.TestSubject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

public class ClusterConfigTypeValidatorTest extends EasyMockSupport {

  @Rule
  public EasyMockRule mocks = new EasyMockRule(this);

  @Mock
  private Configuration clusterConfigurationMock;

  @Mock
  private BlueprintV2 blueprintMock;

  @Mock
  private StackV2 stackMock;

  @Mock
  private Service yarnMock;

  @Mock
  private Service hdfsMock;

  @Mock
  private ClusterTopology clusterTopologyMock;

  private Set<String> clusterRequestConfigTypes;

  @TestSubject
  private ClusterConfigTypeValidator clusterConfigTypeValidator = new ClusterConfigTypeValidator();

  @Before
  public void before() {
    EasyMock.expect(clusterTopologyMock.getConfiguration()).andReturn(clusterConfigurationMock).anyTimes();

    EasyMock.expect(clusterTopologyMock.getBlueprint()).andReturn(blueprintMock).anyTimes();
    EasyMock.expect(blueprintMock.getStackById("1")).andReturn(stackMock).anyTimes();
    EasyMock.expect(yarnMock.getStackId()).andReturn("1").anyTimes();
    EasyMock.expect(yarnMock.getType()).andReturn("YARN").anyTimes();
    EasyMock.expect(hdfsMock.getStackId()).andReturn("1").anyTimes();
    EasyMock.expect(hdfsMock.getType()).andReturn("HDFS").anyTimes();
  }

  @After
  public void after() {
    resetAll();
  }


  @Test
  public void testShouldValidationPassWhenNoConfigTypesSpecifiedInCCTemplate() throws Exception {
    // GIVEN
    clusterRequestConfigTypes = Collections.emptySet();
    EasyMock.expect(clusterConfigurationMock.getAllConfigTypes()).andReturn(clusterRequestConfigTypes).anyTimes();

    replayAll();

    // WHEN
    clusterConfigTypeValidator.validate(clusterTopologyMock);

    // THEN
  }


  @Test
  public void testShouldValidationPassWhenAllConfigTypesAreValid() throws Exception {
    // GIVEN
    // all the config types are OK
    clusterRequestConfigTypes = new HashSet<>(Arrays.asList("core-site", "yarn-site"));
    EasyMock.expect(clusterConfigurationMock.getAllConfigTypes()).andReturn(clusterRequestConfigTypes).anyTimes();

    EasyMock.expect(blueprintMock.getAllServices()).andReturn(new HashSet<>(Arrays.asList(yarnMock, hdfsMock)));

    EasyMock.expect(stackMock.getConfigurationTypes("HDFS")).andReturn(Arrays.asList("core-site"));
    EasyMock.expect(stackMock.getConfigurationTypes("YARN")).andReturn(Arrays.asList("yarn-site"));

    replayAll();

    // WHEN
    clusterConfigTypeValidator.validate(clusterTopologyMock);

    // THEN
    // Exception is thrown

  }

  @Test(expected = InvalidTopologyException.class)
  public void testShouldValidationFailWhenInvalidConfigGroupsSpecifiedInCCTemplate() throws Exception {
    // GIVEN

    // the config type that is not present in the stack definition for services
    clusterRequestConfigTypes = new HashSet<>(Arrays.asList("oozie-site"));
    EasyMock.expect(clusterConfigurationMock.getAllConfigTypes()).andReturn(clusterRequestConfigTypes).anyTimes();

    EasyMock.expect(blueprintMock.getAllServices()).andReturn(new HashSet<>(Arrays.asList(yarnMock, hdfsMock)));
    EasyMock.expect(stackMock.getConfigurationTypes("HDFS")).andReturn(Arrays.asList("core-site"));
    EasyMock.expect(stackMock.getConfigurationTypes("YARN")).andReturn(Arrays.asList("yarn-site"));

    replayAll();

    // WHEN
    clusterConfigTypeValidator.validate(clusterTopologyMock);

    // THEN
    // Exception is thrown
  }


  @Test(expected = InvalidTopologyException.class)
  public void testShouldValidationFailWhenThereIsAnInvalidConfigGroupProvided() throws Exception {
    // GIVEN
    // oozzie-type is wrong!
    clusterRequestConfigTypes = new HashSet<>(Arrays.asList("core-site", "yarn-site", "oozie-site"));
    EasyMock.expect(clusterConfigurationMock.getAllConfigTypes()).andReturn(clusterRequestConfigTypes).anyTimes();

    EasyMock.expect(blueprintMock.getAllServices()).andReturn(new HashSet<>(Arrays.asList(yarnMock, hdfsMock)));

    EasyMock.expect(stackMock.getConfigurationTypes("HDFS")).andReturn(Arrays.asList("core-site"));
    EasyMock.expect(stackMock.getConfigurationTypes("YARN")).andReturn(Arrays.asList("yarn-site"));

    replayAll();

    // WHEN
    clusterConfigTypeValidator.validate(clusterTopologyMock);

    // THEN
    // Exception is thrown
  }

  @Test
  public void testEquals() throws Exception {
    EqualsVerifier.forClass(ClusterConfigTypeValidator.class).usingGetClass().verify();
  }
}
