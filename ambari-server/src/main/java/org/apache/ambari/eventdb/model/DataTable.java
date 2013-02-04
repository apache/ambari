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

import org.apache.ambari.eventdb.model.Workflows.WorkflowDBEntry;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class DataTable {
  int sEcho;
  int iTotalRecords;
  int iTotalDisplayRecords;
  int startIndex;
  int endIndex;
  List<WorkflowDBEntry> aaData;
  Summary summary;
  
  @XmlRootElement
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class Summary {
    public static enum SummaryFields {
      numRows,
      avgJobs,
      minJobs,
      maxJobs,
      avgInput,
      minInput,
      maxInput,
      avgOutput,
      minOutput,
      maxOutput,
      avgDuration,
      minDuration,
      maxDuration,
      youngest,
      oldest;
      
      public int getInt(ResultSet rs) throws SQLException {
        return rs.getInt(this.toString());
      }
      
      public long getLong(ResultSet rs) throws SQLException {
        return rs.getLong(this.toString());
      }
      
      public double getDouble(ResultSet rs) throws SQLException {
        return rs.getDouble(this.toString());
      }
    }
    
    int numRows;
    AvgData jobs;
    AvgData input;
    AvgData output;
    AvgData duration;
    Times times;
    
    public int getNumRows() {
      return numRows;
    }
    
    public void setNumRows(int numRows) {
      this.numRows = numRows;
    }
    
    public AvgData getJobs() {
      return jobs;
    }
    
    public void setJobs(AvgData jobs) {
      this.jobs = jobs;
    }
    
    public AvgData getInput() {
      return input;
    }
    
    public void setInput(AvgData input) {
      this.input = input;
    }
    
    public AvgData getOutput() {
      return output;
    }
    
    public void setOutput(AvgData output) {
      this.output = output;
    }
    
    public AvgData getDuration() {
      return duration;
    }
    
    public void setDuration(AvgData duration) {
      this.duration = duration;
    }
    
    public Times getTimes() {
      return times;
    }
    
    public void setTimes(Times times) {
      this.times = times;
    }
  }
  
  @XmlRootElement
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class AvgData {
    double avg;
    long min;
    long max;
    
    public double getAvg() {
      return avg;
    }
    
    public void setAvg(double avg) {
      this.avg = avg;
    }
    
    public long getMin() {
      return min;
    }
    
    public void setMin(long min) {
      this.min = min;
    }
    
    public long getMax() {
      return max;
    }
    
    public void setMax(long max) {
      this.max = max;
    }
  }
  
  @XmlRootElement
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class Times {
    long oldest;
    long youngest;
    
    public long getOldest() {
      return oldest;
    }
    
    public void setOldest(long oldest) {
      this.oldest = oldest;
    }
    
    public long getYoungest() {
      return youngest;
    }
    
    public void setYoungest(long youngest) {
      this.youngest = youngest;
    }
  }
  
  public DataTable() {}
  
  public int getsEcho() {
    return sEcho;
  }
  
  public void setsEcho(int sEcho) {
    this.sEcho = sEcho;
  }
  
  public int getiTotalRecords() {
    return iTotalRecords;
  }
  
  public void setiTotalRecords(int iTotalRecords) {
    this.iTotalRecords = iTotalRecords;
  }
  
  public int getiTotalDisplayRecords() {
    return iTotalDisplayRecords;
  }
  
  public void setiTotalDisplayRecords(int iTotalDisplayRecords) {
    this.iTotalDisplayRecords = iTotalDisplayRecords;
  }
  
  public int getStartIndex() {
    return startIndex;
  }
  
  public void setStartIndex(int startIndex) {
    this.startIndex = startIndex;
  }
  
  public int getEndIndex() {
    return endIndex;
  }
  
  public void setEndIndex(int endIndex) {
    this.endIndex = endIndex;
  }
  
  public List<WorkflowDBEntry> getAaData() {
    return aaData;
  }
  
  public void setAaData(List<WorkflowDBEntry> aaData) {
    this.aaData = aaData;
  }
  
  public Summary getSummary() {
    return summary;
  }
  
  public void setSummary(Summary summary) {
    this.summary = summary;
  }
}
