/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.eventdb.model;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.lang.StringUtils;

/**
 * 
 */
public class TaskAttempt {
  public static enum TaskAttemptFields {
    JOBID,
    TASKATTEMPTID,
    TASKTYPE,
    STARTTIME,
    FINISHTIME,
    MAPFINISHTIME,
    SHUFFLEFINISHTIME,
    SORTFINISHTIME,
    INPUTBYTES,
    OUTPUTBYTES,
    STATUS,
    LOCALITY;
    
    public String getString(ResultSet rs) throws SQLException {
      return rs.getString(this.toString());
    }
    
    public int getInt(ResultSet rs) throws SQLException {
      return rs.getInt(this.toString());
    }
    
    public long getLong(ResultSet rs) throws SQLException {
      return rs.getLong(this.toString());
    }
    
    public static String join() {
      String[] tmp = new String[TaskAttemptFields.values().length];
      for (int i = 0; i < tmp.length; i++)
        tmp[i] = TaskAttemptFields.values()[i].toString();
      return StringUtils.join(tmp, ",");
    }

    public static String join(String tableName) {
      String[] tmp = new String[TaskAttemptFields.values().length];
      for (int i = 0; i < tmp.length; i++)
        tmp[i] = tableName + "." + TaskAttemptFields.values()[i].toString();
      return StringUtils.join(tmp, ",");
    }
  }
  
  public static final String TASK_ATTEMPT_FIELDS = TaskAttemptFields.join();
  
  private String jobId;
  private String taskAttemptId;
  private String taskType;
  private long startTime;
  private long finishTime;
  private long mapFinishTime;
  private long shuffleFinishTime;
  private long sortFinishTime;
  private long inputBytes;
  private long outputBytes;
  private String status;
  private String locality;
  
  public TaskAttempt() {}
  
  public String getJobId() {
    return jobId;
  }
  
  public void setJobId(String jobId) {
    this.jobId = jobId;
  }
  
  public String getTaskAttemptId() {
    return taskAttemptId;
  }
  
  public void setTaskAttemptId(String taskAttemptId) {
    this.taskAttemptId = taskAttemptId;
  }
  
  public String getTaskType() {
    return taskType;
  }
  
  public void setTaskType(String taskType) {
    this.taskType = taskType;
  }
  
  public long getStartTime() {
    return startTime;
  }
  
  public void setStartTime(long startTime) {
    this.startTime = startTime;
  }
  
  public long getFinishTime() {
    return finishTime;
  }
  
  public void setFinishTime(long finishTime) {
    this.finishTime = finishTime;
  }
  
  public long getMapFinishTime() {
    return mapFinishTime;
  }
  
  public void setMapFinishTime(long mapFinishTime) {
    this.mapFinishTime = mapFinishTime;
  }
  
  public long getShuffleFinishTime() {
    return shuffleFinishTime;
  }
  
  public void setShuffleFinishTime(long shuffleFinishTime) {
    this.shuffleFinishTime = shuffleFinishTime;
  }
  
  public long getSortFinishTime() {
    return sortFinishTime;
  }
  
  public void setSortFinishTime(long sortFinishTime) {
    this.sortFinishTime = sortFinishTime;
  }
  
  public long getInputBytes() {
    return inputBytes;
  }
  
  public long getOutputBytes() {
    return outputBytes;
  }
  
  public void setInputBytes(long inputBytes) {
    this.inputBytes = inputBytes;
  }
  
  public void setOutputBytes(long outputBytes) {
    this.outputBytes = outputBytes;
  }
  
  public String getStatus() {
    return status;
  }
  
  public void setStatus(String status) {
    this.status = status;
  }
  
  public String getLocality() {
    return locality;
  }
  
  public void setLocality(String locality) {
    this.locality = locality;
  }
}
