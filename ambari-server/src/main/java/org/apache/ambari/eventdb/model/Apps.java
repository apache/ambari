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

import org.apache.commons.lang.StringUtils;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Apps {
  List<AppDBEntry> apps;

  public static class AppDBEntry {
    public static enum AppFields {
      APPID,
      APPNAME,
      APPTYPE,
      STATUS,
      USERNAME,
      QUEUE,
      SUBMITTIME,
      LAUNCHTIME,
      FINISHTIME,
      APPINFO,
      WORKFLOWID,
      WORKFLOWENTITYNAME;

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
        String[] tmp = new String[AppFields.values().length];
        for (int i = 0; i < tmp.length; i++)
          tmp[i] = AppFields.values()[i].toString();
        return StringUtils.join(tmp, ",");
      }
    }

    @XmlTransient
    public static final String APP_FIELDS = AppFields.join();

    private String appId;
    private String appName;
    private String appType;
    private String status;
    private String userName;
    private String queue;
    private long submitTime;
    private long launchTime;
    private long finishTime;
    private List<Integer> stages;
    private String workflowId;
    private String workflowEntityName;

    public AppDBEntry() {
      /* Required by JAXB. */
    }

    public String getAppId() {
      return appId;
    }

    public void setAppId(String appId) {
      this.appId = appId;
    }

    public String getAppName() {
      return appName;
    }

    public void setAppName(String appName) {
      this.appName = appName;
    }

    public String getAppType() {
      return appType;
    }

    public void setAppType(String appType) {
      this.appType = appType;
    }

    public String getStatus() {
      return status;
    }

    public void setStatus(String status) {
      this.status = status;
    }

    public String getUserName() {
      return userName;
    }

    public void setUserName(String userName) {
      this.userName = userName;
    }

    public String getQueue() {
      return queue;
    }

    public void setQueue(String queue) {
      this.queue = queue;
    }

    public long getSubmitTime() {
      return submitTime;
    }

    public void setSubmitTime(long submitTime) {
      this.submitTime = submitTime;
    }

    public long getLaunchTime() {
      return launchTime;
    }

    public void setLaunchTime(long launchTime) {
      this.launchTime = launchTime;
    }

    public long getFinishTime() {
      return finishTime;
    }

    public void setFinishTime(long finishTime) {
      this.finishTime = finishTime;
    }

    public List<Integer> getStages() {
      return stages;
    }

    public void setStages(List<Integer> stages) {
      this.stages = stages;
    }

    public String getWorkflowId() {
      return workflowId;
    }

    public void setWorkflowId(String workflowId) {
      this.workflowId = workflowId;
    }

    public String getWorkflowEntityName() {
      return workflowEntityName;
    }

    public void setWorkflowEntityName(String workflowEntityName) {
      this.workflowEntityName = workflowEntityName;
    }
  }

  public List<AppDBEntry> getApps() {
    return apps;
  }

  public void setApps(List<AppDBEntry> apps) {
    this.apps = apps;
  }
}
