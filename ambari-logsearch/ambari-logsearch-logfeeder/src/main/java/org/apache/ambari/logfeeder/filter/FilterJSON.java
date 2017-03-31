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
package org.apache.ambari.logfeeder.filter;

import java.util.Map;

import org.apache.ambari.logfeeder.common.LogfeederException;
import org.apache.ambari.logfeeder.input.InputMarker;
import org.apache.ambari.logfeeder.util.DateUtil;
import org.apache.ambari.logfeeder.util.LogFeederUtil;
import org.apache.log4j.Logger;

public class FilterJSON extends Filter {
  
  private static final Logger LOG  = Logger.getLogger(FilterJSON.class);

  @Override
  public void apply(String inputStr, InputMarker inputMarker) throws LogfeederException {
    Map<String, Object> jsonMap = null;
    try {
      jsonMap = LogFeederUtil.toJSONObject(inputStr);
    } catch (Exception e) {
      LOG.error(e.getLocalizedMessage());
      throw new LogfeederException("Json parsing failed for inputstr = " + inputStr ,e.getCause());
    }
    Double lineNumberD = (Double) jsonMap.get("line_number");
    if (lineNumberD != null) {
      long lineNumber = lineNumberD.longValue();
      jsonMap.put("line_number", lineNumber);
    }
    String timeStampStr = (String) jsonMap.get("logtime");
    if (timeStampStr != null && !timeStampStr.isEmpty()) {
      String logtime = DateUtil.getDate(timeStampStr);
      jsonMap.put("logtime", logtime);
    }
    super.apply(jsonMap, inputMarker);
  }
}
