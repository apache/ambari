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

package org.apache.ambari.server.controller;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.RequestFactory;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.actionmanager.StageFactory;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.internal.RequestStageContainer;
import org.apache.ambari.server.metadata.RoleCommandOrder;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.security.SecurityHelper;
import org.apache.ambari.server.serveraction.kerberos.KerberosCredential;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.SecurityState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.cluster.ClusterFactory;
import org.apache.ambari.server.state.cluster.ClustersImpl;
import org.apache.ambari.server.state.host.HostFactory;
import org.apache.ambari.server.state.kerberos.KerberosComponentDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosIdentityDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosKeytabDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosPrincipalDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosServiceDescriptor;
import org.apache.ambari.server.state.stack.OsFamily;
import org.easymock.EasyMockSupport;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.EntityManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.easymock.EasyMock.*;

public class KerberosHelperTest extends EasyMockSupport {

  private static Injector injector;

  @Before
  public void setUp() throws Exception {
    injector = Guice.createInjector(new AbstractModule() {

      @Override
      protected void configure() {
        bind(EntityManager.class).toInstance(createNiceMock(EntityManager.class));
        bind(DBAccessor.class).toInstance(createNiceMock(DBAccessor.class));
        bind(ClusterFactory.class).toInstance(createNiceMock(ClusterFactory.class));
        bind(HostFactory.class).toInstance(createNiceMock(HostFactory.class));
        bind(SecurityHelper.class).toInstance(createNiceMock(SecurityHelper.class));
        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        bind(AmbariCustomCommandExecutionHelper.class).toInstance(createNiceMock(AmbariCustomCommandExecutionHelper.class));
        bind(MaintenanceStateHelper.class).toInstance(createNiceMock(MaintenanceStateHelper.class));
        bind(AmbariManagementController.class).toInstance(createNiceMock(AmbariManagementController.class));
        bind(AmbariMetaInfo.class).toInstance(createNiceMock(AmbariMetaInfo.class));
        bind(ActionManager.class).toInstance(createNiceMock(ActionManager.class));
        bind(RequestFactory.class).toInstance(createNiceMock(RequestFactory.class));
        bind(StageFactory.class).toInstance(createNiceMock(StageFactory.class));
        bind(Clusters.class).toInstance(createNiceMock(ClustersImpl.class));
        bind(ConfigHelper.class).toInstance(createNiceMock(ConfigHelper.class));
      }
    });
  }

  @After
  public void tearDown() throws Exception {

  }

  @Test(expected = AmbariException.class)
  public void testMissingClusterEnv() throws Exception {
    KerberosHelper kerberosHelper = injector.getInstance(KerberosHelper.class);

    Cluster cluster = createNiceMock(Cluster.class);
    KerberosDescriptor kerberosDescriptor = createNiceMock(KerberosDescriptor.class);
    RequestStageContainer requestStageContainer = createNiceMock(RequestStageContainer.class);

    replayAll();
    kerberosHelper.toggleKerberos(cluster, kerberosDescriptor, requestStageContainer);
    verifyAll();
  }

  @Test(expected = AmbariException.class)
  public void testMissingKrb5Conf() throws Exception {
    KerberosHelper kerberosHelper = injector.getInstance(KerberosHelper.class);

    final Map<String, String> clusterEnvProperties = createNiceMock(Map.class);
    expect(clusterEnvProperties.get("security_enabled")).andReturn("true").once();
    expect(clusterEnvProperties.get("kerberos_domain")).andReturn("FOOBAR.COM").once();

    final Config clusterEnvConfig = createNiceMock(Config.class);
    expect(clusterEnvConfig.getProperties()).andReturn(clusterEnvProperties).once();

    final Cluster cluster = createNiceMock(Cluster.class);
    expect(cluster.getDesiredConfigByType("cluster-env")).andReturn(clusterEnvConfig).once();

    final KerberosDescriptor kerberosDescriptor = createNiceMock(KerberosDescriptor.class);

    replayAll();
    kerberosHelper.toggleKerberos(cluster, kerberosDescriptor, null);
    verifyAll();
  }

