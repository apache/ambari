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

import org.apache.ambari.eventdb.db.PostgresConnector;
import org.apache.ambari.eventdb.model.Jobs;
import org.apache.ambari.eventdb.model.Jobs.JobDBEntry;
import org.apache.ambari.eventdb.model.TaskAttempt;
import org.apache.ambari.eventdb.model.TaskData;
import org.apache.ambari.eventdb.model.TaskData.Point;
import org.apache.ambari.eventdb.model.TaskLocalityData;
import org.apache.ambari.eventdb.model.TaskLocalityData.DataPoint;
import org.apache.ambari.eventdb.model.Workflows;
import org.apache.ambari.eventdb.model.Workflows.WorkflowDBEntry;

@Path("/jobhistory")
public class WorkflowJsonService {
  private static final String PREFIX = "eventdb.";
  private static final String HOSTNAME = PREFIX + "db.hostname";
  private static final String DBNAME = PREFIX + "db.name";
  private static final String USERNAME = PREFIX + "db.user";
  private static final String PASSWORD = PREFIX + "db.password";
  
  private static final String DEFAULT_HOSTNAME = "localhost";
  private static final String DEFAULT_DBNAME = "ambarirca";
  private static final String DEFAULT_USERNAME = "mapred";
  private static final String DEFAULT_PASSWORD = "mapred";
  
  private static final List<WorkflowDBEntry> EMPTY_WORKFLOWS = Collections.emptyList();
  private static final List<JobDBEntry> EMPTY_JOBS = Collections.emptyList();
  
  PostgresConnector getConnector() throws IOException {
    return new PostgresConnector(DEFAULT_HOSTNAME, DEFAULT_DBNAME, DEFAULT_USERNAME, DEFAULT_PASSWORD);
  }
  
