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
package org.apache.ambari.log4j.hadoop.mapreduce.jobhistory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ambari.eventdb.model.WorkflowContext;
import org.apache.ambari.eventdb.model.WorkflowDag;
import org.apache.ambari.eventdb.model.WorkflowDag.WorkflowDagEntry;
import org.apache.ambari.log4j.common.LogStoreUpdateProvider;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.mapreduce.Counter;
import org.apache.hadoop.mapreduce.CounterGroup;
import org.apache.hadoop.mapreduce.Counters;
import org.apache.hadoop.mapreduce.TaskType;
import org.apache.hadoop.tools.rumen.HistoryEvent;
import org.apache.hadoop.tools.rumen.JhCounter;
import org.apache.hadoop.tools.rumen.JhCounterGroup;
import org.apache.hadoop.tools.rumen.JhCounters;
import org.apache.hadoop.tools.rumen.JobFinishedEvent;
import org.apache.hadoop.tools.rumen.JobInfoChangeEvent;
import org.apache.hadoop.tools.rumen.JobInitedEvent;
import org.apache.hadoop.tools.rumen.JobStatusChangedEvent;
import org.apache.hadoop.tools.rumen.JobSubmittedEvent;
import org.apache.hadoop.tools.rumen.JobUnsuccessfulCompletionEvent;
import org.apache.hadoop.tools.rumen.MapAttemptFinishedEvent;
import org.apache.hadoop.tools.rumen.ReduceAttemptFinishedEvent;
import org.apache.hadoop.tools.rumen.TaskAttemptFinishedEvent;
import org.apache.hadoop.tools.rumen.TaskAttemptStartedEvent;
import org.apache.hadoop.tools.rumen.TaskAttemptUnsuccessfulCompletionEvent;
import org.apache.hadoop.tools.rumen.TaskFailedEvent;
import org.apache.hadoop.tools.rumen.TaskFinishedEvent;
import org.apache.hadoop.tools.rumen.TaskStartedEvent;
import org.apache.hadoop.util.StringUtils;
import org.apache.log4j.spi.LoggingEvent;
import org.codehaus.jackson.map.ObjectMapper;

public class MapReduceJobHistoryUpdater implements LogStoreUpdateProvider {
  
  private static final Log LOG = 
      LogFactory.getLog(MapReduceJobHistoryUpdater.class);
  
  private Connection connection;
  
  private static final String WORKFLOW_TABLE = "workflow";
  private static final String JOB_TABLE = "job";
  private static final String TASK_TABLE = "task";
  private static final String TASKATTEMPT_TABLE = "taskAttempt";
  
  private PreparedStatement workflowPS = null;
  private PreparedStatement workflowSelectPS = null;
  private PreparedStatement workflowUpdateTimePS = null;
  private PreparedStatement workflowUpdateNumCompletedPS = null;
  
  private Map<Class<? extends HistoryEvent>, PreparedStatement> entitySqlMap =
      new HashMap<Class<? extends HistoryEvent>, PreparedStatement>();
  
  @Override
  public void init(Connection connection) throws IOException {
    this.connection = connection;
    
    try {
      initializePreparedStatements();
    } catch (SQLException sqle) {
      throw new IOException(sqle);
    }
  }
  
  private void initializePreparedStatements() throws SQLException {
    initializeJobPreparedStatements();
    initializeTaskPreparedStatements();
    initializeTaskAttemptPreparedStatements();
  }
  
  private PreparedStatement jobEndUpdate;
  
