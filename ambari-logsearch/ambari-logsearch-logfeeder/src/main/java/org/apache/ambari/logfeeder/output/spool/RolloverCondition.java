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

/**
 * An interface that is used to determine whether a rollover of a locally spooled log file should be triggered.
 */
public interface RolloverCondition {

  /**
   * Check if the active spool file should be rolled over.
   *
   * If this returns true, the {@link LogSpooler} will initiate activities related
   * to rollover of the file
   * @param currentSpoolerContext {@link LogSpoolerContext} that holds state about the file being checked
   *                                                       for rollover.
   * @return true if active spool file should be rolled over, false otherwise
   */
  boolean shouldRollover(LogSpoolerContext currentSpoolerContext);
}
