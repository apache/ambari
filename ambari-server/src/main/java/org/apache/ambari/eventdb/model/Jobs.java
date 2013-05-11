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

import org.apache.commons.lang.StringUtils;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Jobs {
  int numJobs;
  List<JobDBEntry> jobs;
  
  public static class JobDBEntry {
    public static enum JobFields {
      JOBID,
      JOBNAME,
      STATUS,
      USERNAME,
      SUBMITTIME,
      FINISHTIME,
      MAPS,
      REDUCES,
      INPUTBYTES,
      OUTPUTBYTES,
      CONFPATH,
      WORKFLOWID,
      WORKFLOWENTITYNAME;
      
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
        String[] tmp = new String[JobFields.values().length];
        for (int i = 0; i < tmp.length; i++)
          tmp[i] = JobFields.values()[i].toString();
        return StringUtils.join(tmp, ",");
      }
    }
    
    @XmlTransient
    public static final String JOB_FIELDS = JobFields.join();
    
    private String jobId;
    private String jobName;
    private String status;
    private String userName;
    private long submitTime;
    private long elapsedTime;
    private int maps;
    private int reduces;
    private long inputBytes;
    private long outputBytes;
    private String confPath;
    private String workflowId;
    private String workflowEntityName;
    
    public JobDBEntry() {
      /* Required by JAXB. */
    }
    
    public String getJobId() {
      return jobId;
    }
    
    public String getJobName() {
      return jobName;
    }
    
    public String getStatus() {
      return status;
    }
    
    public String getUserName() {
      return userName;
    }
    
    public long getSubmitTime() {
      return submitTime;
    }
    
    public long getElapsedTime() {
      return elapsedTime;
    }
    
    public int getMaps() {
      return maps;
    }
    
    public int getReduces() {
      return reduces;
    }
    
    public long getInputBytes() {
      return inputBytes;
    }
    
    public long getOutputBytes() {
      return outputBytes;
    }
    
    public String getConfPath() {
      return confPath;
    }
    
    public String getWorkflowId() {
      return workflowId;
    }
    
    public String getWorkflowEntityName() {
      return workflowEntityName;
    }
    
    public void setJobId(String jobId) {
      this.jobId = jobId;
    }
    
    public void setJobName(String jobName) {
      this.jobName = jobName;
    }
    
    public void setStatus(String status) {
      this.status = status;
    }
    
    public void setUserName(String userName) {
      this.userName = userName;
    }
    
    public void setSubmitTime(long submitTime) {
      this.submitTime = submitTime;
    }
    
    public void setElapsedTime(long elapsedTime) {
      this.elapsedTime = elapsedTime;
    }
    
    public void setMaps(int maps) {
      this.maps = maps;
    }
    
    public void setReduces(int reduces) {
      this.reduces = reduces;
    }
    
    public void setInputBytes(long inputBytes) {
      this.inputBytes = inputBytes;
    }
    
    public void setOutputBytes(long outputBytes) {
      this.outputBytes = outputBytes;
    }
    
    public void setConfPath(String confPath) {
      this.confPath = confPath;
    }
    
    public void setWorkflowId(String workflowId) {
      this.workflowId = workflowId;
    }
    
    public void setWorkflowEntityName(String workflowEntityName) {
      this.workflowEntityName = workflowEntityName;
    }
  }
  
  public Jobs() {}
  
  public int getNumJobs() {
    return numJobs;
  }
  
  public void setNumJobs(int numJobs) {
    this.numJobs = numJobs;
  }
  
  public List<JobDBEntry> getJobs() {
    return jobs;
  }
  
  public void setJobs(List<JobDBEntry> jobs) {
    this.jobs = jobs;
    this.numJobs = jobs.size();
  }
}
