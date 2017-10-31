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
package org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.source;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.sink.ExternalMetricsSink;

public class DefaultInternalMetricsSourceProvider implements InternalSourceProvider {
  private static final Log LOG = LogFactory.getLog(DefaultInternalMetricsSourceProvider.class);

  // TODO: Implement read based sources for higher order data
  @Override
  public InternalMetricsSource getInternalMetricsSource(SOURCE_NAME sourceName, int sinkIntervalSeconds, ExternalMetricsSink sink) {
    if (sink == null) {
      LOG.warn("No external sink configured for source " + sourceName);
      return null;
    }

    switch (sourceName) {
      case RAW_METRICS:
        return new RawMetricsSource(sinkIntervalSeconds, sink);
      default:
        throw new UnsupportedOperationException("Unimplemented source type " + sourceName);
    }
  }
}
