/**
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
package org.apache.ambari.server.serveraction.upgrades;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ambari.server.actionmanager.ExecutionCommandWrapper;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Injector;

import junit.framework.Assert;

/**
 * Tests HiveEnvClasspathAction logic
 */
public class HiveEnvClasspathActionTest {
  private Injector m_injector;
  private Clusters m_clusters;
  private Field m_clusterField;

  @Before
  public void setup() throws Exception {
    m_injector = EasyMock.createMock(Injector.class);
    m_clusters = EasyMock.createMock(Clusters.class);
    Cluster cluster = EasyMock.createMock(Cluster.class);

    Map<String, String> mockProperties = new HashMap<String, String>() {{
      put("content", "      export HADOOP_USER_CLASSPATH_FIRST=true  #this prevents old metrics libs from mapreduce lib from bringing in old jar deps overriding HIVE_LIB\n" +
        "      if [ \"$SERVICE\" = \"cli\" ]; then\n" +
        "      if [ -z \"$DEBUG\" ]; then\n" +
        "      export HADOOP_OPTS=\"$HADOOP_OPTS -XX:NewRatio=12 -XX:MaxHeapFreeRatio=40 -XX:MinHeapFreeRatio=15 -XX:+UseNUMA -XX:+UseParallelGC -XX:-UseGCOverheadLimit\"\n" +
        "      else\n" +
        "      export HADOOP_OPTS=\"$HADOOP_OPTS -XX:NewRatio=12 -XX:MaxHeapFreeRatio=40 -XX:MinHeapFreeRatio=15 -XX:-UseGCOverheadLimit\"\n" +
        "      fi\n" +
        "      fi\n" +
        "\n" +
        "      # The heap size of the jvm stared by hive shell script can be controlled via:\n" +
        "\n" +
        "      if [ \"$SERVICE\" = \"metastore\" ]; then\n" +
        "      export HADOOP_HEAPSIZE={{hive_metastore_heapsize}} # Setting for HiveMetastore\n" +
        "      else\n" +
        "      export HADOOP_HEAPSIZE={{hive_heapsize}} # Setting for HiveServer2 and Client\n" +
        "      fi\n" +
        "\n" +
        "      export HADOOP_CLIENT_OPTS=\"$HADOOP_CLIENT_OPTS  -Xmx${HADOOP_HEAPSIZE}m\"\n" +
        "\n" +
        "      # Larger heap size may be required when running queries over large number of files or partitions.\n" +
        "      # By default hive shell scripts use a heap size of 256 (MB).  Larger heap size would also be\n" +
        "      # appropriate for hive server (hwi etc).\n" +
        "\n" +
        "\n" +
        "      # Set HADOOP_HOME to point to a specific hadoop install directory\n" +
        "      HADOOP_HOME=${HADOOP_HOME:-{{hadoop_home}}}\n" +
        "\n" +
        "      # Hive Configuration Directory can be controlled by:\n" +
        "      export HIVE_CONF_DIR=test\n" +
        "\n" +
        "      # Folder containing extra libraries required for hive compilation/execution can be controlled by:\n" +
        "      if [ \"${HIVE_AUX_JARS_PATH}\" != \"\" ]; then\n" +
        "      if [ -f \"${HIVE_AUX_JARS_PATH}\" ]; then\n" +
        "      export HIVE_AUX_JARS_PATH=${HIVE_AUX_JARS_PATH}\n" +
        "      elif [ -d \"/usr/hdp/current/hive-webhcat/share/hcatalog\" ]; then\n" +
        "      export HIVE_AUX_JARS_PATH=/usr/hdp/current/hive-webhcat/share/hcatalog/hive-hcatalog-core.jar\n" +
        "      fi\n" +
        "      elif [ -d \"/usr/hdp/current/hive-webhcat/share/hcatalog\" ]; then\n" +
        "      export HIVE_AUX_JARS_PATH=/usr/hdp/current/hive-webhcat/share/hcatalog/hive-hcatalog-core.jar\n" +
        "      fi\n" +
        "\n" +
        "      export METASTORE_PORT={{hive_metastore_port}}\n" +
        "\n" +
        "      {% if sqla_db_used or lib_dir_available %}\n" +
        "      export LD_LIBRARY_PATH=\"$LD_LIBRARY_PATH:{{jdbc_libs_dir}}\"\n" +
        "      export JAVA_LIBRARY_PATH=\"$JAVA_LIBRARY_PATH:{{jdbc_libs_dir}}\"\n" +
        "      {% endif %}");
    }};

    Config hiveEnv = EasyMock.createNiceMock(Config.class);
    expect(hiveEnv.getType()).andReturn("hive-env").anyTimes();
    expect(hiveEnv.getProperties()).andReturn(mockProperties).anyTimes();

    expect(cluster.getDesiredConfigByType("hive-env")).andReturn(hiveEnv).atLeastOnce();

    expect(m_clusters.getCluster((String) anyObject())).andReturn(cluster).anyTimes();
    expect(m_injector.getInstance(Clusters.class)).andReturn(m_clusters).atLeastOnce();

    replay(m_injector, m_clusters, cluster, hiveEnv);

    m_clusterField = HiveEnvClasspathAction.class.getDeclaredField("clusters");
    m_clusterField.setAccessible(true);
  }


  @Test
  public void testAction() throws Exception {
    Pattern regex = Pattern.compile("^\\s*export\\s(?<property>HIVE_HOME|HIVE_CONF_DIR)\\s*=\\s*(?<value>.*)$", Pattern.MULTILINE);
    Map<String, String> commandParams = new HashMap<String, String>();
    commandParams.put("clusterName", "c1");

    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setCommandParams(commandParams);
    executionCommand.setClusterName("c1");

    HostRoleCommand hrc = EasyMock.createMock(HostRoleCommand.class);
    expect(hrc.getRequestId()).andReturn(1L).anyTimes();
    expect(hrc.getStageId()).andReturn(2L).anyTimes();
    expect(hrc.getExecutionCommandWrapper()).andReturn(new ExecutionCommandWrapper(executionCommand)).anyTimes();
    replay(hrc);

    HiveEnvClasspathAction action = new HiveEnvClasspathAction();

    m_clusterField.set(action, m_clusters);

    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hrc);

    CommandReport report = action.execute(null);
    assertNotNull(report);

    Cluster c = m_clusters.getCluster("c1");
    Config config = c.getDesiredConfigByType("hive-env");
    Map<String, String> map = config.getProperties();

    Assert.assertTrue(map.containsKey("content"));
    String content = map.get("content");

    Matcher m = regex.matcher(content);

    int matches_found = 0;
    while (m.find()){
      if (m.group("property").equals("HIVE_HOME")){
        matches_found++;
        assertEquals("${HIVE_HOME:-{{hive_home_dir}}}", m.group("value"));
      } else if (m.group("property").equals("HIVE_CONF_DIR")) {
        matches_found++;
        assertEquals("test", m.group("value"));
      }

    }

    Assert.assertEquals(2, matches_found);

  }

}
