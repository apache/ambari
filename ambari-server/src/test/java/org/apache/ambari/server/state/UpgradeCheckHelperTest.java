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
package org.apache.ambari.server.state;

import java.util.List;

import junit.framework.Assert;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ClusterNotFoundException;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.PreUpgradeCheckRequest;
import org.apache.ambari.server.orm.dao.HostVersionDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.state.UpgradeCheckHelper.UpgradeCheckDescriptor;
import org.apache.ambari.server.state.stack.upgrade.UpgradeCheck;
import org.apache.ambari.server.state.stack.upgrade.UpgradeCheckStatus;
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
 * Tests the {@link UpgradeCheckHelper} class
 */
public class UpgradeCheckHelperTest {

  /**
   * Makes sure that people don't forget to add new checks to registry.
   */
  @Test
  public void defaultConstructorTest() throws Exception {
    final UpgradeCheckHelper helper = new UpgradeCheckHelper();

    Assert.assertEquals(UpgradeCheckHelper.class.getDeclaredClasses().length - 1, helper.registry.size());
  }

  @Test
  public void performPreUpgradeChecksTest_ok() throws Exception {
    final UpgradeCheckHelper helper = new UpgradeCheckHelper();
    helper.registry.clear();
    UpgradeCheckDescriptor descriptor = EasyMock.createNiceMock(UpgradeCheckDescriptor.class);
    descriptor.perform(EasyMock.<UpgradeCheck> anyObject(), EasyMock.<PreUpgradeCheckRequest> anyObject());
    EasyMock.expectLastCall().times(1);
    EasyMock.expect(descriptor.isApplicable(EasyMock.<PreUpgradeCheckRequest> anyObject())).andReturn(true);
    EasyMock.replay(descriptor);
    helper.registry.add(descriptor);
    helper.performPreUpgradeChecks(new PreUpgradeCheckRequest("cluster"));
    EasyMock.verify(descriptor);
  }

  @Test
  public void performPreUpgradeChecksTest_notApplicable() throws Exception {
    final UpgradeCheckHelper helper = new UpgradeCheckHelper();
    helper.registry.clear();
    UpgradeCheckDescriptor descriptor = EasyMock.createNiceMock(UpgradeCheckDescriptor.class);
    EasyMock.expect(descriptor.isApplicable(EasyMock.<PreUpgradeCheckRequest> anyObject())).andReturn(false);
    EasyMock.replay(descriptor);
    helper.registry.add(descriptor);
    helper.performPreUpgradeChecks(new PreUpgradeCheckRequest("cluster"));
    EasyMock.verify(descriptor);
  }

  @Test
  public void performPreUpgradeChecksTest_throwsException() throws Exception {
    final UpgradeCheckHelper helper = new UpgradeCheckHelper();
    helper.registry.clear();
    UpgradeCheckDescriptor descriptor = EasyMock.createNiceMock(UpgradeCheckDescriptor.class);
    descriptor.perform(EasyMock.<UpgradeCheck> anyObject(), EasyMock.<PreUpgradeCheckRequest> anyObject());
    EasyMock.expectLastCall().andThrow(new AmbariException("error"));
    EasyMock.expect(descriptor.isApplicable(EasyMock.<PreUpgradeCheckRequest> anyObject())).andReturn(true);
    EasyMock.replay(descriptor);
    helper.registry.add(descriptor);
    final List<UpgradeCheck> upgradeChecks = helper.performPreUpgradeChecks(new PreUpgradeCheckRequest("cluster"));
    EasyMock.verify(descriptor);
    Assert.assertEquals(UpgradeCheckStatus.FAIL, upgradeChecks.get(0).getStatus());
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
        bind(Configuration.class).toProvider(Providers.<Configuration> of(null));
        bind(HostVersionDAO.class).toProvider(Providers.<HostVersionDAO> of(null));
        bind(RepositoryVersionDAO.class).toProvider(Providers.<RepositoryVersionDAO> of(null));
      }
    });
    final UpgradeCheckHelper helper = injector.getInstance(UpgradeCheckHelper.class);
    helper.registry.clear();
    helper.registry.add(helper.new ServicesUpCheck()); //mocked Cluster has no services, so the check should always be PASS
    List<UpgradeCheck> upgradeChecks = helper.performPreUpgradeChecks(new PreUpgradeCheckRequest("existing"));
    Assert.assertEquals(UpgradeCheckStatus.PASS, upgradeChecks.get(0).getStatus());
    upgradeChecks = helper.performPreUpgradeChecks(new PreUpgradeCheckRequest("non-existing"));
    Assert.assertEquals(UpgradeCheckStatus.FAIL, upgradeChecks.get(0).getStatus());
    //non existing cluster is an expected error
    Assert.assertTrue(!upgradeChecks.get(0).getFailReason().equals("Unexpected server error happened"));
  }

}
