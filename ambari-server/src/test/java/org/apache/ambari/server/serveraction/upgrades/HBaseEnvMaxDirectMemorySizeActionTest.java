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
package org.apache.ambari.server.serveraction.upgrades;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ambari.server.actionmanager.ExecutionCommandWrapper;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.agent.stomp.AgentConfigsHolder;
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
public class HBaseEnvMaxDirectMemorySizeActionTest {
  private Injector injector;
  private Clusters clusters;
  private AgentConfigsHolder agentConfigsHolder;
  private Field m_clusterField;
  private Field agentConfigsHolderField;

  @Before
  public void setup() throws Exception {
    injector = EasyMock.createMock(Injector.class);
    clusters = EasyMock.createMock(Clusters.class);
    agentConfigsHolder = createMock(AgentConfigsHolder.class);
    Cluster cluster = EasyMock.createMock(Cluster.class);
    Config hbaseEnv = EasyMock.createNiceMock(Config.class);

    Map<String, String> mockProperties = new HashMap<String, String>() {{
      put("content","# Set environment variables here.\n" +
        "\n" +
        "# The java implementation to use. Java 1.6 required.\n" +
        "export JAVA_HOME={{java64_home}}\n" +
        "\n" +
        "# HBase Configuration directory\n" +
        "export HBASE_CONF_DIR=${HBASE_CONF_DIR:-{{hbase_conf_dir}}}\n" +
        "\n" +
        "# Extra Java CLASSPATH elements. Optional.\n" +
        "export HBASE_CLASSPATH=${HBASE_CLASSPATH}\n" +
        "\n" +
        "# The maximum amount of heap to use, in MB. Default is 1000.\n" +
        "# export HBASE_HEAPSIZE=1000\n" +
        "\n" +
        "# Extra Java runtime options.\n" +
        "# Below are what we set by default. May only work with SUN JVM.\n" +
        "# For more on why as well as other possible settings,\n" +
        "# see http://wiki.apache.org/hadoop/PerformanceTuning\n" +
        "export SERVER_GC_OPTS=\"-verbose:gc -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:{{log_dir}}/gc.log-`date +'%Y%m%d%H%M'`\"\n" +
        "# Uncomment below to enable java garbage collection logging.\n" +
        "# export HBASE_OPTS=\"$HBASE_OPTS -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:$HBASE_HOME/logs/gc-hbase.log -Djava.io.tmpdir={{java_io_tmpdir}}\"\n" +
        "\n" +
        "# Uncomment and adjust to enable JMX exporting\n" +
        "# See jmxremote.password and jmxremote.access in $JRE_HOME/lib/management to configure remote password access.\n" +
        "# More details at: http://java.sun.com/javase/6/docs/technotes/guides/management/agent.html\n" +
        "#\n" +
        "# export HBASE_JMX_BASE=\"-Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false\"\n" +
        "# If you want to configure BucketCache, specify '-XX: MaxDirectMemorySize=' with proper direct memory size\n" +
        "# export HBASE_THRIFT_OPTS=\"$HBASE_JMX_BASE -Dcom.sun.management.jmxremote.port=10103\"\n" +
        "# export HBASE_ZOOKEEPER_OPTS=\"$HBASE_JMX_BASE -Dcom.sun.management.jmxremote.port=10104\"\n" +
        "\n" +
        "# File naming hosts on which HRegionServers will run. $HBASE_HOME/conf/regionservers by default.\n" +
        "export HBASE_REGIONSERVERS=${HBASE_CONF_DIR}/regionservers\n" +
        "\n" +
        "# Extra ssh options. Empty by default.\n" +
        "# export HBASE_SSH_OPTS=\"-o ConnectTimeout=1 -o SendEnv=HBASE_CONF_DIR\"\n" +
        "\n" +
        "# Where log files are stored. $HBASE_HOME/logs by default.\n" +
        "export HBASE_LOG_DIR={{log_dir}}\n" +
        "\n" +
        "# A string representing this instance of hbase. $USER by default.\n" +
        "# export HBASE_IDENT_STRING=$USER\n" +
        "\n" +
        "# The scheduling priority for daemon processes. See 'man nice'.\n" +
        "# export HBASE_NICENESS=10\n" +
        "\n" +
        "# The directory where pid files are stored. /tmp by default.\n" +
        "export HBASE_PID_DIR={{pid_dir}}\n" +
        "\n" +
        "# Seconds to sleep between slave commands. Unset by default. This\n" +
        "# can be useful in large clusters, where, e.g., slave rsyncs can\n" +
        "# otherwise arrive faster than the master can service them.\n" +
        "# export HBASE_SLAVE_SLEEP=0.1\n" +
        "\n" +
        "# Tell HBase whether it should manage it's own instance of Zookeeper or not.\n" +
        "export HBASE_MANAGES_ZK=false\n" +
        "\n" +
        "{% if security_enabled %}\n" +
        "export HBASE_OPTS=\"$HBASE_OPTS -XX:+UseConcMarkSweepGC -XX:ErrorFile={{log_dir}}/hs_err_pid%p.log -Djava.security.auth.login.config={{client_jaas_config_file}} -Djava.io.tmpdir={{java_io_tmpdir}}\"\n" +
        "export HBASE_MASTER_OPTS=\"$HBASE_MASTER_OPTS -Xmx{{master_heapsize}} -Djava.security.auth.login.config={{master_jaas_config_file}}\"\n" +
        "export HBASE_REGIONSERVER_OPTS=\"$HBASE_REGIONSERVER_OPTS -Xmn{{regionserver_xmn_size}} -XX:CMSInitiatingOccupancyFraction=70  -Xms{{regionserver_heapsize}} -Xmx{{regionserver_heapsize}} -Djava.security.auth.login.config={{regionserver_jaas_config_file}}\"\n" +
        "{% else %}\n" +
        "export HBASE_OPTS=\"$HBASE_OPTS -XX:+UseConcMarkSweepGC -XX:ErrorFile={{log_dir}}/hs_err_pid%p.log -Djava.io.tmpdir={{java_io_tmpdir}}\"\n" +
        "export HBASE_MASTER_OPTS=\"$HBASE_MASTER_OPTS -Xmx{{master_heapsize}}\"\n" +
        "export HBASE_REGIONSERVER_OPTS=\"$HBASE_REGIONSERVER_OPTS -Xmn{{regionserver_xmn_size}} -XX:CMSInitiatingOccupancyFraction=70  -Xms{{regionserver_heapsize}} -Xmx{{regionserver_heapsize}}\"\n" +
        "{% endif %}");
    }};

    expect(hbaseEnv.getType()).andReturn("hbase-env").anyTimes();
    expect(hbaseEnv.getProperties()).andReturn(mockProperties).anyTimes();

    expect(cluster.getDesiredConfigByType("hbase-env")).andReturn(hbaseEnv).atLeastOnce();

    expect(clusters.getCluster((String) anyObject())).andReturn(cluster).anyTimes();
    expect(cluster.getClusterId()).andReturn(1L).atLeastOnce();
    expect(cluster.getHosts()).andReturn(Collections.emptyList()).atLeastOnce();
    agentConfigsHolder.updateData(eq(1L), eq(Collections.emptyList()));
    expectLastCall().atLeastOnce();

    expect(injector.getInstance(Clusters.class)).andReturn(clusters).atLeastOnce();

    replay(injector, clusters, cluster, hbaseEnv, agentConfigsHolder);

    m_clusterField = AbstractUpgradeServerAction.class.getDeclaredField("m_clusters");
    m_clusterField.setAccessible(true);
    agentConfigsHolderField = AbstractUpgradeServerAction.class.getDeclaredField("agentConfigsHolder");
    agentConfigsHolderField.setAccessible(true);
  }


