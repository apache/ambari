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

import org.apache.ambari.logfeeder.conf.LogFeederProps;
import org.apache.ambari.logfeeder.plugin.filter.mapper.Mapper;
import org.apache.ambari.logfeeder.util.LogFeederUtil;
import org.apache.ambari.logsearch.config.api.model.inputconfig.MapFieldDescriptor;
import org.apache.ambari.logsearch.config.api.model.inputconfig.MapFieldNameDescriptor;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.util.Map;

/**
 * Overrides the value for the field
 */
public class MapperFieldName extends Mapper<LogFeederProps> {
  private static final Logger LOG = Logger.getLogger(MapperFieldName.class);

  private String newValue = null;

  @Override
  public boolean init(String inputDesc, String fieldName, String mapClassCode, MapFieldDescriptor mapFieldDescriptor) {
    init(inputDesc, fieldName, mapClassCode);

    newValue = ((MapFieldNameDescriptor)mapFieldDescriptor).getNewFieldName();
    if (StringUtils.isEmpty(newValue)) {
      LOG.fatal("Map field value is empty.");
      return false;
    }
    return true;
  }

  @Override
  public Object apply(Map<String, Object> jsonObj, Object value) {
    if (newValue != null) {
      jsonObj.remove(getFieldName());
      jsonObj.put(newValue, value);
    } else {
      LogFeederUtil.logErrorMessageByInterval(this.getClass().getSimpleName() + ":apply",
          "New fieldName is null, so transformation is not applied. " + this.toString(), null, LOG, Level.ERROR);
    }
    return value;
  }
}