  private void initializeJobPreparedStatements() throws SQLException {

    /** 
     * Job events
     */

    // JobSubmittedEvent

    PreparedStatement jobSubmittedPrepStmnt =
        connection.prepareStatement(
            "INSERT INTO " + 
                JOB_TABLE + 
                " (" +
                "jobId, " +
                "jobName, " +
                "userName, " +
                "confPath, " +
                "queue, " +
                "submitTime, " +
                "workflowId, " +
                "workflowEntityName " +
                ") " +
                "VALUES" +
                " (?, ?, ?, ?, ?, ?, ?, ?)"
            );
    entitySqlMap.put(JobSubmittedEvent.class, jobSubmittedPrepStmnt);
    
    workflowSelectPS =
        connection.prepareStatement(
            "SELECT workflowId FROM " + WORKFLOW_TABLE + " where workflowId = ?"
            );

    workflowPS = 
        connection.prepareStatement(
            "INSERT INTO " +
                WORKFLOW_TABLE +
                " (" +
                "workflowId, " +
                "workflowName, " +
                "workflowContext, " +
                "userName, " +
                "startTime, " +
                "lastUpdateTime, " +
                "numJobsTotal, " +
                "numJobsCompleted" +
                ") " +
                "VALUES" +
                " (?, ?, ?, ?, ?, ?, ?, ?)"
            );
    
    workflowUpdateTimePS =
        connection.prepareStatement(
            "UPDATE " +
                WORKFLOW_TABLE +
                " SET " +
                "lastUpdateTime = ? " +
                "WHERE workflowId = ?"
            );
    
    workflowUpdateNumCompletedPS =
        connection.prepareStatement(
            "UPDATE " +
                WORKFLOW_TABLE +
                " SET " +
                "lastUpdateTime = ?, " +
                "numJobsCompleted = numJobsCompleted + 1 " +
                "WHERE workflowId = " +
                "(SELECT workflowId FROM " +
                JOB_TABLE +
                " WHERE jobId = ?)"
            );
    
    // JobFinishedEvent

    PreparedStatement jobFinishedPrepStmnt = 
        connection.prepareStatement(
            "UPDATE " +
                JOB_TABLE +
                " SET " +
                "finishTime = ?, " +
                "finishedMaps = ?, " +
                "finishedReduces= ?, " +
                "failedMaps = ?, " +
                "failedReduces = ?, " +
                "inputBytes = ?, " +
                "outputBytes = ? " +
                "WHERE " +
                "jobId = ?" 
            );
    entitySqlMap.put(JobFinishedEvent.class, jobFinishedPrepStmnt);

    // JobInitedEvent
    
    PreparedStatement jobInitedPrepStmnt = 
        connection.prepareStatement(
            "UPDATE " +
                JOB_TABLE +
                " SET " +
                "launchTime = ?, " +
                "maps = ?, " +
                "reduces = ?, " +
                "status = ? "+
                "WHERE " +
                "jobId = ?" 
            );
    entitySqlMap.put(JobInitedEvent.class, jobInitedPrepStmnt);

    // JobStatusChangedEvent
    
    PreparedStatement jobStatusChangedPrepStmnt = 
        connection.prepareStatement(
            "UPDATE " +
                JOB_TABLE +
                " SET " +
                "status = ? "+
                "WHERE " +
                "jobId = ?" 
            );
    entitySqlMap.put(JobStatusChangedEvent.class, jobStatusChangedPrepStmnt);

    // JobInfoChangedEvent
    
    PreparedStatement jobInfoChangedPrepStmnt = 
        connection.prepareStatement(
            "UPDATE " +
                JOB_TABLE +
                " SET " +
                "submitTime = ?, " +
                "launchTime = ? " +
                "WHERE " +
                "jobId = ?" 
            );
    entitySqlMap.put(JobInfoChangeEvent.class, jobInfoChangedPrepStmnt);

    // JobUnsuccessfulCompletionEvent
    PreparedStatement jobUnsuccessfulPrepStmnt = 
        connection.prepareStatement(
            "UPDATE " +
                JOB_TABLE +
                " SET " +
                "finishTime = ?, " +
                "finishedMaps = ?, " +
                "finishedReduces = ?, " +
                "status = ? " +
                "WHERE " +
                "jobId = ?" 
            );
    entitySqlMap.put(
        JobUnsuccessfulCompletionEvent.class, jobUnsuccessfulPrepStmnt);

    // Job update at the end
    jobEndUpdate =
        connection.prepareStatement(
            "UPDATE " +
              JOB_TABLE +
              " SET " +
              " mapsRuntime = (" +
                "SELECT " +
                "SUM(" + 
                      TASKATTEMPT_TABLE + ".finishTime" +  " - " + 
                      TASKATTEMPT_TABLE + ".startTime" +
                		  ")" +
                " FROM " +
                TASKATTEMPT_TABLE + 
                " WHERE " +
                TASKATTEMPT_TABLE + ".jobId = " + JOB_TABLE + ".jobId " +
                " AND " +
                TASKATTEMPT_TABLE + ".taskType = ?)" +
              ", " +
              " reducesRuntime = (" +
                "SELECT SUM(" + 
                             TASKATTEMPT_TABLE + ".finishTime" +  " - " + 
                             TASKATTEMPT_TABLE + ".startTime" +
                		        ")" +
              	" FROM " +
                TASKATTEMPT_TABLE + 
                " WHERE " +
                TASKATTEMPT_TABLE + ".jobId = " + JOB_TABLE + ".jobId " +
                " AND " +
                TASKATTEMPT_TABLE + ".taskType = ?) " +
              " WHERE " +
                 "jobId = ?"
            );
  }
  
