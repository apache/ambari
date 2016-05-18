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
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.apache.ambari.logsearch.common.LogSearchConstants;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DateUtil {

  static Logger logger = Logger.getLogger(DateUtil.class);

  @Autowired
  StringUtil stringUtil;

  private DateUtil() {

  }

  public String addOffsetToDate(String date, Long utcOffset, String dateFormat) {
    if (stringUtil.isEmpty(date)) {
      logger.debug("input date is empty or null.");
      return null;
    }
    if (utcOffset == null) {
      logger
          .debug("Utc offset is null, Return input date without adding offset.");
      return date;
    }
    if (stringUtil.isEmpty(dateFormat)) {
      logger
          .debug("dateFormat is null or empty, Return input date without adding offset.");
      return date;
    }
    String retDate = "";
    try {
      String modifiedDate = date;
      if (date.contains(".")) {
        modifiedDate = date.replace(".", ",");
      }
      SimpleDateFormat formatter = new SimpleDateFormat(dateFormat,
          Locale.ENGLISH);
      Date startDate = formatter.parse(modifiedDate);
      long toWithOffset = getTimeWithOffset(startDate, utcOffset, dateFormat);
      Calendar calendar = Calendar.getInstance();
      calendar.setTimeInMillis(toWithOffset);
      retDate = formatter.format(calendar.getTime());
    } catch (Exception e) {
      logger.error(e);
    }
    return retDate;
  }

  public long getTimeWithOffset(Date date, Long utcOffset, String dateFormate) {
    return date.getTime() + TimeUnit.MINUTES.toMillis(utcOffset);
  }

  public Date getUTCDate(long epoh) {
    if (epoh == 0) {
      return null;
    }
    try {
      TimeZone gmtTimeZone = TimeZone.getTimeZone("GMT+0");
      Calendar local = Calendar.getInstance();
      int offset = local.getTimeZone().getOffset(epoh);
      GregorianCalendar utc = new GregorianCalendar(gmtTimeZone);
      utc.setTimeInMillis(epoh);
      utc.add(Calendar.MILLISECOND, -offset);
      return utc.getTime();
    } catch (Exception ex) {
      return null;
    }
  }

  public String dateToString(Date date, String dateFormat) {
    if (date == null || dateFormat == null || dateFormat.isEmpty()) {
      return "";
    }
    SimpleDateFormat formatter = new SimpleDateFormat(dateFormat, Locale.ENGLISH);
    TimeZone timeZone = TimeZone.getTimeZone("GMT");
    formatter.setTimeZone(timeZone);
    return formatter.format(date);
  }

  public String getCurrentDateInString() {
    DateFormat df = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss", Locale.ENGLISH);
    Date today = Calendar.getInstance().getTime();
    return df.format(today);
  }

  public String getTimeInSolrFormat(String timeString) {
    String time;
    if (stringUtil.isEmpty(timeString)) {
      return null;
    }
    time = timeString.replace(" ", "T");
    time = time.replace(",", ".");
    time = time + "Z";

    return time;
  }
  
  public Date getTodayFromDate() {
    Calendar c = new GregorianCalendar();
    c.set(Calendar.HOUR_OF_DAY, 0); 
    c.set(Calendar.MINUTE, 0);
    c.set(Calendar.SECOND, 0);
    return c.getTime();
  }

  public Date addHoursToDate(Date date, int hours) {
    GregorianCalendar greorianCalendar = new GregorianCalendar();
    greorianCalendar.setTime(date);
    greorianCalendar.add(GregorianCalendar.HOUR_OF_DAY, hours);
    return greorianCalendar.getTime();
  }

  public Date addMinsToDate(Date date, int mins) {
    GregorianCalendar greorianCalendar = new GregorianCalendar();
    greorianCalendar.setTime(date);
    greorianCalendar.add(GregorianCalendar.MINUTE, mins);
    return greorianCalendar.getTime();
  }

  public Date addSecondsToDate(Date date, int secs) {
    GregorianCalendar greorianCalendar = new GregorianCalendar();
    greorianCalendar.setTime(date);
    greorianCalendar.add(GregorianCalendar.SECOND, secs);
    return greorianCalendar.getTime();
  }

  public Date addMilliSecondsToDate(Date date, int secs) {
    GregorianCalendar greorianCalendar = new GregorianCalendar();
    greorianCalendar.setTime(date);
    greorianCalendar.add(GregorianCalendar.MILLISECOND, secs);
    return greorianCalendar.getTime();
  }

  public String convertGivenDateFormatToSolrDateFormat(Date date)
    throws ParseException {
    String time = date.toString();
    SimpleDateFormat input = new SimpleDateFormat(
      "EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);
    SimpleDateFormat output = new SimpleDateFormat(
      LogSearchConstants.SOLR_DATE_FORMAT_PREFIX_Z, Locale.ENGLISH);
    Date d = input.parse(time);
    TimeZone timeZone = TimeZone.getTimeZone("UTC");
    output.setTimeZone(timeZone);

    return output.format(d);
  }

  public String convertDateWithMillisecondsToSolrDate(Date date) {
    if (date == null) {
      return "";
    }
    SimpleDateFormat formatter = new SimpleDateFormat(
      LogSearchConstants.SOLR_DATE_FORMAT_PREFIX_Z, Locale.ENGLISH);
    TimeZone timeZone = TimeZone.getTimeZone("GMT");
    formatter.setTimeZone(timeZone);

    return formatter.format(date);
  }

  public String convertSolrDateToNormalDateFormat(long d, long utcOffset)
    throws ParseException {
    Date date = new Date(d);
    SimpleDateFormat formatter = new SimpleDateFormat(
      LogSearchConstants.SOLR_DATE_FORMAT, Locale.ENGLISH);
    TimeZone timeZone = TimeZone.getTimeZone("GMT");
    formatter.setTimeZone(timeZone);
    String stringDate = formatter.format(date);
    return addOffsetToDate(stringDate, Long.parseLong("" + utcOffset),
      LogSearchConstants.SOLR_DATE_FORMAT);

  }

  public Date convertStringToDate(String dateString) {
    SimpleDateFormat formatter = new SimpleDateFormat(
      LogSearchConstants.SOLR_DATE_FORMAT_PREFIX_Z, Locale.ENGLISH);
    TimeZone timeZone = TimeZone.getTimeZone("GMT");
    formatter.setTimeZone(timeZone);
    try {
      return formatter.parse(dateString);
    } catch (ParseException e) {
      //do nothing
    }
    return null;
  }
  
  public boolean isDateValid(String value) {
    if(stringUtil.isEmpty(value)){
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
      //do nothing
    }
    return date != null;
  }
}
