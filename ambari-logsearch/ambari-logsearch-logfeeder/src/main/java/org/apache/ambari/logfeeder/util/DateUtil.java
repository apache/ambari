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
package org.apache.ambari.logfeeder.util;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;

public class DateUtil {
  private static final Logger logger = Logger.getLogger(DateUtil.class);

  public static String dateToString(Date date, String dateFormat) {
    if (date == null || dateFormat == null || dateFormat.isEmpty()) {
      return "";
    }
    try {
      SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);
      return formatter.format(date).toString();
    } catch (Exception e) {
      logger.error("Error in coverting dateToString  format :" + dateFormat, e);
    }
    return "";
  }
}