  private void initializeTaskPreparedStatements() throws SQLException {

    /** 
     * Task events
     */

    // TaskStartedEvent 
    
    PreparedStatement taskStartedPrepStmnt =
        connection.prepareStatement(
            "INSERT INTO " +
                TASK_TABLE +
                " (" +
                "jobId, " +
                "taskType, " +
                "splits, " +
                "startTime, " +
                "taskId" +
                ") " +
                "VALUES (?, ?, ?, ?, ?)"
            );
    entitySqlMap.put(TaskStartedEvent.class, taskStartedPrepStmnt);
    
    // TaskFinishedEvent
    
    PreparedStatement taskFinishedPrepStmnt =
        connection.prepareStatement(
            "UPDATE " +
                TASK_TABLE +
                " SET " +
                "jobId = ?, " +
                "taskType = ?, " +
                "status = ?, " +
                "finishTime = ? " +
                " WHERE " +
                "taskId = ?"
            );
    entitySqlMap.put(TaskFinishedEvent.class, taskFinishedPrepStmnt);

    // TaskFailedEvent

    PreparedStatement taskFailedPrepStmnt =
        connection.prepareStatement(
            "UPDATE " + 
                TASK_TABLE + 
                " SET " +
                "jobId = ?, " +
                "taskType = ?, " +
                "status = ?, " +
                "finishTime = ?, " +
                "error = ?, " +
                "failedAttempt = ? " +
                "WHERE " +
                "taskId = ?"
            );
    entitySqlMap.put(TaskFailedEvent.class, taskFailedPrepStmnt);
  }

  private void initializeTaskAttemptPreparedStatements() throws SQLException {

    /**
     * TaskAttempt events
     */

    // TaskAttemptStartedEvent
    
    PreparedStatement taskAttemptStartedPrepStmnt =
        connection.prepareStatement(
            "INSERT INTO " +
                TASKATTEMPT_TABLE +
                " (" +
                "jobId, " +
                "taskId, " +
                "taskType, " +
                "startTime, " +
                "taskTracker, " +
                "locality, " +
                "avataar, " +
                "taskAttemptId" +
                ") " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
            );
    entitySqlMap.put(
        TaskAttemptStartedEvent.class, taskAttemptStartedPrepStmnt);

    // TaskAttemptFinishedEvent
    
    PreparedStatement taskAttemptFinishedPrepStmnt =
        connection.prepareStatement(
            "UPDATE " +
                TASKATTEMPT_TABLE +
                " SET " +
                "jobId = ?, " +
                "taskId = ?, " +
                "taskType = ?, " +
                "finishTime = ?, " +
                "status = ?, " +
                "taskTracker = ? " +
                " WHERE " +
                "taskAttemptId = ?"
            );
    entitySqlMap.put(
        TaskAttemptFinishedEvent.class, taskAttemptFinishedPrepStmnt);

    // TaskAttemptUnsuccessfulEvent
    
    PreparedStatement taskAttemptUnsuccessfulPrepStmnt =
        connection.prepareStatement(
            "UPDATE " +
                TASKATTEMPT_TABLE +
                " SET " +
                "jobId = ?, " +
                "taskId = ?, " +
                "taskType = ?, " +
                "finishTime = ?, " +
                "status = ?, " +
                "taskTracker = ?, " +
                "error = ? " +
                " WHERE " +
                "taskAttemptId = ?"
            );
    entitySqlMap.put(
        TaskAttemptUnsuccessfulCompletionEvent.class, 
        taskAttemptUnsuccessfulPrepStmnt);

    // MapAttemptFinishedEvent
    
    PreparedStatement mapAttemptFinishedPrepStmnt =
        connection.prepareStatement(
            "UPDATE " +
                TASKATTEMPT_TABLE +
                " SET " +
                "jobId = ?, " +
                "taskId = ?, " +
                "taskType = ?, " +
                "mapFinishTime = ?, " +
                "finishTime = ?, " +
                "inputBytes = ?, " +
                "outputBytes = ?, " +
                "status = ?, " +
                "taskTracker = ? " +
                " WHERE " +
                "taskAttemptId = ?"
            );
    entitySqlMap.put(
        MapAttemptFinishedEvent.class, mapAttemptFinishedPrepStmnt);

    // ReduceAttemptFinishedEvent
    
    PreparedStatement reduceAttemptFinishedPrepStmnt =
        connection.prepareStatement(
            "UPDATE " +
                TASKATTEMPT_TABLE +
                " SET " +
                "jobId = ?, " +
                "taskId = ?, " +
                "taskType = ?, " +
                "shuffleFinishTime = ?, " +
                "sortFinishTime = ?, " +
                "finishTime = ?, " +
                "inputBytes = ?, " +
                "outputBytes = ?, " +
                "status = ?, " +
                "taskTracker = ? " +
                " WHERE " +
                "taskAttemptId = ?"
            );
    entitySqlMap.put(
        ReduceAttemptFinishedEvent.class, reduceAttemptFinishedPrepStmnt);
  }

