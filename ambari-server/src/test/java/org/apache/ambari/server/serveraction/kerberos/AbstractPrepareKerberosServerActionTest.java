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

import static org.easymock.EasyMock.anyBoolean;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.audit.AuditLogger;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.kerberos.KerberosComponentDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosServiceDescriptor;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class AbstractPrepareKerberosServerActionTest {
  private class PrepareKerberosServerAction extends AbstractPrepareKerberosServerAction{

    @Override
    public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext) throws AmbariException, InterruptedException {
      return null;
    }
  }

  private Injector injector;
  private final PrepareKerberosServerAction prepareKerberosServerAction = new PrepareKerberosServerAction();

  private final AuditLogger auditLogger = EasyMock.createNiceMock(AuditLogger.class);
  private final Clusters clusters = EasyMock.createNiceMock(Clusters.class);
  private final KerberosHelper kerberosHelper = EasyMock.createNiceMock(KerberosHelper.class);
  private final KerberosIdentityDataFileWriterFactory kerberosIdentityDataFileWriterFactory = EasyMock.createNiceMock(KerberosIdentityDataFileWriterFactory.class);

  @Before
  public void setUp() throws Exception {
    injector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(KerberosHelper.class).toInstance(kerberosHelper);
        bind(KerberosIdentityDataFileWriterFactory.class).toInstance(kerberosIdentityDataFileWriterFactory);
        bind(Clusters.class).toInstance(clusters);
        bind(AuditLogger.class).toInstance(auditLogger);
      }
    });

    injector.injectMembers(prepareKerberosServerAction);
  }

  /**
   * Test checks that {@code KerberosHelper.applyStackAdvisorUpdates} would be called with
   * full list of the services and not only list of services with KerberosDescriptior.
   * In this test HDFS service will have KerberosDescriptor, while Zookeeper not.
   * @throws Exception
   */
  @Test
  @SuppressWarnings("unchecked")
  public void testProcessServiceComponentHosts() throws Exception {
    final Cluster cluster =  EasyMock.createNiceMock(Cluster.class);
    final KerberosIdentityDataFileWriter kerberosIdentityDataFileWriter = EasyMock.createNiceMock(KerberosIdentityDataFileWriter.class);
    final KerberosDescriptor kerberosDescriptor = EasyMock.createNiceMock(KerberosDescriptor.class);
    final ServiceComponentHost serviceComponentHostHDFS = EasyMock.createNiceMock(ServiceComponentHost.class);
    final ServiceComponentHost serviceComponentHostZK = EasyMock.createNiceMock(ServiceComponentHost.class);
    final KerberosServiceDescriptor serviceDescriptor = EasyMock.createNiceMock(KerberosServiceDescriptor.class);
    final KerberosComponentDescriptor componentDescriptor = EasyMock.createNiceMock(KerberosComponentDescriptor.class);

    final String hdfsService = "HDFS";
    final String zookeeperService = "ZOOKEEPER";
    final String hostName = "host1";
    final String hdfsComponent = "DATANODE";
    final String zkComponent = "ZK";

    Collection<String> identityFilter = new ArrayList<>();
    Map<String, Map<String, String>> kerberosConfigurations = new HashMap<>();
    Map<String, Map<String, String>> propertiesToInsert = new HashMap<>();
    Map<String, Set<String>> propertiesToRemove = new HashMap<>();
    Map<String, String> descriptorProperties = new HashMap<>();
    Map<String, Map<String, String>> configurations = new HashMap<>();

    List<ServiceComponentHost> serviceComponentHosts = new ArrayList<ServiceComponentHost>() {{
      add(serviceComponentHostHDFS);
      add(serviceComponentHostZK);
    }};
    Map<String, Service> clusterServices = new HashMap<String, Service>(){{
      put(hdfsService, null);
      put(zookeeperService, null);
    }};

    Capture<Set<String>> serviceCapture = Capture.newInstance(CaptureType.LAST);

    expect(kerberosDescriptor.getProperties()).andReturn(descriptorProperties).atLeastOnce();
    expect(kerberosHelper.calculateConfigurations((Cluster)anyObject(), anyString(), (Map<String,String>)anyObject())).andReturn(configurations).atLeastOnce();
    expect(kerberosIdentityDataFileWriterFactory.createKerberosIdentityDataFileWriter((File)anyObject())).andReturn(kerberosIdentityDataFileWriter);
    // it's important to pass a copy of clusterServices
    expect(cluster.getServices()).andReturn(new HashMap<>(clusterServices)).atLeastOnce();

    expect(serviceComponentHostHDFS.getHostName()).andReturn(hostName).atLeastOnce();
    expect(serviceComponentHostHDFS.getServiceName()).andReturn(hdfsService).atLeastOnce();
    expect(serviceComponentHostHDFS.getServiceComponentName()).andReturn(hdfsComponent).atLeastOnce();

    expect(serviceComponentHostZK.getHostName()).andReturn(hostName).atLeastOnce();
    expect(serviceComponentHostZK.getServiceName()).andReturn(zookeeperService).atLeastOnce();
    expect(serviceComponentHostZK.getServiceComponentName()).andReturn(zkComponent).atLeastOnce();

    expect(kerberosDescriptor.getService(hdfsService)).andReturn(serviceDescriptor).once();

    expect(serviceDescriptor.getComponent(hdfsComponent)).andReturn(componentDescriptor).once();
    expect(componentDescriptor.getConfigurations(anyBoolean())).andReturn(null);

    expect(kerberosHelper.applyStackAdvisorUpdates(
      (Cluster)anyObject(),
      capture(serviceCapture),
      (Map<String, Map<String, String>>)anyObject(),
      (Map<String, Map<String, String>>)anyObject(),
      (Map<String, Set<String>>)anyObject(),
      (Map<String, Map<String, String>>)anyObject(),
      (Map<String, Set<String>>)anyObject(),
      anyBoolean())).andReturn(null).atLeastOnce();

    replay(kerberosDescriptor, kerberosHelper, kerberosIdentityDataFileWriterFactory,
      cluster, serviceComponentHostHDFS, serviceComponentHostZK, serviceDescriptor, componentDescriptor);

    prepareKerberosServerAction.processServiceComponentHosts(cluster,
      kerberosDescriptor,
      serviceComponentHosts,
      identityFilter,
      "",
      kerberosConfigurations,
      propertiesToInsert,
      propertiesToRemove,
      false, false);

    verify(kerberosHelper);

    Set<String> resultServices = serviceCapture.getValue();
    Assert.assertEquals(clusterServices.keySet(), resultServices);
  }

}
