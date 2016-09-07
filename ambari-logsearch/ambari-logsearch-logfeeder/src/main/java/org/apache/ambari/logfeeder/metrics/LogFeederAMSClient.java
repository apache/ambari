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

package org.apache.ambari.logfeeder.metrics;

import org.apache.ambari.logfeeder.util.LogFeederUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.metrics2.sink.timeline.AbstractTimelineMetricsSink;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.apache.log4j.Logger;

public class LogFeederAMSClient extends AbstractTimelineMetricsSink {
  private static final Logger LOG = Logger.getLogger(LogFeederAMSClient.class);

  private String collectorHosts = null;

  public LogFeederAMSClient() {
    collectorHosts = LogFeederUtil.getStringProperty("logfeeder.metrics.collector.hosts");
    if (StringUtils.isBlank(collectorHosts)) {
      collectorHosts = null;
    }
    if (collectorHosts != null) {
      collectorHosts = collectorHosts.trim();
    }
    LOG.info("AMS collector URL=" + collectorHosts);
  }

  @Override
  public String getCollectorUri() {
    return collectorHosts;
  }

  @Override
  protected int getTimeoutSeconds() {
    // TODO: Hard coded timeout
    return 10;
  }

  @Override
  protected boolean emitMetrics(TimelineMetrics metrics) {
    return super.emitMetrics(metrics);
  }

}
