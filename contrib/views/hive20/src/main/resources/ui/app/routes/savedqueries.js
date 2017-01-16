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

export default Ember.Route.extend({

  savedQueries: Ember.inject.service(),

  model() {
    return this.get('savedQueries').getAllQueries();
  },

  setupController(controller, model) {
    this._super(...arguments);
    controller.set('showDeleteSaveQueryModal', false);
    controller.set('selectedSavedQueryId', null);
  },

  actions: {
    historySavedQuery(id){
      console.log('historySavedQuery', id);
    },

    deleteSavedQuery(){
      let queryId = this.get('controller').get('selectedSavedQueryId');

      console.log('deleteSavedQuery', queryId);
      this.get('savedQueries').deleteSaveQuery(queryId)
        .then((data) => {
          console.log('Deleted saved query.', data);
          this.get('controller').set('showDeleteSaveQueryModal', false );
          //$(window).reload();
        }, (error) => {
          console.log("Error encountered", error);
        });
    },

    deleteSavedQuerypDeclined(){
      this.get('controller').set('selectedSavedQueryId', null);
      this.get('controller').set('showDeleteSaveQueryModal', false );
    },

    openDeleteSavedQueryModal(id){
      this.get('controller').set('showDeleteSaveQueryModal', true );
      this.get('controller').set('selectedSavedQueryId', id );
    },

    openAsWorksheet(savedQuery){

      let hasWorksheetModel = this.modelFor('queries');
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

      let localWs = {
        id: worksheetId,
        title: savedQuery.title,
        query: savedQuery.shortQuery,
        selectedDb : savedQuery.dataBase,
        owner: savedQuery.owner,
        selected: true
      };

      this.store.createRecord('worksheet', localWs );

      this.transitionTo('queries.query', localWs.title);
    }
  }

});
