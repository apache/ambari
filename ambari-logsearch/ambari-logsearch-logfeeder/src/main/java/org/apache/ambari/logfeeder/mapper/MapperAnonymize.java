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

package org.apache.ambari.logfeeder.mapper;

import java.util.Map;

import org.apache.ambari.logfeeder.util.LogFeederUtil;
import org.apache.ambari.logsearch.config.api.model.inputconfig.MapAnonymizeDescriptor;
import org.apache.ambari.logsearch.config.api.model.inputconfig.MapFieldDescriptor;
import org.apache.commons.lang.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.google.common.base.Splitter;

public class MapperAnonymize extends Mapper {
  private static final Logger LOG = Logger.getLogger(MapperAnonymize.class);
  
  private static final char DEFAULT_HIDE_CHAR = '*';

  private String pattern;
  private Iterable<String> patternParts;
  private char hideChar;

  @Override
  public boolean init(String inputDesc, String fieldName, String mapClassCode, MapFieldDescriptor mapFieldDescriptor) {
    init(inputDesc, fieldName, mapClassCode);
    
    pattern = ((MapAnonymizeDescriptor)mapFieldDescriptor).getPattern();
    if (StringUtils.isEmpty(pattern)) {
      LOG.fatal("pattern is empty.");
      return false;
    }
    
    patternParts = Splitter.on("<hide>").omitEmptyStrings().split(pattern);
    hideChar = CharUtils.toChar(((MapAnonymizeDescriptor)mapFieldDescriptor).getHideChar(), DEFAULT_HIDE_CHAR);
    
    return true;
  }

  @Override
  public Object apply(Map<String, Object> jsonObj, Object value) {
    if (value != null) {
      try {
        hide((String)value, jsonObj);
      } catch (Throwable t) {
        LogFeederUtil.logErrorMessageByInterval(this.getClass().getSimpleName() + ":apply", "Error applying anonymization." +
            " pattern=" + pattern + ", hideChar=" + hideChar, t, LOG, Level.ERROR);
      }
    }
    return value;
  }
  
  private void hide(String value, Map<String, Object> jsonObj) {
    StringBuilder sb = new StringBuilder();
    boolean first = true;
    String rest = value;
    for (String patternPart : patternParts) {
      int pos = rest.indexOf(patternPart);
      if (pos == -1) {
        return;
      }
      
      int end = pos + patternPart.length();
      if (first) {
        if (pattern.startsWith("<hide>")) {
          String beginning = rest.substring(0, pos);
          int spacePos = beginning.lastIndexOf(" ");
          if (spacePos == -1) {
            sb.append(StringUtils.repeat(hideChar, beginning.length()));
          } else {
            sb.append(beginning.substring(0, spacePos+1));
            sb.append(StringUtils.repeat(hideChar, beginning.length() - spacePos - 1));
          }
          sb.append(rest.substring(pos, end));
        } else {
          sb.append(rest.substring(0, end));
        }
        first = false;
      } else {
        sb.append(StringUtils.repeat(hideChar, pos));
        sb.append(rest.substring(pos, end));
      }
      rest = rest.substring(end);
    }
    
    if (pattern.endsWith("<hide>")) {
      int spacePos = rest.indexOf(" ");
      if (spacePos == -1) {
        sb.append(StringUtils.repeat(hideChar, rest.length()));
        rest = "";
      } else {
        sb.append(StringUtils.repeat(hideChar, spacePos));
        rest = rest.substring(spacePos);
      }
    }
    
    sb.append(rest);
    
    jsonObj.put(fieldName, sb.toString());
  }
}