  @Test
  public void testAction() throws Exception {
    Pattern regex = Pattern.compile("^.*\\s*(HBASE_MASTER_OPTS)\\s*=.*(XX:MaxDirectMemorySize).*$", Pattern.MULTILINE);
    Map<String, String> commandParams = new HashMap<>();
    commandParams.put("clusterName", "c1");

    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setCommandParams(commandParams);
    executionCommand.setClusterName("c1");

    HostRoleCommand hrc = EasyMock.createMock(HostRoleCommand.class);
    expect(hrc.getRequestId()).andReturn(1L).anyTimes();
    expect(hrc.getStageId()).andReturn(2L).anyTimes();
    expect(hrc.getExecutionCommandWrapper()).andReturn(new ExecutionCommandWrapper(executionCommand)).anyTimes();
    replay(hrc);

    HBaseEnvMaxDirectMemorySizeAction action = new HBaseEnvMaxDirectMemorySizeAction();

    m_clusterField.set(action, clusters);
    agentConfigsHolderField.set(action, agentConfigsHolder);

    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hrc);

    CommandReport report = action.execute(null);
    assertNotNull(report);

    Cluster c = clusters.getCluster("c1");
    Config config = c.getDesiredConfigByType("hbase-env");
    Map<String, String> map = config.getProperties();

    Assert.assertTrue(map.containsKey("content"));
    String content = map.get("content");
    Matcher m = regex.matcher(content);

    assertEquals(true, m.find());
  }

}