  private void doUpdates(LoggingEvent originalEvent,
      Object parsedEvent) throws SQLException {
    Class<?> eventClass = parsedEvent.getClass();
    
    PreparedStatement entityPS = entitySqlMap.get(eventClass);
    if (entityPS == null) {
      LOG.debug("No prepared statement for " + eventClass);
      return;
    }
  
    if (eventClass == JobSubmittedEvent.class) {
      processJobSubmittedEvent(entityPS, workflowSelectPS, workflowPS, 
          workflowUpdateTimePS, originalEvent, 
          (JobSubmittedEvent)parsedEvent);
    } else if (eventClass == JobFinishedEvent.class) {
      processJobFinishedEvent(entityPS, 
          originalEvent, (JobFinishedEvent)parsedEvent);
    } else if (eventClass == JobInitedEvent.class){
      processJobInitedEvent(entityPS, 
          originalEvent, (JobInitedEvent)parsedEvent);
    } else if (eventClass == JobStatusChangedEvent.class) {
      processJobStatusChangedEvent(entityPS, workflowUpdateNumCompletedPS,
          originalEvent, (JobStatusChangedEvent)parsedEvent);
    } else if (eventClass == JobInfoChangeEvent.class) {
      processJobInfoChangeEvent(entityPS, 
          originalEvent, (JobInfoChangeEvent)parsedEvent);
    } else if (eventClass == JobUnsuccessfulCompletionEvent.class) {
      processJobUnsuccessfulEvent(entityPS, 
          originalEvent, (JobUnsuccessfulCompletionEvent)parsedEvent);
    } else if (eventClass == TaskStartedEvent.class) {
      processTaskStartedEvent(entityPS, 
          originalEvent, (TaskStartedEvent)parsedEvent);
    } else if (eventClass == TaskFinishedEvent.class) {
      processTaskFinishedEvent(entityPS, 
          originalEvent, (TaskFinishedEvent)parsedEvent);
    } else if (eventClass == TaskFailedEvent.class) {
      processTaskFailedEvent(entityPS, 
          originalEvent, (TaskFailedEvent)parsedEvent);
    } else if (eventClass == TaskAttemptStartedEvent.class) {
      processTaskAttemptStartedEvent(entityPS, 
          originalEvent, (TaskAttemptStartedEvent)parsedEvent);
    } else if (eventClass == TaskAttemptFinishedEvent.class) {
      processTaskAttemptFinishedEvent(entityPS, 
          originalEvent, (TaskAttemptFinishedEvent)parsedEvent);
    } else if (eventClass == TaskAttemptUnsuccessfulCompletionEvent.class) {
      processTaskAttemptUnsuccessfulEvent(entityPS, 
          originalEvent, (TaskAttemptUnsuccessfulCompletionEvent)parsedEvent);
    } else if (eventClass == MapAttemptFinishedEvent.class) {
      processMapAttemptFinishedEvent(entityPS, 
          originalEvent, (MapAttemptFinishedEvent)parsedEvent);
    } else if (eventClass == ReduceAttemptFinishedEvent.class) {
      processReduceAttemptFinishedEvent(entityPS, 
          originalEvent, (ReduceAttemptFinishedEvent)parsedEvent);
    }
  }
  
  private void updateJobStatsAtFinish(String jobId) {
    try {
      jobEndUpdate.setString(1, "MAP");
      jobEndUpdate.setString(2, "REDUCE");
      jobEndUpdate.setString(3, jobId);
      jobEndUpdate.executeUpdate();
    } catch (SQLException sqle) {
      LOG.info("Failed to update mapsRuntime/reducesRuntime for " + jobId, 
          sqle);
    }
  }
  
  private static WorkflowContext generateWorkflowContext(
      JobSubmittedEvent historyEvent) {
    WorkflowDag wfDag = new WorkflowDag();
    WorkflowDagEntry wfDagEntry = new WorkflowDagEntry();
    wfDagEntry.setSource("X");
    wfDag.addEntry(wfDagEntry);
    
    WorkflowContext wc = new WorkflowContext();
    wc.setWorkflowId(historyEvent.getJobId().toString().replace("job_", "mr_"));
    wc.setWorkflowName(historyEvent.getJobName());
    wc.setWorkflowEntityName("X");
    wc.setWorkflowDag(wfDag);
    return wc;
  }
  
  // this is based on the regex in org.apache.hadoop.tools.rumen.ParsedLine
  // except this assumes the format "key"="value" so that both key and value
  // are quoted and may contain escaped characters
  private static final Pattern adjPattern = 
      Pattern.compile("\"([^\"\\\\]*+(?:\\\\.[^\"\\\\]*+)*+)\"" + "=" + 
          "\"([^\"\\\\]*+(?:\\\\.[^\"\\\\]*+)*+)\" ");
  
