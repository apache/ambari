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
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

import org.apache.ambari.server.actionmanager.ExecutionCommandWrapper;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.orm.dao.ArtifactDAO;
import org.apache.ambari.server.orm.entities.ArtifactEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.kerberos.KerberosDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosDescriptorFactory;
import org.apache.ambari.server.state.kerberos.KerberosDescriptorUpdateHelper;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Tests OozieConfigCalculation logic
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(KerberosDescriptorUpdateHelper.class)
public class UpgradeUserKerberosDescriptorTest {
  private Clusters clusters;
  private Cluster cluster;
  private AmbariMetaInfo ambariMetaInfo;
  private KerberosDescriptorFactory kerberosDescriptorFactory;
  private ArtifactDAO artifactDAO;

  private TreeMap<String, Field> fields = new TreeMap<>();

  @Before
  public void setup() throws Exception {
    clusters = EasyMock.createMock(Clusters.class);
    cluster = EasyMock.createMock(Cluster.class);
    kerberosDescriptorFactory = EasyMock.createNiceMock(KerberosDescriptorFactory.class);
    ambariMetaInfo = EasyMock.createMock(AmbariMetaInfo.class);
    artifactDAO = EasyMock.createNiceMock(ArtifactDAO.class);

    expect(clusters.getCluster((String) anyObject())).andReturn(cluster).anyTimes();
    expect(cluster.getClusterId()).andReturn(1l).atLeastOnce();
    replay(clusters, cluster);

    prepareFields();

  }

  @Test
  public void testUpgrade() throws Exception {

    Map<String, String> commandParams = new HashMap<String, String>();
    commandParams.put("clusterName", "c1");
    commandParams.put("upgrade_direction", "UPGRADE");
    commandParams.put("original_stack", "HDP-2.4");
    commandParams.put("target_stack", "HDP-2.5");

    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setCommandParams(commandParams);
    executionCommand.setClusterName("c1");

    HostRoleCommand hrc = EasyMock.createMock(HostRoleCommand.class);
    expect(hrc.getRequestId()).andReturn(1L).anyTimes();
    expect(hrc.getStageId()).andReturn(2L).anyTimes();
    expect(hrc.getExecutionCommandWrapper()).andReturn(new ExecutionCommandWrapper(executionCommand)).anyTimes();
    replay(hrc);

    UpgradeUserKerberosDescriptor action = new UpgradeUserKerberosDescriptor();
    injectFields(action);

    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hrc);

    ArtifactEntity entity = EasyMock.createNiceMock(ArtifactEntity.class);
    expect(entity.getArtifactData()).andReturn(null).anyTimes();
    expect(entity.getForeignKeys()).andReturn(null).anyTimes();
    expect(artifactDAO.findByNameAndForeignKeys(anyString(), (TreeMap<String, String>) anyObject())).andReturn(entity).atLeastOnce();

    KerberosDescriptor userDescriptor = EasyMock.createMock(KerberosDescriptor.class);
    KerberosDescriptor newDescriptor = EasyMock.createMock(KerberosDescriptor.class);
    KerberosDescriptor previousDescriptor = EasyMock.createMock(KerberosDescriptor.class);
    KerberosDescriptor updatedKerberosDescriptor = EasyMock.createMock(KerberosDescriptor.class);

    PowerMockito.mockStatic(KerberosDescriptorUpdateHelper.class);
    PowerMockito.when(KerberosDescriptorUpdateHelper.updateUserKerberosDescriptor(previousDescriptor, newDescriptor, userDescriptor)).thenReturn(updatedKerberosDescriptor);
    expect(kerberosDescriptorFactory.createInstance((Map)null)).andReturn(userDescriptor).atLeastOnce();
    expect(ambariMetaInfo.getKerberosDescriptor("HDP","2.5")).andReturn(newDescriptor).atLeastOnce();
    expect(ambariMetaInfo.getKerberosDescriptor("HDP","2.4")).andReturn(previousDescriptor).atLeastOnce();


    expect(updatedKerberosDescriptor.toMap()).andReturn(null).once();


    expect(artifactDAO.merge(entity)).andReturn(entity).once();
    Capture<ArtifactEntity> createCapture = Capture.newInstance();
    artifactDAO.create(capture(createCapture));
    EasyMock.expectLastCall().once();

    replay(artifactDAO, entity, ambariMetaInfo, kerberosDescriptorFactory, updatedKerberosDescriptor);

    action.execute(null);

    verify(artifactDAO, updatedKerberosDescriptor);
    assertEquals(createCapture.getValue().getArtifactName(), "kerberos_descriptor_backup");
  }

  @Test
  public void testDowngrade() throws Exception {

    Map<String, String> commandParams = new HashMap<String, String>();
    commandParams.put("clusterName", "c1");
    commandParams.put("upgrade_direction", "DOWNGRADE");
    commandParams.put("original_stack", "HDP-2.4");
    commandParams.put("target_stack", "HDP-2.5");

    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setCommandParams(commandParams);
    executionCommand.setClusterName("c1");

    HostRoleCommand hrc = EasyMock.createMock(HostRoleCommand.class);
    expect(hrc.getRequestId()).andReturn(1L).anyTimes();
    expect(hrc.getStageId()).andReturn(2L).anyTimes();
    expect(hrc.getExecutionCommandWrapper()).andReturn(new ExecutionCommandWrapper(executionCommand)).anyTimes();
    replay(hrc);

    UpgradeUserKerberosDescriptor action = new UpgradeUserKerberosDescriptor();
    injectFields(action);

    action.setExecutionCommand(executionCommand);
    action.setHostRoleCommand(hrc);

    ArtifactEntity entity = EasyMock.createNiceMock(ArtifactEntity.class);
    expect(entity.getArtifactData()).andReturn(null).anyTimes();
    expect(entity.getForeignKeys()).andReturn(null).anyTimes();
    expect(artifactDAO.findByNameAndForeignKeys(anyString(), (TreeMap<String, String>) anyObject())).andReturn(entity).atLeastOnce();

    KerberosDescriptor userDescriptor = EasyMock.createMock(KerberosDescriptor.class);

    expect(kerberosDescriptorFactory.createInstance((Map)null)).andReturn(userDescriptor).atLeastOnce();

    Capture<ArtifactEntity> createCapture = Capture.newInstance();
    artifactDAO.create(capture(createCapture));
    EasyMock.expectLastCall().once();

    artifactDAO.remove(entity);
    EasyMock.expectLastCall().atLeastOnce();

    replay(artifactDAO, entity, ambariMetaInfo, kerberosDescriptorFactory);

    action.execute(null);

    verify(artifactDAO);
    assertEquals(createCapture.getValue().getArtifactName(), "kerberos_descriptor");
  }

  private void prepareFields() throws NoSuchFieldException {
    String[] fieldsNames = {"artifactDAO","clusters","ambariMetaInfo","kerberosDescriptorFactory"};
    for(String fieldName : fieldsNames)
    {
      Field clustersField = UpgradeUserKerberosDescriptor.class.getDeclaredField(fieldName);
      clustersField.setAccessible(true);
      fields.put(fieldName, clustersField);
    }
  }
  private void injectFields(UpgradeUserKerberosDescriptor action) throws IllegalAccessException {
    fields.get("artifactDAO").set(action, artifactDAO);
    fields.get("clusters").set(action, clusters);
    fields.get("ambariMetaInfo").set(action, ambariMetaInfo);
    fields.get("kerberosDescriptorFactory").set(action, kerberosDescriptorFactory);
  }
}
