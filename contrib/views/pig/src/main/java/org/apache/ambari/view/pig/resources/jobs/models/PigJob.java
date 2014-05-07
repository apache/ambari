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

package org.apache.ambari.view.pig.resources.jobs.models;

import org.apache.ambari.view.pig.persistence.utils.PersonalResource;
import org.apache.commons.beanutils.BeanUtils;
import org.codehaus.jackson.annotate.JsonIgnore;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * Bean to represent Pig job
 *
 * Job lifecycle:
 * SUBMITTING
 *     |
 *   [POST to Templeton]
 *     |            |
 * SUBMITTED   SUBMIT_FAILED
 *     |
 *     |
 *   [GET result from job/:job_id]
 *     |            |             |
 * COMPLETED      KILLED        FAILED
 */
public class PigJob implements Serializable, PersonalResource {

  public enum Status {
    UNKNOWN,
    SUBMITTING, SUBMITTED, RUNNING,  // in progress
    SUBMIT_FAILED, COMPLETED, FAILED, KILLED  // finished
  }

  public boolean isInProgress() {
    return status == Status.SUBMITTED || status == Status.SUBMITTING ||
        status == Status.RUNNING;
  }

  public static final int RUN_STATE_RUNNING = 1;
  public static final int RUN_STATE_SUCCEEDED = 2;
  public static final int RUN_STATE_FAILED = 3;
  public static final int RUN_STATE_PREP = 4;
  public static final int RUN_STATE_KILLED = 5;

  public PigJob() {
  }

  public PigJob(Map<String, Object> stringObjectMap) throws InvocationTargetException, IllegalAccessException {
    BeanUtils.populate(this, stringObjectMap);
  }

  private String id = null;
  private String scriptId = null;

  // cloned script data
  private String pigScript = null;
  private String pythonScript = null;
  private String title = null;
  private String templetonArguments = null;
  private String owner;

  // job info
  private String forcedContent = null;

  /**
   * jobType possible values:
   * null - regular execute
   * "explain"
   * "syntax_check"
   */
  private String jobType = null;

  /**
   * Additional file to use in Explain job
   */
  private String sourceFile = null;
  private String sourceFileContent = null;

  private String statusDir;
  private Long dateStarted = 0L;
  private String jobId = null;

  // status fields (not reliable)
  private Status status = Status.UNKNOWN;
  private Integer percentComplete = null;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PigJob)) return false;

    PigJob pigScript = (PigJob) o;

    if (!id.equals(pigScript.id)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return id.hashCode();
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

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public String getScriptId() {
    return scriptId;
  }

  public void setScriptId(String scriptId) {
    this.scriptId = scriptId;
  }

  public String getTempletonArguments() {
    return templetonArguments;
  }

  public void setTempletonArguments(String templetonArguments) {
    this.templetonArguments = templetonArguments;
  }

  public String getPigScript() {
    return pigScript;
  }

  public void setPigScript(String pigScript) {
    this.pigScript = pigScript;
  }

  public String getJobId() {
    return jobId;
  }

  public void setJobId(String jobId) {
    this.jobId = jobId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public void setStatusDir(String statusDir) {
    this.statusDir = statusDir;
  }

  public String getStatusDir() {
    return statusDir;
  }

  public Long getDateStarted() {
    return dateStarted;
  }

  public void setDateStarted(Long dateStarted) {
    this.dateStarted = dateStarted;
  }

  public Integer getPercentComplete() {
    return percentComplete;
  }

  public void setPercentComplete(Integer percentComplete) {
    this.percentComplete = percentComplete;
  }

  public String getPythonScript() {
    return pythonScript;
  }

  public void setPythonScript(String pythonScript) {
    this.pythonScript = pythonScript;
  }

  public String getForcedContent() {
    return forcedContent;
  }

  public void setForcedContent(String forcedContent) {
    this.forcedContent = forcedContent;
  }

  public String getJobType() {
    return jobType;
  }

  public void setJobType(String jobType) {
    this.jobType = jobType;
  }

  public String getSourceFileContent() {
    return sourceFileContent;
  }

  public void setSourceFileContent(String sourceFileContent) {
    this.sourceFileContent = sourceFileContent;
  }

  public String getSourceFile() {
    return sourceFile;
  }

  public void setSourceFile(String sourceFile) {
    this.sourceFile = sourceFile;
  }
}
