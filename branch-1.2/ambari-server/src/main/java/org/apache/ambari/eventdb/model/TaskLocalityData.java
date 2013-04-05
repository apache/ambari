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

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class TaskLocalityData {
  private List<DataPoint> mapNodeLocal;
  private List<DataPoint> mapRackLocal;
  private List<DataPoint> mapOffSwitch;
  private List<DataPoint> reduceOffSwitch;
  private long submitTime;
  private long finishTime;
  
  public static class DataPoint {
    private long x;
    private long y;
    private long r;
    private long io;
    private String label;
    private String status;
    
    public DataPoint() {}
    
    public DataPoint(long x) {
      this(x, 0, 0, 0, null, null);
    }
    
    public DataPoint(long x, long y, long r, long io, String taskAttemptId, String status) {
      this.x = x;
      this.y = y;
      this.r = r;
      this.io = io;
      this.label = taskAttemptId;
      this.status = status;
    }
    
    public long getX() {
      return x;
    }
    
    public long getY() {
      return y;
    }
    
    public long getR() {
      return r;
    }
    
    public long getIO() {
      return io;
    }
    
    public String getLabel() {
      return label;
    }
    
    public String getStatus() {
      return status;
    }
    
    public void setX(long x) {
      this.x = x;
    }
    
    public void setY(long y) {
      this.y = y;
    }
    
    public void setR(long r) {
      this.r = r;
    }
    
    public void setIO(long io) {
      this.io = io;
    }
    
    public void setLabel(String label) {
      this.label = label;
    }
    
    public void setStatus(String status) {
      this.status = status;
    }
  }
  
  public TaskLocalityData() {}
  
  public List<DataPoint> getMapNodeLocal() {
    return mapNodeLocal;
  }
  
  public void setMapNodeLocal(List<DataPoint> mapNodeLocal) {
    this.mapNodeLocal = mapNodeLocal;
  }
  
  public List<DataPoint> getMapRackLocal() {
    return mapRackLocal;
  }
  
  public void setMapRackLocal(List<DataPoint> mapRackLocal) {
    this.mapRackLocal = mapRackLocal;
  }
  
  public List<DataPoint> getMapOffSwitch() {
    return mapOffSwitch;
  }
  
  public void setMapOffSwitch(List<DataPoint> mapOffSwitch) {
    this.mapOffSwitch = mapOffSwitch;
  }
  
  public List<DataPoint> getReduceOffSwitch() {
    return reduceOffSwitch;
  }
  
  public void setReduceOffSwitch(List<DataPoint> reduceOffSwitch) {
    this.reduceOffSwitch = reduceOffSwitch;
  }
  
  public long getSubmitTime() {
    return submitTime;
  }
  
  public void setSubmitTime(long submitTime) {
    this.submitTime = submitTime;
  }
  
  public long getFinishTime() {
    return finishTime;
  }
  
  public void setFinishTime(long finishTime) {
    this.finishTime = finishTime;
  }
}
