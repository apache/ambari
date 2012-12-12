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



module.exports = {
  "href" : "http://localhost:8080/api/clusters/mycluster/requests/1",
  "Requests" : {
    "id" : 1
  },
  "tasks" : [
    {
      "href" : "http://localhost:8080/api/clusters/mycluster/requests/1/tasks/1",
      "Tasks" : {
        "id" : "1",
        "attempt_cnt" : "0",
        "exit_code" : "999",
        "stdout" : "",
        "status" : "COMPLETED",
        "command" : "START",
        "start_time" : "-1",
        "role" : "DATANODE",
        "stderr" : "",
        "host_name" : "localhost.localdomain",
        "stage_id" : "1"
      }
    },
    {
      "href" : "http://localhost:8080/api/clusters/mycluster/requests/1/tasks/2",
      "Tasks" : {
        "id" : "2",
        "attempt_cnt" : "0",
        "exit_code" : "999",
        "stdout" : "",
        "status" : "COMPLETED",
        "command" : "START",
        "start_time" : "-1",
        "role" : "NAMENODE",
        "stderr" : "",
        "host_name" : "localhost.localdomain",
        "stage_id" : "1"
      }
    }
  ]
}