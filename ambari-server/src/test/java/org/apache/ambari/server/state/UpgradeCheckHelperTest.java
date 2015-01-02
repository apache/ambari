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
import org.apache.ambari.server.controller.PreUpgradeCheckRequest;
import org.apache.ambari.server.state.UpgradeCheckHelper.UpgradeCheckDescriptor;
import org.apache.ambari.server.state.stack.upgrade.UpgradeCheck;
import org.apache.ambari.server.state.stack.upgrade.UpgradeCheckStatus;
import org.easymock.EasyMock;
import org.junit.Test;

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

}
