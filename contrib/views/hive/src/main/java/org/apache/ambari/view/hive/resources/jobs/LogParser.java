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

package org.apache.ambari.view.hive.resources.jobs;

import java.util.LinkedHashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogParser {
  public static final Pattern HADOOP_MR_APPS_RE = Pattern.compile("(http[^\\s]*/proxy/([a-z0-9_]+?)/)");
  public static final Pattern HADOOP_TEZ_APPS_RE = Pattern.compile("\\(Executing on YARN cluster with App id ([a-z0-9_]+?)\\)");
  private LinkedHashSet<AppId> appsList;

  private LogParser() {}

  public static LogParser parseLog(String logs) {
    LogParser parser = new LogParser();

    parser.setAppsList(parseApps(logs, parser));
    return parser;
  }

  public static LinkedHashSet<AppId> parseApps(String logs, LogParser parser) {
    LinkedHashSet<AppId> mrAppIds = getMRAppIds(logs);
    LinkedHashSet<AppId> tezAppIds = getTezAppIds(logs);

    LinkedHashSet<AppId> appIds = new LinkedHashSet<AppId>();
    appIds.addAll(mrAppIds);
    appIds.addAll(tezAppIds);

    return appIds;
  }

  private static LinkedHashSet<AppId> getMRAppIds(String logs) {
    Matcher m = HADOOP_MR_APPS_RE.matcher(logs);
    LinkedHashSet<AppId> list = new LinkedHashSet<AppId>();
    while (m.find()) {
      AppId applicationInfo = new AppId();
      applicationInfo.setTrackingUrl(m.group(1));
      applicationInfo.setIdentifier(m.group(2));
      list.add(applicationInfo);
    }
    return list;
  }

  private static LinkedHashSet<AppId> getTezAppIds(String logs) {
    Matcher m = HADOOP_TEZ_APPS_RE.matcher(logs);
    LinkedHashSet<AppId> list = new LinkedHashSet<AppId>();
    while (m.find()) {
      AppId applicationInfo = new AppId();
      applicationInfo.setTrackingUrl("");
      applicationInfo.setIdentifier(m.group(1));
      list.add(applicationInfo);
    }
    return list;
  }

  public void setAppsList(LinkedHashSet<AppId> appsList) {
    this.appsList = appsList;
  }

  public LinkedHashSet<AppId> getAppsList() {
    return appsList;
  }

  public AppId getLastAppInList() {
    Object[] appIds = appsList.toArray();
    if (appIds.length == 0) {
      return null;
    }
    return (AppId) appIds[appsList.size()-1];
  }

  public static class AppId {
    private String trackingUrl;
    private String identifier;

    public String getTrackingUrl() {
      return trackingUrl;
    }

    public void setTrackingUrl(String trackingUrl) {
      this.trackingUrl = trackingUrl;
    }

    public String getIdentifier() {
      return identifier;
    }

    public void setIdentifier(String identifier) {
      this.identifier = identifier;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof AppId)) return false;

      AppId appId = (AppId) o;

      if (!identifier.equals(appId.identifier)) return false;

      return true;
    }

    @Override
    public int hashCode() {
      return identifier.hashCode();
    }
  }

  public static class EmptyAppId extends AppId {
    @Override
    public String getTrackingUrl() {
      return "";
    }

    @Override
    public String getIdentifier() {
      return "";
    }
  }
}
