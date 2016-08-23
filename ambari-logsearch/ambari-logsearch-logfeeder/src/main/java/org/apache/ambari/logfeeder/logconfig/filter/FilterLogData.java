/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ambari.logfeeder.logconfig.filter;

import java.util.Map;

import org.apache.ambari.logfeeder.logconfig.filter.ApplyLogFilter;
import org.apache.ambari.logfeeder.util.LogFeederUtil;
import org.apache.log4j.Logger;

/**
 * Read configuration from solr and filter the log
 */
public enum FilterLogData {
  INSTANCE;
  private ApplyLogFilter applyLogFilter = new ApplyLogFilter();
  private static Logger logger = Logger.getLogger(FilterLogData.class);
  // by default allow every log
  boolean defaultValue = true;

  public boolean isAllowed(String jsonBlock) {
    if (jsonBlock == null || jsonBlock.isEmpty()) {
      return defaultValue;
    }
    Map<String, Object> jsonObj = LogFeederUtil.toJSONObject(jsonBlock);
    return isAllowed(jsonObj);
  }

  public boolean isAllowed(Map<String, Object> jsonObj) {
    boolean isAllowed = applyLogFilter.applyFilter(jsonObj, defaultValue);
    if (!isAllowed) {
      logger.trace("Filter block the content :" + LogFeederUtil.getGson().toJson(jsonObj));
    }
    return isAllowed;
  }
}
