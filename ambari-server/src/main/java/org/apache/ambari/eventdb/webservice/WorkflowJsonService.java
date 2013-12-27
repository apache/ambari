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
package org.apache.ambari.eventdb.webservice;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.ServletContext;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.ambari.eventdb.db.MySQLConnector;
import org.apache.ambari.eventdb.db.OracleConnector;
import org.apache.ambari.eventdb.db.PostgresConnector;
import org.apache.ambari.eventdb.model.DataTable;
import org.apache.ambari.eventdb.model.Jobs;
import org.apache.ambari.eventdb.model.Jobs.JobDBEntry;
import org.apache.ambari.eventdb.model.TaskAttempt;
import org.apache.ambari.eventdb.model.TaskData;
import org.apache.ambari.eventdb.model.TaskData.Point;
import org.apache.ambari.eventdb.model.TaskLocalityData;
import org.apache.ambari.eventdb.model.TaskLocalityData.DataPoint;
import org.apache.ambari.eventdb.model.Workflows;
import org.apache.ambari.eventdb.model.Workflows.WorkflowDBEntry;
import org.apache.ambari.eventdb.model.Workflows.WorkflowDBEntry.WorkflowFields;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/jobhistory")
public class WorkflowJsonService {
  private static final String PREFIX = "eventdb.";
  private static final String HOSTNAME = PREFIX + "db.hostname";
  private static final String DBNAME = PREFIX + "db.name";
  private static final String USERNAME = PREFIX + "db.user";
  private static final String PASSWORD = PREFIX + "db.password";
  
  private static String DEFAULT_DRIVER;
  private static String DEFAULT_URL;
  private static String DEFAULT_USERNAME = "mapred";
  private static String DEFAULT_PASSWORD = "mapred";
  
  private static final Workflows EMPTY_WORKFLOWS = new Workflows();
  private static final List<JobDBEntry> EMPTY_JOBS = Collections.emptyList();
  {
    List<WorkflowDBEntry> emptyWorkflows = Collections.emptyList();
    EMPTY_WORKFLOWS.setWorkflows(emptyWorkflows);
  }

  private static final Logger LOG = LoggerFactory.getLogger(WorkflowJsonService.class);
  
  PostgresConnector getConnector() throws IOException {
    //TODO fix temp hack
    if (StringUtils.contains(DEFAULT_DRIVER, "oracle")) {
      return new OracleConnector(DEFAULT_URL, DEFAULT_DRIVER, DEFAULT_USERNAME, DEFAULT_PASSWORD);
    }else if (StringUtils.contains(DEFAULT_DRIVER, "mysql")) {
      return new MySQLConnector(DEFAULT_URL, DEFAULT_DRIVER, DEFAULT_USERNAME, DEFAULT_PASSWORD);
    } else {
      return new PostgresConnector(DEFAULT_URL, DEFAULT_DRIVER, DEFAULT_USERNAME, DEFAULT_PASSWORD);
    }
  }

  public static void setDBProperties(Configuration configuration) {
    DEFAULT_DRIVER = configuration.getRcaDatabaseDriver();
    DEFAULT_URL = configuration.getRcaDatabaseUrl();
    if (DEFAULT_URL.contains(Configuration.HOSTNAME_MACRO)) {
      DEFAULT_URL = DEFAULT_URL.replace(Configuration.HOSTNAME_MACRO, "localhost");
    }
    DEFAULT_USERNAME = configuration.getRcaDatabaseUser();
    DEFAULT_PASSWORD = configuration.getRcaDatabasePassword();
  }
  
