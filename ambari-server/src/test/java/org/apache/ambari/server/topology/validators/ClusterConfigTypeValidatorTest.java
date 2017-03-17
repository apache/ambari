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
import java.util.Map;

import org.apache.ambari.server.controller.internal.Stack;
import org.apache.ambari.server.topology.Blueprint;
import org.apache.ambari.server.topology.ClusterTopology;
import org.apache.ambari.server.topology.Configuration;
import org.apache.ambari.server.topology.InvalidTopologyException;
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
  private Map<String, Map<String, String>> clusterConfigurationMapMock;

  @Mock
  private Blueprint blueprintMock;

  @Mock
  private Stack stackMock;

  @Mock
  private ClusterTopology clusterTopologyMock;

  @TestSubject
  private ClusterConfigTypeValidator clusterConfigTypeValidator = new ClusterConfigTypeValidator();

  @Before
  public void before() {
    EasyMock.expect(clusterTopologyMock.getConfiguration()).andReturn(clusterConfigurationMock).anyTimes();
    EasyMock.expect(clusterConfigurationMock.getProperties()).andReturn(clusterConfigurationMapMock).anyTimes();

    EasyMock.expect(clusterTopologyMock.getBlueprint()).andReturn(blueprintMock).anyTimes();
    EasyMock.expect(blueprintMock.getStack()).andReturn(stackMock).anyTimes();
  }

  @After
  public void after() {
    resetAll();
  }

  @Test(expected = InvalidTopologyException.class)
  public void testShouldValidationFailWhenInvalidConfigGroupSpecifiedInCCTemplate() throws Exception {
    // given
    EasyMock.expect(clusterConfigurationMapMock.keySet()).andReturn(new HashSet<String>(Arrays.asList("oozie-site")));
    EasyMock.expect(blueprintMock.getServices()).andReturn(new HashSet<String>(Arrays.asList("YARN", "HDFS")));

    EasyMock.expect(stackMock.getConfigurationTypes("HDFS")).andReturn(Arrays.asList("core-site"));
    EasyMock.expect(stackMock.getConfigurationTypes("YARN")).andReturn(Arrays.asList("yarn-site"));

    replayAll();

    //when
    clusterConfigTypeValidator.validate(clusterTopologyMock);

    // then
    // Exception is thrown
  }

  @Test
  public void testShouldValidationPassIfNoConfigTypesSpecifiedInCCTemplate() throws Exception {
    //GIVEN
    EasyMock.expect(clusterConfigurationMapMock.keySet()).andReturn(Collections.<String>emptySet());
    replayAll();

    //WHEN
    clusterConfigTypeValidator.validate(clusterTopologyMock);

  }

  @Test
  public void testEquals() throws Exception {
    EqualsVerifier.forClass(ClusterConfigTypeValidator.class).usingGetClass().verify();
  }
}
