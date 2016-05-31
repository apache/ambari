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

package org.apache.ambari.view.hive2.resources.jobs.atsJobs;

import org.json.simple.JSONObject;

import java.util.List;

public class HiveQueryId {
  public static long ATS_15_RESPONSE_VERSION = 2; // version returned from ATS 1.5 release

  public String url;

  public String entity;
  public String query;

  public List<String> dagNames;

  public List<JSONObject> stages;

  public long starttime;
  public long duration;
  public String operationId;
  public String user;
  public long version;
}
