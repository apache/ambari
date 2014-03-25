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
    if (json) {
      if(!json.entities) {
        json.entities = [];
        if(json.entity){
          json.entities = [json];
        }
      }
      var currentEntityMap = {}
      json.entities.forEach(function(entity) {
        currentEntityMap[entity.entity] = entity.entity;
        var hiveJob = {
          id : entity.entity,
          name : entity.entity,
          user : entity.primaryfilters.user
        }
        hiveJob.has_tez_dag = false;
        hiveJob.query_text = '';
        if (entity.otherinfo && entity.otherinfo.query) {
          // Explicit false match needed for when failure hook not set
          hiveJob.failed = entity.otherinfo.status===false;
          hiveJob.has_tez_dag = entity.otherinfo.query.match("\"Tez\".*\"DagName:\"");
          var queryJson = $.parseJSON(entity.otherinfo.query);
          if (queryJson && queryJson.queryText) {
            hiveJob.query_text = queryJson.queryText;
          }
        }
        if (entity.events != null) {
          entity.events.forEach(function(event) {
            switch (event.eventtype) {
            case "QUERY_SUBMITTED":
              hiveJob.start_time = event.timestamp;
              break;
            case "QUERY_COMPLETED":
              hiveJob.end_time = event.timestamp;
              break;
            default:
              break;
            }
          });
        }
        if (!hiveJob.start_time && entity.starttime > 0) {
          hiveJob.start_time = entity.starttime;
        }
        if (!hiveJob.end_time && entity.endtime > 0) {
          hiveJob.end_time = entity.endtime;
        }
        hiveJobs.push(hiveJob);
        hiveJob = null;
        entity = null;
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
    json = null;
    hiveJobs = null;
  },
  config : {}
});