  @Context
  ServletContext servletContext;
  
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/workflow")
  public Workflows getWorkflows(@QueryParam("orderBy") String field, @DefaultValue(PostgresConnector.SORT_ASC) @QueryParam("sortDir") String sortDir,
      @DefaultValue("0") @QueryParam("offset") int offset, @DefaultValue("-1") @QueryParam("limit") int limit) {
    Workflows workflows = EMPTY_WORKFLOWS;
    PostgresConnector conn = null;
    try {
      conn = getConnector();
      if (field == null)
        workflows = conn.fetchWorkflows();
      else {
        field = field.toUpperCase();
        if ("ELAPSEDTIME".equals(field))
          field = "DURATION";
        workflows = conn.fetchWorkflows(WorkflowFields.valueOf(field), sortDir.toUpperCase().equals(PostgresConnector.SORT_ASC), offset, limit);
      }
    } catch (IOException e) {
      LOG.error("Error interacting with RCA database ", e);
      workflows = EMPTY_WORKFLOWS;
    } finally {
      if (conn != null) {
        conn.close();
      }
    }
    return workflows;
  }
  
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/datatable")
  public DataTable getWorkflowDataTable(@DefaultValue("0") @QueryParam("iDisplayStart") int start,
      @DefaultValue("10") @QueryParam("iDisplayLength") int amount, @QueryParam("sSearch") String searchTerm, @DefaultValue("0") @QueryParam("sEcho") int echo,
      @DefaultValue("0") @QueryParam("iSortCol_0") int col, @DefaultValue(PostgresConnector.SORT_ASC) @QueryParam("sSortDir_0") String sdir,
      @QueryParam("sSearch_0") String workflowId, @QueryParam("sSearch_1") String workflowName, @QueryParam("sSearch_2") String workflowType,
      @QueryParam("sSearch_3") String userName, @DefaultValue("-1") @QueryParam("minJobs") int minJobs, @DefaultValue("-1") @QueryParam("maxJobs") int maxJobs,
      @DefaultValue("-1") @QueryParam("minInputBytes") long minInputBytes, @DefaultValue("-1") @QueryParam("maxInputBytes") long maxInputBytes,
      @DefaultValue("-1") @QueryParam("minOutputBytes") long minOutputBytes, @DefaultValue("-1") @QueryParam("maxOutputBytes") long maxOutputBytes,
      @DefaultValue("-1") @QueryParam("minDuration") long minDuration, @DefaultValue("-1") @QueryParam("maxDuration") long maxDuration,
      @DefaultValue("-1") @QueryParam("minStartTime") long minStartTime, @DefaultValue("-1") @QueryParam("maxStartTime") long maxStartTime,
      @DefaultValue("-1") @QueryParam("minFinishTime") long minFinishTime, @DefaultValue("-1") @QueryParam("maxFinishTime") long maxFinishTime) {
    
    if (start < 0)
      start = 0;
    if (amount < 10 || amount > 100)
      amount = 10;
    
    boolean sortAscending = true;
    if (!sdir.toUpperCase().equals(PostgresConnector.SORT_ASC))
      sortAscending = false;
    
    WorkflowFields field = null;
    switch (col) {
      case 0: // workflowId
        field = WorkflowFields.WORKFLOWID;
        break;
      case 1: // workflowName
        field = WorkflowFields.WORKFLOWNAME;
        break;
      case 2: // workflowType
        field = WorkflowFields.WORKFLOWID;
        break;
      case 3: // userName
        field = WorkflowFields.USERNAME;
        break;
      case 4: // numJobsTotal
        field = WorkflowFields.NUMJOBSTOTAL;
        break;
      case 5: // inputBytes
        field = WorkflowFields.INPUTBYTES;
        break;
      case 6: // outputBytes
        field = WorkflowFields.OUTPUTBYTES;
        break;
      case 7: // duration
        field = WorkflowFields.DURATION;
        break;
      case 8: // startTime
        field = WorkflowFields.STARTTIME;
        break;
      case 9: // lastUpdateTime
        field = WorkflowFields.LASTUPDATETIME;
        break;
      default:
        field = WorkflowFields.WORKFLOWID;
    }
    
    DataTable table = null;
    PostgresConnector conn = null;
    try {
      conn = getConnector();
      table = conn.fetchWorkflows(start, amount, searchTerm, echo, field, sortAscending, workflowId, workflowName, workflowType, userName, minJobs, maxJobs,
          minInputBytes, maxInputBytes, minOutputBytes, maxOutputBytes, minDuration, maxDuration, minStartTime, maxStartTime, minFinishTime, maxFinishTime);
    } catch (IOException e) {
      LOG.error("Error interacting with RCA database ", e);
    } finally {
      if (conn != null) {
        conn.close();
      }
    }
    return table;
  }
  
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/job")
  public Jobs getJobs(@QueryParam("workflowId") String workflowId, @DefaultValue("-1") @QueryParam("startTime") long minFinishTime,
      @DefaultValue("-1") @QueryParam("endTime") long maxStartTime) {
    Jobs jobs = new Jobs();
    PostgresConnector conn = null;
    try {
      conn = getConnector();
      if (workflowId != null)
        jobs.setJobs(conn.fetchJobDetails(workflowId));
      else if (maxStartTime >= minFinishTime)
        jobs.setJobs(conn.fetchJobDetails(minFinishTime, maxStartTime));
    } catch (IOException e) {
      LOG.error("Error interacting with RCA database ", e);
      jobs.setJobs(EMPTY_JOBS);
    } finally {
      if (conn != null) {
        conn.close();
      }
    }
    return jobs;
  }
  
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/task")
  public TaskData getTaskSummary(@QueryParam("jobId") String jobId, @QueryParam("width") int steps, @QueryParam("workflowId") String workflowId,
      @DefaultValue("-1") @QueryParam("startTime") long minFinishTime, @DefaultValue("-1") @QueryParam("endTime") long maxStartTime) {
    TaskData points = new TaskData();
    PostgresConnector conn = null;
    try {
      conn = getConnector();
      List<TaskAttempt> taskAttempts = null;
      long startTime = -1;
      long endTime = -1;
      if (jobId != null) {
        long[] times = conn.fetchJobStartStopTimes(jobId);
        if (times != null) {
          startTime = times[0];
          endTime = times[1];
          taskAttempts = conn.fetchJobTaskAttempts(jobId);
        }
      } else {
        startTime = minFinishTime;
        endTime = maxStartTime;
        if (workflowId != null)
          taskAttempts = conn.fetchWorkflowTaskAttempts(workflowId);
        else
          taskAttempts = conn.fetchTaskAttempts(minFinishTime, maxStartTime);
      }
      if (startTime > 0 && endTime > 0 && endTime >= startTime) {
        double submitTimeSecs = startTime / 1000.0;
        double finishTimeSecs = endTime / 1000.0;
        double step = (finishTimeSecs - submitTimeSecs) / steps;
        if (step < 1)
          step = 1;
        if (taskAttempts != null)
          getTaskDetails(taskAttempts, points, submitTimeSecs, finishTimeSecs, step);
      }
    } catch (IOException e) {
      LOG.error("Error interacting with RCA database ", e);
    } finally {
      if (conn != null) {
        conn.close();
      }
    }
    return points;
  }
  
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/taskdetails")
  public List<TaskAttempt> getTaskDetails(@QueryParam("jobId") String jobId, @QueryParam("workflowId") String workflowId) {
    List<TaskAttempt> taskAttempts = new ArrayList<TaskAttempt>();
    PostgresConnector conn = null;
    try {
      conn = getConnector();
      if (jobId != null) {
        taskAttempts = conn.fetchJobTaskAttempts(jobId);
      } else if (workflowId != null) {
        taskAttempts = conn.fetchWorkflowTaskAttempts(workflowId);
      }
    } catch (IOException e) {
      LOG.error("Error interacting with RCA database ", e);
    } finally {
      if (conn != null) {
        conn.close();
      }
    }
    return taskAttempts;
  }
  
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/tasklocality")
  public TaskLocalityData getTaskLocalitySummary(@QueryParam("jobId") String jobId, @DefaultValue("4") @QueryParam("minr") int minr,
      @DefaultValue("24") @QueryParam("maxr") int maxr, @QueryParam("workflowId") String workflowId) {
    if (maxr < minr)
      maxr = minr;
    TaskLocalityData data = new TaskLocalityData();
    PostgresConnector conn = null;
    try {
      conn = getConnector();
      if (jobId != null) {
        long[] times = conn.fetchJobStartStopTimes(jobId);
        if (times != null) {
          getApproxTaskAttemptsByLocality(conn.fetchJobTaskAttempts(jobId), times[0], times[1], data, minr, maxr);
        }
      } else if (workflowId != null) {
        getExactTaskAttemptsByLocality(conn.fetchWorkflowTaskAttempts(workflowId), data, minr, maxr);
      }
    } catch (IOException e) {
      LOG.error("Error interacting with RCA database ", e);
    } finally {
      if (conn != null) {
        conn.close();
      }
    }
    return data;
  }
  
