/**
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
package org.apache.ambari.server.actionmanager;

//This class encapsulates the action scheduler thread.
//Action schedule frequently looks at action database and determines if
//there is an action that can be scheduled.
public class ActionScheduler implements Runnable {

  private final long actionTimeout;
  private final long sleepTime;

  public ActionScheduler(long sleepTimeMilliSec, long actionTimeoutMilliSec) {
    this.sleepTime = sleepTimeMilliSec;
    this.actionTimeout = actionTimeoutMilliSec;
  }

  @Override
  public void run() {
    try {
      //Check db for any pending actions and determine if something can be scheduled.
      Thread.sleep(sleepTime);
    } catch (InterruptedException ex) {
      //Shutting down;
      return;
    }
  }
}
