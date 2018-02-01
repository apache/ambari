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

export default Ember.Route.extend(UILoggerMixin, {

  savedQueries: Ember.inject.service(),

  model() {
    return this.store.findAll('savedQuery').then(savedQueries => savedQueries.toArray());
  },

  setupController(controller, model) {
    this._super(...arguments);

    controller.set('savedQuerylist', model);

    controller.set('showDeleteSaveQueryModal', false);
    controller.set('selectedSavedQueryId', null);
    controller.set('preview', {"noSort":true});
    controller.set('title', {"noSort":true});
    controller.set('dataBase', {"noSort":true});
    controller.set('owner', {"noSort":true});
  },

  actions: {
    sort(sortProp, sortField, key) {
      let perm = {};
      perm[key] = true;
      this.get('controller').set(sortField, perm);
      this.get('controller').set('sortProp', [sortProp]);
    },
    deleteSavedQuery(){
      let queryId = this.get('controller').get('selectedSavedQueryId');
      let self = this;

      console.log('deleteSavedQuery', queryId);
      let recordToDelete = this.get('store').peekRecord('saved-query', queryId);
      recordToDelete.destroyRecord().then(function (data) {
        self.send('deleteSavedQueryDeclined');
        self.send('refreshSavedQueryList');
      }, (error) => {
        console.log('error', error);
      });
    },

    refreshSavedQueryList(){
      this.get('store').findAll('saved-query').then(data => {
        let savedQueryList = [];
        data.forEach(x => {
          let localSavedQuery = {
            'id': x.get('id'),
            'dataBase': x.get('dataBase'),
            'title': x.get('title'),
            'queryFile': x.get('queryFile'),
            'owner': x.get('owner'),
            'shortQuery': x.get('shortQuery')
          };
          savedQueryList.pushObject(localSavedQuery);
        });

        this.get('controller').set('savedQuerylist',savedQueryList);
      });
    },

    deleteSavedQueryDeclined(){
      this.get('controller').set('selectedSavedQueryId', null);
      this.get('controller').set('showDeleteSaveQueryModal', false );
    },

    openDeleteSavedQueryModal(id){
      this.get('controller').set('showDeleteSaveQueryModal', true );
      this.get('controller').set('selectedSavedQueryId', id );
    },

    openAsWorksheet(savedQuery){

      let hasWorksheetModel = this.modelFor('queries'), self = this;
      let worksheetId;

      if (Ember.isEmpty(hasWorksheetModel)){
        worksheetId = 1;
      }else {

        let isWorksheetExist = (this.controllerFor('queries').get('model').filterBy('title', savedQuery.title).get('length') > 0);
        if(isWorksheetExist) {
          this.transitionTo('queries.query', savedQuery.title);
          return;
        }

        let worksheets = this.controllerFor('queries').get('model');
        worksheets.forEach((worksheet) => {
          worksheet.set('selected', false);
        });
        worksheetId = `worksheet${worksheets.get('length') + 1}`;
      }
      var isTabExisting = this.store.peekRecord('worksheet', savedQuery.id);
      if(isTabExisting) {
        self.transitionTo('queries.query', isTabExisting.get("id"));
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

        self.store.createRecord('worksheet', localWs );
        self.controllerFor('queries').set('worksheets', self.store.peekAll('worksheet'));

        self.transitionTo('queries.query', savedQuery.get('id'));
      }, (error) => {
         self.get('logger').danger('Failed to load the query', self.extractError(error));
      });
    }
  }

});
