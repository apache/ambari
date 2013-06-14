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


import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
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
  private static Injector injector;
  private static HBaseMasterPortScannerMock scaner;
  private static ServiceFactory serviceFactory;
  private static AmbariMetaInfo metaInfo;
  private static Clusters clusters;
  private static Cluster cluster;
  private static Host host;
  private static ServiceComponentHost serviceComponentHost;
  private static int scanTimeOut = 100; 
  private static int reScanTimeOut = 1000;
  private static int maxAttempts = 2;
  private static Timer timerMock = mock(Timer.class);

  public HBaseMasterPortScannerTest() {
  }
  
  @BeforeClass
  public static void setUpClass() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    hostnames = new ArrayList<String>();
    hostnames.add("127.0.0.1");
    hostnames.add("host1");
    hostnames.add("host2");
    hostnames.add("host3");
    clusters = injector.getInstance(Clusters.class);
    scaner = new HBaseMasterPortScannerMock(clusters);
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
      if (hostname.equals("127.0.0.1")) {
        host = hostObject;
      }
    }
    clusters.mapHostsToCluster(hostNamesSet, DummyCluster);
    Service service = cluster.addService(HDFS);
    service.persist();
    service.addServiceComponent(NAMENODE).persist();
    service.getServiceComponent(NAMENODE).addServiceComponentHost("127.0.0.1").persist();
    service.addServiceComponent(DATANODE).persist();
    service.getServiceComponent(DATANODE).addServiceComponentHost("127.0.0.1").persist();
    service = serviceFactory.createNew(cluster, "HBASE");
    cluster.addService(service);
    service.persist();
    service = cluster.getService("HBASE");
    service.addServiceComponent(HBASE_MASTER).persist();
    service.persist();
    for (String hostname : hostnames) {
      service.getServiceComponent(HBASE_MASTER).addServiceComponentHost(hostname).persist();
      if (hostname.equals("127.0.0.1")) {
        serviceComponentHost = service.getServiceComponent(HBASE_MASTER).getServiceComponentHost(hostname);
      }
    }
    when(timerMock.purge()).thenReturn(0);
  }


  @Before
  public void setUp() throws AmbariException, Exception {
    serviceComponentHost.setHAState("passive");
  }

  /**
   * Test of updateHBaseMaster method, of class HBaseMasterPortScaner.
   */
  @Test
  public void testUpdateHBaseMaster_Cluster() throws InterruptedException {
    scaner.setDefaultScanTimeoutMsc(scanTimeOut);
    scaner.setMaxAttempts(maxAttempts);
    scaner.setRescanTimeoutMsc(reScanTimeOut);
    log.debug("updateHBaseMaster - pass Cluster");
    scaner.updateHBaseMaster(cluster);
    scaner.execute();
    assertEquals("active", serviceComponentHost.convertToResponse().getHa_status());
  }

  /**
   * Test of updateHBaseMaster method, of class HBaseMasterPortScaner.
   */
  @Test
  public void testUpdateHBaseMaster_Host() throws InterruptedException {
    scaner.setDefaultScanTimeoutMsc(scanTimeOut);
    scaner.setMaxAttempts(maxAttempts);
    scaner.setRescanTimeoutMsc(reScanTimeOut);
    log.debug("updateHBaseMaster - pass Host");
    scaner.updateHBaseMaster(host);
    scaner.execute();
    assertEquals("active", serviceComponentHost.convertToResponse().getHa_status());
  }

  /**
   * Test of updateHBaseMaster method, of class HBaseMasterPortScaner.
   */
  @Test
  public void testUpdateHBaseMaster_ServiceComponentHost() throws InterruptedException {
    scaner.setDefaultScanTimeoutMsc(scanTimeOut);
    scaner.setMaxAttempts(maxAttempts);
    scaner.setRescanTimeoutMsc(reScanTimeOut);    
    log.debug("updateHBaseMaster - pass ServiceComponentHost");
    scaner.updateHBaseMaster(serviceComponentHost);
    scaner.execute();
    assertEquals("active", serviceComponentHost.convertToResponse().getHa_status());
  }


  /**
   * Test case of if port is closed or not enough scan timeout.
   */
  @Test
  public void testOfBrokenMasterScenario() throws InterruptedException {
    scaner.setLiveHBaseHost("");
    scaner.setDefaultScanTimeoutMsc(scanTimeOut);
    scaner.setMaxAttempts(maxAttempts);
    scaner.setRescanTimeoutMsc(reScanTimeOut);
    log.debug("testOfBrokenMasterScenario start");
    scaner.updateHBaseMaster(cluster);
    scaner.execute(3);
    //Should not be active masters
    assertEquals("passive", serviceComponentHost.convertToResponse().getHa_status());
    serviceComponentHost.setHAState("passive");
    //Scanner should try to scan maxAttempts times
    assertEquals(maxAttempts, scaner.getCountAttempts()-1);
    //Timeout for scan should be scanTimeOut * scaner.getCountAttempts()
    assertEquals(scanTimeOut * scaner.getCountAttempts(), scaner.getTestScanTimeoutMsc());
    //Task for latter scan shoul be created
    assertNotNull(scaner.getRescanSchedulerTask());
    scaner.setLiveHBaseHost("127.0.0.1");
    scaner.execute(3);
    //Test active masters after latter rescan
    assertEquals("active", serviceComponentHost.convertToResponse().getHa_status());
  }
  

  public static class HBaseMasterPortScannerMock extends HBaseMasterPortScanner {

    private String liveHBaseHost = "127.0.0.1";

    public void setLiveHBaseHost(String liveHBaseHost) {
      this.liveHBaseHost = liveHBaseHost;
    }
    
    @Override
    protected boolean scan(String hostname) {
      return (hostname.equals(liveHBaseHost)) ? true : false;
    }

    public int getCountAttempts() {
      return countAttempts;
    }

        
    public HBaseMasterPortScannerMock(Clusters c) {
      super(timerMock);
      clusters = c;
    }

    @Override
    public void execute() {
      super.execute();
    }
    
    public void execute(int count) {
      for (int i = 0; i < count; i++) {
        execute();
      }
    }    
    
  }
  
}