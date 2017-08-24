/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.serveraction.upgrades;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.apache.ambari.server.actionmanager.ExecutionCommandWrapper;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.kerberos.KerberosDescriptor;
import org.easymock.Capture;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.google.inject.Injector;

import junit.framework.Assert;

public class FixAuthToLocalMappingActionTest {

  String authToLocalRulesOriginal = "RULE:[1:$1@$0](ambari-qa-c1@EXAMPLE.COM)s/.*/ambari-qa/\nRULE:[1:$1@$0](hbase-c1@EXAMPLE.COM)s/.*/hbase/\nRULE:[1:$1@$0](hdfs-c1@EXAMPLE.COM)s/.*/hdfs/\nRULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//\nRULE:[2:$1@$0](HTTP@EXAMPLE.COM)s/.*/hbase/\nRULE:[2:$1@$0](amshbase@EXAMPLE.COM)s/.*/ams/\nRULE:[2:$1@$0](dn@EXAMPLE.COM)s/.*/hdfs/\nRULE:[2:$1@$0](hbase@EXAMPLE.COM)s/.*/hbase/\nRULE:[2:$1@$0](hive@EXAMPLE.COM)s/.*/hive/\nRULE:[2:$1@$0](jhs@EXAMPLE.COM)s/.*/mapred/\nRULE:[2:$1@$0](nm@EXAMPLE.COM)s/.*/yarn/\nRULE:[2:$1@$0](nn@EXAMPLE.COM)s/.*/hdfs/\nRULE:[2:$1@$0](rm@EXAMPLE.COM)s/.*/yarn/\nRULE:[2:$1@$0](yarn@EXAMPLE.COM)s/.*/yarn/\nRULE:[2:$1@$0](zookeeper@EXAMPLE.COM)s/.*/ams/\nRULE:[2:$1@$0]([nd]n@.*)s/.*/hdfs/\nRULE:[2:$1@$0]([rn]m@.*)s/.*/yarn/\nRULE:[2:$1@$0](hm@.*)s/.*/hbase/\nRULE:[2:$1@$0](jhs@.*)s/.*/mapred/\nRULE:[2:$1@$0](rs@.*)s/.*/hbase/\nDEFAULT";
  String authToLocalRulesUpdated = "RULE:[1:$1@$0](ambari-qa-c1@EXAMPLE.COM)s/.*/ambari-qa/\nRULE:[1:$1@$0](hbase-c1@EXAMPLE.COM)s/.*/hbase/\nRULE:[1:$1@$0](hdfs-c1@EXAMPLE.COM)s/.*/hdfs/\nRULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//\nRULE:[2:$1@$0](amshbase@EXAMPLE.COM)s/.*/ams/\nRULE:[2:$1@$0](dn@EXAMPLE.COM)s/.*/hdfs/\nRULE:[2:$1@$0](hbase@EXAMPLE.COM)s/.*/hbase/\nRULE:[2:$1@$0](hive@EXAMPLE.COM)s/.*/hive/\nRULE:[2:$1@$0](jhs@EXAMPLE.COM)s/.*/mapred/\nRULE:[2:$1@$0](nm@EXAMPLE.COM)s/.*/yarn/\nRULE:[2:$1@$0](nn@EXAMPLE.COM)s/.*/hdfs/\nRULE:[2:$1@$0](rm@EXAMPLE.COM)s/.*/yarn/\nRULE:[2:$1@$0](yarn@EXAMPLE.COM)s/.*/yarn/\nRULE:[2:$1@$0]([nd]n@.*)s/.*/hdfs/\nRULE:[2:$1@$0]([rn]m@.*)s/.*/yarn/\nRULE:[2:$1@$0](hm@.*)s/.*/hbase/\nRULE:[2:$1@$0](jhs@.*)s/.*/mapred/\nRULE:[2:$1@$0](rs@.*)s/.*/hbase/\nDEFAULT";

  @Test
  public void testExecute() throws Exception {
    String clusterName = "c1";

    Injector injector = createNiceMock(Injector.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    KerberosHelper kerberosHelper = createNiceMock(KerberosHelper.class);
    KerberosDescriptor descriptor = createNiceMock(KerberosDescriptor.class);

    expect(kerberosHelper.getKerberosDescriptor(cluster, false )).andReturn(descriptor).anyTimes();
    Set<String> mappings = new HashSet<>();
    mappings.add("core-site/hadoop.security.auth_to_local");

    expect(descriptor.getAllAuthToLocalProperties()).andReturn(mappings);

    Map<String, String> commandParams = Maps.newHashMap();
    commandParams.put("clusterName", clusterName);

    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setCommandParams(commandParams);
    executionCommand.setClusterName(clusterName);

    HostRoleCommand hrc = createNiceMock(HostRoleCommand.class);
    expect(hrc.getExecutionCommandWrapper()).andReturn(new ExecutionCommandWrapper(executionCommand));

    Config hbaseEnv = createNiceMock(Config.class);
    expect(cluster.getDesiredConfigByType("hbase-env")).andReturn(hbaseEnv);
    expect(hbaseEnv.getProperties()).andReturn(Collections.singletonMap("hbase_user", "hbase"));

    Config amsEnv = createNiceMock(Config.class);
    expect(cluster.getDesiredConfigByType("ams-env")).andReturn(amsEnv);
    expect(amsEnv.getProperties()).andReturn(Collections.singletonMap("ambari_metrics_user", "ams"));

    Config coreSite = createNiceMock(Config.class);
    expect(cluster.getDesiredConfigByType("core-site")).andReturn(coreSite);

    Map<String, String> original = Maps.newHashMap();
    original.put("hadoop.security.auth_to_local", authToLocalRulesOriginal);
    expect(coreSite.getProperties()).andReturn(original);

    Capture<Map<String, String>> updated = Capture.newInstance();
    coreSite.setProperties(capture(updated));
    expectLastCall();
    coreSite.save();
    expectLastCall();

    expect(clusters.getCluster(clusterName)).andReturn(cluster).anyTimes();
    expect(injector.getInstance(Clusters.class)).andReturn(clusters).atLeastOnce();

    FixAuthToLocalMappingAction action = new FixAuthToLocalMappingAction();
    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hrc);

    Field clustersField = FixAuthToLocalMappingAction.class.getDeclaredField("clusters");
    clustersField.setAccessible(true);
    clustersField.set(action, clusters);

    Field kerberosHelperField = FixAuthToLocalMappingAction.class.getDeclaredField("kerberosHelper");
    kerberosHelperField.setAccessible(true);
    kerberosHelperField.set(action, kerberosHelper);

    replay(kerberosHelper, descriptor, injector, clusters, cluster, hrc, hbaseEnv, amsEnv, coreSite);

    ConcurrentMap<String, Object> emptyMap = Maps.newConcurrentMap();
    action.execute(emptyMap);

    verifyAll();

    Assert.assertEquals(authToLocalRulesUpdated, updated.getValue().get("hadoop.security.auth_to_local"));
  }
}
