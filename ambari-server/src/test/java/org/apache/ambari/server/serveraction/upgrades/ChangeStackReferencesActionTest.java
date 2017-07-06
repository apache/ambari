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

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.apache.ambari.server.actionmanager.ExecutionCommandWrapper;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.DesiredConfig;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.google.inject.Injector;

public class ChangeStackReferencesActionTest {

  @Test
  public void testExecute() throws Exception {
    String clusterName = "c1";
    String configType = "cluster-env";

    Injector injector = createNiceMock(Injector.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);

    Map<String, String> commandParams = Maps.newHashMap();
    commandParams.put("clusterName", clusterName);

    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setCommandParams(commandParams);
    executionCommand.setClusterName(clusterName);

    HostRoleCommand hrc = createNiceMock(HostRoleCommand.class);
    expect(hrc.getExecutionCommandWrapper()).andReturn(new ExecutionCommandWrapper(executionCommand));

    // it's difficult to set up a real ConfigImpl, so use a mock
    Config clusterEnv = createNiceMock(Config.class);
    expect(clusterEnv.getType()).andReturn(configType).anyTimes();

    Map<String, String> originalProperties = Maps.newHashMap();
    originalProperties.put("mapreduce_tar_source", "/usr/iop/current/hadoop-client/mapreduce.tar.gz");
    originalProperties.put("pig_tar_destination_folder", "hdfs:///iop/apps/{{ stack_version }}/pig/");
    originalProperties.put("pig_tar_source", "/usr/iop/current/pig-client/pig.tar.gz");
    expect(clusterEnv.getProperties()).andReturn(originalProperties).anyTimes();

    // this is the crux of the test
    Map<String, String> updatedProperties = Maps.newHashMap();
    updatedProperties.put("mapreduce_tar_source", "/usr/hdp/current/hadoop-client/mapreduce.tar.gz");
    updatedProperties.put("pig_tar_destination_folder", "hdfs:///hdp/apps/{{ stack_version }}/pig/");
    updatedProperties.put("pig_tar_source", "/usr/hdp/current/pig-client/pig.tar.gz");
    clusterEnv.updateProperties(updatedProperties); expectLastCall();

    Map<String, DesiredConfig> desiredConfigs = Collections.singletonMap(configType, createNiceMock(DesiredConfig.class));
    expect(cluster.getDesiredConfigs()).andReturn(desiredConfigs);
    expect(cluster.getDesiredConfigByType(configType)).andReturn(clusterEnv).atLeastOnce();
    expect(clusters.getCluster(clusterName)).andReturn(cluster).anyTimes();
    expect(injector.getInstance(Clusters.class)).andReturn(clusters).atLeastOnce();

    ChangeStackReferencesAction underTest = new ChangeStackReferencesAction();
    underTest.setExecutionCommand(executionCommand);
    underTest.setHostRoleCommand(hrc);

    Field clustersField = ChangeStackReferencesAction.class.getDeclaredField("clusters");
    clustersField.setAccessible(true);
    clustersField.set(underTest, clusters);

    replay(injector, clusters, cluster, clusterEnv, hrc);

    ConcurrentMap<String, Object> emptyMap = Maps.newConcurrentMap();
    underTest.execute(emptyMap);

    verifyAll();
  }
}
