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
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.lang.time.DateUtils;

public class ManageStartEndTime extends TimerTask {
  private static final int UPDATE_TIME_IN_SECONDS = 40;

  private static Date startDate;
  private static Date endDate;
  
  public static void manage() {
    Timer timer = new Timer();
    timer.schedule(new ManageStartEndTime(), 0, UPDATE_TIME_IN_SECONDS * 1000);
  }
  
  private ManageStartEndTime() {
    endDate = new Date();
    startDate = DateUtils.addHours(endDate, -1);
  }

  @Override
  public synchronized void run() {
    synchronized (ManageStartEndTime.class) {
      startDate = DateUtils.addSeconds(startDate, UPDATE_TIME_IN_SECONDS);
      endDate = DateUtils.addHours(startDate, 1);
    }
  }

  public static synchronized Date[] getStartEndTime() {
    return new Date[] {startDate, endDate};
  }
}
