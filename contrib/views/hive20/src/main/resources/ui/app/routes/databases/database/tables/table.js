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
import tabs from '../../../../configs/table-level-tabs';

export default Ember.Route.extend({
  model(params) {
    let database = this.modelFor('databases.database').get('name');
    let table = params.name;
    return this.store.queryRecord('tableInfo', {databaseId: database, tableName: table});
  },

  setupController: function (controller, model) {
    this._super(controller, model);
    let newTabs = Ember.copy(tabs);
    if (Ember.isEmpty(model.get('partitionInfo'))) {
      newTabs = newTabs.rejectBy('name', 'partitions');
    }

    console.log(model.get('detailedInfo.tableType').toLowerCase());
    if (model.get('detailedInfo.tableType').toLowerCase().indexOf('view') === -1) {
      newTabs = newTabs.rejectBy('name', 'viewInfo');
    }
    controller.set('tabs', newTabs);
  },

  actions: {}
});
