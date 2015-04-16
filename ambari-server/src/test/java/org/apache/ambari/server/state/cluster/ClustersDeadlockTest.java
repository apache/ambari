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

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ServiceComponentNotFoundException;
import org.apache.ambari.server.ServiceNotFoundException;
import org.apache.ambari.server.events.listeners.upgrade.HostVersionOutOfSyncListener;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentFactory;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceComponentHostFactory;
import org.apache.ambari.server.state.ServiceFactory;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.persist.PersistService;
import com.google.inject.util.Modules;

/**
 * Tests AMBARI-9738 which produced a deadlock during read and writes between
 * {@link ClustersImpl} and {@link ClusterImpl}.
 */
public class ClustersDeadlockTest {
  private static final String CLUSTER_NAME = "c1";
  private static final int NUMBER_OF_HOSTS = 100;
  private static final int NUMBER_OF_THREADS = 3;

  private final AtomicInteger hostNameCounter = new AtomicInteger(0);

  private final StackId stackId = new StackId("HDP-0.1");

  @Inject
  private Injector injector;

  @Inject
  private Clusters clusters;

  @Inject
  private ServiceFactory serviceFactory;

  @Inject
  private ServiceComponentFactory serviceComponentFactory;

  @Inject
  private ServiceComponentHostFactory serviceComponentHostFactory;

  @Inject
  private OrmTestHelper helper;

  private Cluster cluster;

  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(Modules.override(
        new InMemoryDefaultTestModule()).with(new MockModule()));

    injector.getInstance(GuiceJpaInitializer.class);
    injector.injectMembers(this);

    StackId stackId = new StackId("HDP-0.1");
    clusters.addCluster(CLUSTER_NAME, stackId);

    cluster = clusters.getCluster(CLUSTER_NAME);
    helper.getOrCreateRepositoryVersion(stackId, stackId.getStackVersion());
    cluster.createClusterVersion(stackId, stackId.getStackVersion(), "admin",
        RepositoryVersionState.UPGRADING);

    // install HDFS
    installService("HDFS");
  }

  @After
  public void teardown() {
    injector.getInstance(PersistService.class).stop();
  }

  /**
   * Tests that no deadlock exists when adding hosts while reading from the
   * cluster.
   *
   * @throws Exception
   */
  @Test(timeout = 40000)
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
   * Tests that no deadlock exists when adding hosts while reading from the
   * cluster. This test ensures that there are service components installed on
   * the hosts so that the cluster health report does some more work.
   *
   * @throws Exception
   */
  @Test(timeout = 40000)
  public void testDeadlockWhileMappingHostsWithExistingServices()
      throws Exception {
    List<Thread> threads = new ArrayList<Thread>();
    for (int i = 0; i < NUMBER_OF_THREADS; i++) {
      ClusterReaderThread readerThread = new ClusterReaderThread();
      ClustersHostAndComponentMapperThread writerThread = new ClustersHostAndComponentMapperThread();

      threads.add(readerThread);
      threads.add(writerThread);

      readerThread.start();
      writerThread.start();
    }

    for (Thread thread : threads) {
      thread.join();
    }
  }

  /**
   * Tests that no deadlock exists when adding hosts while reading from the
   * cluster.
   *
   * @throws Exception
   */
  @Test(timeout = 40000)
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
   * The {@link ClustersHostAndComponentMapperThread} is used to map hosts to a
   * cluster over and over. This will also add components to the hosts that are
   * being mapped to further exercise the cluster health report concurrency.
   */
  private final class ClustersHostAndComponentMapperThread extends Thread {

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

          // create DATANODE on this host so that we end up exercising the
          // cluster health report since we need a service component host
          createNewServiceComponentHost("HDFS", "DATANODE", hostName);

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

  private Service installService(String serviceName) throws AmbariException {
    Service service = null;

    try {
      service = cluster.getService(serviceName);
    } catch (ServiceNotFoundException e) {
      service = serviceFactory.createNew(cluster, serviceName);
      cluster.addService(service);
      service.persist();
    }

    return service;
  }

  private ServiceComponent addServiceComponent(Service service,
      String componentName) throws AmbariException {
    ServiceComponent serviceComponent = null;
    try {
      serviceComponent = service.getServiceComponent(componentName);
    } catch (ServiceComponentNotFoundException e) {
      serviceComponent = serviceComponentFactory.createNew(service,
          componentName);
      service.addServiceComponent(serviceComponent);
      serviceComponent.setDesiredState(State.INSTALLED);
      serviceComponent.persist();
    }

    return serviceComponent;
  }

  private ServiceComponentHost createNewServiceComponentHost(String svc,
      String svcComponent, String hostName) throws AmbariException {
    Assert.assertNotNull(cluster.getConfigGroups());
    Service s = installService(svc);
    ServiceComponent sc = addServiceComponent(s, svcComponent);

    ServiceComponentHost sch = serviceComponentHostFactory.createNew(sc,
        hostName);

    sc.addServiceComponentHost(sch);
    sch.setDesiredState(State.INSTALLED);
    sch.setState(State.INSTALLED);
    sch.setDesiredStackVersion(stackId);
    sch.setStackVersion(stackId);

    sch.persist();
    return sch;
  }

  /**
  *
  */
  private class MockModule implements Module {
    /**
    *
    */
    @Override
    public void configure(Binder binder) {
      // this listener gets in the way of actually testing the concurrency
      // between the threads; it slows them down too much, so mock it out
      binder.bind(HostVersionOutOfSyncListener.class).toInstance(
          EasyMock.createNiceMock(HostVersionOutOfSyncListener.class));
    }
  }
}