  private static void getTaskDetails(List<TaskAttempt> taskAttempts, TaskData points, double submitTimeSecs, double finishTimeSecs, double step)
      throws IOException {
    List<Point> mapPoints = new ArrayList<Point>();
    List<Point> shufflePoints = new ArrayList<Point>();
    List<Point> reducePoints = new ArrayList<Point>();
    for (double time = submitTimeSecs; time < finishTimeSecs; time += step) {
      int numTasks = 0;
      int numShuffleTasks = 0;
      int numReduceTasks = 0;
      for (TaskAttempt taskAttempt : taskAttempts) {
        if (taskAttempt.getTaskType().equals("MAP")) {
          if ((taskAttempt.getStartTime() / 1000.0) <= (time + step) && (taskAttempt.getFinishTime() / 1000.0) >= time)
            numTasks++;
        } else if (taskAttempt.getTaskType().equals("REDUCE")) {
          if ((taskAttempt.getStartTime() / 1000.0) <= (time + step) && (taskAttempt.getShuffleFinishTime() / 1000.0) >= time) {
            numShuffleTasks++;
          } else if ((taskAttempt.getShuffleFinishTime() / 1000.0) < (time + step) && (taskAttempt.getFinishTime() / 1000.0) >= time) {
            numReduceTasks++;
          }
        }
      }
      mapPoints.add(new Point(Math.round(time), numTasks));
      shufflePoints.add(new Point(Math.round(time), numShuffleTasks));
      reducePoints.add(new Point(Math.round(time), numReduceTasks));
    }
    points.setMapData(mapPoints);
    points.setShuffleData(shufflePoints);
    points.setReduceData(reducePoints);
  }
  
