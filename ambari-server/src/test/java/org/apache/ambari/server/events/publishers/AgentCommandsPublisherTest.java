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
package org.apache.ambari.server.events.publishers;

import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DATANODE;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DummyCluster;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DummyHostname1;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.HDFS;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.NAMENODE;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.SECONDARY_NAMENODE;
import static org.apache.ambari.server.controller.KerberosHelperImpl.SET_KEYTAB;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.ActionManagerTestHelper;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactory;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.agent.HeartbeatTestHelper;
import org.apache.ambari.server.audit.AuditLogger;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.serveraction.kerberos.KerberosIdentityDataFileWriter;
import org.apache.ambari.server.serveraction.kerberos.KerberosServerAction;
import org.apache.ambari.server.serveraction.kerberos.stageutils.KerberosKeytabController;
import org.apache.ambari.server.serveraction.kerberos.stageutils.ResolvedKerberosKeytab;
import org.apache.ambari.server.serveraction.kerberos.stageutils.ResolvedKerberosPrincipal;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.kerberos.KerberosIdentityDescriptor;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

import junit.framework.Assert;

public class AgentCommandsPublisherTest {
  private static final Logger LOG = LoggerFactory.getLogger(AgentCommandsPublisherTest.class);
  private Injector injector;

  @Inject
  private HostRoleCommandFactory hostRoleCommandFactory;

  @Inject
  private HeartbeatTestHelper heartbeatTestHelper;

  @Inject
  private ActionManagerTestHelper actionManagerTestHelper;

  @Inject
  private AuditLogger auditLogger;

  @Inject
  private OrmTestHelper helper;

  @Inject
  private AgentCommandsPublisher agentCommandsPublisher;

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private InMemoryDefaultTestModule module;

  @Before
  public void setup() throws Exception {
    module = HeartbeatTestHelper.getTestModule();
    injector = Guice.createInjector(module);
    injector.getInstance(GuiceJpaInitializer.class);
    injector.injectMembers(this);
    EasyMock.replay(auditLogger, injector.getInstance(STOMPUpdatePublisher.class));
  }

  @After
  public void teardown() throws Exception {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
    EasyMock.reset(auditLogger);
  }

  @Test
  public void testInjectKeytabApplicableHost() throws Exception {
    List<Map<String, String>> kcp;
    Map<String, String> properties;

    Cluster cluster = heartbeatTestHelper.getDummyCluster();
    Service hdfs = heartbeatTestHelper.addService(cluster, HDFS);
    hdfs.addServiceComponent(DATANODE);
    hdfs.addServiceComponent(NAMENODE);
    hdfs.addServiceComponent(SECONDARY_NAMENODE);

    kcp = testInjectKeytabSetKeytab("c6403.ambari.apache.org");
    Assert.assertNotNull(kcp);
    Assert.assertEquals(1, kcp.size());

    properties = kcp.get(0);
    Assert.assertNotNull(properties);
    Assert.assertEquals("c6403.ambari.apache.org", properties.get(KerberosIdentityDataFileWriter.HOSTNAME));
    Assert.assertEquals("dn/_HOST@_REALM", properties.get(KerberosIdentityDataFileWriter.PRINCIPAL));
    Assert.assertEquals("/etc/security/keytabs/dn.service.keytab", properties.get(KerberosIdentityDataFileWriter.KEYTAB_FILE_PATH));
    Assert.assertEquals("hdfs", properties.get(KerberosIdentityDataFileWriter.KEYTAB_FILE_OWNER_NAME));
    Assert.assertEquals("r", properties.get(KerberosIdentityDataFileWriter.KEYTAB_FILE_OWNER_ACCESS));
    Assert.assertEquals("hadoop", properties.get(KerberosIdentityDataFileWriter.KEYTAB_FILE_GROUP_NAME));
    Assert.assertEquals("", properties.get(KerberosIdentityDataFileWriter.KEYTAB_FILE_GROUP_ACCESS));

    Assert.assertEquals(Base64.encodeBase64String("hello".getBytes()), kcp.get(0).get(KerberosServerAction.KEYTAB_CONTENT_BASE64));


    kcp = testInjectKeytabRemoveKeytab("c6403.ambari.apache.org");

    Assert.assertNotNull(kcp);
    Assert.assertEquals(1, kcp.size());

    properties = kcp.get(0);
    Assert.assertNotNull(properties);
    Assert.assertEquals("c6403.ambari.apache.org", properties.get(KerberosIdentityDataFileWriter.HOSTNAME));
    Assert.assertEquals("dn/_HOST@_REALM", properties.get(KerberosIdentityDataFileWriter.PRINCIPAL));
    Assert.assertEquals("/etc/security/keytabs/dn.service.keytab", properties.get(KerberosIdentityDataFileWriter.KEYTAB_FILE_PATH));
    Assert.assertFalse(properties.containsKey(KerberosIdentityDataFileWriter.KEYTAB_FILE_OWNER_NAME));
    Assert.assertFalse(properties.containsKey(KerberosIdentityDataFileWriter.KEYTAB_FILE_OWNER_ACCESS));
    Assert.assertFalse(properties.containsKey(KerberosIdentityDataFileWriter.KEYTAB_FILE_GROUP_NAME));
    Assert.assertFalse(properties.containsKey(KerberosIdentityDataFileWriter.KEYTAB_FILE_GROUP_ACCESS));
    Assert.assertFalse(properties.containsKey(KerberosServerAction.KEYTAB_CONTENT_BASE64));
  }

