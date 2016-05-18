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
package org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.availability;

import com.google.common.base.Joiner;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.MetricsSystemInitializationException;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.TimelineMetricConfiguration;
import org.apache.helix.HelixAdmin;
import org.apache.helix.HelixManager;
import org.apache.helix.HelixManagerFactory;
import org.apache.helix.InstanceType;
import org.apache.helix.NotificationContext;
import org.apache.helix.controller.GenericHelixController;
import org.apache.helix.manager.zk.ZKHelixAdmin;
import org.apache.helix.model.ExternalView;
import org.apache.helix.model.InstanceConfig;
import org.apache.helix.model.LiveInstance;
import org.apache.helix.model.OnlineOfflineSMD;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.apache.helix.model.IdealState.RebalanceMode.FULL_AUTO;

public class TimelineMetricHAController {
  private static final Log LOG = LogFactory.getLog(TimelineMetricHAController.class);

  static final String CLUSTER_NAME = "ambari-metrics-cluster";
  static final String METRIC_AGGREGATORS = "METRIC_AGGREGATORS";
  static final String STATE_MODEL_NAME = OnlineOfflineSMD.name;
  static final String INSTANCE_NAME_DELIMITER = "_";

  final String zkConnectUrl;
  final String instanceHostname;
  final InstanceConfig instanceConfig;
  final AggregationTaskRunner aggregationTaskRunner;

  // Cache list of known live instances
  final List<String> liveInstanceNames = new ArrayList<>();

  // Helix Admin
  HelixAdmin admin;
  // Helix Manager
  HelixManager manager;

  private volatile boolean isInitialized = false;

  public TimelineMetricHAController(TimelineMetricConfiguration configuration) {
    String instancePort;
    try {
      instanceHostname = configuration.getInstanceHostnameFromEnv();
      instancePort = configuration.getInstancePort();

    } catch (Exception e) {
      LOG.error("Error reading configs from classpath, will resort to defaults.", e);
      throw new MetricsSystemInitializationException(e.getMessage());
    }

    try {
      String zkClientPort = configuration.getZKClientPort();
      String zkQuorum = configuration.getZKQuorum();

      if (StringUtils.isEmpty(zkClientPort) || StringUtils.isEmpty(zkQuorum)) {
        throw new Exception("Unable to parse zookeeper quorum. clientPort = "
          + zkClientPort +", quorum = " + zkQuorum);
      }

      zkConnectUrl = getZkConnectionUrl(zkClientPort, zkQuorum);

    } catch (Exception e) {
      LOG.error("Unable to load hbase-site from classpath.", e);
      throw new MetricsSystemInitializationException(e.getMessage());
    }

    instanceConfig = new InstanceConfig(instanceHostname + INSTANCE_NAME_DELIMITER + instancePort);
    instanceConfig.setHostName(instanceHostname);
    instanceConfig.setPort(instancePort);
    instanceConfig.setInstanceEnabled(true);
    aggregationTaskRunner = new AggregationTaskRunner(instanceConfig.getInstanceName(), zkConnectUrl);
  }

