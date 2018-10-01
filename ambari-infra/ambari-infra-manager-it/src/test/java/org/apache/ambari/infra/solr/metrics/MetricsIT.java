/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ambari.infra.solr.metrics;

import static java.lang.System.currentTimeMillis;
import static org.apache.ambari.infra.Solr.HADOOP_LOGS_COLLECTION;
import static org.apache.ambari.infra.TestUtil.runCommand;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.io.File;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.apache.ambari.infra.Solr;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetricsIT {
  private static final Logger LOG = LoggerFactory.getLogger(MetricsIT.class);

  private static MockMetricsServer metricsServer;
  private static String shellScriptLocation;

  @BeforeClass
  public static void setupMetricsServer() throws Exception {
    URL location = MetricsIT.class.getProtectionDomain().getCodeSource().getLocation();
    String ambariFolder = new File(location.toURI()).getParentFile().getParentFile().getParentFile().getParent();

    // TODO: use the same containers as ambari-infra-manager-it
    shellScriptLocation = ambariFolder + "/ambari-infra/ambari-infra-solr-plugin/docker/infra-solr-docker-compose.sh";
    LOG.info("Creating new docker containers for testing Ambari Infra Solr Metrics plugin ...");
    runCommand(new String[]{shellScriptLocation, "start"});

    Solr solr = new Solr("/usr/lib/ambari-infra-solr/server/solr");
    solr.waitUntilSolrIsUp();
    solr.createSolrCollection(HADOOP_LOGS_COLLECTION);

    metricsServer = new MockMetricsServer();
    metricsServer.init();
  }

  @AfterClass
  public static void tearDown() throws Exception {
    LOG.info("shutdown containers");
    runCommand(new String[]{shellScriptLocation, "stop"});
  }

  @Test
  public void testAllMetricsArrived() throws Exception {
    metricsServer.addExpectedMetrics(EXPECTED_METRICS);
    long start = currentTimeMillis();
    while (!metricsServer.getNotReceivedMetrics().isEmpty()) {
      Thread.sleep(1000);
      if (currentTimeMillis() - start > 30 * 1000)
        break;
      LOG.info("Checking any metrics arrived...");
    }

    metricsServer.getNotReceivedMetrics().forEach(metric -> LOG.info("Metric not received: {}", metric));
    assertThat(metricsServer.getNotReceivedMetrics().isEmpty(), is(true));
  }

  private static final Set<String> EXPECTED_METRICS = new HashSet<String>() {{
    add("infra.solr.jvm.threads.count");
    add("infra.solr.jvm.threads.deadlock.count");
    add("infra.solr.jvm.memory.heap.used");
    add("infra.solr.jvm.memory.heap.max");
    add("infra.solr.jvm.memory.non-heap.used");
    add("infra.solr.jvm.memory.non-heap.max");
    add("infra.solr.jvm.memory.pools.CMS-Old-Gen.used");
    add("infra.solr.jvm.memory.pools.CMS-Old-Gen.max");
    add("infra.solr.jvm.gc.ConcurrentMarkSweep.count");
    add("infra.solr.jvm.gc.ConcurrentMarkSweep.time");
    add("infra.solr.jvm.gc.ParNew.count");
    add("infra.solr.jvm.gc.ParNew.time");
    add("infra.solr.jvm.memory.pools.Metaspace.used");
    add("infra.solr.jvm.memory.pools.Metaspace.max");
    add("infra.solr.jvm.memory.pools.Par-Eden-Space.used");
    add("infra.solr.jvm.memory.pools.Par-Eden-Space.max");
    add("infra.solr.jvm.memory.pools.Par-Survivor-Space.used");
    add("infra.solr.jvm.memory.pools.Par-Survivor-Space.max");
    add("infra.solr.jvm.os.processCpuLoad");
    add("infra.solr.jvm.os.systemCpuLoad");
    add("infra.solr.jvm.os.openFileDescriptorCount");
    add("infra.solr.core.hadoop_logs.shard1.replica_n1.UPDATE.updateHandler.adds");
    add("infra.solr.core.hadoop_logs.shard1.replica_n1.UPDATE.updateHandler.deletesById");
    add("infra.solr.core.hadoop_logs.shard1.replica_n1.UPDATE.updateHandler.errors");
    add("infra.solr.core.hadoop_logs.shard1.replica_n1.UPDATE.updateHandler.docsPending");
    add("infra.solr.core.hadoop_logs.shard1.replica_n1.QUERY./select.requests");
    add("infra.solr.core.hadoop_logs.shard1.replica_n1.QUERY./select.requestTimes.avgRequestsPerSecond");
    add("infra.solr.core.hadoop_logs.shard1.replica_n1.QUERY./select.requestTimes.avgTimePerRequest");
    add("infra.solr.core.hadoop_logs.shard1.replica_n1.QUERY./select.requestTimes.medianRequestTime");
    add("infra.solr.core.hadoop_logs.shard1.replica_n1.UPDATE./update.requests");
    add("infra.solr.core.hadoop_logs.shard1.replica_n1.UPDATE./update.requestTimes.avgRequestsPerSecond");
    add("infra.solr.core.hadoop_logs.shard1.replica_n1.UPDATE./update.requestTimes.avgTimePerRequest");
    add("infra.solr.core.hadoop_logs.shard1.replica_n1.UPDATE./update.requestTimes.medianRequestTime");
    add("infra.solr.core.hadoop_logs.shard1.replica_n1.QUERY./get.requests");
    add("infra.solr.core.hadoop_logs.shard1.replica_n1.QUERY./get.requestTimes.avgRequestsPerSecond");
    add("infra.solr.core.hadoop_logs.shard1.replica_n1.QUERY./get.requestTimes.avgTimePerRequest");
    add("infra.solr.core.hadoop_logs.shard1.replica_n1.QUERY./get.requestTimes.medianRequestTime");
    add("infra.solr.core.hadoop_logs.shard1.replica_n1.ADMIN./admin/luke.requests");
    add("infra.solr.core.hadoop_logs.shard1.replica_n1.ADMIN./admin/luke.requestTimes.avgRequestsPerSecond");
    add("infra.solr.core.hadoop_logs.shard1.replica_n1.ADMIN./admin/luke.requestTimes.avgTimePerRequest");
    add("infra.solr.core.hadoop_logs.shard1.replica_n1.ADMIN./admin/luke.requestTimes.medianRequestTime");
    add("infra.solr.core.hadoop_logs.shard1.replica_n1.QUERY./query.requests");
    add("infra.solr.core.hadoop_logs.shard1.replica_n1.QUERY./query.requestTimes.avgRequestsPerSecond");
    add("infra.solr.core.hadoop_logs.shard1.replica_n1.QUERY./query.requestTimes.avgTimePerRequest");
    add("infra.solr.core.hadoop_logs.shard1.replica_n1.QUERY./query.requestTimes.medianRequestTime");
    add("infra.solr.core.hadoop_logs.shard1.replica_n1.INDEX.sizeInBytes");
    add("infra.solr.core.hadoop_logs.shard1.replica_n1.CACHE.searcher.filterCache.hitratio");
    add("infra.solr.core.hadoop_logs.shard1.replica_n1.CACHE.searcher.filterCache.size");
    add("infra.solr.core.hadoop_logs.shard1.replica_n1.CACHE.searcher.filterCache.warmupTime");
    add("infra.solr.core.hadoop_logs.shard1.replica_n1.CACHE.searcher.queryResultCache.hitratio");
    add("infra.solr.core.hadoop_logs.shard1.replica_n1.CACHE.searcher.queryResultCache.size");
    add("infra.solr.core.hadoop_logs.shard1.replica_n1.CACHE.searcher.queryResultCache.warmupTime");
    add("infra.solr.core.hadoop_logs.shard1.replica_n1.CACHE.searcher.documentCache.hitratio");
    add("infra.solr.core.hadoop_logs.shard1.replica_n1.CACHE.searcher.documentCache.size");
    add("infra.solr.core.hadoop_logs.shard1.replica_n1.CACHE.searcher.documentCache.warmupTime");
    add("infra.solr.core.hadoop_logs.shard1.replica_n1.CACHE.core.fieldCache.entries_count");
  }};
}