  @Test
  public void testInjectKeytabNotApplicableHost() throws Exception {
    Cluster cluster = heartbeatTestHelper.getDummyCluster();
    Service hdfs = heartbeatTestHelper.addService(cluster, HDFS);
    hdfs.addServiceComponent(DATANODE);
    hdfs.addServiceComponent(NAMENODE);
    hdfs.addServiceComponent(SECONDARY_NAMENODE);

    List<Map<String, String>> kcp;
    kcp = testInjectKeytabSetKeytab("c6401.ambari.apache.org");
    Assert.assertNotNull(kcp);
    Assert.assertTrue(kcp.isEmpty());

    kcp = testInjectKeytabRemoveKeytab("c6401.ambari.apache.org");
    Assert.assertNotNull(kcp);
    Assert.assertTrue(kcp.isEmpty());
  }

  private List<Map<String, String>> testInjectKeytabSetKeytab(String targetHost) throws Exception {

    ExecutionCommand executionCommand = new ExecutionCommand();

    Map<String, String> hlp = new HashMap<>();
    hlp.put("custom_command", SET_KEYTAB);
    executionCommand.setHostLevelParams(hlp);

    Map<String, String> commandparams = new HashMap<>();
    commandparams.put(KerberosServerAction.AUTHENTICATED_USER_NAME, "admin");
    executionCommand.setCommandParams(commandparams);
    executionCommand.setClusterName(DummyCluster);

    final HostRoleCommand command = hostRoleCommandFactory.create(DummyHostname1,
        Role.DATANODE, null, null);

    ActionManager am = actionManagerTestHelper.getMockActionManager();
    expect(am.getTasks(EasyMock.<List<Long>>anyObject())).andReturn(
        new ArrayList<HostRoleCommand>() {{
          add(command);
        }});
    replay(am);

    Method injectKeytabMethod = agentCommandsPublisher.getClass().getDeclaredMethod("injectKeytab",
        ExecutionCommand.class, String.class, String.class, Map.class);
    injectKeytabMethod.setAccessible(true);
    commandparams.put(KerberosServerAction.DATA_DIRECTORY, createTestKeytabData(agentCommandsPublisher, false).getAbsolutePath());
    injectKeytabMethod.invoke(agentCommandsPublisher, executionCommand, "SET_KEYTAB", targetHost, null);

    return executionCommand.getKerberosCommandParams();
  }


  private List<Map<String, String>> testInjectKeytabRemoveKeytab(String targetHost) throws Exception {

    ExecutionCommand executionCommand = new ExecutionCommand();

    Map<String, String> hlp = new HashMap<>();
    hlp.put("custom_command", "REMOVE_KEYTAB");
    executionCommand.setHostLevelParams(hlp);

    Map<String, String> commandparams = new HashMap<>();
    commandparams.put(KerberosServerAction.AUTHENTICATED_USER_NAME, "admin");
    executionCommand.setCommandParams(commandparams);
    executionCommand.setClusterName(DummyCluster);

    final HostRoleCommand command = hostRoleCommandFactory.create(DummyHostname1,
        Role.DATANODE, null, null);

    ActionManager am = actionManagerTestHelper.getMockActionManager();
    expect(am.getTasks(EasyMock.<List<Long>>anyObject())).andReturn(
        new ArrayList<HostRoleCommand>() {{
          add(command);
        }});
    replay(am);

    Method injectKeytabMethod = agentCommandsPublisher.getClass().getDeclaredMethod("injectKeytab",
        ExecutionCommand.class, String.class, String.class, Map.class);
    injectKeytabMethod.setAccessible(true);
    commandparams.put(KerberosServerAction.DATA_DIRECTORY, createTestKeytabData(agentCommandsPublisher, true).getAbsolutePath());
    injectKeytabMethod.invoke(agentCommandsPublisher, executionCommand, "REMOVE_KEYTAB", targetHost, null);

    return executionCommand.getKerberosCommandParams();
  }


