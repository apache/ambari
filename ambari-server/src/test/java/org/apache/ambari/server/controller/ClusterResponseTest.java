package org.apache.ambari.server.controller;

import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

public class ClusterResponseTest {

  @Test
  public void testBasicGetAndSet() {
    Long clusterId = new Long(10);
    String clusterName = "foo";
    Set<String> hostNames = new HashSet<String>();
    hostNames.add("h1");

    ClusterResponse r1 =
        new ClusterResponse(clusterId, clusterName, hostNames);

    Assert.assertEquals(clusterId, r1.getClusterId());
    Assert.assertEquals(clusterName, r1.getClusterName());
    Assert.assertArrayEquals(hostNames.toArray(), r1.getHostNames().toArray());

  }

  @Test
  public void testToString() {
    ClusterResponse r = new ClusterResponse(null, null, null);
    r.toString();
  }
}
