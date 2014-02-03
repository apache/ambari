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

App.MainJobsController = Em.ArrayController.extend({

  name:'mainJobsController',

  content: [],

  loaded : false,
  loading : false,
  /**
   * The number of jobs to be shown by last submitted time.
   */
  jobsLimit : -1,

  /**
   * List of users.
   * Will be used for filtering in user column.
   * Go to App.MainJobsView.userFilterView for more information
   */
  users: function () {
    return this.get('content').mapProperty("user").uniq().map(function(userName){
      return {
        name: userName,
        checked: false
      };
    });
  }.property('content.length'),

  columnsName: Ember.ArrayController.create({
    content: [
      { name: Em.I18n.t('jobs.column.id'), index: 0 },
      { name: Em.I18n.t('jobs.column.user'), index: 1 },
      { name: Em.I18n.t('jobs.column.start.time'), index: 2 },
      { name: Em.I18n.t('jobs.column.end.time'), index: 3 },
      { name: Em.I18n.t('jobs.column.duration'), index: 4 }
    ]
  }),

  loadJobs : function() {
    var self = this;
    var jobsLimit = this.get('jobsLimit');
    var yarnService = App.YARNService.find().objectAt(0);
    if (yarnService != null) {
      this.set('loading', true);
      var historyServerHostName = yarnService.get('resourceManagerNode.hostName')
      var hiveQueriesUrl = App.testMode ? "/data/jobs/hive-queries.json" : App.apiPrefix + "/proxy?url=http://" + historyServerHostName
          + ":8188/ws/v1/apptimeline/HIVE_QUERY_ID?fields=events,primaryfilters";
      if (jobsLimit > 0) {
        hiveQueriesUrl += ("?limit=" + jobsLimit);
      }
      App.HttpClient.get(hiveQueriesUrl, App.hiveJobsMapper, {
        complete : function(jqXHR, textStatus) {
          self.set('loading', false);
          self.set('loaded', true);
        }
      });
    }
  },

  refreshLoadedJobs : function() {
    this.loadJobs();
  }.observes('jobsLimit', 'App.router.clusterController.isLoaded')
})