  @Context
  ServletContext servletContext;
  
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/workflow")
  public Workflows getWorkflows() {
    Workflows workflows = new Workflows();
    PostgresConnector conn = null;
    try {
      conn = getConnector();
      workflows.setWorkflows(conn.fetchWorkflows());
    } catch (IOException e) {
      e.printStackTrace();
      workflows.setWorkflows(EMPTY_WORKFLOWS);
    } finally {
      if (conn != null) {
        conn.close();
      }
    }
    return workflows;
  }
  
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/job")
  public Jobs getJobs(@QueryParam("workflowId") String workflowId) {
    Jobs jobs = new Jobs();
    PostgresConnector conn = null;
    try {
      conn = getConnector();
      jobs.setJobs(conn.fetchJobDetails(workflowId));
    } catch (IOException e) {
      e.printStackTrace();
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
  public TaskData getTaskDetails(@QueryParam("jobId") String jobId, @QueryParam("width") int steps) {
    TaskData points = new TaskData();
    PostgresConnector conn = null;
    try {
      conn = getConnector();
      long[] times = conn.fetchJobStartStopTimes(jobId);
      if (times != null) {
        double submitTimeSecs = times[0] / 1000.0;
        double finishTimeSecs = times[1] / 1000.0;
        double step = (finishTimeSecs - submitTimeSecs) / steps;
        if (step < 1)
          step = 1;
        getMapDetails(conn, points, jobId, submitTimeSecs, finishTimeSecs, step);
        getReduceDetails(conn, points, jobId, submitTimeSecs, finishTimeSecs, step);
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (conn != null) {
        conn.close();
      }
    }
    return points;
  }
  
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/tasklocality")
  public TaskLocalityData getTaskLocalityDetails(@QueryParam("jobId") String jobId, @DefaultValue("4") @QueryParam("minr") int minr,
      @DefaultValue("24") @QueryParam("maxr") int maxr) {
    if (maxr < minr)
      maxr = minr;
    TaskLocalityData data = new TaskLocalityData();
    PostgresConnector conn = null;
    try {
      conn = getConnector();
      long[] times = conn.fetchJobStartStopTimes(jobId);
      if (times != null) {
        getTaskAttemptsByLocality(conn, jobId, times[0], times[1], data, minr, maxr);
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (conn != null) {
        conn.close();
      }
    }
    return data;
  }
  
  private static void getMapDetails(PostgresConnector conn, TaskData points, String jobId, double submitTimeSecs, double finishTimeSecs, double step)
      throws IOException {
    List<TaskAttempt> taskAttempts = conn.fetchTaskAttempts(jobId, "MAP");
    List<Point> mapPoints = new ArrayList<Point>();
    for (double time = submitTimeSecs; time < finishTimeSecs; time += step) {
      int numTasks = 0;
      for (TaskAttempt taskAttempt : taskAttempts)
        if ((taskAttempt.getStartTime() / 1000.0) <= (time + step) && (taskAttempt.getFinishTime() / 1000.0) >= time)
          numTasks++;
      mapPoints.add(new Point(Math.round(time), numTasks));
    }
    points.setMapData(mapPoints);
  }
  
  private static void getReduceDetails(PostgresConnector conn, TaskData points, String jobId, double submitTimeSecs, double finishTimeSecs, double step)
      throws IOException {
    List<TaskAttempt> taskAttempts = conn.fetchTaskAttempts(jobId, "REDUCE");
    List<Point> shufflePoints = new ArrayList<Point>();
    List<Point> reducePoints = new ArrayList<Point>();
    for (double time = submitTimeSecs; time < finishTimeSecs; time += step) {
      int numShuffleTasks = 0;
      int numReduceTasks = 0;
      for (TaskAttempt taskAttempt : taskAttempts) {
        if ((taskAttempt.getStartTime() / 1000.0) <= (time + step) && (taskAttempt.getShuffleFinishTime() / 1000.0) >= time) {
          numShuffleTasks++;
        } else if ((taskAttempt.getShuffleFinishTime() / 1000.0) < (time + step) && (taskAttempt.getFinishTime() / 1000.0) >= time) {
          numReduceTasks++;
        }
      }
      shufflePoints.add(new Point(Math.round(time), numShuffleTasks));
      reducePoints.add(new Point(Math.round(time), numReduceTasks));
    }
    points.setShuffleData(shufflePoints);
    points.setReduceData(reducePoints);
  }
  
  private static void getTaskAttemptsByLocality(PostgresConnector conn, String jobId, long submitTime, long finishTime, TaskLocalityData data, int minr,
      int maxr) throws IOException {
    long submitTimeX = transformX(submitTime);
    long finishTimeX = transformX(finishTime);
    List<TaskAttempt> mapAttempts = conn.fetchTaskAttempts(jobId, "MAP");
    List<TaskAttempt> reduceAttempts = conn.fetchTaskAttempts(jobId, "REDUCE");
    Set<Long> xPoints = getXPoints(mapAttempts, reduceAttempts, submitTimeX, finishTimeX);
    Long[] xList = xPoints.toArray(new Long[xPoints.size()]);
    MinMax io = new MinMax();
    data.setMapNodeLocal(processLocalityData(mapAttempts, "NODE_LOCAL", xList, io));
    data.setMapRackLocal(processLocalityData(mapAttempts, "RACK_LOCAL", xList, io));
    data.setMapOffSwitch(processLocalityData(mapAttempts, "OFF_SWITCH", xList, io));
    data.setReduceOffSwitch(processLocalityData(reduceAttempts, "OFF_SWITCH", xList, io));
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
  
  private static Set<Long> getXPoints(List<TaskAttempt> mapAttempts, List<TaskAttempt> reduceAttempts, long submitTimeX, long finishTimeX) {
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
    sortedAttempts.addAll(mapAttempts);
    sortedAttempts.addAll(reduceAttempts);
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
  
  private static List<DataPoint> processLocalityData(List<TaskAttempt> taskAttempts, String locality, Long[] xPoints, MinMax io) {
    List<DataPoint> data = new ArrayList<DataPoint>();
    int i = 0;
    for (TaskAttempt taskAttempt : taskAttempts) {
      if (locality.equals(taskAttempt.getLocality())) {
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