  public static WorkflowContext buildWorkflowContext(JobSubmittedEvent historyEvent) {
    String workflowId = historyEvent.getWorkflowId()
        .replace("\\", "");
    if (workflowId.isEmpty())
      return generateWorkflowContext(historyEvent);
    String workflowName = historyEvent.getWorkflowName()
        .replace("\\", "");
    String workflowNodeName = historyEvent.getWorkflowNodeName()
        .replace("\\", "");
    String workflowAdjacencies = StringUtils.unEscapeString(
        historyEvent.getWorkflowAdjacencies(),
        StringUtils.ESCAPE_CHAR, new char[] {'"', '=', '.'});
    WorkflowContext context = new WorkflowContext();
    context.setWorkflowId(workflowId);
    context.setWorkflowName(workflowName);
    context.setWorkflowEntityName(workflowNodeName);
    WorkflowDag dag = new WorkflowDag();
    Matcher matcher = adjPattern.matcher(workflowAdjacencies);

    while(matcher.find()){
      WorkflowDagEntry dagEntry = new WorkflowDagEntry();
      dagEntry.setSource(matcher.group(1).replace("\\", ""));
      String[] values = StringUtils.getStrings(
          matcher.group(2).replace("\\", ""));
      if (values != null) {
        for (String target : values) {
          dagEntry.addTarget(target);
        }
      }
      dag.addEntry(dagEntry);
    }
    if (dag.getEntries().isEmpty()) {
      WorkflowDagEntry wfDagEntry = new WorkflowDagEntry();
      wfDagEntry.setSource(workflowNodeName);
      dag.addEntry(wfDagEntry);
    }
    context.setWorkflowDag(dag);
    return context;
  }
  
  private void processJobSubmittedEvent(
      PreparedStatement jobPS, 
      PreparedStatement workflowSelectPS, PreparedStatement workflowPS, 
      PreparedStatement workflowUpdateTimePS, LoggingEvent logEvent, 
      JobSubmittedEvent historyEvent) {

    try {
      String jobId = historyEvent.getJobId().toString();
      jobPS.setString(1, jobId);
      jobPS.setString(2, historyEvent.getJobName());
      jobPS.setString(3, historyEvent.getUserName());
      jobPS.setString(4, historyEvent.getJobConfPath());
      jobPS.setString(5, historyEvent.getJobQueueName());
      jobPS.setLong(6, historyEvent.getSubmitTime());
      
      WorkflowContext workflowContext = buildWorkflowContext(historyEvent);
      
      // Get workflow information
      boolean insertWorkflow = false;
      
      try {
        workflowSelectPS.setString(1, workflowContext.getWorkflowId());
        workflowSelectPS.execute();
        ResultSet rs = workflowSelectPS.getResultSet();
        insertWorkflow = !rs.next();
      } catch (SQLException sqle) {
        LOG.warn("workflow select failed with: ", sqle);
        insertWorkflow = false;
      }

      // Insert workflow 
      if (insertWorkflow) {
        WorkflowContext sanitizedWC = new WorkflowContext();
        sanitizedWC.setWorkflowDag(workflowContext.getWorkflowDag());
        sanitizedWC.setParentWorkflowContext(workflowContext.getParentWorkflowContext());

        String sanitizedWCString = null;
        try {
          ObjectMapper om = new ObjectMapper();
          sanitizedWCString = om.writeValueAsString(sanitizedWC);
        } catch (IOException e) {
          e.printStackTrace();
          sanitizedWCString = "";
        } 

        workflowPS.setString(1, workflowContext.getWorkflowId());
        workflowPS.setString(2, workflowContext.getWorkflowName());
        workflowPS.setString(3, sanitizedWCString);
        workflowPS.setString(4, historyEvent.getUserName());
        workflowPS.setLong(5, historyEvent.getSubmitTime());
        workflowPS.setLong(6, historyEvent.getSubmitTime());
        workflowPS.setLong(7, workflowContext.getWorkflowDag().size());
        workflowPS.setLong(8, 0);
        workflowPS.executeUpdate();
        LOG.debug("Successfully inserted workflowId = " + 
            workflowContext.getWorkflowId());
      } else {
        workflowUpdateTimePS.setLong(1, historyEvent.getSubmitTime());
        workflowUpdateTimePS.setString(2, workflowContext.getWorkflowId());
        workflowUpdateTimePS.executeUpdate();
        LOG.debug("Successfully updated workflowId = " + 
            workflowContext.getWorkflowId());
      }

      // Insert job
      jobPS.setString(7, workflowContext.getWorkflowId());
      jobPS.setString(8, workflowContext.getWorkflowEntityName());
      jobPS.executeUpdate();
      LOG.debug("Successfully inserted job = " + jobId + 
          " and workflowId = " + workflowContext.getWorkflowId());

    } catch (SQLException sqle) {
      LOG.info("Failed to store " + historyEvent.getEventType() + " for job " + 
          historyEvent.getJobId() + " into " + JOB_TABLE, sqle);
    } catch (Exception e) {
      LOG.info("Failed to store " + historyEvent.getEventType() + " for job " + 
          historyEvent.getJobId() + " into " + JOB_TABLE, e);
    }
  }
  
