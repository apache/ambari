/*
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
package org.apache.ambari.metrics.core.timeline.availability;

import static org.apache.ambari.metrics.core.timeline.availability.AggregationTaskRunner.ACTUAL_AGGREGATOR_NAMES;
import static org.apache.helix.model.IdealState.RebalanceMode.FULL_AUTO;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.I0Itec.zkclient.exception.ZkNoNodeException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ambari.metrics.core.timeline.MetricsSystemInitializationException;
import org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration;
import org.apache.helix.HelixAdmin;
import org.apache.helix.HelixException;
import org.apache.helix.HelixManager;
import org.apache.helix.HelixManagerFactory;
import org.apache.helix.InstanceType;
import org.apache.helix.LiveInstanceChangeListener;
import org.apache.helix.NotificationContext;
import org.apache.helix.PropertyKey;
import org.apache.helix.manager.zk.ZKHelixAdmin;
import org.apache.helix.model.ExternalView;
import org.apache.helix.model.InstanceConfig;
import org.apache.helix.model.LiveInstance;
import org.apache.helix.model.OnlineOfflineSMD;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;


public class MetricCollectorHAController {
  private static final Log LOG = LogFactory.getLog(MetricCollectorHAController.class);

  @VisibleForTesting
  static final String CLUSTER_NAME = "ambari-metrics-cluster";
  @VisibleForTesting
  static final String METRIC_AGGREGATORS = "METRIC_AGGREGATORS";
  @VisibleForTesting
  static final String DEFAULT_STATE_MODEL = OnlineOfflineSMD.name;
  private static final String INSTANCE_NAME_DELIMITER = "_";
  private static final int PARTITION_NUMBER = 2;
  private static final int REPLICATION_FACTOR = 1;

  @VisibleForTesting
  final String zkConnectUrl;
  private final String instanceHostname;
  private final InstanceConfig instanceConfig;
  private final AggregationTaskRunner aggregationTaskRunner;

  // Cache list of known live instances
  private final List<String> liveInstanceNames = new ArrayList<>(2);
  private final LiveInstanceTracker liveInstanceTracker = new LiveInstanceTracker();

  // Helix Admin
  @VisibleForTesting
  HelixAdmin admin;
  // Helix Manager
  private HelixManager manager;

  private volatile boolean isInitialized = false;

  public MetricCollectorHAController(TimelineMetricConfiguration configuration) {
    String instancePort;
    try {
      instanceHostname = configuration.getInstanceHostnameFromEnv();
      instancePort = configuration.getInstancePort();
    } catch (Exception e) {
      LOG.error("Error reading configs from classpath, will resort to defaults.", e);
      throw new MetricsSystemInitializationException(e.getMessage());
    }

    try {
      String zkClientPort = configuration.getClusterZKClientPort();
      String zkQuorum = configuration.getClusterZKQuorum();

      if (StringUtils.isEmpty(zkClientPort) || StringUtils.isEmpty(zkQuorum)) {
        throw new Exception(String.format("Unable to parse zookeeper quorum. clientPort = %s, quorum = %s", zkClientPort, zkQuorum));
      }

      zkConnectUrl = configuration.getZkConnectionUrl(zkClientPort, zkQuorum);
    } catch (Exception e) {
      LOG.error("Unable to load hbase-site from classpath.", e);
      throw new MetricsSystemInitializationException(e.getMessage(), e);
    }

    instanceConfig = new InstanceConfig(instanceHostname + INSTANCE_NAME_DELIMITER + instancePort);
    instanceConfig.setHostName(instanceHostname);
    instanceConfig.setPort(instancePort);
    instanceConfig.setInstanceEnabled(true);

    aggregationTaskRunner = new AggregationTaskRunner(instanceConfig.getInstanceName(), zkConnectUrl, CLUSTER_NAME);
  }

  /**
   * Initialize the instance with zookeeper via Helix
   */
  public void initializeHAController() throws Exception {
    // Create setup tool instance
    admin = new ZKHelixAdmin(zkConnectUrl);
    // Create cluster namespace in zookeeper. Don't recreate if exists.
    LOG.info(String.format("Creating zookeeper cluster node: %s", CLUSTER_NAME));
    boolean clusterAdded = admin.addCluster(CLUSTER_NAME, false);
    LOG.info(String.format("Was cluster added successfully? %s", clusterAdded));

    // Adding host to the cluster
    boolean success = false;
    int tries = 5;
    int sleepTimeInSeconds = 5;

    for (int i = 0; i < tries && !success; i++) {
      try {
        List<String> nodes = admin.getInstancesInCluster(CLUSTER_NAME);
        if (!nodes.contains(instanceConfig.getInstanceName())) {
          LOG.info(String.format("Adding participant instance %s", instanceConfig));
          admin.addInstance(CLUSTER_NAME, instanceConfig);
        }
        success = true;
      } catch (HelixException | ZkNoNodeException ex) {
        LOG.warn("Helix Cluster not yet setup fully.");
        if (i < tries - 1) {
          LOG.info(String.format("Waiting for %d seconds and retrying.", sleepTimeInSeconds));
          TimeUnit.SECONDS.sleep(sleepTimeInSeconds);
        } else {
          LOG.error(ex);
        }
      }
    }

    if (!success) {
      LOG.info(String.format("Trying to create %s again since waiting for the creation did not help.", CLUSTER_NAME));
      admin.addCluster(CLUSTER_NAME, true);
      List<String> nodes = admin.getInstancesInCluster(CLUSTER_NAME);
      if (!nodes.contains(instanceConfig.getInstanceName())) {
        LOG.info(String.format("Adding participant instance %s", instanceConfig));
        admin.addInstance(CLUSTER_NAME, instanceConfig);
      }
    }

    // Add an ONLINE-OFFLINE state model
    if (admin.getStateModelDef(CLUSTER_NAME, DEFAULT_STATE_MODEL) == null) {
      LOG.info("Adding ONLINE-OFFLINE state model to the cluster");
      admin.addStateModelDef(CLUSTER_NAME, DEFAULT_STATE_MODEL, OnlineOfflineSMD.build());
    }

    // Add resources with 1 cluster-wide replica
    // Since our aggregators are unbalanced in terms of work distribution we
    // only need to distribute writes to METRIC_AGGREGATE and
    // METRIC_RECORD_MINUTE, i.e. the Host level and Cluster level aggregations
    List<String> resources = admin.getResourcesInCluster(CLUSTER_NAME);
    if (!resources.contains(METRIC_AGGREGATORS)) {
      LOG.info(String.format("Adding resource %s with %d partitions and %d replicas", METRIC_AGGREGATORS, PARTITION_NUMBER, REPLICATION_FACTOR));
      admin.addResource(CLUSTER_NAME, METRIC_AGGREGATORS, PARTITION_NUMBER, DEFAULT_STATE_MODEL, FULL_AUTO.toString());
    }
    // This will set up the ideal state, it calculates the preference list for each partition similar to consistent hashing.
    admin.rebalance(CLUSTER_NAME, METRIC_AGGREGATORS, REPLICATION_FACTOR);

    // Start participant
    startAggregators();

    // Start controller
    startController();

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      shutdownHAController();
    }));

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
      throw new MetricsSystemInitializationException(e.getMessage(), e);
    }
  }

  private void startController() throws Exception {
    manager = HelixManagerFactory.getZKHelixManager(CLUSTER_NAME, instanceHostname, InstanceType.CONTROLLER, zkConnectUrl);

    manager.connect();
    manager.addLiveInstanceChangeListener(liveInstanceTracker);
  }

  public void shutdownHAController() {
    if (isInitialized) {
      LOG.info("Shooting down Metrics Collector's HAController.");

      PropertyKey.Builder keyBuilder = new PropertyKey.Builder(CLUSTER_NAME);
      manager.removeListener(keyBuilder.liveInstances(), liveInstanceTracker);
      liveInstanceTracker.shutdown();
      aggregationTaskRunner.stop();
      manager.disconnect();
      admin.close();

      isInitialized = false;
      LOG.info("Shutdown of Metrics Collector's HAController finished.");
    }
  }

  public AggregationTaskRunner getAggregationTaskRunner() {
    return aggregationTaskRunner;
  }

  public List<String> getLiveInstanceHostNames() {
    List<String> liveInstanceHostNames = new ArrayList<>(2);

    for (String instance : liveInstanceNames) {
      liveInstanceHostNames.add(instance.split(INSTANCE_NAME_DELIMITER)[0]);
    }

    return liveInstanceHostNames;
  }

  public final class LiveInstanceTracker implements LiveInstanceChangeListener {
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private final Joiner joiner = Joiner.on(", ").skipNulls();

    @Override
    public void onLiveInstanceChange(List<LiveInstance> liveInstances, NotificationContext changeContext) {
      liveInstanceNames.clear();
      for (LiveInstance instance : liveInstances) {
        liveInstanceNames.add(instance.getInstanceName());
      }

      LOG.info(String.format("Detected change in liveliness of Collector instances. LiveInstances = %s", joiner.join(liveInstanceNames)));
      // Print HA state - after some delay
      executorService.schedule(() -> printClusterState(), 30, TimeUnit.SECONDS);
    }

    public void shutdown() {
      executorService.shutdown();
    }
  }

  public void printClusterState() {
    StringBuilder sb = new StringBuilder("\n######################### Cluster HA state ########################");

    ExternalView resourceExternalView = admin.getResourceExternalView(CLUSTER_NAME, METRIC_AGGREGATORS);
    if (resourceExternalView != null) {
      getPrintableResourceState(resourceExternalView, sb);
    }
    sb.append("\n##################################################");
    LOG.info(sb.toString());
  }

  private void getPrintableResourceState(ExternalView resourceExternalView, StringBuilder sb) {
    TreeSet<String> sortedSet = new TreeSet<>(resourceExternalView.getPartitionSet());
    sb.append("\nCLUSTER: ");
    sb.append(CLUSTER_NAME);
    sb.append("\nRESOURCE: ");
    sb.append(MetricCollectorHAController.METRIC_AGGREGATORS);
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

  public Map<String, String> getAggregationSummary() {
    Map<String, String> summary = new HashMap<>();

    CheckpointManager checkpointManager = aggregationTaskRunner.getCheckpointManager();

    summary.put(ACTUAL_AGGREGATOR_NAMES.get(AggregationTaskRunner.AGGREGATOR_NAME.METRIC_RECORD_MINUTE),
      new Date(checkpointManager.readCheckpoint(AggregationTaskRunner.AGGREGATOR_NAME.METRIC_RECORD_MINUTE)).toString());
    summary.put(ACTUAL_AGGREGATOR_NAMES.get(AggregationTaskRunner.AGGREGATOR_NAME.METRIC_RECORD_HOURLY),
      new Date(checkpointManager.readCheckpoint(AggregationTaskRunner.AGGREGATOR_NAME.METRIC_RECORD_HOURLY)).toString());

    summary.put(ACTUAL_AGGREGATOR_NAMES.get(AggregationTaskRunner.AGGREGATOR_NAME.METRIC_AGGREGATE_SECOND),
      new Date(checkpointManager.readCheckpoint(AggregationTaskRunner.AGGREGATOR_NAME.METRIC_AGGREGATE_SECOND)).toString());
    summary.put(ACTUAL_AGGREGATOR_NAMES.get(AggregationTaskRunner.AGGREGATOR_NAME.METRIC_AGGREGATE_MINUTE),
      new Date(checkpointManager.readCheckpoint(AggregationTaskRunner.AGGREGATOR_NAME.METRIC_AGGREGATE_MINUTE)).toString());
    summary.put(ACTUAL_AGGREGATOR_NAMES.get(AggregationTaskRunner.AGGREGATOR_NAME.METRIC_AGGREGATE_HOURLY),
      new Date(checkpointManager.readCheckpoint(AggregationTaskRunner.AGGREGATOR_NAME.METRIC_AGGREGATE_HOURLY)).toString());

    return summary;
  }
}
