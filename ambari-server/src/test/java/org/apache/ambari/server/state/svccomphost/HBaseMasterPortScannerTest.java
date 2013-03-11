/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.ambari.server.state.svccomphost;

import com.google.inject.Guice;
import com.google.inject.Injector;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.ambari.server.AmbariException;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DATANODE;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DummyCluster;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.DummyOsType;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.HDFS;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.HBASE_MASTER;
import static org.apache.ambari.server.agent.DummyHeartbeatConstants.NAMENODE;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceFactory;
import org.apache.ambari.server.state.StackId;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author root
 */
public class HBaseMasterPortScannerTest {

  private static final Logger log = LoggerFactory.getLogger(HBaseMasterPortScannerTest.class);
  private static List<String> hostnames;
  private static ServerSocket serverSocket;
  private static Injector injector;
  private static HBaseMasterPortScanner scaner;
  private static ServiceFactory serviceFactory;
  private static AmbariMetaInfo metaInfo;
  private static Clusters clusters;
  private static Cluster cluster;
  private static Host host;
  private static ServiceComponentHost serviceComponentHost;

  public HBaseMasterPortScannerTest() {
  }

  @BeforeClass
  public static void setUpClass() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    hostnames = new ArrayList<String>();
    hostnames.add("localhost");
    hostnames.add("localhost1");
    hostnames.add("localhost2");
    hostnames.add("localhost3");
    try {
      serverSocket = new ServerSocket(60010);
    } catch (IOException e) {
      try {
        serverSocket.close();
      } catch (IOException ex) {
        log.debug("Could not close on port: 60010");
        log.error(ex.getMessage());
      }
      log.error("Could not listen on port: 60010");
    }
    scaner = injector.getInstance(HBaseMasterPortScanner.class);
    clusters = injector.getInstance(Clusters.class);
    metaInfo = injector.getInstance(AmbariMetaInfo.class);
    serviceFactory = injector.getInstance(ServiceFactory.class);
    metaInfo.init();
    clusters.addCluster(DummyCluster);
    cluster = clusters.getCluster(DummyCluster);
    cluster.setDesiredStackVersion(new StackId("HDP-0.2"));
    Set<String> hostNamesSet = new HashSet<String>();
    for (String hostname : hostnames) {
      clusters.addHost(hostname);
      clusters.getHost(hostname).persist();
      Host hostObject = clusters.getHost(hostname);
      hostObject.setIPv4("ipv4");
      hostObject.setIPv6("ipv6");
      hostObject.setOsType(DummyOsType);
      hostNamesSet.add(hostname);
      if (hostname.equals("localhost")) {
        host = hostObject;
      }
    }
    clusters.mapHostsToCluster(hostNamesSet, DummyCluster);
    Service service = cluster.addService(HDFS);
    service.persist();
    service.addServiceComponent(NAMENODE).persist();
    service.getServiceComponent(NAMENODE).addServiceComponentHost("localhost").persist();
    service.addServiceComponent(DATANODE).persist();
    service.getServiceComponent(DATANODE).addServiceComponentHost("localhost").persist();
    service = serviceFactory.createNew(cluster, "HBASE");
    cluster.addService(service);
    service.persist();
    service = cluster.getService("HBASE");
    service.addServiceComponent(HBASE_MASTER).persist();
    service.persist();
    for (String hostname : hostnames) {
      service.getServiceComponent(HBASE_MASTER).addServiceComponentHost(hostname).persist();
      if (hostname.equals("localhost")) {
        serviceComponentHost = service.getServiceComponent(HBASE_MASTER).getServiceComponentHost(hostname);
      }
    }

  }

  @AfterClass
  public static void tearDownUpClass() {
    try {
      serverSocket.close();
    } catch (IOException ex) {
      log.debug("Could not close on port: 60010");
      log.error(ex.getMessage());
    }
  }

  @Before
  public void setUp() throws AmbariException, Exception {
    serviceComponentHost.convertToResponse().setHa_status("passive");
  }

  /**
   * Test of updateHBaseMaster method, of class HBaseMasterPortScaner.
   */
  @Test
  public void testUpdateHBaseMaster_Cluster() throws InterruptedException {
    log.debug("updateHBaseMaster - pass Cluster");
    scaner.updateHBaseMaster(cluster);
    Thread.sleep(2000);
    assertEquals("active", serviceComponentHost.convertToResponse().getHa_status());
  }

  /**
   * Test of updateHBaseMaster method, of class HBaseMasterPortScaner.
   */
  @Test
  public void testUpdateHBaseMaster_Host() throws InterruptedException {
    log.debug("updateHBaseMaster - pass Host");
    scaner.updateHBaseMaster(host);
    Thread.sleep(2000);
    assertEquals("active", serviceComponentHost.convertToResponse().getHa_status());
  }

  /**
   * Test of updateHBaseMaster method, of class HBaseMasterPortScaner.
   */
  @Test
  public void testUpdateHBaseMaster_ServiceComponentHost() throws InterruptedException {
    log.debug("updateHBaseMaster - pass ServiceComponentHost");
    scaner.updateHBaseMaster(serviceComponentHost);
    Thread.sleep(2000);
    assertEquals("active", serviceComponentHost.convertToResponse().getHa_status());
  }
}