package org.apache.ambari.server.state.live.svccomphost;

import junit.framework.Assert;

import org.apache.ambari.server.state.ConfigVersion;
import org.apache.ambari.server.state.StackVersion;
import org.junit.Test;

public class TestServiceComponentHostState {

  @Test
  public void testEquals() {
    ServiceComponentHostState s1 = new ServiceComponentHostState();
    ServiceComponentHostState s2 = new ServiceComponentHostState();

    Assert.assertTrue(s1.equals(s2));

    s1.setConfigVersion(new ConfigVersion("x.y.z"));
    Assert.assertFalse(s1.equals(s2));
    Assert.assertFalse(s2.equals(s1));

    s2.setConfigVersion(new ConfigVersion("x.y.foo"));
    Assert.assertFalse(s1.equals(s2));
    Assert.assertFalse(s2.equals(s1));

    s2.setConfigVersion(new ConfigVersion("x.y.z"));
    Assert.assertTrue(s1.equals(s2));

    s2.setStackVersion(new StackVersion("1.x"));
    Assert.assertFalse(s1.equals(s2));
    Assert.assertFalse(s2.equals(s1));

    s1.setStackVersion(new StackVersion("1.x"));
    Assert.assertTrue(s1.equals(s2));

    s1.setStackVersion(new StackVersion("2.x"));
    Assert.assertFalse(s1.equals(s2));
    Assert.assertFalse(s2.equals(s1));

    s1.setState(ServiceComponentHostLiveState.INSTALLED);
    Assert.assertFalse(s1.equals(s2));

  }

}
