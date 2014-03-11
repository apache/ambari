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
  job: null,
  sortingColumn: null,

  loadJobDetails : function() {
    var self = this;
    var timeout = this.get('loadTimeout');
    var yarnService = App.YARNService.find().objectAt(0);
    if (yarnService != null) {
      var content = this.get('content');
      if (content != null) {
        jobsUtils.refreshJobDetails(content, function() {
          self.set('content', self.get('job'));
          self.set('loaded', true);
        }, function(errorId) {
          switch (errorId) {
          case 'job.dag.noId':
            App.showAlertPopup(Em.I18n.t('jobs.hive.tez.dag.error.noDagId.title'), Em.I18n.t('jobs.hive.tez.dag.error.noDagId.message'));
            break;
          case 'job.dag.noname':
            App.showAlertPopup(Em.I18n.t('jobs.hive.tez.dag.error.noDag.title'), Em.I18n.t('jobs.hive.tez.dag.error.noDag.message'));
            break;
          case 'job.dag.id.noDag':
            App.showAlertPopup(Em.I18n.t('jobs.hive.tez.dag.error.noDagForId.title'), Em.I18n.t('jobs.hive.tez.dag.error.noDagForId.message'));
            break;
          case 'job.dag.id.loaderror':
          case 'job.dag.name.loaderror':
            break;
          default:
            break;
          }
          self.routeToJobs();
        });
      }
    } else {
      clearTimeout(timeout);
      timeout = setTimeout(function() {
        self.loadJobDetails();
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
