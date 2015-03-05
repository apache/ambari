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

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ServiceComponentNotFoundException;
import org.apache.ambari.server.ServiceNotFoundException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentFactory;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceComponentHostFactory;
import org.apache.ambari.server.state.ServiceFactory;
import org.apache.ambari.server.state.StackId;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

/**
 * Tests AMBARI-9368 which produced a deadlock during read and writes of some of
 * the impl classes.
 */
public class ClusterDeadlockTest {
  private final AtomicInteger hostNameCounter = new AtomicInteger(0);

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
  private AmbariMetaInfo metaInfo;


  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    injector.injectMembers(this);
    clusters.addCluster("c1");

    StackId stackId = new StackId("HDP-0.1");
    Cluster c1 = clusters.getCluster("c1");
    c1.setDesiredStackVersion(stackId);
    metaInfo.init();

    // 100 hosts
    for (int i = 0; i < 100; i++) {
      String hostName = "c64-" + i;
      clusters.addHost(hostName);
      setOsFamily(clusters.getHost(hostName), "redhat", "6.4");
      clusters.getHost(hostName).persist();
      clusters.mapHostToCluster(hostName, "c1");
    }

    // force creation of the service and the components on the last host
    createNewServiceComponentHost("HDFS", "NAMENODE", "c64-99", false);
    createNewServiceComponentHost("HDFS", "HDFS_CLIENT", "c64-99", true);
  }

  @After
  public void teardown() {
    injector.getInstance(PersistService.class).stop();
  }

  /**
   * Tests that concurrent impl serialization and impl writing doesn't cause a
   * deadlock.
   *
   * @throws Exception
   */
  @Test(timeout = 30000)
  public void testDeadlockBetweenImplementations() throws Exception {
    Cluster cluster = clusters.getCluster("c1");
    Service service = cluster.getService("HDFS");
    ServiceComponent namenodeComponent = service.getServiceComponent("NAMENODE");
    ServiceComponent hdfsClientComponent = service.getServiceComponent("HDFS_CLIENT");

    ServiceComponentHost namenodeSCH = createNewServiceComponentHost("HDFS",
        "NAMENODE", "c64-0", false);

    ServiceComponentHost hdfsClientSCH = createNewServiceComponentHost("HDFS",
        "HDFS_CLIENT", "c64-0", true);

    List<Thread> threads = new ArrayList<Thread>();
    for (int i = 0; i < 3; i++) {
      DeadlockExerciserThread thread = new DeadlockExerciserThread();
      thread.setCluster(cluster);
      thread.setService(service);
      thread.setHdfsClientComponent(hdfsClientComponent);
      thread.setNamenodeComponent(namenodeComponent);
      thread.setNamenodeSCH(namenodeSCH);
      thread.setHdfsClientSCH(hdfsClientSCH);
      thread.start();
      threads.add(thread);
    }

    for (Thread thread : threads) {
      thread.join();
    }
  }

  /**
   * Tests that while serializing a service component, writes to that service
   * component do not cause a deadlock with the global cluster lock.
   *
   * @throws Exception
   */
  @Test(timeout = 30000)
  public void testAddingHostComponentsWhileReading() throws Exception {
    Cluster cluster = clusters.getCluster("c1");
    Service service = cluster.getService("HDFS");
    ServiceComponent namenodeComponent = service.getServiceComponent("NAMENODE");
    ServiceComponent hdfsClientComponent = service.getServiceComponent("HDFS_CLIENT");

    List<Thread> threads = new ArrayList<Thread>();
    for (int i = 0; i < 5; i++) {
      ServiceComponentDeadlockThread thread = new ServiceComponentDeadlockThread();
      thread.setHdfsClientComponent(hdfsClientComponent);
      thread.setNamenodeComponent(namenodeComponent);
      thread.start();
      threads.add(thread);
    }

    for (Thread thread : threads) {
      thread.join();
    }
  }

  /**
   * Tests AMBARI-9368 which saw a deadlock when adding a service component host
   * while reading a service component.
   */
  private final class ServiceComponentDeadlockThread extends Thread {
    private ServiceComponent namenodeComponent;
    private ServiceComponent hdfsClientComponent;

    /**
     * @param namenodeComponent
     *          the namenodeComponent to set
     */
    public void setNamenodeComponent(ServiceComponent namenodeComponent) {
      this.namenodeComponent = namenodeComponent;
    }

    /**
     * @param hdfsClientComponent
     *          the hdfsClientComponent to set
     */
    public void setHdfsClientComponent(ServiceComponent hdfsClientComponent) {
      this.hdfsClientComponent = hdfsClientComponent;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
      try {
        for (int i = 0; i < 15; i++) {
          int hostNumeric = hostNameCounter.getAndIncrement();

          namenodeComponent.convertToResponse();
          createNewServiceComponentHost("HDFS", "NAMENODE", "c64-"
              + hostNumeric, false);

          hdfsClientComponent.convertToResponse();
          createNewServiceComponentHost("HDFS", "HDFS_CLIENT", "c64-"
              + hostNumeric, true);

          Thread.sleep(10);
        }
      } catch (Exception exception) {
        throw new RuntimeException(exception);
      }
    }
  }

  /**
   * Tests AMBARI-9368 which produced a deadlock during read and writes of some
   * of the impl classes.
   */
  private static final class DeadlockExerciserThread extends Thread {
    private Cluster cluster;
    private Service service;
    private ServiceComponent namenodeComponent;
    private ServiceComponent hdfsClientComponent;
    private ServiceComponentHost namenodeSCH;
    private ServiceComponentHost hdfsClientSCH;

    /**
     * @param cluster
     *          the cluster to set
     */
    public void setCluster(Cluster cluster) {
      this.cluster = cluster;
    }

    /**
     * @param service
     *          the service to set
     */
    public void setService(Service service) {
      this.service = service;
    }

    /**
     * @param namenodeComponent
     *          the namenodeComponent to set
     */
    public void setNamenodeComponent(ServiceComponent namenodeComponent) {
      this.namenodeComponent = namenodeComponent;
    }

    /**
     * @param hdfsClientComponent
     *          the hdfsClientComponent to set
     */
    public void setHdfsClientComponent(ServiceComponent hdfsClientComponent) {
      this.hdfsClientComponent = hdfsClientComponent;
    }

    /**
     * @param namenodeSCH
     *          the namenodeSCH to set
     */
    public void setNamenodeSCH(ServiceComponentHost namenodeSCH) {
      this.namenodeSCH = namenodeSCH;
    }

    /**
     * @param hdfsClientSCH
     *          the hdfsClientSCH to set
     */
    public void setHdfsClientSCH(ServiceComponentHost hdfsClientSCH) {
      this.hdfsClientSCH = hdfsClientSCH;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
      try {
        for (int i = 0; i < 10; i++) {
          cluster.convertToResponse();
          service.convertToResponse();
          namenodeComponent.convertToResponse();
          hdfsClientComponent.convertToResponse();
          namenodeSCH.convertToResponse();
          hdfsClientSCH.convertToResponse();

          cluster.setProvisioningState(org.apache.ambari.server.state.State.INIT);
          service.setMaintenanceState(MaintenanceState.OFF);
          namenodeComponent.setDesiredState(org.apache.ambari.server.state.State.STARTED);
          hdfsClientComponent.setDesiredState(org.apache.ambari.server.state.State.INSTALLED);

          namenodeSCH.setState(org.apache.ambari.server.state.State.STARTED);
          hdfsClientSCH.setState(org.apache.ambari.server.state.State.INSTALLED);

          Thread.sleep(100);
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

  private ServiceComponentHost createNewServiceComponentHost(String svc,
      String svcComponent, String hostName, boolean isClient)
      throws AmbariException {
    Cluster c = clusters.getCluster("c1");
    Assert.assertNotNull(c.getConfigGroups());
    return createNewServiceComponentHost(c, svc, svcComponent, hostName);
  }

  private ServiceComponentHost createNewServiceComponentHost(Cluster c,
      String svc, String svcComponent, String hostName) throws AmbariException {

    Service s = null;

    try {
      s = c.getService(svc);
    } catch (ServiceNotFoundException e) {
      s = serviceFactory.createNew(c, svc);
      c.addService(s);
      s.persist();
    }

    ServiceComponent sc = null;
    try {
      sc = s.getServiceComponent(svcComponent);
    } catch (ServiceComponentNotFoundException e) {
      sc = serviceComponentFactory.createNew(s, svcComponent);
      s.addServiceComponent(sc);
      sc.persist();
    }

    ServiceComponentHost impl = serviceComponentHostFactory.createNew(sc,
        hostName);

    impl.persist();
    return impl;
  }
}
