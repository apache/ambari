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

/**
 * An interface that is used to trigger the handling of a rolled over file.
 *
 * Implementations of this interface will typically upload the rolled over file to
 * a target destination, like HDFS.
 */
public interface RolloverHandler {
  /**
   * Handle a rolled over file.
   *
   * This method is called inline from the {@link LogSpooler#rollover()} method.
   * Hence implementations should either complete the handling fast, or do so
   * asynchronously. The cleanup of the file is left to implementors, but should
   * typically be done once the upload the file to the target destination is complete.
   * @param rolloverFile The file that has been rolled over.
   */
  void handleRollover(File rolloverFile);
}
