/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.logfeeder.output.spool;

import java.io.File;
import java.util.Date;

/**
 * A class that holds the state of an spool file.
 *
 * The state in this class can be used by a {@link RolloverCondition} to determine
 * if an active spool file should be rolled over.
 */
public class LogSpoolerContext {

  private File activeSpoolFile;
  private long numEventsSpooled;
  private Date activeLogCreationTime;

  /**
   * Create a new LogSpoolerContext
   * @param activeSpoolFile the spool file for which to hold state
   */
  public LogSpoolerContext(File activeSpoolFile) {
    this.activeSpoolFile = activeSpoolFile;
    this.numEventsSpooled = 0;
    this.activeLogCreationTime = new Date();
  }

  /**
   * Increment number of spooled events by one.
   */
  public void logEventSpooled() {
    numEventsSpooled++;
  }

  public File getActiveSpoolFile() {
    return activeSpoolFile;
  }

  public long getNumEventsSpooled() {
    return numEventsSpooled;
  }

  public Date getActiveLogCreationTime() {
    return activeLogCreationTime;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    LogSpoolerContext that = (LogSpoolerContext) o;

    if (numEventsSpooled != that.numEventsSpooled) return false;
    if (!activeSpoolFile.equals(that.activeSpoolFile)) return false;
    return activeLogCreationTime.equals(that.activeLogCreationTime);

  }

  @Override
  public int hashCode() {
    int result = activeSpoolFile.hashCode();
    result = 31 * result + (int) (numEventsSpooled ^ (numEventsSpooled >>> 32));
    result = 31 * result + activeLogCreationTime.hashCode();
    return result;
  }
}
