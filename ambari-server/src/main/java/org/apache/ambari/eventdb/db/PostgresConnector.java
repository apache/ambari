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

import org.apache.ambari.eventdb.model.DataTable;
import org.apache.ambari.eventdb.model.DataTable.AvgData;
import org.apache.ambari.eventdb.model.DataTable.Summary;
import org.apache.ambari.eventdb.model.DataTable.Summary.SummaryFields;
import org.apache.ambari.eventdb.model.DataTable.Times;
import org.apache.ambari.eventdb.model.Jobs.JobDBEntry;
import org.apache.ambari.eventdb.model.Jobs.JobDBEntry.JobFields;
import org.apache.ambari.eventdb.model.TaskAttempt;
import org.apache.ambari.eventdb.model.TaskAttempt.TaskAttemptFields;
import org.apache.ambari.eventdb.model.WorkflowContext;
import org.apache.ambari.eventdb.model.Workflows;
import org.apache.ambari.eventdb.model.Workflows.WorkflowDBEntry;
import org.apache.ambari.eventdb.model.Workflows.WorkflowDBEntry.WorkflowFields;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

public class PostgresConnector implements DBConnector {
  private static Log LOG = LogFactory.getLog(PostgresConnector.class);
  private static final String WORKFLOW_TABLE_NAME = "workflow";
  private static final String JOB_TABLE_NAME = "job";
  private static final String TASK_ATTEMPT_TABLE_NAME = "taskattempt";
  public static final String SORT_ASC = "ASC";
  public static final String SORT_DESC = "DESC";
  protected static final int DEFAULT_LIMIT = 10;
  
  private static final ObjectMapper jsonMapper = new ObjectMapper();
  
  protected Connection db;
  
  public static enum Statements {
    SJ_INSERT_JOB_PS(""),
    SJ_CHECK_WORKFLOW_PS(""),
    SJ_INSERT_WORKFLOW_PS(""),
    UJ_UPDATE_JOB_PS(""),
    UJ_UPDATE_WORKFLOW_PS(""),
    FW_PS("SELECT " + WorkflowDBEntry.WORKFLOW_FIELDS + " FROM " + WORKFLOW_TABLE_NAME),
    FW_COUNT_PS("SELECT count(*) as " + SummaryFields.numRows + " FROM " + WORKFLOW_TABLE_NAME),
    FW_SUMMARY_PS("SELECT count(*) as " + SummaryFields.numRows + ", "
        + getAvg(WorkflowFields.NUMJOBSTOTAL, SummaryFields.avgJobs, SummaryFields.minJobs, SummaryFields.maxJobs) + ", "
        + getAvg(WorkflowFields.INPUTBYTES, SummaryFields.avgInput, SummaryFields.minInput, SummaryFields.maxInput) + ", "
        + getAvg(WorkflowFields.OUTPUTBYTES, SummaryFields.avgOutput, SummaryFields.minOutput, SummaryFields.maxOutput) + ", "
        + getAvg(WorkflowFields.DURATION, SummaryFields.avgDuration, SummaryFields.minDuration, SummaryFields.maxDuration) + ", min("
        + WorkflowFields.STARTTIME + ") as " + SummaryFields.oldest + ", max(" + WorkflowFields.STARTTIME + ") as " + SummaryFields.youngest + " FROM "
        + WORKFLOW_TABLE_NAME),
    FJD_PS("SELECT " + JobDBEntry.JOB_FIELDS + " FROM " + JOB_TABLE_NAME + " WHERE " + JobFields.WORKFLOWID.toString() + " = ?"),
    FJD_TIMERANGE_PS("SELECT " + JobDBEntry.JOB_FIELDS + " FROM " + JOB_TABLE_NAME + " WHERE " + JobFields.FINISHTIME.toString() + " >= ? AND "
        + JobFields.SUBMITTIME.toString() + " <= ? ORDER BY " + JobFields.WORKFLOWID + ", " + JobFields.JOBID),
    FJSS_PS("SELECT " + JobFields.SUBMITTIME + ", " + JobFields.FINISHTIME + " FROM " + JOB_TABLE_NAME + " WHERE " + JobFields.JOBID + " = ?"),
    FJTA_PS("SELECT " + TaskAttempt.TASK_ATTEMPT_FIELDS + " FROM " + TASK_ATTEMPT_TABLE_NAME + " WHERE " + TaskAttemptFields.JOBID + " = ? ORDER BY "
        + TaskAttemptFields.STARTTIME),
    FWTA_PS("SELECT " + TaskAttemptFields.join(TASK_ATTEMPT_TABLE_NAME) + " FROM " + TASK_ATTEMPT_TABLE_NAME + ", " + JOB_TABLE_NAME + " WHERE "
        + TASK_ATTEMPT_TABLE_NAME + "." + TaskAttemptFields.JOBID + " = " + JOB_TABLE_NAME + "." + JobFields.JOBID + " AND " + JOB_TABLE_NAME + "."
        + JobFields.WORKFLOWID + " = ?"
        + " ORDER BY " + TaskAttemptFields.JOBID + "," + TaskAttemptFields.STARTTIME + ", " + TaskAttemptFields.FINISHTIME),
    FTA_TIMERANGE_PS("SELECT " + TaskAttempt.TASK_ATTEMPT_FIELDS + " FROM " + TASK_ATTEMPT_TABLE_NAME + " WHERE " + TaskAttemptFields.FINISHTIME + " >= ? AND "
        + TaskAttemptFields.STARTTIME + " <= ? AND (" + TaskAttemptFields.TASKTYPE + " = 'MAP' OR  " + TaskAttemptFields.TASKTYPE + " = 'REDUCE') ORDER BY "
        + TaskAttemptFields.STARTTIME);
    
