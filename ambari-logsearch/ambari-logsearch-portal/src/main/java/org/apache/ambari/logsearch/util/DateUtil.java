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

package org.apache.ambari.logsearch.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.apache.ambari.logsearch.common.LogSearchConstants;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.log4j.Logger;

public class DateUtil {

  private static final Logger logger = Logger.getLogger(DateUtil.class);

  private DateUtil() {
    throw new UnsupportedOperationException();
  }

  public static String addOffsetToDate(String date, Long utcOffset, String dateFormat) {
    if (StringUtils.isBlank(date)) {
      logger.debug("input date is empty or null.");
      return null;
    }
    if (utcOffset == null) {
      logger.debug("Utc offset is null, Return input date without adding offset.");
      return date;
    }
    if (StringUtils.isBlank(dateFormat)) {
      logger.debug("dateFormat is null or empty, Return input date without adding offset.");
      return date;
    }
    String retDate = "";
    try {
      String modifiedDate = date;
      if (date.contains(".")) {
        modifiedDate = date.replace(".", ",");
      }
      SimpleDateFormat formatter = new SimpleDateFormat(dateFormat, Locale.ENGLISH);
      Date startDate = formatter.parse(modifiedDate);
      long toWithOffset = startDate.getTime() + TimeUnit.MINUTES.toMillis(utcOffset);
      Calendar calendar = Calendar.getInstance();
      calendar.setTimeInMillis(toWithOffset);
      retDate = formatter.format(calendar.getTime());
    } catch (Exception e) {
      logger.error(e);
    }
    return retDate;
  }

  public static String getCurrentDateInString() {
    DateFormat df = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss", Locale.ENGLISH);
    Date today = Calendar.getInstance().getTime();
    return df.format(today);
  }

  public static Date getTodayFromDate() {
    return DateUtils.truncate(new Date(), Calendar.DATE);
  }

  public static String convertGivenDateFormatToSolrDateFormat(Date date) throws ParseException {
    String time = date.toString();
    SimpleDateFormat input = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);
    SimpleDateFormat output = new SimpleDateFormat(LogSearchConstants.SOLR_DATE_FORMAT_PREFIX_Z, Locale.ENGLISH);
    Date d = input.parse(time);
    TimeZone timeZone = TimeZone.getTimeZone("UTC");
    output.setTimeZone(timeZone);

    return output.format(d);
  }

  public static String convertDateWithMillisecondsToSolrDate(Date date) {
    if (date == null) {
      return "";
    }
    SimpleDateFormat formatter = new SimpleDateFormat(LogSearchConstants.SOLR_DATE_FORMAT_PREFIX_Z, Locale.ENGLISH);
    TimeZone timeZone = TimeZone.getTimeZone("GMT");
    formatter.setTimeZone(timeZone);

    return formatter.format(date);
  }

  public static String convertSolrDateToNormalDateFormat(long d, long utcOffset) throws ParseException {
    Date date = new Date(d);
    SimpleDateFormat formatter = new SimpleDateFormat(LogSearchConstants.SOLR_DATE_FORMAT, Locale.ENGLISH);
    TimeZone timeZone = TimeZone.getTimeZone("GMT");
    formatter.setTimeZone(timeZone);
    String stringDate = formatter.format(date);
    return addOffsetToDate(stringDate, Long.parseLong("" + utcOffset), LogSearchConstants.SOLR_DATE_FORMAT);

  }

  public static Date convertStringToSolrDate(String dateStr) {
    try {
      SimpleDateFormat formatter = new SimpleDateFormat(LogSearchConstants.SOLR_DATE_FORMAT_PREFIX_Z);
      return formatter.parse(dateStr);
    } catch (Exception e){
      throw new RuntimeException("Cannot parse date from request", e.getCause());
    }
  }

  public static boolean isDateValid(String value) {
    if (StringUtils.isBlank(value)) {
      return false;
    }
    Date date = null;
    try {
      SimpleDateFormat sdf = new SimpleDateFormat(LogSearchConstants.SOLR_DATE_FORMAT_PREFIX_Z);
      date = sdf.parse(value);
      if (!value.equals(sdf.format(date))) {
        date = null;
      }
    } catch (Exception ex) {
      // do nothing
    }
    return date != null;
  }
}
