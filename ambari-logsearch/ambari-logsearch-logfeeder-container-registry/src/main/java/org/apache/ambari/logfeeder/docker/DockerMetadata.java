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
package org.apache.ambari.logfeeder.docker;

import org.apache.ambari.logfeeder.ContainerMetadata;

public class DockerMetadata implements ContainerMetadata {

  private final String id;
  private final String name;
  private final String logTypeLabel;
  private final String logPath;
  private final String hostName;
  private final boolean running;
  private final long timestamp;

  public DockerMetadata(String id, String name, String hostName, String logTypeLabel, String logPath, boolean running, long timestamp) {
    this.id = id;
    this.name = name;
    this.hostName = hostName;
    this.logTypeLabel = logTypeLabel;
    this.logPath = logPath;
    this.running = running;
    this.timestamp = timestamp;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getHostName() {
    return hostName;
  }

  public String getLogTypeLabel() {
    return logTypeLabel;
  }

  public String getLogPath() {
    return logPath;
  }

  public boolean isRunning() {
    return running;
  }

  public long getTimestamp() {
    return timestamp;
  }

  @Override
  public String toString() {
    return "DockerMetadata{" +
      "id='" + id + '\'' +
      ", name='" + name + '\'' +
      ", logTypeLabel='" + logTypeLabel + '\'' +
      ", logPath='" + logPath + '\'' +
      ", hostName='" + hostName + '\'' +
      '}';
  }
}