  private void processJobFinishedEvent(
      PreparedStatement entityPS,
      LoggingEvent logEvent, JobFinishedEvent historyEvent) {
    Counters counters = historyEvent.getMapCounters();
    long inputBytes = 0;
    if (counters != null) {
      for (CounterGroup group : counters) {
        for (Counter counter : group) {
          if (counter.getName().equals("HDFS_BYTES_READ"))
            inputBytes += counter.getValue();
        }
      }
    }
    if (historyEvent.getFinishedReduces() != 0)
      counters = historyEvent.getReduceCounters();
    long outputBytes = 0;
    if (counters != null) {
      for (CounterGroup group : counters) {
        for (Counter counter : group) {
          if (counter.getName().equals("HDFS_BYTES_WRITTEN"))
            outputBytes += counter.getValue();
        }
      }
    }
    try {
      entityPS.setLong(1, historyEvent.getFinishTime());
      entityPS.setInt(2, historyEvent.getFinishedMaps());
      entityPS.setInt(3, historyEvent.getFinishedReduces());
      entityPS.setInt(4, historyEvent.getFailedMaps());
      entityPS.setInt(5, historyEvent.getFailedReduces());
      entityPS.setLong(6, inputBytes);
      entityPS.setLong(7, outputBytes);
      entityPS.setString(8, historyEvent.getJobid().toString());
      entityPS.executeUpdate();
    } catch (SQLException sqle) {
      LOG.info("Failed to store " + historyEvent.getEventType() + " for job " + 
          historyEvent.getJobid() + " into " + JOB_TABLE, sqle);
    }
    
    updateJobStatsAtFinish(historyEvent.getJobid().toString());

  }

  private void processJobInitedEvent(
      PreparedStatement entityPS, 
      LoggingEvent logEvent, JobInitedEvent historyEvent) {
    try {
      entityPS.setLong(1, historyEvent.getLaunchTime());
      entityPS.setInt(2, historyEvent.getTotalMaps());
      entityPS.setInt(3, historyEvent.getTotalReduces());
      entityPS.setString(4, historyEvent.getStatus());
      entityPS.setString(5, historyEvent.getJobId().toString());
      entityPS.executeUpdate();
    } catch (SQLException sqle) {
      LOG.info("Failed to store " + historyEvent.getEventType() + " for job " + 
          historyEvent.getJobId() + " into " + JOB_TABLE, sqle);
    }
  }

  private void processJobStatusChangedEvent(
      PreparedStatement entityPS, 
      PreparedStatement workflowUpdateNumCompletedPS, 
      LoggingEvent logEvent, JobStatusChangedEvent historyEvent) {
    try {
      entityPS.setString(1, historyEvent.getStatus());
      entityPS.setString(2, historyEvent.getJobId().toString());
      entityPS.executeUpdate();
      if ("SUCCESS".equals(historyEvent.getStatus())) {
        workflowUpdateNumCompletedPS.setLong(1, System.currentTimeMillis());
        workflowUpdateNumCompletedPS.setString(2, 
            historyEvent.getJobId().toString());
        workflowUpdateNumCompletedPS.executeUpdate();
      }
    } catch (SQLException sqle) {
      LOG.info("Failed to store " + historyEvent.getEventType() + " for job " + 
          historyEvent.getJobId() + " into " + JOB_TABLE, sqle);
    }
  }

  private void processJobInfoChangeEvent(
      PreparedStatement entityPS, 
      LoggingEvent logEvent, JobInfoChangeEvent historyEvent) {
    try {
      entityPS.setLong(1, historyEvent.getSubmitTime());
      entityPS.setLong(2, historyEvent.getLaunchTime());
      entityPS.setString(3, historyEvent.getJobId().toString());
      entityPS.executeUpdate();
    } catch (SQLException sqle) {
      LOG.info("Failed to store " + historyEvent.getEventType() + " for job " + 
          historyEvent.getJobId() + " into " + JOB_TABLE, sqle);
    }
  }

  private void processJobUnsuccessfulEvent(
      PreparedStatement entityPS, 
      LoggingEvent logEvent, JobUnsuccessfulCompletionEvent historyEvent) {
    try {
      entityPS.setLong(1, historyEvent.getFinishTime());
      entityPS.setLong(2, historyEvent.getFinishedMaps());
      entityPS.setLong(3, historyEvent.getFinishedReduces());
      entityPS.setString(4, historyEvent.getStatus());
      entityPS.setString(5, historyEvent.getJobId().toString());
      entityPS.executeUpdate();
    } catch (SQLException sqle) {
      LOG.info("Failed to store " + historyEvent.getEventType() + " for job " + 
          historyEvent.getJobId() + " into " + JOB_TABLE, sqle);
    }
    
    updateJobStatsAtFinish(historyEvent.getJobId().toString());
  }

