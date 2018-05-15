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


import org.apache.ambari.view.hive2.persistence.utils.Indexed;
import org.apache.ambari.view.hive2.persistence.utils.PersonalResource;

import java.io.Serializable;

/**
 * Interface for ExecuteJob bean to create Proxy for it
 */
public interface Job extends Serializable,Indexed,PersonalResource {
  String JOB_STATE_UNKNOWN = "UNKNOWN";
  String JOB_STATE_INITIALIZED = "INITIALIZED";
  String JOB_STATE_RUNNING = "RUNNING";
  String JOB_STATE_FINISHED = "SUCCEEDED";
  String JOB_STATE_CANCELED = "CANCELED";
  String JOB_STATE_CLOSED = "CLOSED";
  String JOB_STATE_ERROR = "ERROR";
  String JOB_STATE_PENDING = "PENDING";

  String getId();

  void setId(String id);

  String getOwner();

  void setOwner(String owner);

  String getTitle();

  void setTitle(String title);

  String getQueryFile();

  void setQueryFile(String queryFile);

  Long getDateSubmitted();

  void setDateSubmitted(Long dateSubmitted);

  Long getDuration();

  void setDuration(Long duration);

  String getStatus();

  void setStatus(String status);

  String getForcedContent();

  void setForcedContent(String forcedContent);

  String getQueryId();

  void setQueryId(String queryId);

  String getStatusDir();

  void setStatusDir(String statusDir);

  String getDataBase();

  void setDataBase(String dataBase);

  String getLogFile();

  void setLogFile(String logFile);

  String getConfFile();

  void setConfFile(String confFile);

  String getApplicationId();

  void setApplicationId(String applicationId);

  String getDagName();

  void setDagName(String dagName);

  String getDagId();

  void setDagId(String dagId);

  String getSessionTag();

  void setSessionTag(String sessionTag);

  String getSqlState();

  void setSqlState(String sqlState);

  String getStatusMessage();

  void setStatusMessage(String message);

  String getReferrer();

  void setReferrer(String referrer);

  String getGlobalSettings();

  void setGlobalSettings(String globalSettings);

  String getGuid();

  void setGuid(String guid);

  String getHiveQueryId();

  void setHiveQueryId(String hiveQueryId);
}
