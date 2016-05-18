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

export default Ember.Controller.extend({
  jobProgressService: Ember.inject.service(constants.namingConventions.jobProgress),
  openQueries   : Ember.inject.controller(constants.namingConventions.openQueries),
  notifyService: Ember.inject.service(constants.namingConventions.notify),

  index: Ember.inject.controller(),
  verticesProgress: Ember.computed.alias('jobProgressService.currentJob.stages'),

  actions: {
    onTabOpen: function () {
      var self = this;

      // Empty query
      if(this.get('openQueries.currentQuery.fileContent').length == 0){
        this.set('json', undefined);
        this.set('noquery', 'hive.errors.no.query');
        return;
      } else {
        this.set('noquery', undefined);
      }
      // Introducing a common function
      var getVisualExplainJson = function(){
        self.set('showSpinner', undefined);
        self.set('rerender');
        self.get('index')._executeQuery(constants.jobReferrer.visualExplain, true, true).then(function (json) {
          //this condition should be changed once we change the way of retrieving this json
          if (json['STAGE PLANS']['Stage-1']) {
            self.set('json', json);
          } else {
            self.set('json', {})
          }
        }, function (error) {
          self.set('json', undefined);
          self.get('notifyService').error(error);
        });
        self.toggleProperty('shouldChangeGraph');
      }

      getVisualExplainJson();

    }
  }
});