  private void processTaskStartedEvent(PreparedStatement entityPS,
      LoggingEvent logEvent, TaskStartedEvent historyEvent) {
    try {
      entityPS.setString(1, 
          historyEvent.getTaskId().getJobID().toString());
      entityPS.setString(2, historyEvent.getTaskType().toString());
      entityPS.setString(3, historyEvent.getSplitLocations());
      entityPS.setLong(4, historyEvent.getStartTime());
      entityPS.setString(5, historyEvent.getTaskId().toString());
      entityPS.executeUpdate();
    } catch (SQLException sqle) {
      LOG.info("Failed to store " + historyEvent.getEventType() + " for task " + 
          historyEvent.getTaskId() + " into " + TASK_TABLE, sqle);
    }
  }

  private void processTaskFinishedEvent(
      PreparedStatement entityPS,  
      LoggingEvent logEvent, TaskFinishedEvent historyEvent) {
    try {
      entityPS.setString(1, 
          historyEvent.getTaskId().getJobID().toString());
      entityPS.setString(2, historyEvent.getTaskType().toString());
      entityPS.setString(3, historyEvent.getTaskStatus());
      entityPS.setLong(4, historyEvent.getFinishTime());
      entityPS.setString(5, historyEvent.getTaskId().toString());
      entityPS.executeUpdate();
    } catch (SQLException sqle) {
      LOG.info("Failed to store " + historyEvent.getEventType() + " for task " + 
          historyEvent.getTaskId() + " into " + TASK_TABLE, sqle);
    }
  }

  private void processTaskFailedEvent(
      PreparedStatement entityPS,  
      LoggingEvent logEvent, TaskFailedEvent historyEvent) {
    try {
      entityPS.setString(1, 
          historyEvent.getTaskId().getJobID().toString());
      entityPS.setString(2, historyEvent.getTaskType().toString());
      entityPS.setString(3, historyEvent.getTaskStatus());
      entityPS.setLong(4, historyEvent.getFinishTime());
      entityPS.setString(5, historyEvent.getError());
      entityPS.setString(6, historyEvent.getFailedAttemptID().toString());
      entityPS.setString(7, historyEvent.getTaskId().toString());
      entityPS.executeUpdate();
    } catch (SQLException sqle) {
      LOG.info("Failed to store " + historyEvent.getEventType() + " for task " + 
          historyEvent.getTaskId() + " into " + TASK_TABLE, sqle);
    }
  }

  private void processTaskAttemptStartedEvent(
      PreparedStatement entityPS,  
      LoggingEvent logEvent, TaskAttemptStartedEvent historyEvent) {
    try {
      entityPS.setString(1, 
          historyEvent.getTaskId().getJobID().toString());
      entityPS.setString(2, historyEvent.getTaskId().toString());
      entityPS.setString(3, historyEvent.getTaskType().toString());
      entityPS.setLong(4, historyEvent.getStartTime());
      entityPS.setString(5, historyEvent.getTrackerName());
      entityPS.setString(6, historyEvent.getLocality().toString());
      entityPS.setString(7, historyEvent.getAvataar().toString());
      entityPS.setString(8, historyEvent.getTaskAttemptId().toString());
      entityPS.executeUpdate();
    } catch (SQLException sqle) {
      LOG.info("Failed to store " + historyEvent.getEventType() + 
          " for taskAttempt " + historyEvent.getTaskAttemptId() + 
          " into " + TASKATTEMPT_TABLE, sqle);
    }
  }
  
  private void processTaskAttemptFinishedEvent(
      PreparedStatement entityPS,  
      LoggingEvent logEvent, TaskAttemptFinishedEvent historyEvent) {
    
    if (historyEvent.getTaskType() == TaskType.MAP || 
        historyEvent.getTaskType() == TaskType.REDUCE) {
      LOG.debug("Ignoring TaskAttemptFinishedEvent for " + 
        historyEvent.getTaskType());
      return;
    }
    
    try {
      entityPS.setString(1, 
          historyEvent.getTaskId().getJobID().toString());
      entityPS.setString(2, historyEvent.getTaskId().toString());
      entityPS.setString(3, historyEvent.getTaskType().toString());
      entityPS.setLong(4, historyEvent.getFinishTime());
      entityPS.setString(5, historyEvent.getTaskStatus());
      entityPS.setString(6, historyEvent.getHostname());
      entityPS.setString(7, historyEvent.getAttemptId().toString());
      entityPS.executeUpdate();
    } catch (SQLException sqle) {
      LOG.info("Failed to store " + historyEvent.getEventType() + 
          " for taskAttempt " + historyEvent.getAttemptId() + 
          " into " + TASKATTEMPT_TABLE, sqle);
    }
  }
  
