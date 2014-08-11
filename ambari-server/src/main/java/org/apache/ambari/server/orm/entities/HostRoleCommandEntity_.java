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

package org.apache.ambari.server.orm.entities;

import javax.persistence.metamodel.SingularAttribute;


/**
 * This class exists so that JPQL can use static singular attributes that are strongly typed
 * as opposed to Java reflection like HostRoleCommandEntity.get("fieldname")
 */
@javax.persistence.metamodel.StaticMetamodel(HostRoleCommandEntity.class)
public class HostRoleCommandEntity_ {
  public static volatile SingularAttribute<HostRoleCommandEntity, Long> taskId;
  public static volatile SingularAttribute<HostRoleCommandEntity, Long> requestId;
  public static volatile SingularAttribute<HostRoleCommandEntity, Long> stageId;
  public static volatile SingularAttribute<HostRoleCommandEntity, String> hostName;
  public static volatile SingularAttribute<HostRoleCommandEntity, String> role;
  public static volatile SingularAttribute<HostRoleCommandEntity, String> event;
  public static volatile SingularAttribute<HostRoleCommandEntity, Integer> exitcode;
  public static volatile SingularAttribute<HostRoleCommandEntity, String> status;
  public static volatile SingularAttribute<HostRoleCommandEntity, byte[]> stdError;
  public static volatile SingularAttribute<HostRoleCommandEntity, byte[]> stdOut;
  public static volatile SingularAttribute<HostRoleCommandEntity, String> outputLog;
  public static volatile SingularAttribute<HostRoleCommandEntity, String> errorLog;
  public static volatile SingularAttribute<HostRoleCommandEntity, byte[]> structuredOut;
  public static volatile SingularAttribute<HostRoleCommandEntity, Long> startTime;
  public static volatile SingularAttribute<HostRoleCommandEntity, Long> endTime;
  public static volatile SingularAttribute<HostRoleCommandEntity, Long> lastAttemptTime;
  public static volatile SingularAttribute<HostRoleCommandEntity, Short> attemptCount;
  public static volatile SingularAttribute<HostRoleCommandEntity, String> roleCommand;
  public static volatile SingularAttribute<HostRoleCommandEntity, String> commandDetail;
  public static volatile SingularAttribute<HostRoleCommandEntity, String> customCommandName;
}

