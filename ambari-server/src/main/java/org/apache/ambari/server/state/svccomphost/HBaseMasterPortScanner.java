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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class encapsulates the HBaseMaster scanner thread. HBaseMaster scanner
 * start scan if Host, ServiceComponentHost or Cluster change own state.
 */
@Singleton
public class HBaseMasterPortScanner implements Runnable {

  private static Log LOG = LogFactory.getLog(HBaseMasterPortScanner.class);
  private Thread schedulerThread = null;
  private final Object wakeupSyncObject = new Object();
  private int scanTimeoutMsc = 300;
  private final int port = 60010;
  private Set<ServiceComponentHost> componentHostSet;
  @Inject
  private Clusters clusters;
  /**
   * true if scanner should run ASAP. We need this flag to avoid sleep in
   * situations, when we receive updateHBaseMaster request during running a
   * scanner iteration.
   */
  private boolean activeAwakeRequest = false;

  public HBaseMasterPortScanner(int scanTimeoutMsc) {
    this.scanTimeoutMsc = scanTimeoutMsc;
    this.start();
  }

  public HBaseMasterPortScanner() {
     this.start();
  }
  
  private void start() {
    schedulerThread = new Thread(this, this.getClass().getSimpleName());
    schedulerThread.start();
    if (LOG.isDebugEnabled()) {
      LOG.debug("HBaseMasterPortScaner started");
    }
  }

  public void stop() {
    schedulerThread.interrupt();
  }

  /**
   * Should be called from another thread when we want HBase Master scanner to
   * make a run ASAP (for example, to process desired configs of SCHs). The
   * method is guaranteed to return quickly.
   */
  public void updateHBaseMaster(Cluster cluster) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("HBaseMasterPortScaner start scanning for cluster " + cluster.getClusterName());
    }
    synchronized (wakeupSyncObject) {
      collectServiceComponentHostsForCluster(cluster);
      activeAwakeRequest = true;
      wakeupSyncObject.notify();
    }
  }

  public void updateHBaseMaster(Host host) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("HBaseMasterPortScaner start scanning for Host " + host.getHostName());
    }
    synchronized (wakeupSyncObject) {
      Set<Cluster> clustersSet;
      try {
        clustersSet = clusters.getClustersForHost(host.getHostName());
      } catch (AmbariException ex) {
        return;
      }
      Iterator<Cluster> iter = clustersSet.iterator();
      while (iter.hasNext()) {
        collectServiceComponentHostsForCluster(iter.next());
      }
      activeAwakeRequest = true;
      wakeupSyncObject.notify();
    }
  }

  public void updateHBaseMaster(ServiceComponentHost host) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("HBaseMasterPortScaner start scanning for ServiceComponentHost " + host.getServiceComponentName());
    }
    synchronized (wakeupSyncObject) {
      try {
        collectServiceComponentHostsForCluster(clusters.getCluster(host.getClusterName()));
      } catch (AmbariException ex) {
        LOG.warn(ex);
        return;
      }
      activeAwakeRequest = true;
      wakeupSyncObject.notify();
    }
  }

  private void collectServiceComponentHostsForCluster(Cluster cluster) {
    componentHostSet = new HashSet<ServiceComponentHost>();
    Map<String, Host> hosts = null;
    try {
      hosts = clusters.getHostsForCluster(cluster.getClusterName());
    } catch (AmbariException ex) {
      LOG.warn(ex);
      return;
    }
    for (Map.Entry<String, Host> entry : hosts.entrySet()) {
      if (entry.getValue() != null) {
        List<ServiceComponentHost> componentHosts = cluster.getServiceComponentHosts(entry.getValue().getHostName());
        for (ServiceComponentHost componentHost : componentHosts) {
          if (componentHost != null && componentHost.getServiceComponentName() != null && componentHost.getServiceComponentName().equals(Role.HBASE_MASTER.toString())) {
            componentHostSet.add(componentHost);
          }
        }
      }
    }

  }

  @Override
  public void run() {
    while (true) {
      activeAwakeRequest = false;
      if (componentHostSet != null) {
        Iterator<ServiceComponentHost> iter = componentHostSet.iterator();
        while (iter.hasNext()) {
          ServiceComponentHost componentHost = iter.next();
          boolean active =
                  scan(componentHost.getHostName());
          componentHost.setHAState((active) ? "active" : "passive");

          if (schedulerThread.isInterrupted()) {
            return;
          }
          if (activeAwakeRequest) {
            break;
          }
        }
      }
      if (activeAwakeRequest) {
        activeAwakeRequest = false;
        continue;
      }
      try {
        synchronized (wakeupSyncObject) {
          wakeupSyncObject.wait();
        }
      } catch (InterruptedException ex) {
        activeAwakeRequest = true;
      }
    }
  }

  private boolean scan(String hostname) {
    try {
      Socket socket = new Socket();
      socket.connect(new InetSocketAddress(hostname, port), scanTimeoutMsc);
      socket.close();
      LOG.info(hostname + ":" + port + " HBASE_MASTER active");
      return true;
    } catch (ConnectException e) {
      LOG.info(hostname + ":" + port + " HBASE_MASTER passive");
      return false;
    } catch (Exception ex) {
      LOG.info(hostname + ":" + port + " HBASE_MASTER passive");
      LOG.error(ex);
      return false;
    }
  }
}
