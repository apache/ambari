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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import org.apache.ambari.logfeeder.util.LogFeederUtil;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class MapperDate extends Mapper {
  private static final Logger LOG = Logger.getLogger(MapperDate.class);

  private SimpleDateFormat targetDateFormatter = null;
  private boolean isEpoch = false;
  private SimpleDateFormat srcDateFormatter=null;

  @Override
  public boolean init(String inputDesc, String fieldName, String mapClassCode, Object mapConfigs) {
    init(inputDesc, fieldName, mapClassCode);
    if (!(mapConfigs instanceof Map)) {
      LOG.fatal("Can't initialize object. mapConfigs class is not of type Map. " + mapConfigs.getClass().getName() +
        ", map=" + this);
      return false;
    }
    
    @SuppressWarnings("unchecked")
    Map<String, Object> mapObjects = (Map<String, Object>) mapConfigs;
    String targetDateFormat = (String) mapObjects.get("target_date_pattern");
    String srcDateFormat = (String) mapObjects.get("src_date_pattern");
    if (StringUtils.isEmpty(targetDateFormat)) {
      LOG.fatal("Date format for map is empty. " + this);
    } else {
      LOG.info("Date mapper format is " + targetDateFormat);

      if (targetDateFormat.equalsIgnoreCase("epoch")) {
        isEpoch = true;
        return true;
      } else {
        try {
          targetDateFormatter = new SimpleDateFormat(targetDateFormat);
          if (!StringUtils.isEmpty(srcDateFormat)) {
            srcDateFormatter = new SimpleDateFormat(srcDateFormat);
          }
          return true;
        } catch (Throwable ex) {
          LOG.fatal("Error creating date format. format=" + targetDateFormat + ". " + this.toString());
        }
      } 
    }
    return false;
  }

  @Override
  public Object apply(Map<String, Object> jsonObj, Object value) {
    if (value != null) {
      try {
        if (isEpoch) {
          long ms = Long.parseLong(value.toString()) * 1000;
          value = new Date(ms);
        } else if (targetDateFormatter != null) {
          if (srcDateFormatter != null) {
            Date srcDate = srcDateFormatter.parse(value.toString());
            //set year in src_date when src_date does not have year component
            if (!srcDateFormatter.toPattern().contains("yy")) {
              Calendar currentCalendar = Calendar.getInstance();
              Calendar logDateCalendar = Calendar.getInstance();
              logDateCalendar.setTimeInMillis(srcDate.getTime());
              if (logDateCalendar.get(Calendar.MONTH) > currentCalendar.get(Calendar.MONTH)) {
                // set previous year as a log year  when log month is grater than current month
                srcDate = DateUtils.setYears(srcDate, currentCalendar.get(Calendar.YEAR) - 1);
              } else {
                // set current year as a log year
                srcDate = DateUtils.setYears(srcDate, currentCalendar.get(Calendar.YEAR));
              }
            }
            value = targetDateFormatter.format(srcDate);
          } else {
            value = targetDateFormatter.parse(value.toString());
          }
        } else {
          return value;
        }
        jsonObj.put(fieldName, value);
      } catch (Throwable t) {
        LogFeederUtil.logErrorMessageByInterval(this.getClass().getSimpleName() + ":apply", "Error applying date transformation." +
            " isEpoch=" + isEpoch + ", targetateFormat=" + (targetDateFormatter!=null ?targetDateFormatter.toPattern():"")
            + ", value=" + value + ". " + this.toString(), t, LOG, Level.ERROR);
      }
    }
    return value;
  }
}
