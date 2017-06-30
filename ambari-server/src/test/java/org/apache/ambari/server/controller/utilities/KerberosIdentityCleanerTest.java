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
package org.apache.ambari.server.controller.utilities;

import static com.google.common.collect.Lists.newArrayList;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.reset;

import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.events.ServiceComponentUninstalledEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.serveraction.kerberos.Component;
import org.apache.ambari.server.serveraction.kerberos.KerberosMissingAdminCredentialsException;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.kerberos.KerberosDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosDescriptorFactory;
import org.easymock.EasyMockRule;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class KerberosIdentityCleanerTest extends EasyMockSupport {
  @Rule public EasyMockRule mocks = new EasyMockRule(this);
  private static final String HOST = "c6401";
  private static final String OOZIE = "OOZIE";
  private static final String OOZIE_SERVER = "OOZIE_SERVER";
  private static final String OOZIE_2 = "OOZIE2";
  private static final String OOZIE_SERVER_2 = "OOZIE_SERVER2";
  private static final String YARN_2 = "YARN2";
  private static final String RESOURCE_MANAGER_2 = "RESOURCE_MANAGER2";
  private static final String YARN = "YARN";
  private static final String RESOURCE_MANAGER = "RESOURCE_MANAGER";
  private static final long CLUSTER_ID = 1;
  @Mock private KerberosHelper kerberosHelper;
  @Mock private Clusters clusters;
  @Mock private Cluster cluster;
  private Map<String, Service> installedServices = new HashMap<>();
  private KerberosDescriptorFactory kerberosDescriptorFactory = new KerberosDescriptorFactory();
  private KerberosIdentityCleaner kerberosIdentityCleaner;
  private KerberosDescriptor kerberosDescriptor;

  @Test
  public void removesAllKerberosIdentitesOfComponentAfterComponentWasUninstalled() throws Exception {
    installComponent(OOZIE, OOZIE_SERVER);
    kerberosHelper.deleteIdentity(cluster, new Component(HOST, OOZIE, OOZIE_SERVER), newArrayList("oozie_server1", "oozie_server2"));
    expectLastCall().once();
    replayAll();
    uninstallComponent(OOZIE, OOZIE_SERVER, HOST);
    verifyAll();
  }

  @Test
  public void skipsRemovingIdentityWhenServiceDoesNotExist() throws Exception {
    replayAll();
    uninstallComponent("NO_SUCH_SERVICE", OOZIE_SERVER, HOST);
    verifyAll();
  }

  @Test
  public void skipsRemovingIdentityThatIsSharedByPrincipalName() throws Exception {
    installComponent(OOZIE, OOZIE_SERVER);
    installComponent(OOZIE_2, OOZIE_SERVER_2);
    kerberosHelper.deleteIdentity(cluster, new Component(HOST, OOZIE, OOZIE_SERVER), newArrayList("oozie_server1"));
    expectLastCall().once();
    replayAll();
    uninstallComponent(OOZIE, OOZIE_SERVER, HOST);
    verifyAll();
  }

  @Test
  public void skipsRemovingIdentityThatIsSharedByKeyTabFilePath() throws Exception {
    installComponent(YARN, RESOURCE_MANAGER);
    installComponent(YARN_2, RESOURCE_MANAGER_2);
    kerberosHelper.deleteIdentity(cluster, new Component(HOST, YARN, RESOURCE_MANAGER), newArrayList("rm_unique"));
    expectLastCall().once();
    replayAll();
    uninstallComponent(YARN, RESOURCE_MANAGER, HOST);
    verifyAll();
  }

  @Test
  public void skipsRemovingIdentityWhenClusterIsNotKerberized() throws Exception {
    reset(cluster);
    expect(cluster.getSecurityType()).andReturn(SecurityType.NONE).anyTimes();
    replayAll();
    uninstallComponent(OOZIE, OOZIE_SERVER, HOST);
    verifyAll();
  }

  private void installComponent(String serviceName, final String componentName) {
    Service service = createMock(serviceName + "_" + componentName, Service.class);
    installedServices.put(serviceName, service);
    expect(service.getServiceComponents()).andReturn(new HashMap<String, ServiceComponent>() {{
      put(componentName, null);
    }}).anyTimes();
  }

  private void uninstallComponent(String service, String component, String host) throws KerberosMissingAdminCredentialsException {
    kerberosIdentityCleaner.componentRemoved(new ServiceComponentUninstalledEvent(CLUSTER_ID, "any", "any", service, component, host, false));
  }

  @Before
  public void setUp() throws Exception {
    kerberosIdentityCleaner = new KerberosIdentityCleaner(new AmbariEventPublisher(), kerberosHelper, clusters);
    kerberosDescriptor = kerberosDescriptorFactory.createInstance("{" +
      "  'services': [" +
      "    {" +
      "      'name': 'OOZIE'," +
      "      'components': [" +
      "        {" +
      "          'name': 'OOZIE_SERVER'," +
      "          'identities': [" +
      "            {" +
      "              'name': '/HDFS/NAMENODE/hdfs'" +
      "            }," +
      "            {" +
      "              'name': 'oozie_server1'" +
      "            }," +"" +
      "            {" +
      "              'name': 'oozie_server2'," +
      "              'principal': { 'value': 'oozie/_HOST@EXAMPLE.COM' }" +
      "            }" +
      "          ]" +
      "        }" +
      "      ]" +
      "    }," +
      "    {" +
      "      'name': 'OOZIE2'," +
      "      'components': [" +
      "        {" +
      "          'name': 'OOZIE_SERVER2'," +
      "          'identities': [" +
      "            {" +
      "              'name': 'oozie_server3'," +
      "              'principal': { 'value': 'oozie/_HOST@EXAMPLE.COM' }" +
      "            }" +"" +
      "          ]" +
      "        }" +
      "      ]" +
      "    }," +
      "    {" +
      "      'name': 'YARN'," +
      "      'components': [" +
      "        {" +
      "          'name': 'RESOURCE_MANAGER'," +
      "          'identities': [" +
      "            {" +
      "              'name': 'rm_unique'" +
      "            }," +
      "            {" +
      "              'name': 'rm1-shared'," +
      "              'keytab' : { 'file' : 'shared' }" +
      "            }" +
      "          ]" +
      "        }" +
      "      ]" +
      "    }," +
      "    {" +
      "      'name': 'YARN2'," +
      "      'components': [" +
      "        {" +
      "          'name': 'RESOURCE_MANAGER2'," +
      "          'identities': [" +
      "            {" +
      "              'name': 'rm2-shared'," +
      "              'keytab' : { 'file' : 'shared' }" +
      "            }" +
      "          ]" +
      "        }" +
      "      ]" +
      "    }" +
      "  ]" +
      "}");
    expect(clusters.getCluster(CLUSTER_ID)).andReturn(cluster).anyTimes();
    expect(cluster.getSecurityType()).andReturn(SecurityType.KERBEROS).anyTimes();
    expect(kerberosHelper.getKerberosDescriptor(cluster)).andReturn(kerberosDescriptor).anyTimes();
    expect(cluster.getServices()).andReturn(installedServices).anyTimes();
  }
}