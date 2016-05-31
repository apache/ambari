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

package org.apache.ambari.view.hive2.actor.message;

import org.apache.ambari.view.ViewContext;

/**
 * Message to be sent when a statement has to be executed
 */
public class AsyncJob extends DDLJob {
  private final String jobId;
  private final String logFile;

  public AsyncJob(String jobId, String username, String[] statements, String logFile,ViewContext viewContext) {
    super(Type.ASYNC, statements, username,viewContext);
    this.jobId = jobId;
    this.logFile = logFile;
  }

  public String getJobId() {
    return jobId;
  }

  public String getLogFile() {
    return logFile;
  }


  @Override
  public String toString() {
    return "AsyncJob{" +
            "jobId='" + jobId + '\'' +
            ", logFile='" + logFile + '\'' +
            "} " + super.toString();
  }
}
