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
public class TaskData {
  private List<Point> map;
  private List<Point> shuffle;
  private List<Point> reduce;
  
  public static class Point {
    private long x;
    private int y;
    
    public Point() {}
    
    public Point(long x, int y) {
      this.x = x;
      this.y = y;
    }
    
    public long getX() {
      return x;
    }
    
    public int getY() {
      return y;
    }
    
    public void setX(long x) {
      this.x = x;
    }
    
    public void setY(int y) {
      this.y = y;
    }
  }
  
  public TaskData() {}
  
  public List<Point> getMapData() {
    return map;
  }
  
  public void setMapData(List<Point> mapData) {
    this.map = mapData;
  }
  
  public List<Point> getShuffleData() {
    return shuffle;
  }
  
  public void setShuffleData(List<Point> shuffleData) {
    this.shuffle = shuffleData;
  }
  
  public List<Point> getReduceData() {
    return reduce;
  }
  
  public void setReduceData(List<Point> reduceData) {
    this.reduce = reduceData;
  }
}
