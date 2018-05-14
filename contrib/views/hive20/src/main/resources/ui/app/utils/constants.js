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

import Ember from 'ember';

export default Ember.Object.create({
  /**
   * This should reflect the naming conventions accross the application.
   * Changing one value also means changing the filenames for the chain of files
   * represented by that value (routes, controllers, models etc).
   * This dependency goes both ways.
  */
  namingConventions: {
    routes: {
    },

    subroutes: {
    },

    job: 'job'
  },

  services: {
    alertMessages: 'alert-messages',
    jobs: 'jobs',
  },

  jobReferrer: {
    sample: 'SAMPLE',
    explain: 'EXPLAIN',
    visualExplain: 'VISUALEXPLAIN',
    job: 'JOB',
    user: 'USER',
    internal: 'INTERNAL'
  },

  statuses: {
    unknown: "UNKNOWN",
    initialized: "INITIALIZED",
    running: "RUNNING",
    succeeded: "SUCCEEDED",
    canceled: "CANCELED",
    closed: "CLOSED",
    error: "ERROR",
    failed: 'FAILED',
    killed: 'KILLED',
    pending: "PENDING"
  },
});
