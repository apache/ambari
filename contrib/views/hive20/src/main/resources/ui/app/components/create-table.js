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
import Helper from '../configs/helpers';
import FileFormats from '../configs/file-format';

export default Ember.Component.extend({
  init() {
    this._super(...arguments);
    let defaultFileFormat = FileFormats.findBy('default', true);
    this.set('columns', Ember.A());
    this.set('properties', []);
    this.set('settings', {
      fileFormat: { type: defaultFileFormat.name}
    });
    this.set('shouldAddBuckets', null);
    this.set('settingErrors', []);
  },

  didReceiveAttrs() {
    this.get('tabs').setEach('active', false);
    let firstTab = this.get('tabs.firstObject');
    firstTab.set('active', true);
  },

  actions: {
    activate(link) {
      console.log("Activate: ", link);
    },

    create() {
      if (this.validate()) {
        this.sendAction('create', {
          name: this.get('tableName'),
          columns: this.get('columns'),
          settings: this.get('settings'),
          properties: this.get('properties')
        });
      }
    },

    cancel() {
      this.sendAction('cancel');
    }
  },

  validate() {
    if (!this.validateTableName()) {
      return false;
    }
    if (!(this.checkColumnsExists() &&
      this.checkColumnUniqueness() &&
      this.validateColumns() &&
      this.checkClusteringIfTransactional())) {
      this.selectTab("create.table.columns");
      return false;
    }

    if(!(this.validateNumBuckets())) {
      this.selectTab("create.table.advanced");
      return false;
    }

    if (!(this.validateTableProperties())) {
      this.selectTab("create.table.properties");
      return false;
    }
    return true;
  },
  validateTableName() {
    this.set('hasTableNameError');
    this.set('tableNameErrorText');

    if (Ember.isEmpty(this.get('tableName'))) {
      this.set('hasTableNameError', true);
      this.set('tableNameErrorText', 'Name cannot be empty');
      return false;
    }

    return true;
  },

  checkColumnsExists() {
    this.set('hasTableConfigurationError');
    this.set('tableConfigurationErrorText');
    if (this.get('columns.length') === 0) {
      this.set('hasTableConfigurationError', true);
      this.set('tableConfigurationErrorText', 'No columns configured. Add some column definitions.');
      return false;
    }
    return true;
  },

  checkColumnUniqueness() {
    let columnNames = [];
    for (let i = 0; i < this.get('columns.length'); i++) {
      let column = this.get('columns').objectAt(i);
      column.clearError();
      if (columnNames.indexOf(column.get('name')) === -1) {
        columnNames.pushObject(column.get('name'));
      } else {
        column.get('errors').push({type: 'name', error: 'Name should be unique'});
        return false;
      }
    }

    return true;
  },

  validateColumns() {
    for (let i = 0; i < this.get('columns.length'); i++) {
      let column = this.get('columns').objectAt(i);
      if (!column.validate()) {
        return false;
      }
    }
    return true;
  },

  checkClusteringIfTransactional() {
    let clusteredColumns = this.get('columns').filterBy('isClustered', true);
    if (this.get('settings.transactional') && clusteredColumns.get('length') === 0) {
      this.set('hasTableConfigurationError', true);
      this.set('tableConfigurationErrorText', 'Table is marked as transactional but no clustered column defined. Add some clustered column definitions.');
      return false;
    }
    return true;
  },

  validateTableProperties() {
    for (let i = 0; i < this.get('properties.length'); i++) {
      let property = this.get('properties').objectAt(i);
      if (!property.validate()) {
        return false;
      }
    }
    return true;
  },

  validateNumBuckets() {
    let clusteredColumns = this.get('columns').filterBy('isClustered', true);


    function isNumBucketsPresentAndIsAnInteger(context) {
      return (Ember.isEmpty(context.get('settings.numBuckets')) ||
      !Helper.isInteger(context.get('settings.numBuckets')));
    }

    if(clusteredColumns.get('length') > 0 && isNumBucketsPresentAndIsAnInteger(this)) {
      this.get('settingErrors').pushObject({type: 'numBuckets', error: "Some columns are clustered, Number of buckets are required."});
      return false;
    }

    return true;
  },

  selectTab(link) {
    this.get('tabs').setEach('active', false);
    let selectedTab = this.get('tabs').findBy('link', link);
    if (!Ember.isEmpty(selectedTab)) {
      selectedTab.set('active', true);
    }
  }
});
