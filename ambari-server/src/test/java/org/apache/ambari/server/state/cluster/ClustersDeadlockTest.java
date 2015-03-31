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

package org.apache.ambari.server.state.cluster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.StackId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

/**
 * Tests AMBARI-9738 which produced a deadlock during read and writes between
 * {@link ClustersImpl} and {@link ClusterImpl}.
 */
public class ClustersDeadlockTest {
  private static final String CLUSTER_NAME = "c1";
  private static final int NUMBER_OF_HOSTS = 100;
  private static final int NUMBER_OF_THREADS = 3;

  private final AtomicInteger hostNameCounter = new AtomicInteger(0);

  @Inject
  private Injector injector;

  @Inject
  private Clusters clusters;

  @Inject
  private AmbariMetaInfo metaInfo;

  @Inject
  private OrmTestHelper helper;

  private Cluster cluster;

  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    injector.injectMembers(this);
    clusters.addCluster(CLUSTER_NAME);

    StackId stackId = new StackId("HDP-0.1");
    cluster = clusters.getCluster(CLUSTER_NAME);
    cluster.setDesiredStackVersion(stackId);
    helper.getOrCreateRepositoryVersion(stackId.getStackName(), stackId.getStackVersion());
    cluster.createClusterVersion(stackId.getStackName(), stackId.getStackVersion(), "admin", RepositoryVersionState.UPGRADING);
  }

  @After
  public void teardown() {
    injector.getInstance(PersistService.class).stop();
  }

  /**
   * Tests that no deadlock exists when adding hosts from reading from the
   * cluster.
   *
   * @throws Exception
   */
  @Test(timeout = 35000)
  public void testDeadlockWhileMappingHosts() throws Exception {
    List<Thread> threads = new ArrayList<Thread>();
    for (int i = 0; i < NUMBER_OF_THREADS; i++) {
      ClusterReaderThread readerThread = new ClusterReaderThread();
      ClustersHostMapperThread writerThread = new ClustersHostMapperThread();

      threads.add(readerThread);
      threads.add(writerThread);

      readerThread.start();
      writerThread.start();
    }

    for (Thread thread : threads) {
      thread.join();
    }

    Assert.assertEquals(NUMBER_OF_THREADS * NUMBER_OF_HOSTS,
        clusters.getHostsForCluster(CLUSTER_NAME).size());
  }

  /**
   * Tests that no deadlock exists when adding hosts from reading from the
   * cluster.
   *
   * @throws Exception
   */
  @Test(timeout = 35000)
  public void testDeadlockWhileUnmappingHosts() throws Exception {
    List<Thread> threads = new ArrayList<Thread>();
    for (int i = 0; i < NUMBER_OF_THREADS; i++) {
      ClusterReaderThread readerThread = new ClusterReaderThread();
      ClustersHostUnMapperThread writerThread = new ClustersHostUnMapperThread();

      threads.add(readerThread);
      threads.add(writerThread);

      readerThread.start();
      writerThread.start();
    }

    for (Thread thread : threads) {
      thread.join();
    }

    Assert.assertEquals(0,
        clusters.getHostsForCluster(CLUSTER_NAME).size());
  }

  /**
   * The {@link ClusterReaderThread} reads from a cluster over and over again
   * with a slight pause.
   */
  private final class ClusterReaderThread extends Thread {

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
      try {
        for (int i = 0; i < 1000; i++) {
          cluster.convertToResponse();
          Thread.sleep(10);
        }
      } catch (Exception exception) {
        throw new RuntimeException(exception);
      }
    }
  }

  /**
   * The {@link ClustersHostMapperThread} is used to map hosts to a cluster over
   * and over.
   */
  private final class ClustersHostMapperThread extends Thread {

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
      try {
        for (int i = 0; i < NUMBER_OF_HOSTS; i++) {
          String hostName = "c64-" + hostNameCounter.getAndIncrement();
          clusters.addHost(hostName);
          setOsFamily(clusters.getHost(hostName), "redhat", "6.4");
          clusters.getHost(hostName).persist();
          clusters.mapHostToCluster(hostName, CLUSTER_NAME);

          Thread.sleep(10);
        }
      } catch (Exception exception) {
        throw new RuntimeException(exception);
      }
    }
  }

  /**
   * The {@link ClustersHostUnMapperThread} is used to unmap hosts to a cluster
   * over and over.
   */
  private final class ClustersHostUnMapperThread extends Thread {

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
      List<String> hostNames = new ArrayList<String>(100);
      try {
        // pre-map the hosts
        for (int i = 0; i < NUMBER_OF_HOSTS; i++) {
          String hostName = "c64-" + hostNameCounter.getAndIncrement();
          hostNames.add(hostName);

          clusters.addHost(hostName);
          setOsFamily(clusters.getHost(hostName), "redhat", "6.4");
          clusters.getHost(hostName).persist();
          clusters.mapHostToCluster(hostName, CLUSTER_NAME);
        }

        // unmap them all now
        for (String hostName : hostNames) {
          clusters.unmapHostFromCluster(hostName, CLUSTER_NAME);
          Thread.sleep(10);
        }
      } catch (Exception exception) {
        throw new RuntimeException(exception);
      }
    }
  }


  private void setOsFamily(Host host, String osFamily, String osVersion) {
    Map<String, String> hostAttributes = new HashMap<String, String>(2);
    hostAttributes.put("os_family", osFamily);
    hostAttributes.put("os_release_version", osVersion);
    host.setHostAttributes(hostAttributes);
  }
}
