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
package org.apache.ambari.tools.zk;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryOneTime;
import org.apache.curator.test.TestingServer;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category({category.SlowTest.class})
public class ManagedDependencyZkTest {
  private static final String BINDING_ID = "59646bd5-39eb-414e-a07e-f5469d369687";
  private static final String CONTAINER = ManagedDependencyZk.PARENT + "/" + BINDING_ID;
  private static final String ZNODE = CONTAINER + "/hbase";
  private static final String PROBE_ID = "43d3dad3-d0b0-4ba2-a9ae-2ccf7ab758f2";

  private CuratorFramework client;
  private TestingServer server;

  @Before
  public void startZooKeeper() throws Exception {
    server = new TestingServer(freePort());
    server.start();
    client = CuratorFrameworkFactory.newClient(server.getConnectString(), new RetryOneTime(200));
    client.start();
  }

  @After
  public void stopZooKeeper() throws Exception {
    client.close();
    server.stop();
  }

  @Test
  public void prepareIsExactNonrecursiveAndRetryPreservesProviderData() throws Exception {
    ManagedDependencyZk.main(arguments("PREPARE_INSECURE", null));
    byte[] parent = client.getData().forPath(ManagedDependencyZk.PARENT);
    byte[] container = client.getData().forPath(CONTAINER);
    client.create().forPath(ZNODE + "/provider-data", "keep".getBytes(StandardCharsets.UTF_8));

    ManagedDependencyZk.main(arguments("PREPARE_INSECURE", null));

    assertArrayEquals("ambari-managed-hbase-parent:v1".getBytes(StandardCharsets.UTF_8), parent);
    assertArrayEquals(container, client.getData().forPath(CONTAINER));
    assertEquals(ZooDefs.Ids.OPEN_ACL_UNSAFE, client.getACL().forPath(ManagedDependencyZk.PARENT));
    assertEquals(ZooDefs.Ids.OPEN_ACL_UNSAFE, client.getACL().forPath(CONTAINER));
    assertEquals(ZooDefs.Ids.OPEN_ACL_UNSAFE, client.getACL().forPath(ZNODE));
    assertNotNull(client.checkExists().forPath(ZNODE + "/provider-data"));
  }

  @Test
  public void existingUnexpectedNamespaceFailsWithoutRepairOrDelete() throws Exception {
    client.create().forPath(ManagedDependencyZk.PARENT, "foreign".getBytes(StandardCharsets.UTF_8));

    try {
      ManagedDependencyZk.main(arguments("PREPARE_INSECURE", null));
      fail("expected unexpected parent metadata to fail closed");
    } catch (IllegalStateException expected) {
      // Expected.
    }

    assertArrayEquals("foreign".getBytes(StandardCharsets.UTF_8),
        client.getData().forPath(ManagedDependencyZk.PARENT));
    assertNull(client.checkExists().forPath(ZNODE));
  }

  @Test
  public void consumerVerificationUsesAndCleansOnlyItsEphemeralProbe() throws Exception {
    ManagedDependencyZk.main(arguments("PREPARE_INSECURE", null));
    client.create().forPath(ZNODE + "/provider-data", "keep".getBytes(StandardCharsets.UTF_8));

    ManagedDependencyZk.main(arguments("VERIFY_INSECURE", PROBE_ID));

    assertNull(client.checkExists().forPath(ZNODE + "/.ambari-managed-probe-" + PROBE_ID));
    assertNotNull(client.checkExists().forPath(ZNODE + "/provider-data"));
  }

  @Test
  public void secureBaseAcceptsOnlyPrivateOrHBaseNativeCanonicalAcl() {
    ACL consumer = new ACL(ZooDefs.Perms.ALL, new Id("sasl", "hbase_mc_c1"));
    ACL worldRead = new ACL(ZooDefs.Perms.READ, ZooDefs.Ids.ANYONE_ID_UNSAFE);

    assertTrue(ManagedDependencyZk.isSupportedHBaseBaseAcl(
        List.of(consumer), "hbase_mc_c1", 0));
    assertTrue(ManagedDependencyZk.isSupportedHBaseBaseAcl(
        List.of(worldRead, consumer), "hbase_mc_c1", 1));
    assertFalse(ManagedDependencyZk.isSupportedHBaseBaseAcl(
        List.of(consumer), "hbase_mc_c1", 1));
    assertFalse(ManagedDependencyZk.isSupportedHBaseBaseAcl(
        List.of(consumer, new ACL(ZooDefs.Perms.ALL, ZooDefs.Ids.ANYONE_ID_UNSAFE)),
        "hbase_mc_c1", 1));
    assertFalse(ManagedDependencyZk.isSupportedHBaseBaseAcl(
        List.of(new ACL(ZooDefs.Perms.ALL, new Id("sasl", "foreign")), worldRead),
        "hbase_mc_c1", 1));
  }

  private String[] arguments(String operation, String probeId) {
    java.util.List<String> values = new java.util.ArrayList<>(java.util.List.of(
        "--operation", operation,
        "--connection-string", server.getConnectString(),
        "--parent", ManagedDependencyZk.PARENT,
        "--container", CONTAINER,
        "--znode", ZNODE,
        "--binding-id", BINDING_ID,
        "--provider-cluster-id", "2",
        "--owner-user", "hbase_mc_c1"));
    if (probeId != null) {
      values.add("--probe-id");
      values.add(probeId);
    }
    return values.toArray(String[]::new);
  }

  private static int freePort() throws Exception {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    }
  }
}
