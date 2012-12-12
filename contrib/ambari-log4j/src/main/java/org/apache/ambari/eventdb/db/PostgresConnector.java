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
package org.apache.ambari.eventdb.db;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.eventdb.model.WorkflowContext;
import org.apache.ambari.eventdb.model.Jobs.JobDBEntry;
import org.apache.ambari.eventdb.model.Jobs.JobDBEntry.JobFields;
import org.apache.ambari.eventdb.model.Workflows.WorkflowDBEntry;
import org.apache.ambari.eventdb.model.Workflows.WorkflowDBEntry.WorkflowFields;
import org.apache.commons.lang.NotImplementedException;
import org.codehaus.jackson.map.ObjectMapper;

public class PostgresConnector implements DBConnector {
  private static final String WORKFLOW_TABLE_NAME = "workflow";
  private static final String JOB_TABLE_NAME = "job";
  
  private static final ObjectMapper jsonMapper = new ObjectMapper();
  
  private Connection db;
  
  public static enum Statements {
    SJ_INSERT_JOB_PS(""),
    SJ_CHECK_WORKFLOW_PS(""),
    SJ_INSERT_WORKFLOW_PS(""),
    UJ_UPDATE_JOB_PS(""),
    UJ_UPDATE_WORKFLOW_PS(""),
    FW_PS("SELECT " + WorkflowDBEntry.WORKFLOW_FIELDS + " FROM " + WORKFLOW_TABLE_NAME),
    FJD_PS("SELECT " + JobDBEntry.JOB_FIELDS + " FROM " + JOB_TABLE_NAME + " WHERE " + JobFields.WORKFLOWID.toString() + " = ?");
    
    private String statementString;
    
    Statements(String statementString) {
      this.statementString = statementString;
    }
    
    public String getStatementString() {
      return statementString;
    }
  }
  
  private Map<Statements,PreparedStatement> preparedStatements = new EnumMap<Statements,PreparedStatement>(Statements.class);
  
  public PostgresConnector(String hostname, String dbname, String username, String password) throws IOException {
    String url = "jdbc:postgresql://" + hostname + "/" + dbname;
    try {
      Class.forName("org.postgresql.Driver");
      db = DriverManager.getConnection(url, username, password);
    } catch (ClassNotFoundException e) {
      db = null;
      throw new IOException(e);
    } catch (SQLException e) {
      db = null;
      throw new IOException(e);
    }
  }
  
  @Override
  public void submitJob(JobDBEntry j, WorkflowContext context) throws IOException {
    PreparedStatement insertJobPS = getPS(Statements.SJ_INSERT_JOB_PS);
    PreparedStatement checkWorkflowPS = getPS(Statements.SJ_CHECK_WORKFLOW_PS);
    PreparedStatement insertWorkflowPS = getPS(Statements.SJ_INSERT_WORKFLOW_PS);
    throw new NotImplementedException();
  }
  
  @Override
  public void updateJob(JobDBEntry j) throws IOException {
    PreparedStatement updateJobPS = getPS(Statements.UJ_UPDATE_JOB_PS);
    PreparedStatement updateWorkflowPS = getPS(Statements.UJ_UPDATE_WORKFLOW_PS);
    throw new NotImplementedException();
  }
  
  @Override
  public List<WorkflowDBEntry> fetchWorkflows() throws IOException {
    PreparedStatement ps = getPS(Statements.FW_PS);
    List<WorkflowDBEntry> workflows = new ArrayList<WorkflowDBEntry>();
    try {
      ResultSet rs = ps.executeQuery();
      while (rs.next()) {
        WorkflowDBEntry w = new WorkflowDBEntry();
        w.setWorkflowId(WorkflowFields.WORKFLOWID.getString(rs));
        w.setWorkflowName(WorkflowFields.WORKFLOWNAME.getString(rs));
        w.setUserName(WorkflowFields.USERNAME.getString(rs));
        w.setStartTime(WorkflowFields.STARTTIME.getLong(rs));
        long updateTime = WorkflowFields.LASTUPDATETIME.getLong(rs);
        if (updateTime > w.getStartTime())
          w.setElapsedTime(updateTime - w.getStartTime());
        else
          w.setElapsedTime(0);
        w.setNumJobsTotal(WorkflowFields.NUMJOBSTOTAL.getInt(rs));
        w.setNumJobsCompleted(WorkflowFields.NUMJOBSCOMPLETED.getInt(rs));
        w.setWorkflowContext(jsonMapper.readValue(WorkflowFields.WORKFLOWCONTEXT.getString(rs), WorkflowContext.class));
        workflows.add(w);
      }
      rs.close();
    } catch (SQLException e) {
      throw new IOException(e);
    }
    return workflows;
  }
  
  @Override
  public List<JobDBEntry> fetchJobDetails(String workflowId) throws IOException {
    PreparedStatement ps = getPS(Statements.FJD_PS);
    List<JobDBEntry> jobs = new ArrayList<JobDBEntry>();
    try {
      ps.setString(1, workflowId);
      ResultSet rs = ps.executeQuery();
      while (rs.next()) {
        JobDBEntry j = new JobDBEntry();
        j.setConfPath(JobFields.CONFPATH.getString(rs));
        j.setSubmitTime(JobFields.SUBMITTIME.getLong(rs));
        long finishTime = JobFields.FINISHTIME.getLong(rs);
        if (finishTime > j.getSubmitTime())
          j.setElapsedTime(finishTime - j.getSubmitTime());
        else
          j.setElapsedTime(0);
        j.setInputBytes(JobFields.INPUTBYTES.getLong(rs));
        j.setJobId(JobFields.JOBID.getString(rs));
        j.setJobName(JobFields.JOBNAME.getString(rs));
        j.setMaps(JobFields.MAPS.getInt(rs));
        j.setOutputBytes(JobFields.OUTPUTBYTES.getLong(rs));
        j.setReduces(JobFields.REDUCES.getInt(rs));
        j.setStatus(JobFields.STATUS.getString(rs));
        j.setUserName(JobFields.USERNAME.getString(rs));
        j.setWorkflowEntityName(JobFields.WORKFLOWENTITYNAME.getString(rs));
        j.setWorkflowId(JobFields.WORKFLOWID.getString(rs));
        jobs.add(j);
      }
      rs.close();
    } catch (SQLException e) {
      throw new IOException(e);
    }
    return jobs;
  }
  
  private PreparedStatement getPS(Statements statement) throws IOException {
    if (db == null)
      throw new IOException("postgres db not initialized");
    
    synchronized (preparedStatements) {
      if (!preparedStatements.containsKey(statement)) {
        try {
          preparedStatements.put(statement, db.prepareStatement(statement.getStatementString()));
        } catch (SQLException e) {
          throw new IOException(e);
        }
      }
    }
    
    return preparedStatements.get(statement);
  }
  
  @Override
  public void close() {
    if (db != null) {
      try {
        db.close();
      } catch (SQLException e) {}
      db = null;
    }
  }
  
  @Override
  protected void finalize() throws Throwable {
    close();
  }
}
