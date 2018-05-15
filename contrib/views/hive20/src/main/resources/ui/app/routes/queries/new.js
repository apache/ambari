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
  beforeModel() {
    let existingWorksheets = this.store.peekAll('worksheet');
    let newWorksheetName = 'worksheet';
    if(!this.controllerFor("queries").worksheetCount && !existingWorksheets.get("length")) {
      newWorksheetName = newWorksheetName + 1;
    } else {
      let id = parseInt(this.controllerFor("queries").worksheetCount);
      if(!id){
        id = existingWorksheets.get("length")+1;
      }
      newWorksheetName = newWorksheetName + id;
    }
    let newWorksheetTitle = newWorksheetName.capitalize();
    this.store.createRecord('worksheet', {
      id: newWorksheetName,
      title: newWorksheetTitle,
      isQueryDirty: false,
      //query: 'select 1;',
      //owner: 'admin',
      selected: true
    });
    existingWorksheets.setEach('selected', false);
    this.controllerFor('queries').set('worksheets', this.store.peekAll('worksheet'));
    this.transitionTo('queries.query', newWorksheetTitle);
    this.controllerFor("queries.query").set('previewJobData', null);

  }
});
