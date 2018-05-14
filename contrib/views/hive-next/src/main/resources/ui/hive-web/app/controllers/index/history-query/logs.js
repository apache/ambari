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
import constants from 'hive/utils/constants';
import utils from 'hive/utils/functions';

export default Ember.ObjectController.extend({
  fileService: Ember.inject.service(constants.namingConventions.file),
  notifyService: Ember.inject.service(constants.namingConventions.notify),
  ldapService: Ember.inject.service(constants.namingConventions.ldap),

  needs: [ constants.namingConventions.queryTabs,
           constants.namingConventions.index,
           constants.namingConventions.openQueries ],

  queryTabs: Ember.computed.alias('controllers.' + constants.namingConventions.queryTabs),
  index: Ember.computed.alias('controllers.' + constants.namingConventions.index),
  openQueries: Ember.computed.alias('controllers.' + constants.namingConventions.openQueries),

  requestLdapPassword:function(callback) {
    var ldap = this.get('ldapService');
    ldap.requestLdapPassword(this,callback);
  },

  reloadJobLogs: function (job) {
    var self = this;
    var handleError = function (error) {
      job.set('isRunning', false);
      // Check if error is 401
      // show LDAP login and fail job
      // show message to rerun job
      if(error.status === 401){
        self.requestLdapPassword(function(){
          var err = {
            message: Ember.I18n.t('alerts.success.query.rerun')
          };
          self.get('notifyService').error(err);
        });
      } else
        self.get('notifyService').error(error);
    };

    job.reload().then(function () {
      if (utils.insensitiveCompare(job.get('status'), constants.statuses.error) ||
          utils.insensitiveCompare(job.get('status'), constants.statuses.failed)) {
        handleError(job.get('statusMessage'));
      }

      self.get('fileService').reloadFile(job.get('logFile')).then(function (file) {
        var fileContent = file.get('fileContent');
        var stillRunning = self.isJobRunning(job);
        var currentIndexModelId = self.get('index.model.id');
        var currentActiveTab = self.get('queryTabs.activeTab.name');

        if (fileContent) {
          job.set('log', fileContent);
        }

        //if the current model is the same with the one displayed, continue reloading job
        if (stillRunning) {
          Ember.run.later(self, function () {
            this.reloadJobLogs(job);
          }, 10000);
        } else if (!stillRunning) {
          job.set('isRunning', undefined);
          job.set('retrievingLogs', false);

          if (utils.insensitiveCompare(job.get('status'), constants.statuses.succeeded)) {
            self.get('openQueries').updateTabSubroute(job, constants.namingConventions.subroutes.jobResults);

            if (job.get('id') === currentIndexModelId && currentActiveTab === constants.namingConventions.index) {
              self.transitionToRoute(constants.namingConventions.subroutes.historyQuery, job.get('id'));
            }
          }
        }

      },function (err) {
        handleError(err);
      });
    }, function (err) {
      job.set('status', constants.statuses.error);
      handleError(err);
    });
  },



  isJobRunning: function (job) {
    return utils.insensitiveCompare(job.get('status'),
                                    constants.statuses.unknown,
                                    constants.statuses.initialized,
                                    constants.statuses.running,
                                    constants.statuses.pending);
  },

  getLogs: function () {
    var job = this.get('content');

    if (this.isJobRunning(job)) {
      if (!job.get('retrievingLogs')) {
        job.set('retrievingLogs', true);
        job.set('isRunning', true);
        this.reloadJobLogs(job);
      }
    } else if (utils.insensitiveCompare(job.get('status'), constants.statuses.succeeded) && !job.get('dagId')) {
      //if a job that never polled for logs is succeeded, jump straight to results tab.
      this.get('openQueries').updateTabSubroute(job, constants.namingConventions.subroutes.jobResults);
      this.transitionToRoute(constants.namingConventions.subroutes.historyQuery, job.get('id'));
    }
  }.observes('content')
});
