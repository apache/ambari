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
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.ambari.eventdb.model.DataTable.Summary;
import org.apache.commons.lang.StringUtils;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Workflows {
  List<WorkflowDBEntry> workflows;
  Summary summary;
  
  public static class WorkflowDBEntry {
    public static enum WorkflowFields {
      WORKFLOWID,
      WORKFLOWNAME,
      USERNAME,
      STARTTIME,
      LASTUPDATETIME,
      DURATION,
      NUMJOBSTOTAL,
      NUMJOBSCOMPLETED,
      INPUTBYTES,
      OUTPUTBYTES,
      PARENTWORKFLOWID,
      WORKFLOWCONTEXT;
      
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
        String[] tmp = new String[WorkflowFields.values().length];
        for (int i = 0; i < tmp.length; i++)
          tmp[i] = WorkflowFields.values()[i].toString();
        return StringUtils.join(tmp, ",");
      }
    }
    
    @XmlTransient
    public static final String WORKFLOW_FIELDS = WorkflowFields.join();
    
    private String workflowId;
    private String workflowName;
    private String userName;
    private long startTime;
    private long elapsedTime;
    private long inputBytes;
    private long outputBytes;
    private int numJobsTotal;
    private int numJobsCompleted;
    private String parentWorkflowId;
    private WorkflowContext workflowContext;
    
    public WorkflowDBEntry() {
      /* Required by JAXB. */
    }
    
    public String getWorkflowId() {
      return workflowId;
    }
    
    public String getWorkflowName() {
      return workflowName;
    }
    
    public String getUserName() {
      return userName;
    }
    
    public long getStartTime() {
      return startTime;
    }
    
    public long getElapsedTime() {
      return elapsedTime;
    }
    
    public int getNumJobsTotal() {
      return numJobsTotal;
    }
    
    public int getNumJobsCompleted() {
      return numJobsCompleted;
    }
    
    public String getParentWorkflowId() {
      return parentWorkflowId;
    }
    
    public WorkflowContext getWorkflowContext() {
      return workflowContext;
    }
    
    public void setWorkflowId(String workflowId) {
      this.workflowId = workflowId;
    }
    
    public void setWorkflowName(String workflowName) {
      this.workflowName = workflowName;
    }
    
    public void setUserName(String userName) {
      this.userName = userName;
    }
    
    public void setStartTime(long startTime) {
      this.startTime = startTime;
    }
    
    public void setElapsedTime(long elapsedTime) {
      this.elapsedTime = elapsedTime;
    }
    
    public void setNumJobsTotal(int numJobsTotal) {
      this.numJobsTotal = numJobsTotal;
    }
    
    public void setNumJobsCompleted(int numJobsCompleted) {
      this.numJobsCompleted = numJobsCompleted;
    }
    
    public void setParentWorkflowId(String parentWorkflowId) {
      this.parentWorkflowId = parentWorkflowId;
    }
    
    public void setWorkflowContext(WorkflowContext workflowContext) {
      this.workflowContext = workflowContext;
    }
    
    public long getInputBytes() {
      return inputBytes;
    }
    
    public void setInputBytes(long inputBytes) {
      this.inputBytes = inputBytes;
    }
    
    public long getOutputBytes() {
      return outputBytes;
    }
    
    public void setOutputBytes(long outputBytes) {
      this.outputBytes = outputBytes;
    }
  }
  
  public Workflows() {}
  
  public List<WorkflowDBEntry> getWorkflows() {
    return workflows;
  }
  
  public void setWorkflows(List<WorkflowDBEntry> workflows) {
    this.workflows = workflows;
  }
  
  public Summary getSummary() {
    return summary;
  }
  
  public void setSummary(Summary summary) {
    this.summary = summary;
  }
}