    private String statementString;
    
    Statements(String statementString) {
      this.statementString = statementString;
    }
    
    public String getStatementString() {
      return statementString;
    }
    
    private static String getAvg(WorkflowFields field, SummaryFields avg, SummaryFields min, SummaryFields max) {
      return "avg(" + field + ") as " + avg + ", min(" + field + ") as " + min + ", max(" + field + ") as " + max;
    }
  }
  
  private Map<Statements,PreparedStatement> preparedStatements = new EnumMap<Statements,PreparedStatement>(Statements.class);
  
  public PostgresConnector(String connectionURL, String driverName, String username, String password) throws IOException {
    try {
      Class.forName(driverName);
      db = DriverManager.getConnection(connectionURL, username, password);
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
    // PreparedStatement insertJobPS = getPS(Statements.SJ_INSERT_JOB_PS);
    // PreparedStatement checkWorkflowPS = getPS(Statements.SJ_CHECK_WORKFLOW_PS);
    // PreparedStatement insertWorkflowPS = getPS(Statements.SJ_INSERT_WORKFLOW_PS);
    throw new NotImplementedException();
  }
  
  @Override
  public void updateJob(JobDBEntry j) throws IOException {
    // PreparedStatement updateJobPS = getPS(Statements.UJ_UPDATE_JOB_PS);
    // PreparedStatement updateWorkflowPS = getPS(Statements.UJ_UPDATE_WORKFLOW_PS);
    throw new NotImplementedException();
  }
  
  @Override
  public Workflows fetchWorkflows() throws IOException {
    Workflows workflows = new Workflows();
    workflows.setWorkflows(fetchWorkflows(getPS(Statements.FW_PS)));
    workflows.setSummary(fetchSummary(getPS(Statements.FW_SUMMARY_PS)));
    return workflows;
  }
  
  @Override
  public Workflows fetchWorkflows(WorkflowFields field, boolean sortAscending, int offset, int limit) throws IOException {
    if (offset < 0)
      offset = 0;
    Workflows workflows = new Workflows();
    workflows.setWorkflows(fetchWorkflows(getQualifiedPS(Statements.FW_PS, "", field, sortAscending, offset, limit)));
    workflows.setSummary(fetchSummary(getPS(Statements.FW_SUMMARY_PS)));
    return workflows;
  }
  
  private List<WorkflowDBEntry> fetchWorkflows(PreparedStatement ps) throws IOException {
    List<WorkflowDBEntry> workflows = new ArrayList<WorkflowDBEntry>();
    ResultSet rs = null;
    try {
      rs = ps.executeQuery();
      while (rs.next()) {
        workflows.add(getWorkflowDBEntry(rs));
      }
    } catch (SQLException e) {
      throw new IOException(e);
    } finally {
      try {
        if (rs != null){
          rs.close();
        }
        if (ps != null) {
          ps.close();
        }
      } catch (SQLException e) {
        LOG.error("Exception while closing ResultSet", e);
      }
    }
    return workflows;
  }
  
  private Summary fetchSummary(PreparedStatement ps) throws IOException {
    Summary summary = new Summary();
    ResultSet rs = null;
    try {
      rs = ps.executeQuery();
      if (rs.next()) {
        summary.setNumRows(SummaryFields.numRows.getInt(rs));
        summary.setJobs(getAvgData(rs, SummaryFields.avgJobs, SummaryFields.minJobs, SummaryFields.maxJobs));
        summary.setInput(getAvgData(rs, SummaryFields.avgInput, SummaryFields.minInput, SummaryFields.maxInput));
        summary.setOutput(getAvgData(rs, SummaryFields.avgOutput, SummaryFields.minOutput, SummaryFields.maxOutput));
        summary.setDuration(getAvgData(rs, SummaryFields.avgDuration, SummaryFields.minDuration, SummaryFields.maxDuration));
        Times times = new Times();
        times.setYoungest(SummaryFields.youngest.getLong(rs));
        times.setOldest(SummaryFields.oldest.getLong(rs));
        summary.setTimes(times);
      }
    } catch (SQLException e) {
      throw new IOException(e);
    } finally {
      try {
        if (rs != null) {
          rs.close();
        }
        if (ps != null) {
          ps.close();
        }        
      } catch (SQLException e) {
        LOG.error("Exception while closing ResultSet", e);
      }
    }
    return summary;
  }
  
  private static WorkflowDBEntry getWorkflowDBEntry(ResultSet rs) throws SQLException, JsonParseException, JsonMappingException, IOException {
    WorkflowDBEntry w = new WorkflowDBEntry();
    w.setWorkflowId(WorkflowFields.WORKFLOWID.getString(rs));
    w.setWorkflowName(WorkflowFields.WORKFLOWNAME.getString(rs));
    w.setUserName(WorkflowFields.USERNAME.getString(rs));
    w.setStartTime(WorkflowFields.STARTTIME.getLong(rs));
    w.setElapsedTime(WorkflowFields.DURATION.getLong(rs));
    w.setNumJobsTotal(WorkflowFields.NUMJOBSTOTAL.getInt(rs));
    w.setInputBytes(WorkflowFields.INPUTBYTES.getLong(rs));
    w.setOutputBytes(WorkflowFields.OUTPUTBYTES.getLong(rs));
    w.setNumJobsCompleted(WorkflowFields.NUMJOBSCOMPLETED.getInt(rs));
    w.setWorkflowContext(jsonMapper.readValue(WorkflowFields.WORKFLOWCONTEXT.getString(rs), WorkflowContext.class));
    return w;
  }
  
  private static AvgData getAvgData(ResultSet rs, SummaryFields avg, SummaryFields min, SummaryFields max) throws SQLException {
    AvgData avgData = new AvgData();
    avgData.setAvg(avg.getDouble(rs));
    avgData.setMin(min.getLong(rs));
    avgData.setMax(max.getLong(rs));
    return avgData;
  }
  
  @Override
  public DataTable fetchWorkflows(int offset, int limit, String searchTerm, int echo, WorkflowFields col, boolean sortAscending, String searchWorkflowId,
      String searchWorkflowName, String searchWorkflowType, String searchUserName, int minJobs, int maxJobs, long minInputBytes, long maxInputBytes,
      long minOutputBytes, long maxOutputBytes, long minDuration, long maxDuration, long minStartTime, long maxStartTime, long minFinishTime, long maxFinishTime)
      throws IOException {
    int total = 0;
    PreparedStatement ps = getPS(Statements.FW_COUNT_PS);
    ResultSet rs = null;
    try {
      rs = ps.executeQuery();
      if (rs.next())
        total = SummaryFields.numRows.getInt(rs);
    } catch (SQLException e) {
      throw new IOException(e);
    } finally {
      try {
        if (rs != null)
          rs.close();
      } catch (SQLException e) {
        LOG.error("Exception while closing ResultSet", e);
      }
    }
    
    String searchClause = buildSearchClause(searchTerm, searchWorkflowId, searchWorkflowName, searchWorkflowType, searchUserName, minJobs, maxJobs,
        minInputBytes, maxInputBytes, minOutputBytes, maxOutputBytes, minDuration, maxDuration, minStartTime, maxStartTime, minFinishTime, maxFinishTime);
    List<WorkflowDBEntry> workflows = fetchWorkflows(getQualifiedPS(Statements.FW_PS, searchClause, col, sortAscending, offset, limit));
    Summary summary = fetchSummary(getQualifiedPS(Statements.FW_SUMMARY_PS, searchClause));
    DataTable table = new DataTable();
    table.setiTotalRecords(total);
    table.setiTotalDisplayRecords(summary.getNumRows());
    if (workflows.isEmpty()) {
      table.setStartIndex(-1);
      table.setEndIndex(-1);
    } else {
      table.setStartIndex(offset);
      table.setEndIndex(offset + workflows.size() - 1);
    }
    table.setAaData(workflows);
    table.setsEcho(echo);
    table.setSummary(summary);
    return table;
  }
  
  private static JobDBEntry getJobDBEntry(ResultSet rs) throws SQLException {
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
    return j;
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
        jobs.add(getJobDBEntry(rs));
      }
      rs.close();
    } catch (SQLException e) {
      throw new IOException(e);
    } finally {
      if (rs != null)
        try {
          rs.close();
        } catch (SQLException e) {
          LOG.error("Exception while closing ResultSet", e);
        }
      
    }
    return jobs;
  }
  
  @Override
  public List<JobDBEntry> fetchJobDetails(long minFinishTime, long maxStartTime) throws IOException {
    PreparedStatement ps = getPS(Statements.FJD_TIMERANGE_PS);
    List<JobDBEntry> jobs = new ArrayList<JobDBEntry>();
    ResultSet rs = null;
    try {
      ps.setLong(1, minFinishTime);
      ps.setLong(2, maxStartTime);
      rs = ps.executeQuery();
      while (rs.next()) {
        jobs.add(getJobDBEntry(rs));
      }
      rs.close();
    } catch (SQLException e) {
      throw new IOException(e);
    } finally {
      if (rs != null)
        try {
          rs.close();
        } catch (SQLException e) {
          LOG.error("Exception while closing ResultSet", e);
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
    } finally {
      if (rs != null)
        try {
          rs.close();
        } catch (SQLException e) {
          LOG.error("Exception while closing ResultSet", e);
        }
    }
    if (times[1] == 0)
      times[1] = System.currentTimeMillis();
    if (times[1] < times[0])
      times[1] = times[0];
    return times;
  }
  
  private static TaskAttempt getTaskAttempt(ResultSet rs) throws SQLException {
    TaskAttempt t = new TaskAttempt();
    t.setFinishTime(TaskAttemptFields.FINISHTIME.getLong(rs));
    t.setInputBytes(TaskAttemptFields.INPUTBYTES.getLong(rs));
    t.setJobId(TaskAttemptFields.JOBID.getString(rs));
    t.setLocality(TaskAttemptFields.LOCALITY.getString(rs));
    t.setMapFinishTime(TaskAttemptFields.MAPFINISHTIME.getLong(rs));
    t.setOutputBytes(TaskAttemptFields.OUTPUTBYTES.getLong(rs));
    t.setShuffleFinishTime(TaskAttemptFields.SHUFFLEFINISHTIME.getLong(rs));
    t.setSortFinishTime(TaskAttemptFields.SORTFINISHTIME.getLong(rs));
    t.setStartTime(TaskAttemptFields.STARTTIME.getLong(rs));
    t.setStatus(TaskAttemptFields.STATUS.getString(rs));
    t.setTaskAttemptId(TaskAttemptFields.TASKATTEMPTID.getString(rs));
    t.setTaskType(TaskAttemptFields.TASKTYPE.getString(rs));
    return t;
  }
  
  @Override
  public List<TaskAttempt> fetchTaskAttempts(long minFinishTime, long maxStartTime) throws IOException {
    PreparedStatement ps = getPS(Statements.FTA_TIMERANGE_PS);
    List<TaskAttempt> taskAttempts = new ArrayList<TaskAttempt>();
    ResultSet rs = null;
    try {
      ps.setLong(1, minFinishTime);
      ps.setLong(2, maxStartTime);
      rs = ps.executeQuery();
      while (rs.next()) {
        taskAttempts.add(getTaskAttempt(rs));
      }
      rs.close();
    } catch (SQLException e) {
      throw new IOException(e);
    } finally {
      if (rs != null)
        try {
          rs.close();
        } catch (SQLException e) {
          LOG.error("Exception while closing ResultSet", e);
        }
    }
    return taskAttempts;
  }
  
  @Override
  public List<TaskAttempt> fetchJobTaskAttempts(String jobID) throws IOException {
    PreparedStatement ps = getPS(Statements.FJTA_PS);
    List<TaskAttempt> taskAttempts = new ArrayList<TaskAttempt>();
    ResultSet rs = null;
    try {
      ps.setString(1, jobID);
      rs = ps.executeQuery();
      while (rs.next()) {
        taskAttempts.add(getTaskAttempt(rs));
      }
      rs.close();
    } catch (SQLException e) {
      throw new IOException(e);
    } finally {
      if (rs != null)
        try {
          rs.close();
        } catch (SQLException e) {
          LOG.error("Exception while closing ResultSet", e);
        }
    }
    return taskAttempts;
  }
  
  @Override
  public List<TaskAttempt> fetchWorkflowTaskAttempts(String workflowId) throws IOException {
    PreparedStatement ps = getPS(Statements.FWTA_PS);
    List<TaskAttempt> taskAttempts = new ArrayList<TaskAttempt>();
    ResultSet rs = null;
    try {
      ps.setString(1, workflowId);
      rs = ps.executeQuery();
      while (rs.next()) {
        taskAttempts.add(getTaskAttempt(rs));
      }
      rs.close();
    } catch (SQLException e) {
      throw new IOException(e);
    } finally {
      if (rs != null)
        try {
          rs.close();
        } catch (SQLException e) {
          LOG.error("Exception while closing ResultSet", e);
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
          // LOG.debug("preparing " + statement.getStatementString());
          preparedStatements.put(statement, db.prepareStatement(statement.getStatementString()));
        } catch (SQLException e) {
          throw new IOException(e);
        }
      }
    }
    
    return preparedStatements.get(statement);
  }
  
  protected PreparedStatement getQualifiedPS(Statements statement, String searchClause) throws IOException {
    if (db == null)
      throw new IOException("postgres db not initialized");
    try {
      // LOG.debug("preparing " + statement.getStatementString() + searchClause);
      return db.prepareStatement(statement.getStatementString() + searchClause);
    } catch (SQLException e) {
      throw new IOException(e);
    }
  }
  
  protected PreparedStatement getQualifiedPS(Statements statement, String searchClause, WorkflowFields field, boolean sortAscending, int offset, int limit)
      throws IOException {
    if (db == null)
      throw new IOException("postgres db not initialized");
    String limitClause = " ORDER BY " + field.toString() + " " + (sortAscending ? SORT_ASC : SORT_DESC) + " OFFSET " + offset
        + (limit >= 0 ? " LIMIT " + limit : "");
    return getQualifiedPS(statement, searchClause + limitClause);
  }
  
  private static void addRangeSearch(StringBuilder sb, WorkflowFields field, int min, int max) {
    if (min >= 0)
      append(sb, greaterThan(field, Integer.toString(min)));
    if (max >= 0)
      append(sb, lessThan(field, Integer.toString(max)));
  }
  
  private static void addRangeSearch(StringBuilder sb, WorkflowFields field, long min, long max) {
    if (min >= 0)
      append(sb, greaterThan(field, Long.toString(min)));
    if (max >= 0)
      append(sb, lessThan(field, Long.toString(max)));
  }
  
  private static void append(StringBuilder sb, String s) {
    if (sb.length() > WHERE.length())
      sb.append(" and ");
    sb.append(s);
  }
  
  private static String like(WorkflowFields field, String s) {
    return field.toString() + " like '%" + s + "%'";
  }
  
  private static String startsWith(WorkflowFields field, String s) {
    return field.toString() + " like '" + s + "%'";
  }
  
  private static String equals(WorkflowFields field, String s) {
    return field.toString() + " = '" + s + "'";
  }
  
  private static String lessThan(WorkflowFields field, String s) {
    return field.toString() + " <= " + s;
  }
  
  private static String greaterThan(WorkflowFields field, String s) {
    return field.toString() + " >= " + s;
  }
  
  private static final String WHERE = " where ";
  
  private static String buildSearchClause(String searchTerm, String searchWorkflowId, String searchWorkflowName, String searchWorkflowType,
      String searchUserName, int minJobs, int maxJobs, long minInputBytes, long maxInputBytes, long minOutputBytes, long maxOutputBytes, long minDuration,
      long maxDuration, long minStartTime, long maxStartTime, long minFinishTime, long maxFinishTime) {
    StringBuilder sb = new StringBuilder();
    sb.append(WHERE);
    if (searchTerm != null && searchTerm.length() > 0) {
      sb.append("(");
      sb.append(like(WorkflowFields.WORKFLOWID, searchTerm));
      sb.append(" or ");
      sb.append(like(WorkflowFields.WORKFLOWNAME, searchTerm));
      sb.append(" or ");
      sb.append(like(WorkflowFields.USERNAME, searchTerm));
      sb.append(")");
    }
    if (searchWorkflowId != null)
      append(sb, like(WorkflowFields.WORKFLOWID, searchWorkflowId));
    if (searchWorkflowName != null)
      append(sb, like(WorkflowFields.WORKFLOWNAME, searchWorkflowName));
    if (searchWorkflowType != null)
      append(sb, startsWith(WorkflowFields.WORKFLOWID, searchWorkflowType));
    if (searchUserName != null)
      append(sb, equals(WorkflowFields.USERNAME, searchUserName));
    addRangeSearch(sb, WorkflowFields.NUMJOBSTOTAL, minJobs, maxJobs);
    addRangeSearch(sb, WorkflowFields.INPUTBYTES, minInputBytes, maxInputBytes);
    addRangeSearch(sb, WorkflowFields.OUTPUTBYTES, minOutputBytes, maxOutputBytes);
    addRangeSearch(sb, WorkflowFields.DURATION, minDuration, maxDuration);
    addRangeSearch(sb, WorkflowFields.STARTTIME, minStartTime, maxStartTime);
    addRangeSearch(sb, WorkflowFields.LASTUPDATETIME, minFinishTime, maxFinishTime);
    
    if (sb.length() == WHERE.length())
      return "";
    else
      return sb.toString();
  }
  
  @Override
  public void close() {
    if (db != null) {
      try {
        db.close();
      } catch (SQLException e) {
        LOG.error("Exception while closing connector", e);
      }
      db = null;
    }
  }
  
  @Override
  protected void finalize() throws Throwable {
    close();
  }
}
