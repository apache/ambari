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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
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
  private int defaultScanTimeoutMsc = 300;
  private int scanTimeoutMsc = defaultScanTimeoutMsc;
  private int testScanTimeoutMsc;
  private int rescanTimeoutMsc = 60000;
  private final int port = 60010;
  private int maxAttempts = 3;
  private int attempts = 0;
  private int countAttempts = 0;
  private Map<ServiceComponentHost,Boolean> componentHostMap;
  private Cluster currentCluster;
  private Timer scheduleTimer;
  private RescanSchedulerTask rescanSchedulerTask;
  @Inject
  private Clusters clusters;

  /**
   * 
   * @param defaultScanTimeoutMsc set default timeout for port scan
   */
  public void setDefaultScanTimeoutMsc(int defaultScanTimeoutMsc) {
    this.defaultScanTimeoutMsc = defaultScanTimeoutMsc;
    this.scanTimeoutMsc = this.defaultScanTimeoutMsc;
  }


  /**
   * 
   * @param maxAttempts set maximum attempts to scan
   */
  public void setMaxAttempts(int maxAttempts) {
    this.maxAttempts = maxAttempts;
  }

  /**
   * 
   * @param rescanTimeoutMsc timeout for latter rescan
   */
  public void setRescanTimeoutMsc(int rescanTimeoutMsc) {
    this.rescanTimeoutMsc = rescanTimeoutMsc;
  }

  /**
   * 
   * @return tested value (need unitests)
   */
  public int getTestScanTimeoutMsc() {
    return testScanTimeoutMsc;
  }

  /**
   * 
   * @return count attempts (need unitests)
   */
  public int getCountAttempts() {
    return countAttempts;
  }

  /**
   * 
   * @return task for latter scan
   */
  public RescanSchedulerTask getRescanSchedulerTask() {
    return rescanSchedulerTask;
  }
  
  
  /**
   * true if scanner should run ASAP. We need this flag to avoid sleep in
   * situations, when we receive updateHBaseMaster request during running a
   * scanner iteration.
   */
  private boolean activeAwakeRequest = false;

  public HBaseMasterPortScanner(int scanTimeoutMsc) {
    this.defaultScanTimeoutMsc = scanTimeoutMsc;
    this.scanTimeoutMsc = scanTimeoutMsc;
    this.start();
  }

  public HBaseMasterPortScanner() {
     scheduleTimer = new Timer();
     this.start();
  }
  
  private void start() {
    schedulerThread = new Thread(this, this.getClass().getSimpleName());
    schedulerThread.start();
    if (LOG.isDebugEnabled()) {
      LOG.debug("HBaseMasterPortScanner started");
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
    synchronized (wakeupSyncObject) {
      collectServiceComponentHostsForCluster(cluster);
      if(componentHostMap!=null && !componentHostMap.isEmpty()){
        LOG.debug("HBaseMasterPortScanner start scanning for cluster " + cluster.getClusterName());
        activeAwakeRequest = true;
        wakeupSyncObject.notify();
      } else LOG.debug("No for scan (with HBaseMaster component)");
    }
  }

  public void updateHBaseMaster(Host host) {
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
      if(componentHostMap!=null && !componentHostMap.isEmpty()){
        LOG.debug("HBaseMasterPortScanner start scanning for Host " + host.getHostName());
        activeAwakeRequest = true;
        wakeupSyncObject.notify();
      } else LOG.debug("No for scan (with HBaseMaster component)");
    }
  }

  public void updateHBaseMaster(ServiceComponentHost host) {
    synchronized (wakeupSyncObject) {
      try {
        collectServiceComponentHostsForCluster(clusters.getCluster(host.getClusterName()));
      } catch (AmbariException ex) {
        LOG.warn(ex);
        return;
      }
      if(componentHostMap!=null && !componentHostMap.isEmpty()){
        LOG.debug("HBaseMasterPortScanner start scanning for ServiceComponentHost " + host.getServiceComponentName());
        activeAwakeRequest = true;
        wakeupSyncObject.notify();
      } else LOG.debug("No for scan (with HBaseMaster component)");
    }
  }

  private void collectServiceComponentHostsForCluster(Cluster cluster) {
    currentCluster = cluster;
    componentHostMap = new HashMap<ServiceComponentHost, Boolean>();
    Map<String, Host> hosts = null;
    try {
      hosts = clusters.getHostsForCluster(currentCluster.getClusterName());
    } catch (AmbariException ex) {
      LOG.warn(ex);
      return;
    }
    for (Map.Entry<String, Host> entry : hosts.entrySet()) {
      if (entry.getValue() != null) {
        List<ServiceComponentHost> componentHosts = currentCluster.getServiceComponentHosts(entry.getValue().getHostName());
        for (ServiceComponentHost componentHost : componentHosts) {
          if (componentHost != null && componentHost.getServiceComponentName() != null && componentHost.getServiceComponentName().equals(Role.HBASE_MASTER.toString())) {
            componentHostMap.put(componentHost, false);
          }
        }
      }
    }

  }

  @Override
  public void run() {
    while (true) {
      if(rescanSchedulerTask != null){
        rescanSchedulerTask.cancel();
        scheduleTimer.purge();
      }          
      activeAwakeRequest = false;
      if (componentHostMap != null) {
        for (Map.Entry<ServiceComponentHost, Boolean> entry : componentHostMap.entrySet()) {
          entry.setValue(scan(entry.getKey().getHostName()));
          if (schedulerThread.isInterrupted()) {
            scanTimeoutMsc = defaultScanTimeoutMsc;
            return;
          }
          if (activeAwakeRequest) {
            scanTimeoutMsc = defaultScanTimeoutMsc;
            attempts = 0;
            break;
          }
        }
        attempts++;
        countAttempts = attempts;
        LOG.info("Attempt to scan of HBASE_MASTER port : "+ attempts);
        if(validateScanResults(componentHostMap)){
          //If results valid set it to ServiceComponentHost
          setScanResults(componentHostMap);
          scanTimeoutMsc = defaultScanTimeoutMsc;
          attempts = 0;
        } else {
          if(attempts <= maxAttempts){
            //Increase timeout
            scanTimeoutMsc += defaultScanTimeoutMsc;
            testScanTimeoutMsc = scanTimeoutMsc;
            LOG.info("Increase timeout for scan HBASE_MASTER port to : "+ scanTimeoutMsc);
            activeAwakeRequest = true;
          } else {
            LOG.info("No valid data about HBASE_MASTER, ports will rescanned after "+rescanTimeoutMsc/1000 + " seconds");
            scanTimeoutMsc = defaultScanTimeoutMsc;
            attempts = 0;
            //Create task for latter scan
            rescanSchedulerTask = new RescanSchedulerTask(currentCluster);
            scheduleTimer.schedule(rescanSchedulerTask, rescanTimeoutMsc);
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

  private void setScanResults(Map<ServiceComponentHost, Boolean> scanResuls){
    for (Map.Entry<ServiceComponentHost, Boolean> entry : scanResuls.entrySet()) {
      entry.getKey().setHAState((entry.getValue()) ? "active" : "passive");
    }
    LOG.info("Set result of HBASE_MASTER scan");
  }
  
  private boolean validateScanResults(Map<ServiceComponentHost, Boolean> scanResuls){
    boolean res = false;
    int activeMasters = 0;
    for (Map.Entry<ServiceComponentHost, Boolean> entry : scanResuls.entrySet()) {
      activeMasters += (entry.getValue()) ? 1 : 0;
    }
    if(activeMasters == 0 || activeMasters > 1) {
      res = false;
    }
    else {
      res = true;
    } 
    LOG.info("Results of HBASE_MASTER scan are "+ ((res) ? "valid" : "invalid"));
    return res;  
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
  
  private class RescanSchedulerTask  extends TimerTask  {

    private Cluster cl;

    public RescanSchedulerTask(Cluster cl) {
      this.cl = cl;
    }
    
    @Override
    public void run() {
      LOG.info("Start scheduled rescan of HBASE_MASTER ports for cluster "+ cl.getClusterName());
      updateHBaseMaster(cl);
    }
    
  }
   
}
