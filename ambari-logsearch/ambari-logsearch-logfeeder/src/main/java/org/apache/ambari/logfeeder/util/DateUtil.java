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
import java.util.TimeZone;

import org.apache.log4j.Logger;

public class DateUtil {
  private static final Logger LOG = Logger.getLogger(DateUtil.class);
  
  private DateUtil() {
    throw new UnsupportedOperationException();
  }
  
  public static String dateToString(Date date, String dateFormat) {
    if (date == null || dateFormat == null || dateFormat.isEmpty()) {
      return "";
    }
    try {
      SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);
      return formatter.format(date).toString();
    } catch (Exception e) {
      LOG.error("Error in coverting dateToString  format :" + dateFormat, e);
    }
    return "";
  }

  private final static String SOLR_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
  private static ThreadLocal<SimpleDateFormat> dateFormatter = new ThreadLocal<SimpleDateFormat>() {
    @Override
    protected SimpleDateFormat initialValue() {
      SimpleDateFormat sdf = new SimpleDateFormat(SOLR_DATE_FORMAT);
      sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
      return sdf;
    }
  };

  public static String getDate(String timeStampStr) {
    try {
      return dateFormatter.get().format(new Date(Long.parseLong(timeStampStr)));
    } catch (Exception ex) {
      LOG.error(ex);
      return null;
    }
  }

  public static String getActualDateStr() {
    try {
      return dateFormatter.get().format(new Date());
    } catch (Exception ex) {
      LOG.error(ex);
      return null;
    }
  }
}
