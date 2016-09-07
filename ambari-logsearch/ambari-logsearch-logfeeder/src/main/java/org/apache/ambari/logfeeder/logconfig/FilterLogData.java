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

package org.apache.ambari.logfeeder.logconfig;

import java.util.List;
import java.util.Map;

import org.apache.ambari.logfeeder.common.LogFeederConstants;
import org.apache.ambari.logfeeder.util.LogFeederUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

/**
 * Read configuration from solr and filter the log
 */
public enum FilterLogData {
  INSTANCE;
  
  private static final Logger LOG = Logger.getLogger(FilterLogData.class);
  
  private static final boolean DEFAULT_VALUE = true;

  public boolean isAllowed(String jsonBlock) {
    if (StringUtils.isEmpty(jsonBlock)) {
      return DEFAULT_VALUE;
    }
    Map<String, Object> jsonObj = LogFeederUtil.toJSONObject(jsonBlock);
    return isAllowed(jsonObj);
  }

  public boolean isAllowed(Map<String, Object> jsonObj) {
    boolean isAllowed = applyFilter(jsonObj);
    if (!isAllowed) {
      LOG.trace("Filter block the content :" + LogFeederUtil.getGson().toJson(jsonObj));
    }
    return isAllowed;
  }
  

  private boolean applyFilter(Map<String, Object> jsonObj) {
    if (MapUtils.isEmpty(jsonObj)) {
      LOG.warn("Output jsonobj is empty");
      return DEFAULT_VALUE;
    }
    
    String hostName = (String) jsonObj.get(LogFeederConstants.SOLR_HOST);
    String componentName = (String) jsonObj.get(LogFeederConstants.SOLR_COMPONENT);
    String level = (String) jsonObj.get(LogFeederConstants.SOLR_LEVEL);
    if (StringUtils.isNotBlank(hostName) && StringUtils.isNotBlank(componentName) && StringUtils.isNotBlank(level)) {
      LogFeederFilter componentFilter = LogConfigHandler.findComponentFilter(componentName);
      if (componentFilter == null) {
        return DEFAULT_VALUE;
      }
      List<String> allowedLevels = LogConfigHandler.getAllowedLevels(hostName, componentFilter);
      if (CollectionUtils.isEmpty(allowedLevels)) {
        allowedLevels.add(LogFeederConstants.ALL);
      }
      return LogFeederUtil.isListContains(allowedLevels, level, false);
    }
    else {
      return DEFAULT_VALUE;
    }
  }
}
