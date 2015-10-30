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
  selectedRowCount: constants.defaultVisualizationRowCount,
  needs: [ constants.namingConventions.index,
            constants.namingConventions.openQueries,
            constants.namingConventions.jobResults
          ],
  index         : Ember.computed.alias('controllers.' + constants.namingConventions.index),
  openQueries   : Ember.computed.alias('controllers.' + constants.namingConventions.openQueries),
  results   : Ember.computed.alias('controllers.' + constants.namingConventions.jobResults),
  notifyService: Ember.inject.service(constants.namingConventions.notify),

  polestarUrl: '',
  voyagerUrl: '',
  polestarPath: 'polestar/#/',
  voyagerPath: 'voyager/#/',

  showDataExplorer: true,
  showAdvVisulization: false,

  visualizationTabs: function () {
    return [
      Ember.Object.create({
        name: 'Data Visualization',
        id: 'visualization',
        url: this.get('polestarUrl')
      }),
      Ember.Object.create({
        name: 'Data Explorer',
        id: 'data_explorer',
        url: this.get('voyagerUrl')
      })
    ]
  }.property('polestarUrl', 'voyagerUrl'),

  activeTab: function () {
    console.log("I am in activeTab function.");
    this.get('visualizationTabs')[0].active = this.get("showDataExplorer");
    this.get('visualizationTabs')[1].active = this.get("showAdvVisulization");
  }.observes('polestarUrl', 'voyagerUrl'),

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
        url += '&count='+self.get('selectedRowCount')+'&job_id='+model.get('id')
        if (existingJob) {
          if(existingJob.results[0].rows.length === 0){
            this.set("error", "Query has insufficient results to visualize the data.");
            return;
          }
          this.set("error", null);
          var id = model.get('id');
          this.set("polestarUrl", this.get('polestarPath') + "?url=" + url);
          this.set("voyagerUrl", this.get('voyagerPath') + "?url=" + url);
          Ember.run.scheduleOnce('afterRender', this, function(){
            self.alterIframe();
          });
        } else {
          this.set("error", "No visualization available. Please execute a query and wait for the results to visualize the data.");
        }
      }
    },

      changeRowCount: function () {
        var self = this;
        if(isNaN(self.get('selectedRowCount')) || !(self.get('selectedRowCount')%1 === 0) || (self.get('selectedRowCount') <= 0)){
          self.get('notifyService').error("Please enter a posive integer number.");
          return;
        }
        var model = this.get('index.model');
        if (model) {
          var existingJob = this.get('results').get('cachedResults').findBy('id', model.get('id'));
          var url = this.container.lookup('adapter:application').buildURL();
          url += '/' + constants.namingConventions.jobs + '/' + model.get('id') + '/results?&first=true';
          url += '&count='+self.get('selectedRowCount')+'&job_id='+model.get('id');
          if (existingJob) {
            this.set("error", null);
            var id = model.get('id');

            $('.nav-tabs.visualization-tabs li.active').each(function( index ) {

              if($(this)[index].innerText.indexOf("Data Explorer") > -1){
                self.set("showDataExplorer",true);
                self.set("showAdvVisulization",false);
                self.set("voyagerUrl", self.get('voyagerPath') + "?url=" + url);
                self.set("polestarUrl", self.get('polestarPath') + "?url=" + url);
                document.getElementById("visualization_frame").src =  self.get("voyagerUrl");
              }
              if($(this)[index].innerText.indexOf("Advanced Visualization") > -1){
                self.set("showAdvVisulization",true);
                self.set("showDataExplorer",false);
                self.set("voyagerUrl", self.get('voyagerPath') + "?url=" + url);
                self.set("polestarUrl", self.get('polestarPath') + "?url=" + url);
                document.getElementById("visualization_frame").src = self.get("polestarUrl");
              }
            })
            document.getElementById("visualization_frame").contentWindow.location.reload();
          } else {
            this.set("error", "No visualization available. Please execute a query and wait for the results to visualize data.");
          }
        }

      }
  }
});
