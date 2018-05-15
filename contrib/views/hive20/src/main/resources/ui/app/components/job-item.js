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
import UILoggerMixin from '../mixins/ui-logger';

export default Ember.Component.extend(UILoggerMixin, {
  jobs: Ember.inject.service(),
  tagName: '',
  expanded: false,
  expandedValue: null,
  store: Ember.inject.service(),
  savedQueries: Ember.inject.service(),


  actions: {
    toggleExpandJob(jobId) {
      if(this.get('expanded')) {
        this.set('expanded', false);
      } else {
        this.set('expanded', true);
        this.set('valueLoading', true);
        this.get('jobs').getQuery(jobId).then((queryFile) => {
          this.set('queryFile', queryFile);
          this.set('valueLoading', false);
        }).catch((err) => {
          console.log('err', err);
          this.set('valueLoading', false);
        });
      }

    },
    openAsWorksheet(savedQuery){

      let hasWorksheetModel = this.get('model'), self = this;
      let worksheetId;

      if (Ember.isEmpty(hasWorksheetModel)){
        worksheetId = 1;
      }else {

        let isWorksheetExist = (this.get('model').filterBy('title', savedQuery.title).get('length') > 0);
        if(isWorksheetExist) {
          this.sendAction('openWorksheet', savedQuery, true);
          return;
        }

        let worksheets = this.get('model');
        worksheets.forEach((worksheet) => {
          worksheet.set('selected', false);
      });
        worksheetId = `worksheet${worksheets.get('length') + 1}`;
      }
      var isTabExisting = this.get("store").peekRecord('worksheet', savedQuery.id);
      if(isTabExisting) {
        self.sendAction('openWorksheet', savedQuery, true);
        return;
      }
      this.get("savedQueries").fetchSavedQuery(savedQuery.get('queryFile')).then(function(response) {
        let localWs = {
          id: savedQuery.get('id'),
          title: savedQuery.get('title'),
          queryFile: savedQuery.get('queryFile'),
          query: response.file.fileContent,
          selectedDb : savedQuery.get('dataBase'),
          owner: savedQuery.get('owner'),
          selected: true
        };
        self.sendAction('openWorksheet', localWs);
      }, (error) => {
        self.get('logger').danger('Failed to load the query', self.extractError(error));
    });

    }
  }
});
