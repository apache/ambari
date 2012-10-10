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
package org.apache.ambari.server.controller;


import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestInjectorForTest implements Runnable {

  private AmbariManagementController controller;
  private static final Logger LOG =
      LoggerFactory.getLogger(RequestInjectorForTest.class);
  private Clusters clusters;
  
  public RequestInjectorForTest(AmbariManagementController controller,
      Clusters clusters) {
    this.controller = controller;
    this.clusters = clusters;
  }
  
  @Override
  public void run() {
    int counter = 0;
    while (true) {
      try {
        sendAction(++counter);
        Thread.sleep(60000);
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (AmbariException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private void createCluster(String clusterName) throws AmbariException {
    ClusterRequest r = new ClusterRequest(null, clusterName, "1.0.0", null);
    controller.createCluster(r);
  }

  private void createService(String clusterName,
      String serviceName) throws AmbariException {
    ServiceRequest r = new ServiceRequest(clusterName, serviceName, null,
        State.INIT.toString());
    controller.createService(r);
  }

  private void createServiceComponent(String clusterName,
      String serviceName, String componentName)
          throws AmbariException {
    ServiceComponentRequest r = new ServiceComponentRequest(clusterName,
        serviceName, componentName, null, State.INIT.toString());
    controller.createComponent(r);
  }

  private void createServiceComponentHost(String clusterName,
      String serviceName, String componentName, String hostname) throws AmbariException {
    ServiceComponentHostRequest r = new ServiceComponentHostRequest(clusterName,
        serviceName, componentName, hostname, null, State.INIT.toString());
    controller.createHostComponent(r);
  }
  
  
  private void sendAction(int clusterId) throws AmbariException, UnknownHostException {
    String clusterName = "foo" + clusterId;
    createCluster(clusterName);
    LOG.info("Created cluster " + clusterName);
    
    String serviceName = "HDFS";
    createService(clusterName, serviceName);
    LOG.info("Created service " + serviceName);
    
    String componentName1 = "NAMENODE";
    String componentName2 = "DATANODE";
    createServiceComponent(clusterName, serviceName, componentName1);
    LOG.info("Created ServiceComponent " + componentName1);
    createServiceComponent(clusterName, serviceName, componentName2);
    LOG.info("Created ServiceComponent " + componentName2);
    
    String host1 = InetAddress.getLocalHost().getHostName();
    createServiceComponentHost(clusterName, serviceName, componentName1,
        host1);
    LOG.info("Created ServiceComponentHost " + componentName1 + " " + host1);
    createServiceComponentHost(clusterName, serviceName, componentName2,
        host1);
    LOG.info("Created ServiceComponentHost " + componentName2 + " " + host1);

    ServiceRequest r1 = new ServiceRequest(clusterName, serviceName, null,
        State.INSTALLED.toString());

    StringBuilder sb = new StringBuilder();
    clusters.debugDump(sb);
    LOG.info("Dump current cluster state: \n" + sb.toString());
    
    LOG.info("***** Trying to install service now *******");
    controller.updateService(r1);

    sb = new StringBuilder();
    clusters.debugDump(sb);
    LOG.info("Dump current cluster state: \n" + sb.toString());
  }

}
