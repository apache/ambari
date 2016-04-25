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

package org.apache.ambari.logsearch.common;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimerTask;

import org.apache.log4j.Logger;

public class ManageStartEndTime extends TimerTask {
  static Logger logger = Logger.getLogger(ManageStartEndTime.class);

  public static Date startDate = new Date();

  public static Date endDate = new Date();

  public ManageStartEndTime() {
    intailizeStartEndTime();
  }

  @Override
  public void run() {
    if (startDate == null){
      intailizeStartEndTime();
    }else{
      adjustStartEndTime();
    }
  }

  private void adjustStartEndTime() {
    startDate = addSecondsToDate(startDate, 40);
    endDate = addHoursToDate(startDate, 1);
  }

  private Date addSecondsToDate(Date date, int i) {
    GregorianCalendar greorianCalendar = new GregorianCalendar();
    greorianCalendar.setTime(date);
    greorianCalendar.add(GregorianCalendar.SECOND, i);
    return greorianCalendar.getTime();
  }

  private Date addHoursToDate(Date date, int i) {
    GregorianCalendar greorianCalendar = new GregorianCalendar();
    greorianCalendar.setTime(date);
    greorianCalendar.add(GregorianCalendar.HOUR_OF_DAY, i);
    return greorianCalendar.getTime();
  }

  private void intailizeStartEndTime() {

    endDate = new Date();
    startDate = addHoursToDate(endDate, -1);
  }

}
