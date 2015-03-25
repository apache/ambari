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

package org.apache.ambari.server.serveraction.kerberos;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ConfigurationRequest;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.commons.codec.digest.DigestUtils;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import static org.easymock.EasyMock.*;

public class UpdateKerberosConfigsServerActionTest {

  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();
  String dataDir;
  private Injector injector;
  private UpdateKerberosConfigsServerAction action;

  private final AmbariManagementController controller = EasyMock.createNiceMock(AmbariManagementController.class);
  private final ConfigHelper configHelper = createNiceMock(ConfigHelper.class);
  private final Clusters clusters = EasyMock.createNiceMock(Clusters.class);
  private final Cluster cluster = EasyMock.createNiceMock(Cluster.class);

  @Before
  public void setup() throws Exception {
    setupIndexDat();
    setupConfigDat();

    expect(controller.getClusters()).andReturn(clusters).once();
    replay(controller);

    configHelper.updateConfigType(anyObject(Cluster.class), anyObject(AmbariManagementController.class),
        anyObject(String.class), anyObject(Map.class), anyObject(Collection.class), anyObject(String.class), anyObject(String.class));
    expectLastCall().atLeastOnce();
    replay(configHelper);

    replay(cluster);

    expect(clusters.getCluster(anyObject(String.class))).andReturn(cluster).once();
    replay(clusters);

    injector = Guice.createInjector(new AbstractModule() {

      @Override
      protected void configure() {
        bind(AmbariManagementController.class).toInstance(controller);
        bind(ConfigHelper.class).toInstance(configHelper);
      }
    });
    action = injector.getInstance(UpdateKerberosConfigsServerAction.class);
  }

  private void setupIndexDat() throws Exception {

    File indexFile;
    KerberosActionDataFileBuilder kerberosActionDataFileBuilder = null;

    dataDir = testFolder.getRoot().getAbsolutePath();

    indexFile = new File(dataDir, KerberosActionDataFile.DATA_FILE_NAME);
    kerberosActionDataFileBuilder = new KerberosActionDataFileBuilder(indexFile);

    kerberosActionDataFileBuilder.addRecord("c6403.ambari.apache.org", "HDFS", "DATANODE",
      "dn/_HOST@_REALM", "service", "hdfs-site/dfs.namenode.kerberos.principal",
      "/etc/security/keytabs/dn.service.keytab",
      "hdfs", "r", "hadoop", "", "hdfs-site/dfs.namenode.keytab.file", "false");

    kerberosActionDataFileBuilder.close();
    File hostDirectory = new File(dataDir, "c6403.ambari.apache.org");

    // Ensure the host directory exists...
    if (hostDirectory.exists() || hostDirectory.mkdirs()) {
      File file = new File(hostDirectory, DigestUtils.sha1Hex("/etc/security/keytabs/dn.service.keytab"));
      if (!file.exists()) {
        file.createNewFile();
      }

      FileWriter fw = new FileWriter(file.getAbsoluteFile());
      BufferedWriter bw = new BufferedWriter(fw);
      bw.write("hello");
      bw.close();
    }
  }

  private void setupConfigDat() throws Exception {
    File configFile = new File(dataDir, KerberosConfigDataFile.DATA_FILE_NAME);
    FileWriter fw = new FileWriter(configFile.getAbsoluteFile());
    BufferedWriter bw = new BufferedWriter(fw);
    bw.write("config,key,value\n");
    bw.write("hdfs-site,hadoop.security.authentication,kerberos");
    bw.close();
  }

  @Test
  public void testUpdateConfig() throws Exception {
    ExecutionCommand executionCommand = new ExecutionCommand();
    Map<String, String> commandParams = new HashMap<String, String>();
    commandParams.put(KerberosServerAction.DATA_DIRECTORY, dataDir);
    executionCommand.setCommandParams(commandParams);

    action.setExecutionCommand(executionCommand);

    ConcurrentMap<String, Object> requestSharedDataContext = null;

    action.execute(requestSharedDataContext);

    verify(controller, clusters, cluster, configHelper);
  }

  @Test
  public void testUpdateConfigMissingDataDirectory() throws Exception {
    ExecutionCommand executionCommand = new ExecutionCommand();
    Map<String, String> commandParams = new HashMap<String, String>();
    executionCommand.setCommandParams(commandParams);

    action.setExecutionCommand(executionCommand);
    action.execute(null);
  }

  @Test
  public void testUpdateConfigEmptyDataDirectory() throws Exception {
    ExecutionCommand executionCommand = new ExecutionCommand();
    Map<String, String> commandParams = new HashMap<String, String>();
    commandParams.put(KerberosServerAction.DATA_DIRECTORY, testFolder.newFolder().getAbsolutePath());
    executionCommand.setCommandParams(commandParams);

    action.setExecutionCommand(executionCommand);
    action.execute(null);
  }
}
