/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.metrics2.sink.timeline.availability;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.curator.CuratorZookeeperClient;
import org.apache.curator.RetryLoop;
import org.apache.curator.RetryPolicy;
import org.apache.curator.retry.BoundedExponentialBackoffRetry;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * Find a live Collector instance from Zookeeper
 * This class allows connect to ZK on-demand and
 * does not add a watcher on the znode.
 */
public class MetricCollectorHAHelper {
  private final String zookeeperConnectionURL;
  private final int tryCount;
  private final int sleepMsBetweenRetries;

  private static final int CONNECTION_TIMEOUT = 2000;
  private static final int SESSION_TIMEOUT = 10000;
  private static final String ZNODE = "/ambari-metrics-cluster";
  private static final String ZK_PATH = ZNODE + "/LIVEINSTANCES";
  private static final String INSTANCE_NAME_DELIMITER = "_";



  private static final Log LOG = LogFactory.getLog(MetricCollectorHAHelper.class);

  public MetricCollectorHAHelper(String zookeeperConnectionURL, int tryCount, int sleepMsBetweenRetries) {
    this.zookeeperConnectionURL = zookeeperConnectionURL;
    this.tryCount = tryCount;
    this.sleepMsBetweenRetries = sleepMsBetweenRetries;
  }

  /**
   * Connect to Zookeeper to find live instances of metrics collector
   * @return {#link Collection} hostnames
   */
  public Collection<String> findLiveCollectorHostsFromZNode() {
    Set<String> collectors = new HashSet<>();

    RetryPolicy retryPolicy = new BoundedExponentialBackoffRetry(sleepMsBetweenRetries, 10*sleepMsBetweenRetries, tryCount);
    final CuratorZookeeperClient client = new CuratorZookeeperClient(zookeeperConnectionURL,
      SESSION_TIMEOUT, CONNECTION_TIMEOUT, null, retryPolicy);

    List<String> liveInstances = null;

    try {
      client.start();
      //Check if Znode exists
      Stat stat = client.getZooKeeper().exists(ZNODE, false);
      if (stat == null) {
        LOG.info("/ambari-metrics-cluster znode does not exist. Skipping requesting live instances from zookeeper");
        return collectors;
      }
      liveInstances = RetryLoop.callWithRetry(client, new Callable<List<String>>() {
        @Override
        public List<String> call() throws Exception {
          ZooKeeper zookeeper = client.getZooKeeper();
          return zookeeper.getChildren(ZK_PATH, false);
        }
      });
    } catch (Exception e) {
      LOG.warn("Unable to connect to zookeeper.", e);
      LOG.debug(e);
    } finally {
      try {
        client.close();
      } catch (Exception e) {
        LOG.error("Caught exception while trying to close Zk connection.",e);
      }
    }

    // [ambari-sid-3.c.pramod-thangali.internal_12001]
    if (liveInstances != null && !liveInstances.isEmpty()) {
      for (String instanceStr : liveInstances) {
        collectors.add(instanceStr.substring(0, instanceStr.indexOf(INSTANCE_NAME_DELIMITER)));
      }
    }

    return collectors;
  }
}