  private static void getExactTaskAttemptsByLocality(List<TaskAttempt> taskAttempts, TaskLocalityData data, int minr, int maxr) throws IOException {
    MinMax io = new MinMax();
    data.setMapNodeLocal(processExactLocalityData(taskAttempts, "MAP", "NODE_LOCAL", io));
    data.setMapRackLocal(processExactLocalityData(taskAttempts, "MAP", "RACK_LOCAL", io));
    data.setMapOffSwitch(processExactLocalityData(taskAttempts, "MAP", "OFF_SWITCH", io));
    data.setReduceOffSwitch(processExactLocalityData(taskAttempts, "REDUCE", null, io));
    setRValues(data.getMapNodeLocal(), minr, maxr, io.max);
    setRValues(data.getMapRackLocal(), minr, maxr, io.max);
    setRValues(data.getMapOffSwitch(), minr, maxr, io.max);
    setRValues(data.getReduceOffSwitch(), minr, maxr, io.max);
  }

  private static void getApproxTaskAttemptsByLocality(List<TaskAttempt> taskAttempts, long submitTime, long finishTime, TaskLocalityData data, int minr,
      int maxr) throws IOException {
    long submitTimeX = transformX(submitTime);
    long finishTimeX = transformX(finishTime);
    Set<Long> xPoints = getXPoints(taskAttempts, submitTimeX, finishTimeX);
    Long[] xList = xPoints.toArray(new Long[xPoints.size()]);
    MinMax io = new MinMax();
    data.setMapNodeLocal(processLocalityData(taskAttempts, "MAP", "NODE_LOCAL", xList, io));
    data.setMapRackLocal(processLocalityData(taskAttempts, "MAP", "RACK_LOCAL", xList, io));
    data.setMapOffSwitch(processLocalityData(taskAttempts, "MAP", "OFF_SWITCH", xList, io));
    data.setReduceOffSwitch(processLocalityData(taskAttempts, "REDUCE", "OFF_SWITCH", xList, io));
    setRValues(data.getMapNodeLocal(), minr, maxr, io.max);
    setRValues(data.getMapRackLocal(), minr, maxr, io.max);
    setRValues(data.getMapOffSwitch(), minr, maxr, io.max);
    setRValues(data.getReduceOffSwitch(), minr, maxr, io.max);
    data.setSubmitTime(submitTimeX);
    data.setFinishTime(finishTimeX);
  }
  
  private static class MinMax {
    private long min = Long.MAX_VALUE;
    private long max = 0;
  }
  
  private static long transformX(long time) {
    return Math.round(time / 1000.0);
  }
  