  @Test
  public void testEnableKerberos() throws Exception {
    KerberosHelper kerberosHelper = injector.getInstance(KerberosHelper.class);

    final ServiceComponentHost sch1 = createNiceMock(ServiceComponentHost.class);
    expect(sch1.getServiceName()).andReturn("SERVICE1").once();
    expect(sch1.getServiceComponentName()).andReturn("COMPONENT1").once();
    expect(sch1.getSecurityState()).andReturn(SecurityState.UNSECURED).anyTimes();

    final ServiceComponentHost sch2 = createNiceMock(ServiceComponentHost.class);
    expect(sch2.getServiceName()).andReturn("SERVICE2").once();
    expect(sch2.getServiceComponentName()).andReturn("COMPONENT2").once();
    expect(sch2.getSecurityState()).andReturn(SecurityState.UNSECURED).anyTimes();

    final Host host = createNiceMock(Host.class);
    expect(host.getHostName()).andReturn("host1").once();
    expect(host.getState()).andReturn(HostState.HEALTHY).once();

    final Service service1 = createStrictMock(Service.class);
    expect(service1.getName()).andReturn("SERVICE1").anyTimes();
    expect(service1.getServiceComponents())
        .andReturn(Collections.<String, ServiceComponent>emptyMap())
        .once();
    service1.setSecurityState(SecurityState.SECURED_KERBEROS);
    expectLastCall().once();

    final Service service2 = createStrictMock(Service.class);
    expect(service2.getName()).andReturn("SERVICE2").anyTimes();
    expect(service2.getServiceComponents())
        .andReturn(Collections.<String, ServiceComponent>emptyMap())
        .once();
    service2.setSecurityState(SecurityState.SECURED_KERBEROS);
    expectLastCall().once();

    final Map<String, String> clusterEnvProperties = createNiceMock(Map.class);
    expect(clusterEnvProperties.get("security_enabled")).andReturn("true").once();
    expect(clusterEnvProperties.get("kerberos_domain")).andReturn("FOOBAR.COM").once();

    final Config clusterEnvConfig = createNiceMock(Config.class);
    expect(clusterEnvConfig.getProperties()).andReturn(clusterEnvProperties).once();

    final Map<String, String> krb5ConfProperties = createNiceMock(Map.class);
    expect(krb5ConfProperties.get("kdc_type")).andReturn("mit-kdc").once();

    final Config krb5ConfConfig = createNiceMock(Config.class);
    expect(krb5ConfConfig.getProperties()).andReturn(krb5ConfProperties).once();

    final MaintenanceStateHelper maintenanceStateHelper = injector.getInstance(MaintenanceStateHelper.class);
    expect(maintenanceStateHelper.getEffectiveState(anyObject(ServiceComponentHost.class)))
        .andReturn(MaintenanceState.OFF).anyTimes();

    final Cluster cluster = createNiceMock(Cluster.class);
    expect(cluster.getDesiredConfigByType("cluster-env")).andReturn(clusterEnvConfig).once();
    expect(cluster.getDesiredConfigByType("krb5-conf")).andReturn(krb5ConfConfig).once();
    expect(cluster.getClusterName()).andReturn("c1").anyTimes();
    expect(cluster.getServices())
        .andReturn(new HashMap<String, Service>() {
          {
            put("SERVICE1", service1);
            put("SERVICE2", service2);
          }
        })
        .anyTimes();
    expect(cluster.getServiceComponentHosts("host1"))
        .andReturn(new ArrayList<ServiceComponentHost>() {
          {
            add(sch1);
            add(sch2);
          }
        })
        .once();
    expect(cluster.getCurrentStackVersion())
        .andReturn(new StackId("HDP", "2.2"))
        .anyTimes();
    expect(cluster.getSessionAttributes()).andReturn(new HashMap<String, Object>(){{
      put("kerberos_admin/" + KerberosCredential.KEY_NAME_PRINCIPAL, "principal");
      put("kerberos_admin/" + KerberosCredential.KEY_NAME_PASSWORD, "password");
      put("kerberos_admin/" + KerberosCredential.KEY_NAME_KEYTAB, "keytab");
    }}).anyTimes();

    final Clusters clusters = injector.getInstance(Clusters.class);
    expect(clusters.getHostsForCluster("c1"))
        .andReturn(new HashMap<String, Host>() {
          {
            put("host1", host);
          }
        })
        .once();

    final AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    expect(ambariManagementController.findConfigurationTagsWithOverrides(cluster, "host1"))
        .andReturn(Collections.<String, Map<String, String>>emptyMap())
        .once();
    expect(ambariManagementController.getRoleCommandOrder(cluster))
        .andReturn(createNiceMock(RoleCommandOrder.class))
        .once();

    final ConfigHelper configHelper = injector.getInstance(ConfigHelper.class);
    expect(configHelper.getEffectiveConfigProperties(anyObject(Cluster.class), anyObject(Map.class)))
        .andReturn(new HashMap<String, Map<String, String>>() {
          {
            put("cluster-env", new HashMap<String, String>() {{
              put("kerberos_domain", "FOOBAR.COM");
            }});
          }
        })
        .once();
    expect(configHelper.getEffectiveConfigAttributes(anyObject(Cluster.class), anyObject(Map.class)))
        .andReturn(Collections.<String, Map<String, Map<String, String>>>emptyMap())
        .once();

    final KerberosPrincipalDescriptor principalDescriptor1 = createNiceMock(KerberosPrincipalDescriptor.class);
    expect(principalDescriptor1.getValue()).andReturn("component1/_HOST@${realm}").once();
    expect(principalDescriptor1.getConfiguration()).andReturn("service1-site/component1.kerberos.principal").once();

    final KerberosPrincipalDescriptor principalDescriptor2 = createNiceMock(KerberosPrincipalDescriptor.class);
    expect(principalDescriptor2.getValue()).andReturn("component2/${host}@${realm}").once();
    expect(principalDescriptor2.getConfiguration()).andReturn("service2-site/component2.kerberos.principal").once();

    final KerberosKeytabDescriptor keytabDescriptor1 = createNiceMock(KerberosKeytabDescriptor.class);
    expect(keytabDescriptor1.getFile()).andReturn("${keytab_dir}/service1.keytab").once();
    expect(keytabDescriptor1.getOwnerName()).andReturn("service1").once();
    expect(keytabDescriptor1.getOwnerAccess()).andReturn("rw").once();
    expect(keytabDescriptor1.getGroupName()).andReturn("hadoop").once();
    expect(keytabDescriptor1.getGroupAccess()).andReturn("").once();
    expect(keytabDescriptor1.getConfiguration()).andReturn("service1-site/component1.keytab.file").once();

    final KerberosKeytabDescriptor keytabDescriptor2 = createNiceMock(KerberosKeytabDescriptor.class);
    expect(keytabDescriptor2.getFile()).andReturn("${keytab_dir}/service2.keytab").once();
    expect(keytabDescriptor2.getOwnerName()).andReturn("service2").once();
    expect(keytabDescriptor2.getOwnerAccess()).andReturn("rw").once();
    expect(keytabDescriptor2.getGroupName()).andReturn("hadoop").once();
    expect(keytabDescriptor2.getGroupAccess()).andReturn("").once();
    expect(keytabDescriptor2.getConfiguration()).andReturn("service2-site/component2.keytab.file").once();

    final KerberosIdentityDescriptor identityDescriptor1 = createNiceMock(KerberosIdentityDescriptor.class);
    expect(identityDescriptor1.getPrincipalDescriptor()).andReturn(principalDescriptor1).once();
    expect(identityDescriptor1.getKeytabDescriptor()).andReturn(keytabDescriptor1).once();

    final KerberosIdentityDescriptor identityDescriptor2 = createNiceMock(KerberosIdentityDescriptor.class);
    expect(identityDescriptor2.getPrincipalDescriptor()).andReturn(principalDescriptor2).once();
    expect(identityDescriptor2.getKeytabDescriptor()).andReturn(keytabDescriptor2).once();

    final KerberosComponentDescriptor componentDescriptor1 = createNiceMock(KerberosComponentDescriptor.class);
    expect(componentDescriptor1.getIdentities(true)).
        andReturn(new ArrayList<KerberosIdentityDescriptor>() {{
          add(identityDescriptor1);
        }}).once();

    final KerberosComponentDescriptor componentDescriptor2 = createNiceMock(KerberosComponentDescriptor.class);
    expect(componentDescriptor2.getIdentities(true)).
        andReturn(new ArrayList<KerberosIdentityDescriptor>() {{
          add(identityDescriptor2);
        }}).once();

    final KerberosServiceDescriptor serviceDescriptor1 = createNiceMock(KerberosServiceDescriptor.class);
    expect(serviceDescriptor1.getComponent("COMPONENT1")).andReturn(componentDescriptor1).once();

    final KerberosServiceDescriptor serviceDescriptor2 = createNiceMock(KerberosServiceDescriptor.class);
    expect(serviceDescriptor2.getComponent("COMPONENT2")).andReturn(componentDescriptor2).once();

    final KerberosDescriptor kerberosDescriptor = createNiceMock(KerberosDescriptor.class);
    expect(kerberosDescriptor.getService("SERVICE1")).andReturn(serviceDescriptor1).once();
    expect(kerberosDescriptor.getService("SERVICE2")).andReturn(serviceDescriptor2).once();

    final StageFactory stageFactory = injector.getInstance(StageFactory.class);
    expect(stageFactory.createNew(anyLong(), anyObject(String.class), anyObject(String.class),
        anyLong(), anyObject(String.class), anyObject(String.class), anyObject(String.class),
        anyObject(String.class)))
        .andAnswer(new IAnswer<Stage>() {
          @Override
          public Stage answer() throws Throwable {
            Stage stage = createNiceMock(Stage.class);

            expect(stage.getHostRoleCommands())
                .andReturn(Collections.<String, Map<String, HostRoleCommand>>emptyMap())
                .anyTimes();
            replay(stage);
            return stage;
          }
        })
        .anyTimes();

    // This is a STRICT mock to help ensure that the end result is what we want.
    final RequestStageContainer requestStageContainer = createStrictMock(RequestStageContainer.class);
    // Create Principals Stage
    expect(requestStageContainer.getId()).andReturn(1L).once();
    requestStageContainer.addStages(anyObject(List.class));
    expectLastCall().once();
    // Create Keytabs Stage
    expect(requestStageContainer.getId()).andReturn(1L).once();
    requestStageContainer.addStages(anyObject(List.class));
    expectLastCall().once();
    // Distribute Keytabs Stage
    expect(requestStageContainer.getId()).andReturn(1L).once();
    requestStageContainer.addStages(anyObject(List.class));
    expectLastCall().once();
    // Update Configs Stage
    expect(requestStageContainer.getId()).andReturn(1L).once();
    requestStageContainer.addStages(anyObject(List.class));
    expectLastCall().once();
    // TODO: Add more of these when more stages are added.
    // Clean-up/Finalize Stage
    expect(requestStageContainer.getId()).andReturn(1L).once();
    requestStageContainer.addStages(anyObject(List.class));
    expectLastCall().once();

    replayAll();

    // Needed by infrastructure
    injector.getInstance(AmbariMetaInfo.class).init();

    kerberosHelper.toggleKerberos(cluster, kerberosDescriptor, requestStageContainer);

    verifyAll();
  }
}
