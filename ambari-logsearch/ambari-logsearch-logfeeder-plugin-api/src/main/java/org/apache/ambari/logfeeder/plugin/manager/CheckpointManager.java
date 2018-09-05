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
package org.apache.ambari.logfeeder.plugin.manager;

import org.apache.ambari.logfeeder.plugin.common.LogFeederProperties;
import org.apache.ambari.logfeeder.plugin.input.Input;
import org.apache.ambari.logfeeder.plugin.input.InputMarker;

import java.io.IOException;

public interface CheckpointManager<I extends Input, IFM extends InputMarker, P extends LogFeederProperties> {

  void init(P properties);

  void checkIn(I inputFile, IFM inputMarker);

  int resumeLineNumber(I input);

  void cleanupCheckpoints();

  void printCheckpoints(String checkpointLocation, String logTypeFilter,
                        String fileKeyFilter) throws IOException;

  void cleanCheckpoint(String checkpointLocation, String logTypeFilter,
                       String fileKeyFilter, boolean all) throws IOException;

}
