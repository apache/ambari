/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.view.hive.resources.jobs;

import java.util.LinkedHashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogParser {
  public static final Pattern HADOOP_MR_JOBS_RE = Pattern.compile("(http[^\\s]*/proxy/([a-z0-9_]+?)/)");
  public static final Pattern HADOOP_TEZ_JOBS_RE = Pattern.compile("\\(Executing on YARN cluster with App id ([a-z0-9_]+?)\\)");
  private LinkedHashSet<JobId> jobsList;

  public static LogParser parseLog(String logs) {
    LogParser parser = new LogParser();

    LinkedHashSet<JobId> mrJobIds = getMRJobIds(logs);
    LinkedHashSet<JobId> tezJobIds = getTezJobIds(logs);

    LinkedHashSet<JobId> jobIds = new LinkedHashSet<JobId>();
    jobIds.addAll(mrJobIds);
    jobIds.addAll(tezJobIds);

    parser.setJobsList(jobIds);
    return parser;
  }

  private static LinkedHashSet<JobId> getMRJobIds(String logs) {
    Matcher m = HADOOP_MR_JOBS_RE.matcher(logs);
    LinkedHashSet<JobId> list = new LinkedHashSet<JobId>();
    while (m.find()) {
      JobId applicationInfo = new JobId();
      applicationInfo.setTrackingUrl(m.group(1));
      applicationInfo.setIdentifier(m.group(2));
      list.add(applicationInfo);
    }
    return list;
  }

  private static LinkedHashSet<JobId> getTezJobIds(String logs) {
    Matcher m = HADOOP_TEZ_JOBS_RE.matcher(logs);
    LinkedHashSet<JobId> list = new LinkedHashSet<JobId>();
    while (m.find()) {
      JobId applicationInfo = new JobId();
      applicationInfo.setTrackingUrl(null);
      applicationInfo.setIdentifier(m.group(1));
      list.add(applicationInfo);
    }
    return list;
  }

  public void setJobsList(LinkedHashSet<JobId> jobsList) {
    this.jobsList = jobsList;
  }

  public LinkedHashSet<JobId> getJobsList() {
    return jobsList;
  }

  public static class JobId {
    private String trackingUrl;
    private String identifier;

    public String getTrackingUrl() {
      return trackingUrl;
    }

    public void setTrackingUrl(String trackingUrl) {
      this.trackingUrl = trackingUrl;
    }

    public String getIdentifier() {
      return identifier;
    }

    public void setIdentifier(String identifier) {
      this.identifier = identifier;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof JobId)) return false;

      JobId jobId = (JobId) o;

      if (!identifier.equals(jobId.identifier)) return false;

      return true;
    }

    @Override
    public int hashCode() {
      return identifier.hashCode();
    }
  }
}
