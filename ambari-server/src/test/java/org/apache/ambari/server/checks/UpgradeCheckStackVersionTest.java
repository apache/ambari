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
package org.apache.ambari.server.checks;

import junit.framework.Assert;

import org.apache.ambari.server.controller.PrereqCheckRequest;
import org.apache.ambari.server.state.StackId;
import org.easymock.EasyMock;
import org.junit.Test;


/**
 * Tests that the {@link AbstractCheckDescriptor} instances will return the
 * correct values for
 * {@link AbstractCheckDescriptor#isApplicable(org.apache.ambari.server.controller.PrereqCheckRequest)}
 * when different stack versions are present.
 */
public class UpgradeCheckStackVersionTest {

  @Test
  public void testUpgradeCheckForMoreRecentStack() throws Exception {
    AbstractCheckDescriptor invalidCheck = EasyMock.createMockBuilder(AbstractCheckDescriptor.class).addMockedMethods(
        "getSourceStack", "getTargetStack").createMock();

    EasyMock.expect(invalidCheck.getSourceStack()).andReturn(new StackId("HDP-2.3"));
    EasyMock.expect(invalidCheck.getTargetStack()).andReturn(new StackId("HDP-2.3"));

    EasyMock.replay(invalidCheck);

    PrereqCheckRequest checkRequest = new PrereqCheckRequest("c1");
    checkRequest.setRepositoryVersion("HDP-2.2.0.0");
    checkRequest.setSourceStackId(new StackId("HDP", "2.2"));
    checkRequest.setTargetStackId(new StackId("HDP", "2.2"));

    // false because the upgrade is for 2.2->2.2 and the check starts at 2.3
    Assert.assertFalse(invalidCheck.isApplicable(checkRequest));

    EasyMock.verify(invalidCheck);
  }

  @Test
  public void testUpgradeCheckForOlderStack() throws Exception {
    AbstractCheckDescriptor invalidCheck = EasyMock.createMockBuilder(AbstractCheckDescriptor.class).addMockedMethods(
        "getSourceStack", "getTargetStack").createMock();

    EasyMock.expect(invalidCheck.getSourceStack()).andReturn(new StackId("HDP-2.2"));
    EasyMock.expect(invalidCheck.getTargetStack()).andReturn(new StackId("HDP-2.2"));

    EasyMock.replay(invalidCheck);

    PrereqCheckRequest checkRequest = new PrereqCheckRequest("c1");
    checkRequest.setRepositoryVersion("HDP-2.3.0.0");
    checkRequest.setSourceStackId(new StackId("HDP", "2.3"));
    checkRequest.setTargetStackId(new StackId("HDP", "2.3"));

    // false because the upgrade is for 2.3->2.3 and the check is only for 2.2
    Assert.assertFalse(invalidCheck.isApplicable(checkRequest));

    EasyMock.verify(invalidCheck);
  }

  @Test
  public void testUpgradeCheckForWithinStackOnly() throws Exception {
    AbstractCheckDescriptor invalidCheck = EasyMock.createMockBuilder(AbstractCheckDescriptor.class).addMockedMethods(
        "getSourceStack", "getTargetStack").createMock();

    EasyMock.expect(invalidCheck.getSourceStack()).andReturn(new StackId("HDP-2.2"));
    EasyMock.expect(invalidCheck.getTargetStack()).andReturn(new StackId("HDP-2.2"));

    EasyMock.replay(invalidCheck);

    PrereqCheckRequest checkRequest = new PrereqCheckRequest("c1");
    checkRequest.setRepositoryVersion("HDP-2.3.0.0");
    checkRequest.setSourceStackId(new StackId("HDP", "2.2"));
    checkRequest.setTargetStackId(new StackId("HDP", "2.3"));

    // false because the upgrade is for 2.2->2.3 and the check is only for 2.2
    // to 2.2
    Assert.assertFalse(invalidCheck.isApplicable(checkRequest));

    EasyMock.verify(invalidCheck);
  }

  @Test
  public void testUpgradeCheckMatchesExactly() throws Exception {
    AbstractCheckDescriptor invalidCheck = EasyMock.createMockBuilder(AbstractCheckDescriptor.class).addMockedMethods(
        "getSourceStack", "getTargetStack").createMock();

    EasyMock.expect(invalidCheck.getSourceStack()).andReturn(new StackId("HDP-2.2"));
    EasyMock.expect(invalidCheck.getTargetStack()).andReturn(new StackId("HDP-2.2"));

    EasyMock.replay(invalidCheck);

    PrereqCheckRequest checkRequest = new PrereqCheckRequest("c1");
    checkRequest.setRepositoryVersion("HDP-2.2.0.0");
    checkRequest.setSourceStackId(new StackId("HDP", "2.2"));
    checkRequest.setTargetStackId(new StackId("HDP", "2.2"));

    // pass because the upgrade is for 2.2->2.2 and the check is only for 2.2
    // to 2.2
    Assert.assertTrue(invalidCheck.isApplicable(checkRequest));

    EasyMock.verify(invalidCheck);
  }

  @Test
  public void testNoUpgradeStacksDefined() throws Exception {
    AbstractCheckDescriptor invalidCheck = EasyMock.createMockBuilder(AbstractCheckDescriptor.class).addMockedMethods(
        "getSourceStack", "getTargetStack").createMock();

    EasyMock.expect(invalidCheck.getSourceStack()).andReturn(null);
    EasyMock.expect(invalidCheck.getTargetStack()).andReturn(null);

    EasyMock.replay(invalidCheck);

    PrereqCheckRequest checkRequest = new PrereqCheckRequest("c1");
    checkRequest.setRepositoryVersion("HDP-2.3.0.0");
    checkRequest.setSourceStackId(new StackId("HDP", "2.2"));
    checkRequest.setTargetStackId(new StackId("HDP", "2.3"));

    // pass because there are no restrictions
    Assert.assertTrue(invalidCheck.isApplicable(checkRequest));

    EasyMock.verify(invalidCheck);
  }

  @Test
  public void testUpgradeStartsAtSpecifiedStackVersion() throws Exception {
    AbstractCheckDescriptor invalidCheck = EasyMock.createMockBuilder(AbstractCheckDescriptor.class).addMockedMethods(
        "getSourceStack", "getTargetStack").createMock();

    EasyMock.expect(invalidCheck.getSourceStack()).andReturn(new StackId("HDP-2.3")).atLeastOnce();
    EasyMock.expect(invalidCheck.getTargetStack()).andReturn(null).atLeastOnce();

    EasyMock.replay(invalidCheck);

    PrereqCheckRequest checkRequest = new PrereqCheckRequest("c1");
    checkRequest.setRepositoryVersion("HDP-2.2.0.0");
    checkRequest.setSourceStackId(new StackId("HDP", "2.2"));
    checkRequest.setTargetStackId(new StackId("HDP", "2.2"));

    // false because this check starts at 2.3 and the upgrade is 2.2 -> 2.2
    Assert.assertFalse(invalidCheck.isApplicable(checkRequest));

    checkRequest.setRepositoryVersion("HDP-2.3.0.0");
    checkRequest.setSourceStackId(new StackId("HDP", "2.2"));
    checkRequest.setTargetStackId(new StackId("HDP", "2.3"));

    // false because this check starts at 2.3 and the upgrade is 2.2 -> 2.3
    Assert.assertFalse(invalidCheck.isApplicable(checkRequest));

    EasyMock.verify(invalidCheck);
  }
}
