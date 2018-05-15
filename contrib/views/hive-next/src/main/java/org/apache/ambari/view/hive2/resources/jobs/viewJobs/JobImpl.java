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

package org.apache.ambari.view.hive2.resources.jobs.viewJobs;

import org.apache.commons.beanutils.PropertyUtils;

import java.beans.Transient;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * Bean to represent saved query
 */
public class JobImpl implements Job {
  private String title = null;
  private String queryFile = null;
  private String statusDir = null;
  private Long dateSubmitted = 0L;
  private Long duration = 0L;
  private String forcedContent = null;
  private String dataBase = null;
  private String queryId = null;

  private String status = JOB_STATE_UNKNOWN;
  private String statusMessage = null;
  private String sqlState = null;

  private String applicationId;
  private String dagId;
  private String dagName;

  private String sessionTag;
  private String referrer;
  private String globalSettings;

  private String id = null;
  private String owner = null;

  private String logFile;
  private String confFile;

  private String guid = null;

  private String hiveQueryId;

  public JobImpl() {}
  public JobImpl(Map<String, Object> stringObjectMap) throws InvocationTargetException, IllegalAccessException {
    for (Map.Entry<String, Object> entry : stringObjectMap.entrySet())  {
      try {
        PropertyUtils.setProperty(this, entry.getKey(), entry.getValue());
      } catch (NoSuchMethodException e) {
        //do nothing, skip
      }
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Job)) return false;

    JobImpl job = (JobImpl) o;

    return id != null ? id.equals(job.id) : job.id == null;

  }

  @Override
  public int hashCode() {
    return id != null ? id.hashCode() : 0;
  }

  @Override
  @Transient
  public String getHiveQueryId() {
    return hiveQueryId;
  }

  @Override
  @Transient
  public void setHiveQueryId(String hiveQueryId) {
    this.hiveQueryId = hiveQueryId;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  @Override
  public String getOwner() {
    return owner;
  }

  @Override
  public void setOwner(String owner) {
    this.owner = owner;
  }

  @Override
  public String getTitle() {
    return title;
  }

  @Override
  public void setTitle(String title) {
    this.title = title;
  }

  @Override
  public String getQueryFile() {
    return queryFile;
  }

  @Override
  public void setQueryFile(String queryFile) {
    this.queryFile = queryFile;
  }

  @Override
  public Long getDateSubmitted() {
    return dateSubmitted;
  }

  @Override
  public void setDateSubmitted(Long dateSubmitted) {
    this.dateSubmitted = dateSubmitted;
  }

  @Override
  public Long getDuration() {
    return duration;
  }

  @Override
  public void setDuration(Long duration) {
    this.duration = duration;
  }

  @Override
  public String getStatus() {
    return status;
  }

  @Override
  public void setStatus(String status) {
    this.status = status;
  }

  @Override
  @Transient
  public String getForcedContent() {
    return forcedContent;
  }

  @Override
  @Transient
  public void setForcedContent(String forcedContent) {
    this.forcedContent = forcedContent;
  }

  @Override
  public String getQueryId() {
    return queryId;
  }

  @Override
  public void setQueryId(String queryId) {
    this.queryId = queryId;
  }

  @Override
  public String getStatusDir() {
    return statusDir;
  }

  @Override
  public void setStatusDir(String statusDir) {
    this.statusDir = statusDir;
  }

  @Override
  public String getDataBase() {
    return dataBase;
  }

  @Override
  public void setDataBase(String dataBase) {
    this.dataBase = dataBase;
  }

  @Override
  public String getLogFile() {
    return logFile;
  }

  @Override
  public void setLogFile(String logFile) {
    this.logFile = logFile;
  }

  @Override
  public String getConfFile() {
    return confFile;
  }

  @Override
  public void setConfFile(String confFile) {
    this.confFile = confFile;
  }

  @Override
  public String getApplicationId() {
    return applicationId;
  }

  @Override
  public void setApplicationId(String applicationId) {
    this.applicationId = applicationId;
  }

  @Override
  public String getDagName() {
    return dagName;
  }

  @Override
  public void setDagName(String dagName) {
    this.dagName = dagName;
  }

  @Override
  public String getDagId() {
    return dagId;
  }

  @Override
  public void setDagId(String dagId) {
    this.dagId = dagId;
  }

  @Override
  public String getSessionTag() {
    return sessionTag;
  }

  @Override
  public void setSessionTag(String sessionTag) {
    this.sessionTag = sessionTag;
  }

  @Override
  @Transient
  public String getStatusMessage() {
    return statusMessage;
  }

  @Override
  @Transient
  public void setStatusMessage(String statusMessage) {
    this.statusMessage = statusMessage;
  }

  @Override
  public String getSqlState() {
    return sqlState;
  }

  @Override
  public void setSqlState(String sqlState) {
    this.sqlState = sqlState;
  }

  @Override
  public String getReferrer() {
    return referrer;
  }

  @Override
  public void setReferrer(String referrer) {
    this.referrer = referrer;
  }

  @Override
  public String getGlobalSettings() {
    return globalSettings;
  }

  @Override
  public void setGlobalSettings(String globalSettings) {
    this.globalSettings = globalSettings;
  }

  @Override
  public String getGuid() {
    return guid;
  }

  @Override
  public void setGuid(String guid) {
    this.guid = guid;
  }

  @Override
  public String toString() {
    return new StringBuilder("JobImpl{")
      .append("id='").append(id)
      .append(", owner='").append(owner)
      .append(", hiveQueryId='").append(hiveQueryId)
      .append(", dagId='").append(dagId)
      .append(", queryId='").append(queryId)
      .append('}').toString();
  }
}