  private void processTaskAttemptUnsuccessfulEvent(
      PreparedStatement entityPS,  
      LoggingEvent logEvent, 
      TaskAttemptUnsuccessfulCompletionEvent historyEvent) {
    try {
      entityPS.setString(1, 
          historyEvent.getTaskId().getJobID().toString());
      entityPS.setString(2, historyEvent.getTaskId().toString());
      entityPS.setString(3, historyEvent.getTaskType().toString());
      entityPS.setLong(4, historyEvent.getFinishTime());
      entityPS.setString(5, historyEvent.getTaskStatus());
      entityPS.setString(6, historyEvent.getHostname());
      entityPS.setString(7, historyEvent.getError());
      entityPS.setString(8, historyEvent.getTaskAttemptId().toString());
      entityPS.executeUpdate();
    } catch (SQLException sqle) {
      LOG.info("Failed to store " + historyEvent.getEventType() + 
          " for taskAttempt " + historyEvent.getTaskAttemptId() + 
          " into " + TASKATTEMPT_TABLE, sqle);
    }
  }
  
  private void processMapAttemptFinishedEvent(
      PreparedStatement entityPS,  
      LoggingEvent logEvent, MapAttemptFinishedEvent historyEvent) {
    
    if (historyEvent.getTaskType() != TaskType.MAP) {
      LOG.debug("Ignoring MapAttemptFinishedEvent for " + 
        historyEvent.getTaskType());
      return;
    }
    
    long[] ioBytes = getInputOutputBytes(historyEvent.getCounters());

    try {
      entityPS.setString(1, 
          historyEvent.getTaskId().getJobID().toString());
      entityPS.setString(2, historyEvent.getTaskId().toString());
      entityPS.setString(3, historyEvent.getTaskType().toString());
      entityPS.setLong(4, historyEvent.getMapFinishTime());
      entityPS.setLong(5, historyEvent.getFinishTime());
      entityPS.setLong(6, ioBytes[0]);
      entityPS.setLong(7, ioBytes[1]);
      entityPS.setString(8, historyEvent.getTaskStatus());
      entityPS.setString(9, historyEvent.getHostname());
      entityPS.setString(10, historyEvent.getAttemptId().toString());
      entityPS.executeUpdate();
    } catch (SQLException sqle) {
      LOG.info("Failed to store " + historyEvent.getEventType() + 
          " for taskAttempt " + historyEvent.getAttemptId() + 
          " into " + TASKATTEMPT_TABLE, sqle);
    }
  }
  
  
  private void processReduceAttemptFinishedEvent(
      PreparedStatement entityPS,  
      LoggingEvent logEvent, ReduceAttemptFinishedEvent historyEvent) {
    if (historyEvent.getTaskType() != TaskType.REDUCE) {
      LOG.debug("Ignoring ReduceAttemptFinishedEvent for " + 
        historyEvent.getTaskType());
      return;
    }
    
    long[] ioBytes = getInputOutputBytes(historyEvent.getCounters());

    try {
      entityPS.setString(1, 
          historyEvent.getTaskId().getJobID().toString());
      entityPS.setString(2, historyEvent.getTaskId().toString());
      entityPS.setString(3, historyEvent.getTaskType().toString());
      entityPS.setLong(4, historyEvent.getShuffleFinishTime());
      entityPS.setLong(5, historyEvent.getSortFinishTime());
      entityPS.setLong(6, historyEvent.getFinishTime());
      entityPS.setLong(7, ioBytes[0]);
      entityPS.setLong(8, ioBytes[1]);
      entityPS.setString(9, historyEvent.getTaskStatus());
      entityPS.setString(10, historyEvent.getHostname());
      entityPS.setString(11, historyEvent.getAttemptId().toString());
      entityPS.executeUpdate();
    } catch (SQLException sqle) {
      LOG.info("Failed to store " + historyEvent.getEventType() + 
          " for taskAttempt " + historyEvent.getAttemptId() + 
          " into " + TASKATTEMPT_TABLE, sqle);
    }
  }
  
  public static long[] getInputOutputBytes(JhCounters counters) {
    long inputBytes = 0;
    long outputBytes = 0;
    if (counters != null) {
      for (JhCounterGroup counterGroup : counters.groups) {
        if (counterGroup.name.equals("FileSystemCounters")) {
          for (JhCounter counter : counterGroup.counts) {
            if (counter.name.equals("HDFS_BYTES_READ") || 
                counter.name.equals("FILE_BYTES_READ"))
              inputBytes += counter.value;
            else if (counter.name.equals("HDFS_BYTES_WRITTEN") || 
                counter.name.equals("FILE_BYTES_WRITTEN"))
              outputBytes += counter.value;
          }
        }
      }
    }
    return new long[]{inputBytes, outputBytes};
  }
  
  
  @Override
  public void update(LoggingEvent originalEvent, Object parsedEvent) 
      throws IOException {
    try {
      doUpdates(originalEvent, parsedEvent);
    } catch (SQLException sqle) {
      throw new IOException(sqle);
    }
  }

}
