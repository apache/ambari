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

  needs: [ constants.namingConventions.index,
            constants.namingConventions.openQueries,
            constants.namingConventions.jobResults
          ],
  index         : Ember.computed.alias('controllers.' + constants.namingConventions.index),
  openQueries   : Ember.computed.alias('controllers.' + constants.namingConventions.openQueries),
  results   : Ember.computed.alias('controllers.' + constants.namingConventions.jobResults),

  polestarUrl: '',
  voyagerUrl: '',
  polestarPath: 'polestar/#/',
  voyagerPath: 'voyager/#/',

  visualizationTabs: function () {
    return [
      Ember.Object.create({
        name: 'Data Explorer',
        id: 'data_explorer',
        url: this.get('voyagerUrl')
      }),
      Ember.Object.create({
        name: 'Advanced Visualization',
        id: 'visualization',
        url: this.get('polestarUrl')
      })
    ]
  }.property('polestarUrl', 'voyagerUrl'),

  alterIframe: function () {
    Ember.$("#visualization_frame").height(Ember.$("#visualization").height());
  },

  actions: {
    onTabOpen: function () {
      var self = this;
      var model = this.get('index.model');
      if (model) {
        var existingJob = this.get('results').get('cachedResults').findBy('id', model.get('id'));
        var url = this.container.lookup('adapter:application').buildURL();
        url += '/' + constants.namingConventions.jobs + '/' + model.get('id') + '/results?&first=true';
        url += '&count='+constants.visualizationRowCount+'&job_id='+model.get('id');
        if (existingJob) {
          this.set("error", null);
          var id = model.get('id');
          this.set("polestarUrl", this.get('polestarPath') + "?url=" + url);
          this.set("voyagerUrl", this.get('voyagerPath') + "?url=" + url);
          Ember.run.scheduleOnce('afterRender', this, function(){
            self.alterIframe();
          });
        } else {
          this.set("error", "No visualization available. Please execute a query and wait for the results to visualize data.");
        }
      }
    }
  }
});
