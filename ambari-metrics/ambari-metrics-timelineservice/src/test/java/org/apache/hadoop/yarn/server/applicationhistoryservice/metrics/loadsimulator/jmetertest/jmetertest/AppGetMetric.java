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
package org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.loadsimulator.jmetertest.jmetertest;

import java.util.List;

public class AppGetMetric {

  private String app;
  private int interval;
  private List<GetMetricRequestInfo> requests;

  public AppGetMetric(List<GetMetricRequestInfo> requests, int interval, String app) {
    this.setMetricRequests(requests);
    this.setInterval(interval);
    this.setApp(app);
  }

  public List<GetMetricRequestInfo> getMetricRequests() {
    return requests;
  }

  public void setMetricRequests(List<GetMetricRequestInfo> requests) {
    this.requests = requests;
  }

  public int getInterval() {
    return interval;
  }

  public void setInterval(int interval) {
    this.interval = interval;
  }

  public String getApp() {
    return app;
  }

  public void setApp(String app) {
    this.app = app;
  }
}
