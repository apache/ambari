/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.metrics.core.timeline.sink;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;

import org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration;
import org.apache.ambari.metrics.core.timeline.source.InternalSourceProvider;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;

import static org.apache.ambari.metrics.core.timeline.TimelineMetricConfiguration.TIMELINE_METRICS_CACHE_COMMIT_INTERVAL;

public class DefaultFSSinkProvider implements ExternalSinkProvider {
  private static final Log LOG = LogFactory.getLog(DefaultFSSinkProvider.class);
  TimelineMetricConfiguration conf = TimelineMetricConfiguration.getInstance();
  private final DefaultExternalMetricsSink sink = new DefaultExternalMetricsSink();
  private long FIXED_FILE_SIZE;
  private final String SINK_FILE_NAME = "external-metrics-sink.dat";
  private final String SEPARATOR = ", ";
  private final String LINE_SEP = System.lineSeparator();
  private final String HEADERS = "METRIC, APP_ID, INSTANCE_ID, HOSTNAME, START_TIME, DATA";

  public DefaultFSSinkProvider() {
    try {
      FIXED_FILE_SIZE = conf.getMetricsConf().getLong("timeline.metrics.service.external.fs.sink.filesize", FileUtils.ONE_MB * 100);
    } catch (Exception ignored) {
      FIXED_FILE_SIZE = FileUtils.ONE_MB * 100;
    }
  }

  @Override
  public ExternalMetricsSink getExternalMetricsSink(InternalSourceProvider.SOURCE_NAME sourceName) {
    return sink;
  }

  class DefaultExternalMetricsSink implements ExternalMetricsSink {

    @Override
    public int getSinkTimeOutSeconds() {
      return 10;
    }

    @Override
    public int getFlushSeconds() {
      try {
        return conf.getMetricsConf().getInt(TIMELINE_METRICS_CACHE_COMMIT_INTERVAL, 3);
      } catch (Exception e) {
        LOG.warn("Cannot read cache commit interval.");
      }
      return 3;
    }

    private boolean createFile(File f) {
      boolean created = false;
      if (!f.exists()) {
        try {
          created = f.createNewFile();
          FileUtils.writeStringToFile(f, HEADERS);
        } catch (IOException e) {
          LOG.error("Cannot create " + SINK_FILE_NAME + " at " + f.getPath());
          return false;
        }
      }

      return created;
    }

    private boolean shouldReCreate(File f) {
      if (!f.exists()) {
        return true;
      }
      if (FileUtils.sizeOf(f) > FIXED_FILE_SIZE) {
        return true;
      }
      return false;
    }

    @Override
    public void sinkMetricData(Collection<TimelineMetrics> metrics) {
      String dirPath = TimelineMetricConfiguration.getInstance().getDefaultMetricsSinkDir();
      File dir = new File(dirPath);
      if (!dir.exists()) {
        LOG.error("Cannot sink data to file system, incorrect dir path " + dirPath);
        return;
      }

      File f = FileUtils.getFile(dirPath, SINK_FILE_NAME);
      if (shouldReCreate(f)) {
        if (!f.delete()) {
          LOG.warn("Unable to delete external sink file.");
          return;
        }
        createFile(f);
      }

      if (metrics != null) {
        for (TimelineMetrics timelineMetrics : metrics) {
          for (TimelineMetric metric : timelineMetrics.getMetrics()) {
            StringBuilder sb = new StringBuilder();
            sb.append(metric.getMetricName());
            sb.append(SEPARATOR);
            sb.append(metric.getAppId());
            sb.append(SEPARATOR);
            if (StringUtils.isEmpty(metric.getInstanceId())) {
              sb.append(SEPARATOR);
            } else {
              sb.append(metric.getInstanceId());
              sb.append(SEPARATOR);
            }
            if (StringUtils.isEmpty(metric.getHostName())) {
              sb.append(SEPARATOR);
            } else {
              sb.append(metric.getHostName());
              sb.append(SEPARATOR);
            }
            sb.append(new Date(metric.getStartTime()));
            sb.append(SEPARATOR);
            sb.append(metric.getMetricValues().toString());
            sb.append(LINE_SEP);
            try {
              FileUtils.writeStringToFile(f, sb.toString());
            } catch (IOException e) {
              LOG.warn("Unable to sink data to file " + f.getPath());
            }
          }
        }
      }
    }
  }
}
