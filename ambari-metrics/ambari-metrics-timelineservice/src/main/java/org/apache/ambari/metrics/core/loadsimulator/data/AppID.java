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
package org.apache.ambari.metrics.core.loadsimulator.data;

public enum AppID {
  HOST("HOST"),
  NAMENODE("namenode"),
  RESOURCEMANAGER("resourcemanager"),
  DATANODE("datanode"),
  NODEMANAGER("nodemanager"),
  MASTER_HBASE("hbase"),
  SLAVE_HBASE("hbase"),
  NIMBUS("nimbus"),
  HIVEMETASTORE("hivemetastore"),
  HIVESERVER2("hiveserver2"),
  KAFKA_BROKER("kafka_broker");

  public static final AppID[] MASTER_APPS = {HOST, NAMENODE, RESOURCEMANAGER, MASTER_HBASE, KAFKA_BROKER, NIMBUS, HIVEMETASTORE, HIVESERVER2};
  public static final AppID[] SLAVE_APPS = {HOST, DATANODE, NODEMANAGER, SLAVE_HBASE};

  private String id;

  private AppID(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }
}