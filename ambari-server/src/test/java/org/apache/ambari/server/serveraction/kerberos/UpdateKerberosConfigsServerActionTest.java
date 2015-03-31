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
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ConfigHelper;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.easymock.EasyMock.*;

public class UpdateKerberosConfigsServerActionTest extends EasyMockSupport{

  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();
  String dataDir;
  private Injector injector;
  private UpdateKerberosConfigsServerAction action;


  @Before
  public void setup() throws Exception {
    final AmbariManagementController controller = createNiceMock(AmbariManagementController.class);
    final Clusters clusters = createNiceMock(Clusters.class);
    final Cluster cluster = createNiceMock(Cluster.class);

    expect(controller.getClusters()).andReturn(clusters).once();

    expect(clusters.getCluster(anyObject(String.class))).andReturn(cluster).once();

    injector = Guice.createInjector(new AbstractModule() {

      @Override
      protected void configure() {
        bind(AmbariManagementController.class).toInstance(controller);
        bind(ConfigHelper.class).toInstance(createNiceMock(ConfigHelper.class));
      }
    });

    dataDir = testFolder.getRoot().getAbsolutePath();

    setupConfigDat();

    action = injector.getInstance(UpdateKerberosConfigsServerAction.class);
  }

  private void setupConfigDat() throws Exception {
    File configFile = new File(dataDir, KerberosConfigDataFileWriter.DATA_FILE_NAME);
    KerberosConfigDataFileWriterFactory factory = injector.getInstance(KerberosConfigDataFileWriterFactory.class);
    KerberosConfigDataFileWriter writer = factory.createKerberosConfigDataFileWriter(configFile);
    writer.addRecord("hdfs-site", "hadoop.security.authentication", "kerberos", KerberosConfigDataFileWriter.OPERATION_TYPE_SET);
    writer.addRecord("hdfs-site", "remove.me", null, KerberosConfigDataFileWriter.OPERATION_TYPE_REMOVE);
    writer.close();
  }

  @Test
  public void testUpdateConfig() throws Exception {
    Map<String, String> commandParams = new HashMap<String, String>();
    commandParams.put(KerberosServerAction.DATA_DIRECTORY, dataDir);

    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setCommandParams(commandParams);

    ConfigHelper configHelper = injector.getInstance(ConfigHelper.class);
    configHelper.updateConfigType(anyObject(Cluster.class), anyObject(AmbariManagementController.class),
        anyObject(String.class), anyObject(Map.class), anyObject(Collection.class), anyObject(String.class), anyObject(String.class));
    expectLastCall().atLeastOnce();

    replayAll();

    action.setExecutionCommand(executionCommand);
    action.execute(null);

    verifyAll();
  }

  @Test
  public void testUpdateConfigMissingDataDirectory() throws Exception {
    ExecutionCommand executionCommand = new ExecutionCommand();
    Map<String, String> commandParams = new HashMap<String, String>();
    executionCommand.setCommandParams(commandParams);

    replayAll();

    action.setExecutionCommand(executionCommand);
    action.execute(null);

    verifyAll();
  }

  @Test
  public void testUpdateConfigEmptyDataDirectory() throws Exception {
    ExecutionCommand executionCommand = new ExecutionCommand();
    Map<String, String> commandParams = new HashMap<String, String>();
    commandParams.put(KerberosServerAction.DATA_DIRECTORY, testFolder.newFolder().getAbsolutePath());
    executionCommand.setCommandParams(commandParams);

    replayAll();

    action.setExecutionCommand(executionCommand);
    action.execute(null);

    verifyAll();
  }
}
