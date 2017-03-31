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

package org.apache.ambari.view.hive20.resources.jobs.atsJobs;

import org.json.simple.JSONObject;

public interface ATSRequestsDelegate {
  String hiveQueryIdDirectUrl(String entity);

  String hiveQueryIdOperationIdUrl(String operationId);

  String tezDagDirectUrl(String entity);

  String tezDagNameUrl(String name);

  String tezVerticesListForDAGUrl(String dagId);

  JSONObject hiveQueryIdsForUser(String username);

  JSONObject hiveQueryIdByOperationId(String operationId);

  JSONObject tezDagByName(String name);

  JSONObject tezVerticesListForDAG(String dagId);

  JSONObject tezDagByEntity(String entity);

  JSONObject hiveQueryIdsForUserByTime(String username, long startTime, long endTime);

  JSONObject hiveQueryEntityByEntityId(String hiveEntityId);
}
