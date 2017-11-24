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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.apache.ambari.server.topology.BlueprintV2;
import org.apache.ambari.server.topology.ClusterTopology;
import org.apache.ambari.server.topology.ComponentV2;
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

public class HiveServiceValidatorTest extends EasyMockSupport {

  @Rule
  public EasyMockRule mocks = new EasyMockRule(this);

  @Mock
  private ClusterTopology clusterTopologyMock;

  @Mock
  private BlueprintV2 blueprintMock;

  @Mock
  private Service hiveMock;

  @Mock
  private ComponentV2 mysqlComponent;

  @Mock
  private Configuration configurationMock;

  @TestSubject
  private HiveServiceValidator hiveServiceValidator = new HiveServiceValidator();

  @Before
  public void setUp() throws Exception {


  }

  @After
  public void tearDown() throws Exception {
    resetAll();
  }

  @Test
  public void testShouldValidationPassWhenHiveServiceIsNotInBlueprint() throws Exception {

    // GIVEN
    EasyMock.expect(clusterTopologyMock.getBlueprint()).andReturn(blueprintMock);
    EasyMock.expect(blueprintMock.getServicesByType("HIVE")).andReturn(Collections.emptySet());
    replayAll();

    // WHEN
    hiveServiceValidator.validate(clusterTopologyMock);

    // THEN

  }

  @Test(expected = InvalidTopologyException.class)
  public void testShouldValidationFailWhenHiveServiceIsMissingConfigType() throws Exception {

    // GIVEN
    Collection<Service> blueprintServices = Arrays.asList(hiveMock);
    EasyMock.expect(clusterTopologyMock.getBlueprint()).andReturn(blueprintMock);
    EasyMock.expect(blueprintMock.getServicesByType("HIVE")).andReturn(blueprintServices);
    EasyMock.expect(hiveMock.getConfiguration()).andReturn(configurationMock);
    EasyMock.expect(configurationMock.getAllConfigTypes()).andReturn(Collections.emptySet());

    replayAll();

    // WHEN
    hiveServiceValidator.validate(clusterTopologyMock);

    // THEN

  }

  @Test
  public void testShouldValidationPassWhenCustomHiveDatabaseSettingsProvided() throws Exception {

    // GIVEN
    Collection<Service> blueprintServices = Arrays.asList(hiveMock);
    Collection<String> configTypes = Arrays.asList("hive-env", "core-site", "hadoop-env");
    EasyMock.expect(clusterTopologyMock.getBlueprint()).andReturn(blueprintMock);
    EasyMock.expect(blueprintMock.getServicesByType("HIVE")).andReturn(blueprintServices);
    EasyMock.expect(hiveMock.getConfiguration()).andReturn(configurationMock);
    EasyMock.expect(configurationMock.getAllConfigTypes()).andReturn(configTypes);

    EasyMock.expect(configurationMock.getPropertyValue("hive-env", "hive_database")).andReturn("PSQL");
    replayAll();

    // WHEN
    hiveServiceValidator.validate(clusterTopologyMock);

    // THEN

  }

  @Test(expected = InvalidTopologyException.class)
  public void testShouldValidationFailWhenDefaultsAreUsedAndMysqlComponentIsMissing() throws Exception {
    // GIVEN
    Collection<Service> blueprintServices = Arrays.asList(hiveMock);
    Collection<String> configTypes = Arrays.asList("hive-env", "core-site", "hadoop-env");
    EasyMock.expect(clusterTopologyMock.getBlueprint()).andReturn(blueprintMock).atLeastOnce();
    EasyMock.expect(blueprintMock.getServicesByType("HIVE")).andReturn(blueprintServices).atLeastOnce();
    EasyMock.expect(blueprintMock.getComponentsByType(hiveMock, "MYSQL_SERVER")).andReturn(Collections.emptyList()).atLeastOnce();
    EasyMock.expect(hiveMock.getConfiguration()).andReturn(configurationMock);
    EasyMock.expect(configurationMock.getAllConfigTypes()).andReturn(configTypes);

    EasyMock.expect(configurationMock.getPropertyValue("hive-env", "hive_database")).andReturn("New MySQL Database");
    replayAll();


    // WHEN
    hiveServiceValidator.validate(clusterTopologyMock);

    // THEN

  }

  @Test
  public void testShouldValidationPassWhenDefaultsAreUsedAndMsqlComponentIsListed() throws Exception {
    // GIVEN
    Collection<Service> blueprintServices = Arrays.asList(hiveMock);
    Collection<ComponentV2> hiveComponents = Arrays.asList(mysqlComponent);
    Collection<String> configTypes = Arrays.asList("hive-env", "core-site", "hadoop-env");
    EasyMock.expect(clusterTopologyMock.getBlueprint()).andReturn(blueprintMock).atLeastOnce();
    EasyMock.expect(blueprintMock.getServicesByType("HIVE")).andReturn(blueprintServices).atLeastOnce();
    EasyMock.expect(blueprintMock.getComponentsByType(hiveMock, "MYSQL_SERVER")).andReturn(hiveComponents).atLeastOnce();
    EasyMock.expect(hiveMock.getConfiguration()).andReturn(configurationMock).atLeastOnce();
    EasyMock.expect(configurationMock.getAllConfigTypes()).andReturn(configTypes).atLeastOnce();

    EasyMock.expect(configurationMock.getPropertyValue("hive-env", "hive_database")).andReturn("New MySQL Database").atLeastOnce();
    replayAll();

    // WHEN
    hiveServiceValidator.validate(clusterTopologyMock);

    // THEN

  }
}