  /**
   * Initialize the instance with zookeeper via Helix
   */
  public void initializeHAController() throws Exception {
    admin = new ZKHelixAdmin(zkConnectUrl);
    // create cluster
    LOG.info("Creating zookeeper cluster node: " + CLUSTER_NAME);
    admin.addCluster(CLUSTER_NAME, false);

    // Adding host to the cluster
    List<String> nodes = admin.getInstancesInCluster(CLUSTER_NAME);
    if (nodes == null || !nodes.contains(instanceConfig.getInstanceName())) {
      LOG.info("Adding participant instance " + instanceConfig);
      admin.addInstance(CLUSTER_NAME, instanceConfig);
    }

    // Add a state model
    if (admin.getStateModelDef(CLUSTER_NAME, STATE_MODEL_NAME) == null) {
      LOG.info("Adding ONLINE-OFFLINE state model to the cluster");
      admin.addStateModelDef(CLUSTER_NAME, STATE_MODEL_NAME, OnlineOfflineSMD.build());
    }

    // Add resources with 1 cluster-wide replica
    // Since our aggregators are unbalanced in terms of work distribution we
    // only need to distribute writes to METRIC_AGGREGATE and
    // METRIC_RECORD_MINUTE
    List<String> resources = admin.getResourcesInCluster(CLUSTER_NAME);
    if (!resources.contains(METRIC_AGGREGATORS)) {
      LOG.info("Adding resource " + METRIC_AGGREGATORS + " with 2 partitions and 1 replicas");
      admin.addResource(CLUSTER_NAME, METRIC_AGGREGATORS, 2, OnlineOfflineSMD.name, FULL_AUTO.toString());
    }
    // this will set up the ideal state, it calculates the preference list for
    // each partition similar to consistent hashing
    admin.rebalance(CLUSTER_NAME, METRIC_AGGREGATORS, 1);

    // Start participant
    startAggregators();

    // Start controller
    startController();

    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        aggregationTaskRunner.stop();
        manager.disconnect();
      }
    });

    isInitialized = true;
  }

  /**
   * Return true if HA controller is enabled.
   */
  public boolean isInitialized() {
    return isInitialized;
  }

  private void startAggregators() {
    try {
      aggregationTaskRunner.initialize();

    } catch (Exception e) {
      LOG.error("Unable to start aggregators.", e);
      throw new MetricsSystemInitializationException(e.getMessage());
    }
  }

  private void startController() throws Exception {
    manager = HelixManagerFactory.getZKHelixManager(
      CLUSTER_NAME,
      instanceHostname,
      InstanceType.CONTROLLER,
      zkConnectUrl
    );

    manager.connect();
    HelixController controller = new HelixController();
    manager.addLiveInstanceChangeListener(controller);
  }

  private String getZkConnectionUrl(String zkClientPort, String zkQuorum) {
    StringBuilder sb = new StringBuilder();
    String[] quorumParts = zkQuorum.split(",");
    String prefix = "";
    for (String part : quorumParts) {
      sb.append(prefix);
      sb.append(part.trim());
      if (!part.contains(":")) {
        sb.append(":");
        sb.append(zkClientPort);
      }
      prefix = ",";
    }

    return sb.toString();
  }

  public AggregationTaskRunner getAggregationTaskRunner() {
    return aggregationTaskRunner;
  }

  public List<String> getLiveInstanceHostNames() {
    List<String> liveInstanceHostNames = new ArrayList<>();

    for (String instance : liveInstanceNames) {
      liveInstanceHostNames.add(instance.split(INSTANCE_NAME_DELIMITER)[0]);
    }

    return liveInstanceHostNames;
  }

  public class HelixController extends GenericHelixController {
    ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    Joiner joiner = Joiner.on(", ").skipNulls();

    @Override
    public void onLiveInstanceChange(List<LiveInstance> liveInstances, NotificationContext changeContext) {
      super.onLiveInstanceChange(liveInstances, changeContext);

      liveInstanceNames.clear();
      for (LiveInstance instance : liveInstances) {
        liveInstanceNames.add(instance.getInstanceName());
      }

      LOG.info("Detected change in liveliness of Collector instances. " +
        "LiveIsntances = " + joiner.join(liveInstanceNames));
      // Print HA state - after some delay
      executorService.schedule(new Runnable() {
        @Override
        public void run() {
          printClusterState();
        }
      }, 30, TimeUnit.SECONDS);


    }
  }

  public void printClusterState() {
    StringBuilder sb = new StringBuilder("\n######################### Cluster HA state ########################");

    ExternalView resourceExternalView = admin.getResourceExternalView(CLUSTER_NAME, METRIC_AGGREGATORS);
    if (resourceExternalView != null) {
      getPrintableResourceState(resourceExternalView, METRIC_AGGREGATORS, sb);
    }
    sb.append("\n##################################################");
    LOG.info(sb.toString());
  }

  private void getPrintableResourceState(ExternalView resourceExternalView,
                                         String resourceName,
                                         StringBuilder sb) {
    TreeSet<String> sortedSet = new TreeSet<>(resourceExternalView.getPartitionSet());
    sb.append("\nCLUSTER: ");
    sb.append(CLUSTER_NAME);
    sb.append("\nRESOURCE: ");
    sb.append(resourceName);
    for (String partitionName : sortedSet) {
      sb.append("\nPARTITION: ");
      sb.append(partitionName).append("\t");
      Map<String, String> states = resourceExternalView.getStateMap(partitionName);
      for (Map.Entry<String, String> stateEntry : states.entrySet()) {
        sb.append("\t");
        sb.append(stateEntry.getKey());
        sb.append("\t");
        sb.append(stateEntry.getValue());
      }
    }
  }
}
