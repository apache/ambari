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

  model() {

    let existingWorksheets = this.store.peekAll('worksheet');

    if(existingWorksheets.get('length') === 0) {
      this.store.createRecord('worksheet', {
        id: 'worksheet1',
        title: 'Worksheet1',
        query: 'select 1;',
        selectedDb : 'default',
        owner: 'admin',
        selected: true
      });
    }

    return this.store.peekAll('worksheet');

  },
  setupController(controller, model) {
    this._super(...arguments);
    controller.set('worksheets', model);

    // This is just the initial currentWorksheet, It will be set on correctly on click of worksheet.
    controller.set('currentWorksheet', controller.get('worksheets').get('firstObject'));

  },

  actions: {

    createNewWorksheet(){

      let worksheets = this.controllerFor('queries').get('model');
      worksheets.forEach((worksheet) => {
        worksheet.set('selected', false);
      });

      let localWs = {
        id: `worksheet${worksheets.get('length') + 1}`,
        title:`Worksheet${worksheets.get('length') + 1}`,
        query: 'select '+ parseInt(worksheets.get('length') + 1) + ';',
        selectedDb : 'default',
        owner: 'admin',
        selected: true
      };

      let newWorksheet = this.store.createRecord('worksheet', localWs );
      this.set('controller.worksheets', this.store.peekAll('worksheet'));

      this.transitionTo('queries.query', localWs.title);
    }

  }
});
