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
var jobsUtils = require('utils/jobs');

App.MainHiveJobDetailsController = Em.Controller.extend({
  name : 'mainHiveJobDetailsController',

  content : null,
  loaded : false,
  loadTimeout: null,

  loadJobDetails : function(job) {
    var self = this;
    var timeout = this.get('loadTimeout');
    var yarnService = App.YARNService.find().objectAt(0);
    if (yarnService != null) {
      var self = this;
      this.set('loaded', false);
      if (job != null) {
        jobsUtils.refreshJobDetails(job, function() {
          self.set('loaded', true);
        });
      }
    }else{
      clearTimeout(timeout);
      timeout = setTimeout(function(){
        self.loadJobDetails(job);
      }, 300);
    }
  },

  /**
   * path to page visited before
   */
  referer : '',
  /**
   * open dashboard page
   */
  routeHome : function() {
    App.router.transitionTo('main.dashboard');
  },

  /**
   * open jobs page
   *
   * @param event
   */
  routeToJobs : function() {
    App.router.transitionTo('main.jobs.index');
  }
});
