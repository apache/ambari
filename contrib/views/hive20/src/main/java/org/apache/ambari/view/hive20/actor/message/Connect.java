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

package org.apache.ambari.view.hive20.actor.message;

import com.google.common.base.Optional;
import org.apache.ambari.view.hive20.AuthParams;
import org.apache.ambari.view.hive20.internal.Connectable;
import org.apache.ambari.view.hive20.internal.HiveConnectionWrapper;

/**
 * Connect message to be sent to the Connection Actor with the connection parameters
 */
public class Connect {

  private final HiveJob.Type type;
  private final String jobId;
  private final String username;
  private final String password;
  private final String jdbcUrl;


  private Connect(HiveJob.Type type, String jobId, String username, String password, String jdbcUrl) {
    this.type = type;
    this.jobId = jobId;
    this.username = username;
    this.password = password;
    this.jdbcUrl = jdbcUrl;
  }

  public Connect(String jobId, String username, String password, String jdbcUrl) {
    this(HiveJob.Type.ASYNC, jobId, username, password, jdbcUrl);
  }

  public Connect(String username, String password, String jdbcUrl) {
    this(HiveJob.Type.SYNC, null, username, password, jdbcUrl);
  }

  public Connectable getConnectable(AuthParams authParams){
    return new HiveConnectionWrapper(getJdbcUrl(),username,password, authParams);
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public String getJdbcUrl() {
    return jdbcUrl;
  }

  public HiveJob.Type getType() {
    return type;
  }

  public Optional<String> getJobId() {
    return Optional.fromNullable(jobId);
  }
}
