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
import static org.easymock.EasyMock.replay;

import java.util.Arrays;
import java.util.stream.Stream;

import org.apache.ambari.server.topology.ClusterTopology;
import org.apache.ambari.server.topology.Configuration;
import org.apache.ambari.server.topology.InvalidTopologyException;
import org.easymock.EasyMockRule;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.easymock.TestSubject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;

public class HiveServiceValidatorTest extends EasyMockSupport {

  @Rule
  public EasyMockRule mocks = new EasyMockRule(this);

  @Mock
  private ClusterTopology topology;

  @Mock
  private Configuration configuration;

  @TestSubject
  private final TopologyValidator hiveServiceValidator = new HiveServiceValidator();

  @Before
  public void setUp() throws Exception {
    expect(topology.getConfiguration()).andReturn(configuration).anyTimes();
  }

  @After
  public void tearDown() throws Exception {
    resetAll();
  }

  @Test
  public void allowsTopologyWithoutHive() throws Exception {
    // GIVEN
    noHiveInTopology();
    noHiveConfig();

    // WHEN
    hiveServiceValidator.validate(topology);

    // THEN
    // OK
  }

  @Test
  public void allowsExistingDatabaseWithoutMysqlComponent() throws Exception {
    // GIVEN
    topologyHasMysql(false);
    hiveDatabaseIs("Existing MySQL");

    // WHEN
    hiveServiceValidator.validate(topology);

    // THEN
    // OK
  }

  @Test
  public void allowsNewMysqlDatabaseWithMysqlComponent() throws Exception {
    // GIVEN
    topologyHasMysql(true);
    hiveDatabaseIs("New MySQL Database");

    // WHEN
    hiveServiceValidator.validate(topology);

    // THEN
    // OK
  }

  @Test(expected = InvalidTopologyException.class) // THEN
  public void rejectsHiveWithoutConfig() throws Exception {
    // GIVEN
    topologyHasMysql(true);
    noHiveConfig();

    // WHEN
    hiveServiceValidator.validate(topology);
  }

  @Test(expected = InvalidTopologyException.class) // THEN
  public void rejectsNewMysqlDatabaseWithoutMysqlComponent() throws Exception {
    // GIVEN
    topologyHasMysql(false);
    hiveDatabaseIs("New MySQL Database");

    // WHEN
    hiveServiceValidator.validate(topology);
  }

  private void noHiveConfig() {
    expect(configuration.getAllConfigTypes()).andReturn(Arrays.asList("core-site", "hadoop-env")).anyTimes();
    replay(configuration);
  }

  private void hiveDatabaseIs(String database) {
    expect(configuration.getAllConfigTypes()).andReturn(Arrays.asList("hive-env", "hive-site", "core-site", "hadoop-env"));
    expect(configuration.getPropertyValue("hive-env", "hive_database")).andReturn(database).anyTimes();
    replay(configuration);
  }

  private void topologyHasMysql(boolean hasMysql) {
    ImmutableSet.Builder<String> components = ImmutableSet.<String>builder().add("HIVE_CLIENT", "HIVE_METASTORE", "HIVE_SERVER");
    if (hasMysql) {
      components.add("MYSQL_SERVER");
    }
    expect(topology.getComponents()).andReturn(Stream.empty()).anyTimes(); // FIXME
    expect(topology.getServices()).andReturn(ImmutableSet.of("HDFS", "YARN", "HIVE")).anyTimes();
    replay(topology);
  }

  private void noHiveInTopology() {
    expect(topology.getServices()).andReturn(ImmutableSet.of("HDFS", "YARN")).anyTimes();
    replay(topology);
  }

}
