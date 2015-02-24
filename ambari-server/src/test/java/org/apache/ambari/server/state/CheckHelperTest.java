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

package org.apache.ambari.server.state;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ClusterNotFoundException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.checks.AbstractCheckDescriptor;
import org.apache.ambari.server.checks.CheckDescription;
import org.apache.ambari.server.checks.ServicesUpCheck;
import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.orm.dao.HostVersionDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.state.stack.PrereqCheckStatus;
import org.apache.ambari.server.state.stack.PrerequisiteCheck;
import org.apache.ambari.server.state.stack.upgrade.RepositoryVersionHelper;
import org.easymock.EasyMock;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Providers;


/**
 * Tests the {@link CheckHelper} class
 */

public class CheckHelperTest {

/**
   * Makes sure that people don't forget to add new checks to registry.
   */

  @Test
  public void performPreUpgradeChecksTest_ok() throws Exception {
    final CheckHelper helper = new CheckHelper();
    List<AbstractCheckDescriptor> updateChecksRegistry = new ArrayList<AbstractCheckDescriptor>();
    AbstractCheckDescriptor descriptor = EasyMock.createNiceMock(AbstractCheckDescriptor.class);
    descriptor.perform(EasyMock.<PrerequisiteCheck> anyObject(), EasyMock.<PrereqCheckRequest> anyObject());
    EasyMock.expectLastCall().times(1);
    EasyMock.expect(descriptor.isApplicable(EasyMock.<PrereqCheckRequest> anyObject())).andReturn(true);
    EasyMock.replay(descriptor);
    updateChecksRegistry.add(descriptor);

    helper.performChecks(new PrereqCheckRequest("cluster"), updateChecksRegistry);
    EasyMock.verify(descriptor);
  }

  @Test
  public void performPreUpgradeChecksTest_notApplicable() throws Exception {
    final CheckHelper helper = new CheckHelper();
    List<AbstractCheckDescriptor> updateChecksRegistry = new ArrayList<AbstractCheckDescriptor>();
    AbstractCheckDescriptor descriptor = EasyMock.createNiceMock(AbstractCheckDescriptor.class);
    EasyMock.expect(descriptor.isApplicable(EasyMock.<PrereqCheckRequest> anyObject())).andReturn(false);
    EasyMock.replay(descriptor);
    updateChecksRegistry.add(descriptor);
    helper.performChecks(new PrereqCheckRequest("cluster"), updateChecksRegistry);
    EasyMock.verify(descriptor);
  }

  @Test
  public void performPreUpgradeChecksTest_throwsException() throws Exception {
    final CheckHelper helper = new CheckHelper();
    List<AbstractCheckDescriptor> updateChecksRegistry = new ArrayList<AbstractCheckDescriptor>();
    AbstractCheckDescriptor descriptor = EasyMock.createNiceMock(AbstractCheckDescriptor.class);

    descriptor.perform(EasyMock.<PrerequisiteCheck> anyObject(), EasyMock.<PrereqCheckRequest> anyObject());
    EasyMock.expectLastCall().andThrow(new AmbariException("error"));
    EasyMock.expect(descriptor.isApplicable(EasyMock.<PrereqCheckRequest> anyObject())).andReturn(true);
    EasyMock.expect(descriptor.getDescription()).andReturn(CheckDescription.HOSTS_HEARTBEAT).anyTimes();
    EasyMock.replay(descriptor);
    updateChecksRegistry.add(descriptor);
    final List<PrerequisiteCheck> upgradeChecks = helper.performChecks(new PrereqCheckRequest("cluster"), updateChecksRegistry);
    EasyMock.verify(descriptor);
    Assert.assertEquals(PrereqCheckStatus.FAIL, upgradeChecks.get(0).getStatus());
  }

  @Test
  public void performPreUpgradeChecksTest_clusterIsMissing() throws Exception {
    final Clusters clusters = Mockito.mock(Clusters.class);
    Mockito.when(clusters.getCluster(Mockito.anyString())).thenAnswer(new Answer<Cluster>() {
      @Override
      public Cluster answer(InvocationOnMock invocation) throws Throwable {
        final String clusterName = invocation.getArguments()[0].toString();
        if (clusterName.equals("existing")) {
          return Mockito.mock(Cluster.class);
        } else {
          throw new ClusterNotFoundException(clusterName);
        }
      }
    });
    final Injector injector = Guice.createInjector(new AbstractModule() {

      @Override
      protected void configure() {
        bind(Clusters.class).toInstance(clusters);
        bind(HostVersionDAO.class).toProvider(Providers.<HostVersionDAO>of(null));
        bind(RepositoryVersionDAO.class).toProvider(Providers.<RepositoryVersionDAO>of(null));
        bind(RepositoryVersionHelper.class).toProvider(Providers.<RepositoryVersionHelper>of(null));
        bind(AmbariMetaInfo.class).toProvider(Providers.<AmbariMetaInfo>of(null));
      }
    });
    final CheckHelper helper = injector.getInstance(CheckHelper.class);
    List<AbstractCheckDescriptor> updateChecksRegistry = new ArrayList<AbstractCheckDescriptor>();
    updateChecksRegistry.add(injector.getInstance(ServicesUpCheck.class)); //mocked Cluster has no services, so the check should always be PASS
    List<PrerequisiteCheck> upgradeChecks = helper.performChecks(new PrereqCheckRequest("existing"), updateChecksRegistry);
    Assert.assertEquals(PrereqCheckStatus.PASS, upgradeChecks.get(0).getStatus());
    upgradeChecks = helper.performChecks(new PrereqCheckRequest("non-existing"), updateChecksRegistry);
    Assert.assertEquals(PrereqCheckStatus.FAIL, upgradeChecks.get(0).getStatus());
    //non existing cluster is an expected error
    Assert.assertTrue(!upgradeChecks.get(0).getFailReason().equals("Unexpected server error happened"));
  }
}