  private File createTestKeytabData(AgentCommandsPublisher agentCommandsPublisher, boolean removeKeytabs) throws Exception {
    KerberosKeytabController kerberosKeytabControllerMock = createMock(KerberosKeytabController.class);
    Map<String, Collection<String>> filter;

    if(removeKeytabs) {
      filter = null;

      Multimap<String, String> serviceMapping = ArrayListMultimap.create();
      serviceMapping.put("HDFS", "DATANODE");

      ResolvedKerberosPrincipal resolvedKerberosPrincipal = createMock(ResolvedKerberosPrincipal.class);
      expect(resolvedKerberosPrincipal.getHostName()).andReturn("c6403.ambari.apache.org");
      expect(resolvedKerberosPrincipal.getPrincipal()).andReturn("dn/_HOST@_REALM");
      expect(resolvedKerberosPrincipal.getServiceMapping()).andReturn(serviceMapping);
      replay(resolvedKerberosPrincipal);

      ResolvedKerberosKeytab resolvedKerberosKeytab = createMock(ResolvedKerberosKeytab.class);
      expect(resolvedKerberosKeytab.getPrincipals()).andReturn(Collections.singleton(resolvedKerberosPrincipal));
      replay(resolvedKerberosKeytab);

      expect(kerberosKeytabControllerMock.getKeytabByFile("/etc/security/keytabs/dn.service.keytab")).andReturn(resolvedKerberosKeytab).once();
    }
    else {
      filter = Collections.singletonMap("HDFS", Collections.singletonList("*"));
    }

    expect(kerberosKeytabControllerMock.adjustServiceComponentFilter(anyObject(), eq(false), anyObject())).andReturn(filter).once();
    expect(kerberosKeytabControllerMock.getFilteredKeytabs((Collection<KerberosIdentityDescriptor>) EasyMock.anyObject(), EasyMock.eq(null), EasyMock.eq(null))).andReturn(
        Sets.newHashSet(
            new ResolvedKerberosKeytab(
                "/etc/security/keytabs/dn.service.keytab",
                "hdfs",
                "r",
                "hadoop",
                "",
                Sets.newHashSet(new ResolvedKerberosPrincipal(
                        1L,
                        "c6403.ambari.apache.org",
                        "dn/_HOST@_REALM",
                        false,
                        "/tmp",
                        "HDFS",
                        "DATANODE",
                        "/etc/security/keytabs/dn.service.keytab"
                    )
                ),
                false,
                false
            )
        )
    ).once();

    expect(kerberosKeytabControllerMock.getServiceIdentities(EasyMock.anyString(), EasyMock.anyObject(), EasyMock.anyObject())).andReturn(Collections.emptySet()).anyTimes();

    replay(kerberosKeytabControllerMock);

    Field controllerField = agentCommandsPublisher.getClass().getDeclaredField("kerberosKeytabController");
    controllerField.setAccessible(true);
    controllerField.set(agentCommandsPublisher, kerberosKeytabControllerMock);

    File dataDirectory = temporaryFolder.newFolder();
    File hostDirectory = new File(dataDirectory, "c6403.ambari.apache.org");
    File keytabFile;
    if(hostDirectory.mkdirs()) {
      keytabFile = new File(hostDirectory, DigestUtils.sha256Hex("/etc/security/keytabs/dn.service.keytab"));
      FileWriter fw = new FileWriter(keytabFile);
      BufferedWriter bw = new BufferedWriter(fw);
      bw.write("hello");
      bw.close();
    } else {
      throw new Exception("Failed to create " + hostDirectory.getAbsolutePath());
    }

    return dataDirectory;
  }
}