  private static long untransformX(long x) {
    return x * 1000;
  }
  
  private static long transformY(long time) {
    return time;
  }
  
  private static Set<Long> getXPoints(List<TaskAttempt> taskAttempts, long submitTimeX, long finishTimeX) {
    TreeSet<Long> xPoints = new TreeSet<Long>();
    TreeSet<TaskAttempt> sortedAttempts = new TreeSet<TaskAttempt>(new Comparator<TaskAttempt>() {
      @Override
      public int compare(TaskAttempt t1, TaskAttempt t2) {
        if (t1.getStartTime() < t2.getStartTime())
          return -1;
        else if (t1.getStartTime() > t2.getStartTime())
          return 1;
        return t1.getTaskAttemptId().compareTo(t2.getTaskAttemptId());
      }
    });
    sortedAttempts.addAll(taskAttempts);
    getXPoints(sortedAttempts, xPoints);
    xPoints.add(submitTimeX);
    xPoints.add(finishTimeX);
    return xPoints;
  }
  
  private static void getXPoints(Iterable<TaskAttempt> taskAttempts, Set<Long> xPoints) {
    for (TaskAttempt taskAttempt : taskAttempts) {
      long x = transformX(taskAttempt.getStartTime());
      while (xPoints.contains(x))
        x += 1;
      xPoints.add(x);
      taskAttempt.setStartTime(untransformX(x));
    }
  }
  
  private static int addDataPoint(List<DataPoint> data, DataPoint point, int index, Long[] xPoints) {
    while (index < xPoints.length) {
      if (point.getX() == xPoints[index]) {
        index++;
        break;
      } else if (point.getX() > xPoints[index]) {
        data.add(new DataPoint(xPoints[index++]));
      }
    }
    data.add(point);
    return index;
  }
  
  private static List<DataPoint> processExactLocalityData(List<TaskAttempt> taskAttempts, String taskType, String locality, MinMax io) {
    List<DataPoint> data = new ArrayList<DataPoint>();
    for (TaskAttempt taskAttempt : taskAttempts) {
      if (taskType.equals(taskAttempt.getTaskType()) && (locality == null || locality.equals(taskAttempt.getLocality()))) {
        DataPoint point = new DataPoint();
        point.setX(taskAttempt.getStartTime());
        point.setY(taskAttempt.getFinishTime() - taskAttempt.getStartTime());
        point.setIO(taskAttempt.getInputBytes() + taskAttempt.getOutputBytes());
        point.setLabel(taskAttempt.getTaskAttemptId());
        point.setStatus(taskAttempt.getStatus());
        data.add(point);
        io.max = Math.max(io.max, point.getIO());
        io.min = Math.min(io.min, point.getIO());
      }
    }
    return data;
  }

  private static List<DataPoint> processLocalityData(List<TaskAttempt> taskAttempts, String taskType, String locality, Long[] xPoints, MinMax io) {
    List<DataPoint> data = new ArrayList<DataPoint>();
    int i = 0;
    for (TaskAttempt taskAttempt : taskAttempts) {
      if (taskType.equals(taskAttempt.getTaskType()) && locality.equals(taskAttempt.getLocality())) {
        DataPoint point = new DataPoint();
        point.setX(transformX(taskAttempt.getStartTime()));
        point.setY(transformY(taskAttempt.getFinishTime() - taskAttempt.getStartTime()));
        point.setIO(taskAttempt.getInputBytes() + taskAttempt.getOutputBytes());
        point.setLabel(taskAttempt.getTaskAttemptId());
        point.setStatus(taskAttempt.getStatus());
        i = addDataPoint(data, point, i, xPoints);
        io.max = Math.max(io.max, point.getIO());
        io.min = Math.min(io.min, point.getIO());
      }
    }
    while (i < xPoints.length)
      data.add(new DataPoint(xPoints[i++]));
    return data;
  }
  
  private static void setRValues(List<DataPoint> data, int minr, int maxr, long maxIO) {
    for (DataPoint point : data) {
      if (point.getY() == 0) {
        continue;
      }
      if (maxIO == 0 || maxr == minr) {
        point.setR(minr);
        continue;
      }
      point.setR(Math.round(Math.sqrt(point.getIO() * 1.0 / maxIO) * (maxr - minr) + minr));
    }
  }
}
