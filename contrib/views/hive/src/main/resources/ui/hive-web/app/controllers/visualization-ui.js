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
  jsonResults: 'Empty',
  cachedObjects: [],
  queryId: function () {
      return this.get('index.model.queryId');
  }.property('index.model.queryId'),
  forcedContent: function () {
    return this.get('index.model.forcedContent');
  }.property('index.model.forcedContent'),
  test: '',

  actions: {
    onTabOpen: function () {
      var self = this;
      this.set('test', 'Hello Query id: ' + this.get('queryId') + ' Query: ' + this.get('forcedContent'));
      var tab;
      var model = this.get('index.model');

      if (model) {
        console.log("Model present");
        var oq = this.get('openQueries');

        tab = oq.getTabForModel(model);

        if (tab) {
          console.log("Tab present");
          this.set('test', 'Tab id: ' + tab.id + ' Tab type: ' + tab.type);
        }
        var query = oq.getQueryForModel(model);
        query = this.get('index').buildQuery(query, false, false);
        console.log("Query: " + query);
        console.log("Title: " + model.get('title'));
        console.log("Query: " + model.get('forcedContent'));
        console.log("Model id: " + model.get('id'));
        console.log("Status: " + model.status);
        console.log("Model");
        console.log(model);
        console.log('Result Object: ');
        console.log(this.get('results'));
        console.log("Cached Results: ");
        console.log(this.get('results').get('cachedResults'));
        console.log("Formatted Results: ");
        console.log(this.get('results').get('formattedResults'));

        var existingJob = this.get('results').get('cachedResults').findBy('id', model.get('id'));
        var url = this.container.lookup('adapter:application').buildURL();
        url += '/' + constants.namingConventions.jobs + '/' + model.get('id') + '/results?first=true&count=1000';

        if (existingJob) {
          console.log("Job exists");
          Ember.$.getJSON(url).then(function (results) {

            var columns = results.schema;
            var rows = results.rows;

            if (!columns || !rows) {
              return;
            }

            columns = columns.map(function (column) {
              return {
                name: column[0],
                type: column[1],
                index: column[2] - 1 //normalize index to 0 based
              };
            });

            self.set('jsonResults', {columns: columns, rows: rows.slice(0,10)});

          }, function (err) {
            self.set('error', err.responseText);
          });
        } else {
          this.set("error", "No visualization available. Please execute a query to visualize data.");
        }
      }
    }
  }

});
