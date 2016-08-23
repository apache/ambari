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

import java.util.List;
import java.util.Map;

import org.apache.ambari.logfeeder.logconfig.FetchConfigFromSolr;
import org.apache.ambari.logfeeder.logconfig.LogFeederConstants;
import org.apache.ambari.logfeeder.util.LogFeederUtil;
import org.apache.ambari.logfeeder.view.VLogfeederFilter;
import org.apache.log4j.Logger;

class ApplyLogFilter extends DefaultDataFilter {

  private static Logger logger = Logger.getLogger(ApplyLogFilter.class);

  @Override
  public boolean applyFilter(Map<String, Object> jsonObj, boolean defaultValue) {
    if (isEmpty(jsonObj)) {
      logger.warn("Output jsonobj is empty");
      return defaultValue;
    }
    String hostName = (String) jsonObj.get(LogFeederConstants.SOLR_HOST);
    if (isNotEmpty(hostName)) {
      String componentName = (String) jsonObj.get(LogFeederConstants.SOLR_COMPONENT);
      if (isNotEmpty(componentName)) {
        String level = (String) jsonObj.get(LogFeederConstants.SOLR_LEVEL);
        if (isNotEmpty(level)) {
          VLogfeederFilter componentFilter = FetchConfigFromSolr.findComponentFilter(componentName);
          if (componentFilter == null) {
            return defaultValue;
          }
          List<String> allowedLevels = FetchConfigFromSolr.getAllowedLevels(
              hostName, componentFilter);
          if (allowedLevels == null || allowedLevels.isEmpty()) {
            allowedLevels.add(LogFeederConstants.ALL);
          }
          return LogFeederUtil.isListContains(allowedLevels, level, false);
        }
      }
    }
    return defaultValue;
  }
}
