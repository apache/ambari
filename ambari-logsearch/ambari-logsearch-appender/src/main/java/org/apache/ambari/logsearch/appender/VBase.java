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

package org.apache.ambari.logsearch.appender;

import java.lang.reflect.Field;

import org.apache.log4j.Logger;

import com.google.gson.Gson;

public class VBase {
  private static Logger logger = Logger.getLogger(VBase.class);

  /**
   *
   */
  @Override
  public String toString() {
    @SuppressWarnings("rawtypes")
    Class klass = this.getClass();
    Field[] fields = klass.getDeclaredFields();
    StringBuilder builder = new StringBuilder(klass.getSimpleName() + "={");
    for (Field field : fields) {
      try {
        field.setAccessible(true);
        Object fieldValue = field.get(this);
        String fieldName = field.getName();
        if (!fieldName.equalsIgnoreCase("serialVersionUID")) {
          builder.append(fieldName + "={" + fieldValue + "} ");
        }

      } catch (Exception e) {
        logger.error(e.getLocalizedMessage(), e);
      }
    }
    builder.append("}");

    return builder.toString();
  }

  public String toJson() {
    Gson gson = new Gson();
    String json = gson.toJson(this);
    return json;
  }
}
