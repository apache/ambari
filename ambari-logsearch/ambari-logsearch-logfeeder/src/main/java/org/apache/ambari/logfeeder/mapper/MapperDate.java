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
import java.util.Date;
import java.util.Map;

import org.apache.ambari.logfeeder.util.LogFeederUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class MapperDate extends Mapper {
  private static final Logger logger = Logger.getLogger(MapperDate.class);

  private String dateFormat = null;
  private SimpleDateFormat dateFormatter = null;
  private boolean isEpoch = false;

  @Override
  public boolean init(String inputDesc, String fieldName,
                      String mapClassCode, Object mapConfigs) {
    super.init(inputDesc, fieldName, mapClassCode, mapConfigs);
    if (!(mapConfigs instanceof Map)) {
      logger.fatal("Can't initialize object. mapConfigs class is not of type Map. "
        + mapConfigs.getClass().getName()
        + ", map="
        + this.toString());
      return false;
    }
    @SuppressWarnings("unchecked")
    Map<String, Object> mapObjects = (Map<String, Object>) mapConfigs;
    dateFormat = (String) mapObjects.get("date_pattern");
    if (StringUtils.isEmpty(dateFormat)) {
      logger.fatal("Date format for map is empty. " + this.toString());
    } else {
      logger.info("Date mapper format is " + dateFormat);

      if (dateFormat.equalsIgnoreCase("epoch")) {
        isEpoch = true;
        return true;
      } else {
        try {
          dateFormatter = new SimpleDateFormat(dateFormat);
          return true;
        } catch (Throwable ex) {
          logger.fatal("Error creating date format. format="
            + dateFormat + ". " + this.toString());
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
        } else if (dateFormatter != null) {
          value = dateFormatter.parse(value.toString());
        } else {
          return value;
        }
        jsonObj.put(fieldName, value);
      } catch (Throwable t) {
        LogFeederUtil.logErrorMessageByInterval(this.getClass()
            .getSimpleName() + ":apply",
          "Error applying date transformation. isEpoch="
            + isEpoch + ", dateFormat=" + dateFormat
            + ", value=" + value + ". " + this.toString(),
          t, logger, Level.ERROR);
      }
    }
    return value;
  }
}
