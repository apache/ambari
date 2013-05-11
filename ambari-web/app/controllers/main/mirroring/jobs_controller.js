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

var App = require('app');

App.MainJobsController = Em.Controller.extend({
  name: 'mainJobsController',
  jobs: function () {
    return App.DataSetJob.find().filterProperty('dataset', this.get('content'));
  }.property('content'),

  actionDesc: function () {
    var dataset_status = this.get('content.status');
    if (dataset_status === "SCHEDULED") {
      return "Suspend";
    } else {
      return "Schedule";
    }
  }.property('content.status'),

  isScheduled: function () {
    var dataset_status = this.get('content.status');
    return dataset_status === "SCHEDULED";
  }.property('content.status'),

  suspend: function () {
    this.set('content.status', 'SUSPENDED');
  },

  schedule: function () {
    this.set('content.status', 'SCHEDULED');
  }

});
