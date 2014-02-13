/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var App = require('app');

App.hiveJobsMapper = App.QuickDataMapper.create({
  model : App.HiveJob,
  map : function(json) {
    var model = this.get('model');
    if (!model) {
      return;
    }
    var hiveJobs = []
    if (json && json.entities) {
      var currentEntityMap = {}
      json.entities.forEach(function(entity) {
        currentEntityMap[entity.entity] = entity.entity;
        var hiveJob = {
          id : entity.entity,
          name : entity.entity,
          user : entity.primaryfilters.user
        }
        if (entity.events != null) {
          entity.events.forEach(function(event) {
            switch (event.eventtype) {
            case "QUERY_SUBMITTED":
              hiveJob.start_time = event.ts;
              break;
            case "QUERY_COMPLETED":
              hiveJob.end_time = event.ts;
              break;
            default:
              break;
            }
          });
        }
        hiveJobs.push(hiveJob);
      });
      // Delete IDs not seen from server
      var hiveJobsModel = model.find().toArray();
      hiveJobsModel.forEach(function(job) {
        if (job && !currentEntityMap[job.get('id')]) {
          this.deleteRecord(job);
        }
      }, this);
    }
    App.store.loadMany(model, hiveJobs);
    App.router.get('mainJobsController').set('content', App.HiveJob.find().toArray());
  },
  config : {}
});
