package org.apache.ambari.server.actionmanager;

import org.junit.Assert;
import org.junit.Test;

/**
 * HostRoleStatus Tests.
 */
public class HostRoleStatusTest {
  @Test
  public void testIsFailedState() throws Exception {
    Assert.assertTrue(HostRoleStatus.ABORTED.isFailedState());
    Assert.assertFalse(HostRoleStatus.COMPLETED.isFailedState());
    Assert.assertTrue(HostRoleStatus.FAILED.isFailedState());
    Assert.assertFalse(HostRoleStatus.IN_PROGRESS.isFailedState());
    Assert.assertFalse(HostRoleStatus.PENDING.isFailedState());
    Assert.assertFalse(HostRoleStatus.QUEUED.isFailedState());
    Assert.assertTrue(HostRoleStatus.TIMEDOUT.isFailedState());
  }

  @Test
  public void testIsCompletedState() throws Exception {
    Assert.assertTrue(HostRoleStatus.ABORTED.isCompletedState());
    Assert.assertTrue(HostRoleStatus.COMPLETED.isCompletedState());
    Assert.assertTrue(HostRoleStatus.FAILED.isCompletedState());
    Assert.assertFalse(HostRoleStatus.IN_PROGRESS.isCompletedState());
    Assert.assertFalse(HostRoleStatus.PENDING.isCompletedState());
    Assert.assertFalse(HostRoleStatus.QUEUED.isCompletedState());
    Assert.assertTrue(HostRoleStatus.TIMEDOUT.isCompletedState());
  }
}
