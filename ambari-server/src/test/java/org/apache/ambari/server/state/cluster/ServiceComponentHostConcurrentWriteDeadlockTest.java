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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ServiceComponentNotFoundException;
import org.apache.ambari.server.ServiceNotFoundException;
import org.apache.ambari.server.events.listeners.upgrade.HostVersionOutOfSyncListener;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigFactory;
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
import org.apache.ambari.server.testing.DeadlockWarningThread;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
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
 * Tests AMBARI-12526 which produced a deadlock during concurrent writes of
 * service component host version and state.
 */
public class ServiceComponentHostConcurrentWriteDeadlockTest {
  private static final int NUMBER_OF_THREADS = 3;

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
  private ConfigFactory configFactory;

  @Inject
  private OrmTestHelper helper;

  private StackId stackId = new StackId("HDP-0.1");

  /**
   * The cluster.
   */
  private Cluster cluster;

  /**
   * Creates 1 host and add it to the cluster.
   *
   * @throws Exception
   */
  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(Modules.override(
        new InMemoryDefaultTestModule()).with(new MockModule()));

    injector.getInstance(GuiceJpaInitializer.class);
    injector.injectMembers(this);
    clusters.addCluster("c1", stackId);
    cluster = clusters.getCluster("c1");
    helper.getOrCreateRepositoryVersion(stackId, stackId.getStackVersion());
    cluster.createClusterVersion(stackId,
        stackId.getStackVersion(), "admin", RepositoryVersionState.INSTALLING);

    Config config1 = configFactory.createNew(cluster, "test-type1", null, new HashMap<String, String>(), new HashMap<String,
        Map<String, String>>());

    Config config2 = configFactory.createNew(cluster, "test-type2", null, new HashMap<String, String>(), new HashMap<String,
        Map<String, String>>());

    cluster.addDesiredConfig("test user", new HashSet<Config>(Arrays.asList(config1, config2)));

    String hostName = "c6401";
    clusters.addHost(hostName);
    setOsFamily(clusters.getHost(hostName), "redhat", "6.4");
    clusters.mapHostToCluster(hostName, "c1");

    Service service = installService("HDFS");
    addServiceComponent(service, "NAMENODE");
  }

  @After
  public void teardown() {
    injector.getInstance(PersistService.class).stop();
  }

  /**
   */
  @Test()
  public void testConcurrentWriteDeadlock() throws Exception {
    ServiceComponentHost nameNodeSCH = createNewServiceComponentHost("HDFS", "NAMENODE", "c6401");
    ServiceComponentHost dataNodeSCH = createNewServiceComponentHost("HDFS", "DATANODE", "c6401");

    List<ServiceComponentHost> serviceComponentHosts = new ArrayList<ServiceComponentHost>();
    serviceComponentHosts.add(nameNodeSCH);
    serviceComponentHosts.add(dataNodeSCH);

    List<Thread> threads = new ArrayList<Thread>();
    for (int i = 0; i < NUMBER_OF_THREADS; i++) {
      ServiceComponentHostDeadlockWriter thread = new ServiceComponentHostDeadlockWriter();
      thread.setServiceComponentHosts(serviceComponentHosts);
      thread.start();
      threads.add(thread);
    }

    DeadlockWarningThread wt = new DeadlockWarningThread(threads);

    while (true) {
      if(!wt.isAlive()) {
          break;
      }
    }

    if (wt.isDeadlocked()){
      Assert.assertFalse(wt.getErrorMessages().toString(), wt.isDeadlocked());
    } else {
      Assert.assertFalse(wt.isDeadlocked());
    }
  }


  /**
   * Tests AMBARI-12526 by constantly writing version and state to service
   * component hosts. The deadlock this is testing for occurred when different
   * rows were concurrently being updated by different threads and different
   * transactions.
   */
  private static final class ServiceComponentHostDeadlockWriter extends Thread {
    private List<ServiceComponentHost> serviceComponentHosts;

    /**
     * @param nameNodeSCH
     *          the nameNodeSCH to set
     */
    public void setServiceComponentHosts(List<ServiceComponentHost> serviceComponentHosts) {
      this.serviceComponentHosts = serviceComponentHosts;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
      try {
        for (int i = 0; i < 1000; i++) {
          org.apache.ambari.server.state.State state = (i % 2 == 0)
              ? org.apache.ambari.server.state.State.INSTALLING
              : org.apache.ambari.server.state.State.INSTALL_FAILED;

          String version = (i % 2 == 0) ? "UNKNOWN" : "2.2.0.0-1234";

          for (ServiceComponentHost serviceComponentHost : serviceComponentHosts) {
            serviceComponentHost.setState(state);
            serviceComponentHost.setVersion(version);
          }

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

    return sch;
  }

  private Service installService(String serviceName) throws AmbariException {
    Service service = null;

    try {
      service = cluster.getService(serviceName);
    } catch (ServiceNotFoundException e) {
      service = serviceFactory.createNew(cluster, serviceName);
      cluster.addService(service);
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
    }

    return serviceComponent;
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
