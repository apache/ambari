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

import org.apache.ambari.eventdb.model.Jobs.JobDBEntry;
import org.apache.ambari.eventdb.model.Jobs.JobDBEntry.JobFields;
import org.apache.ambari.eventdb.model.TaskAttempt;
import org.apache.ambari.eventdb.model.TaskAttempt.TaskAttemptFields;
import org.apache.ambari.eventdb.model.WorkflowContext;
import org.apache.ambari.eventdb.model.Workflows.WorkflowDBEntry;
import org.apache.ambari.eventdb.model.Workflows.WorkflowDBEntry.WorkflowFields;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class PostgresConnector implements DBConnector {
  private static Log LOG = LogFactory.getLog(PostgresConnector.class);
  private static final String WORKFLOW_TABLE_NAME = "workflow";
  private static final String JOB_TABLE_NAME = "job";
  private static final String TASK_ATTEMPT_TABLE_NAME = "taskattempt";
  private static final String AGGREGATE_INPUTBYTES = "inputBytes";
  private static final String AGGREGATE_OUTPUTBYTES = "outputBytes";
  
  private static final ObjectMapper jsonMapper = new ObjectMapper();
  
  private Connection db;
  
  public static enum Statements {
    SJ_INSERT_JOB_PS(""),
    SJ_CHECK_WORKFLOW_PS(""),
    SJ_INSERT_WORKFLOW_PS(""),
    UJ_UPDATE_JOB_PS(""),
    UJ_UPDATE_WORKFLOW_PS(""),
    FW_PS("SELECT " + WorkflowDBEntry.WORKFLOW_FIELDS + " FROM " + WORKFLOW_TABLE_NAME),
    FJD_PS("SELECT " + JobDBEntry.JOB_FIELDS + " FROM " + JOB_TABLE_NAME + " WHERE " + JobFields.WORKFLOWID.toString() + " = ?"),
    WORKFLOW_AGGREGATE_IO("SELECT SUM(" + JobFields.INPUTBYTES + ") as " + AGGREGATE_INPUTBYTES + ", SUM(" + JobFields.OUTPUTBYTES + ") as "
        + AGGREGATE_OUTPUTBYTES + " FROM " + JOB_TABLE_NAME + " WHERE " + JobFields.WORKFLOWID.toString() + " = ?"),
    FJSS_PS("SELECT " + JobFields.SUBMITTIME + ", " + JobFields.FINISHTIME + " FROM " + JOB_TABLE_NAME + " WHERE " + JobFields.JOBID + " = ?"),
    FTA_PS("SELECT " + TaskAttempt.TASK_ATTEMPT_FIELDS + " FROM " + TASK_ATTEMPT_TABLE_NAME + " WHERE " + TaskAttemptFields.JOBID + " = ? AND "
        + TaskAttemptFields.TASKTYPE + " = ? ORDER BY " + TaskAttemptFields.STARTTIME);
    
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
//    PreparedStatement insertJobPS = getPS(Statements.SJ_INSERT_JOB_PS);
//    PreparedStatement checkWorkflowPS = getPS(Statements.SJ_CHECK_WORKFLOW_PS);
//    PreparedStatement insertWorkflowPS = getPS(Statements.SJ_INSERT_WORKFLOW_PS);
    throw new NotImplementedException();
  }
  
  @Override
  public void updateJob(JobDBEntry j) throws IOException {
//    PreparedStatement updateJobPS = getPS(Statements.UJ_UPDATE_JOB_PS);
//    PreparedStatement updateWorkflowPS = getPS(Statements.UJ_UPDATE_WORKFLOW_PS);
    throw new NotImplementedException();
  }
  
  @Override
  public List<WorkflowDBEntry> fetchWorkflows() throws IOException {
    PreparedStatement ps = getPS(Statements.FW_PS);
    List<WorkflowDBEntry> workflows = new ArrayList<WorkflowDBEntry>();
    ResultSet rs = null;
    try {
      rs = ps.executeQuery();
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
        // For each work flow get the aggregate i/o counts from the job table. TODO: clean up the data structures
        //
        try {
          PreparedStatement aggregateio = getPS(Statements.WORKFLOW_AGGREGATE_IO);
          aggregateio.setString(1, w.getWorkflowId());
          ResultSet ioresult = aggregateio.executeQuery();
          if (ioresult.next()) {
            w.setInputBytes(ioresult.getLong(AGGREGATE_INPUTBYTES));
            w.setOutputBytes(ioresult.getLong(AGGREGATE_OUTPUTBYTES));
          }
        } catch (SQLException e) {
          throw new IOException(e);
        }
        workflows.add(w);
      }

    } catch (SQLException e) {
      throw new IOException(e);
    }finally {
        try{
        if (rs!=null)
            rs.close();
        } catch (SQLException e) {
            LOG.error("Exception while closing ResultSet",e);
        }
    }
    return workflows;
  }
  
  @Override
  public List<JobDBEntry> fetchJobDetails(String workflowId) throws IOException {
    PreparedStatement ps = getPS(Statements.FJD_PS);
    List<JobDBEntry> jobs = new ArrayList<JobDBEntry>();
      ResultSet rs = null;
    try {
      ps.setString(1, workflowId);
      rs = ps.executeQuery();
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
    }finally {
        if(rs!=null) try {
            rs.close();
        } catch (SQLException e) {
            LOG.error("Exception while closing ResultSet",e);
        }

    }
    return jobs;
  }
  
  @Override
  public long[] fetchJobStartStopTimes(String jobID) throws IOException {
    PreparedStatement ps = getPS(Statements.FJSS_PS);
    long[] times = new long[2];
      ResultSet rs = null;
    try {
      ps.setString(1, jobID);
      rs = ps.executeQuery();
      if (!rs.next())
        return null;
      times[0] = JobFields.SUBMITTIME.getLong(rs);
      times[1] = JobFields.FINISHTIME.getLong(rs);
      rs.close();
    } catch (SQLException e) {
      throw new IOException(e);
    }finally {
        if (rs!=null) try {
            rs.close();
        } catch (SQLException e) {
            LOG.error("Exception while closing ResultSet",e);
        }
    }
    if (times[1] == 0)
      times[1] = System.currentTimeMillis();
    if (times[1] < times[0])
      times[1] = times[0];
    return times;
  }
  
  @Override
  public List<TaskAttempt> fetchTaskAttempts(String jobID, String taskType) throws IOException {
    PreparedStatement ps = getPS(Statements.FTA_PS);
    List<TaskAttempt> taskAttempts = new ArrayList<TaskAttempt>();
    ResultSet rs = null;
    try {
      ps.setString(1, jobID);
      ps.setString(2, taskType);
      rs = ps.executeQuery();
      while (rs.next()) {
        TaskAttempt t = new TaskAttempt();
        t.setFinishTime(TaskAttemptFields.FINISHTIME.getLong(rs));
        t.setInputBytes(TaskAttemptFields.INPUTBYTES.getLong(rs));
        t.setLocality(TaskAttemptFields.LOCALITY.getString(rs));
        t.setMapFinishTime(TaskAttemptFields.MAPFINISHTIME.getLong(rs));
        t.setOutputBytes(TaskAttemptFields.OUTPUTBYTES.getLong(rs));
        t.setShuffleFinishTime(TaskAttemptFields.SHUFFLEFINISHTIME.getLong(rs));
        t.setSortFinishTime(TaskAttemptFields.SORTFINISHTIME.getLong(rs));
        t.setStartTime(TaskAttemptFields.STARTTIME.getLong(rs));
        t.setStatus(TaskAttemptFields.STATUS.getString(rs));
        t.setTaskAttemptId(TaskAttemptFields.TASKATTEMPTID.getString(rs));
        t.setTaskType(TaskAttemptFields.TASKTYPE.getString(rs));
        taskAttempts.add(t);
      }
      rs.close();
    } catch (SQLException e) {
      throw new IOException(e);
    }finally {
        if (rs!=null) try {
            rs.close();
        } catch (SQLException e) {
            LOG.error("Exception while closing ResultSet",e);
        }
    }
    return taskAttempts;
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
      } catch (SQLException e) {
          LOG.error("Exception while closing connector",e);
      }
      db = null;
    }
  }
  
  @Override
  protected void finalize() throws Throwable {
    close();
  }
}
