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
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Overrides the value for the field
 */
public class MapperFieldName extends Mapper {
  private static final Logger logger = Logger.getLogger(MapperFieldName.class);

  private String newValue = null;

  @Override
  public boolean init(String inputDesc, String fieldName,
      String mapClassCode, Object mapConfigs) {
    super.init(inputDesc, fieldName, mapClassCode, mapConfigs);
    if (!(mapConfigs instanceof Map)) {
      logger.fatal("Can't initialize object. mapConfigs class is not of type Map. "
          + mapConfigs.getClass().getName());
      return false;
    }
    @SuppressWarnings("unchecked")
    Map<String, Object> mapObjects = (Map<String, Object>) mapConfigs;
    newValue = (String) mapObjects.get("new_fieldname");
    if (StringUtils.isEmpty(newValue)) {
      logger.fatal("Map field value is empty.");
      return false;
    }
    return true;
  }

  @Override
  public Object apply(Map<String, Object> jsonObj, Object value) {
    if (newValue != null) {
      jsonObj.remove(fieldName);
      jsonObj.put(newValue, value);
    } else {
      LogFeederUtil.logErrorMessageByInterval(this.getClass()
          .getSimpleName() + ":apply",
          "New fieldName is null, so transformation is not applied. "
              + this.toString(), null, logger, Level.ERROR);
    }
    return value;
  }

}
