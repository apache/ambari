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

import junit.framework.Assert;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.stack.OsFamily;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.easymock.EasyMock.createNiceMock;

public class KerberosServerActionTest {

  Map<String, String> commandParams = new HashMap<String, String>();
  File temporaryDirectory;
  private Injector injector;
  private KerberosServerAction action;

  @Before
  public void setUp() throws Exception {
    final Cluster cluster = mock(Cluster.class);

    final Clusters clusters = mock(Clusters.class);
    when(clusters.getCluster(anyString())).thenReturn(cluster);

    final ExecutionCommand mockExecutionCommand = mock(ExecutionCommand.class);
    final HostRoleCommand mockHostRoleCommand = mock(HostRoleCommand.class);

    injector = Guice.createInjector(new AbstractModule() {

      @Override
      protected void configure() {
        bind(KerberosServerAction.class).toInstance(new KerberosServerAction() {

          @Override
          protected CommandReport processIdentity(Map<String, String> identityRecord, String evaluatedPrincipal,
                                                  KerberosOperationHandler operationHandler,
                                                  Map<String, Object> requestSharedDataContext)
              throws AmbariException {
            Assert.assertNotNull(requestSharedDataContext);

            if (requestSharedDataContext.get("FAIL") != null) {
              return createCommandReport(1, HostRoleStatus.FAILED, "{}", "ERROR", "ERROR");
            } else {
              requestSharedDataContext.put(identityRecord.get(KerberosActionDataFile.PRINCIPAL), evaluatedPrincipal);
              return null;
            }
          }

          @Override
          public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext)
              throws AmbariException, InterruptedException {
            return processIdentities(requestSharedDataContext);
          }
        });

        bind(Clusters.class).toInstance(clusters);
        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
      }
    });

    temporaryDirectory = File.createTempFile("ambari_ut_", ".d");

    Assert.assertTrue(temporaryDirectory.delete());
    Assert.assertTrue(temporaryDirectory.mkdirs());

    // Create a data file
    KerberosActionDataFileBuilder builder =
        new KerberosActionDataFileBuilder(new File(temporaryDirectory, KerberosActionDataFile.DATA_FILE_NAME));
    for (int i = 0; i < 10; i++) {
      builder.addRecord("hostName", "serviceName" + i, "serviceComponentName" + i,
          "principal|_HOST|_REALM" + i, "principal_type", "principalConfiguration" + i, "keytabFilePath" + i,
          "keytabFileOwnerName" + i, "keytabFileOwnerAccess" + i,
          "keytabFileGroupName" + i, "keytabFileGroupAccess" + i,
          "keytabFileConfiguration" + i, "false");
    }
    builder.close();

    commandParams.put(KerberosServerAction.DATA_DIRECTORY, temporaryDirectory.getAbsolutePath());
    commandParams.put(KerberosServerAction.DEFAULT_REALM, "REALM.COM");
    commandParams.put(KerberosServerAction.KDC_TYPE, KDCType.MIT_KDC.toString());
    commandParams.put(KerberosServerAction.ADMINISTRATOR_CREDENTIAL,
        new KerberosCredential("principal", "password", "keytab")
            .encrypt(Integer.toHexString(cluster.hashCode()).getBytes()));

    when(mockExecutionCommand.getCommandParams()).thenReturn(commandParams);
    when(mockExecutionCommand.getClusterName()).thenReturn("c1");

    action = injector.getInstance(KerberosServerAction.class);

    action.setExecutionCommand(mockExecutionCommand);
    action.setHostRoleCommand(mockHostRoleCommand);
  }

  @After
  public void tearDown() throws Exception {
    if (temporaryDirectory != null) {
      new File(temporaryDirectory, KerberosActionDataFile.DATA_FILE_NAME).delete();
      temporaryDirectory.delete();
    }
  }

  @Test
  public void testGetCommandParameterValueStatic() throws Exception {
    Assert.assertNull(KerberosServerAction.getCommandParameterValue(commandParams, "nonexistingvalue"));
    Assert.assertEquals("REALM.COM", KerberosServerAction.getCommandParameterValue(commandParams, KerberosServerAction.DEFAULT_REALM));
  }

  @Test
  public void testGetDefaultRealmStatic() throws Exception {
    Assert.assertEquals("REALM.COM", KerberosServerAction.getDefaultRealm(commandParams));
  }

  @Test
  public void testGetKDCTypeStatic() throws Exception {
    Assert.assertEquals(KDCType.MIT_KDC, KerberosServerAction.getKDCType(commandParams));
  }

  @Test
  public void testGetDataDirectoryPathStatic() throws Exception {
    Assert.assertEquals(temporaryDirectory.getAbsolutePath(),
        KerberosServerAction.getDataDirectoryPath(commandParams));
  }

  @Test
  public void testSetPrincipalPasswordMapStatic() throws Exception {
    ConcurrentMap<String, Object> sharedMap = new ConcurrentHashMap<String, Object>();
    Map<String, String> dataMap = new HashMap<String, String>();

    KerberosServerAction.setPrincipalPasswordMap(sharedMap, dataMap);
    Assert.assertSame(dataMap, KerberosServerAction.getPrincipalPasswordMap(sharedMap));
  }

  @Test
  public void testGetPrincipalPasswordMapStatic() throws Exception {
    ConcurrentMap<String, Object> sharedMap = new ConcurrentHashMap<String, Object>();
    Assert.assertNotNull(KerberosServerAction.getPrincipalPasswordMap(sharedMap));
  }

  @Test
  public void testGetDataDirectoryPath() throws Exception {
    Assert.assertEquals(temporaryDirectory.getAbsolutePath(), action.getDataDirectoryPath());
  }

  @Test
  public void testProcessIdentitiesSuccess() throws Exception {
    ConcurrentMap<String, Object> sharedMap = new ConcurrentHashMap<String, Object>();
    CommandReport report = action.processIdentities(sharedMap);
    Assert.assertNotNull(report);
    Assert.assertEquals(HostRoleStatus.COMPLETED.toString(), report.getStatus());

    for (Map.Entry<String, Object> entry : sharedMap.entrySet()) {
      Assert.assertEquals(entry.getValue(),
          entry.getKey().replace("_HOST", "hostName").replace("_REALM", "REALM.COM"));
    }
  }

  @Test
  public void testProcessIdentitiesFail() throws Exception {
    ConcurrentMap<String, Object> sharedMap = new ConcurrentHashMap<String, Object>();
    sharedMap.put("FAIL", "true");

    CommandReport report = action.processIdentities(sharedMap);
    Assert.assertNotNull(report);
    Assert.assertEquals(HostRoleStatus.FAILED.toString(), report.getStatus());
  }

  @Test
  public void testGetAdministrativeCredentials() throws AmbariException {
    KerberosCredential credentials = action.getAdministratorCredential(commandParams);
    Assert.assertNotNull(credentials);
    Assert.assertEquals("principal", credentials.getPrincipal());
    Assert.assertEquals("password", credentials.getPassword());
    Assert.assertEquals("keytab", credentials.getKeytab());
  }
}