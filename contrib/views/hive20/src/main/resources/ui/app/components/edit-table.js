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
import datatypes from '../configs/datatypes';
import Column from '../models/column';
import Helper from '../configs/helpers';
import TableProperty from '../models/table-property';

export default Ember.Component.extend({

  init() {
    this._super(...arguments);
    this.set('columns', Ember.A());
    this.set('properties', []);
    this.set('settings', {});
    this.set('shouldAddBuckets', null);
    this.set('settingErrors', []);
  },

  didReceiveAttrs() {
    this.get('tabs').setEach('active', false);
    let firstTab = this.get('tabs.firstObject');
    firstTab.set('active', true);
    this.set('columns', this._transformColumns());
    this.set('properties', this._extractParameters());
    this.set('settings', this._extractSettings());
  },

  actions: {
    cancel() {
      this.sendAction('cancel');
    },

    edit() {
      if (this.validate()) {
        this.sendAction('edit', {
          database: this.get('table.database'),
          table: this.get('table.table'),
          columns: this.get('columns'),
          settings: this.get('settings'),
          properties: this.get('properties')
        });
      }
    }
  },

  _transformColumns() {
    let columns = [];
    columns.pushObjects(this.get('table.columns').map((item) => {
      return this._getColumnEntry(item, false, this._isClustered(this.get('table'), item.name));
    }));

    if (!Ember.isEmpty(this.get('table.partitionInfo'))) {
      columns.pushObjects(this.get('table.partitionInfo.columns').map((item) => {
        return this._getColumnEntry(item, true, false);
      }));
    }

    return columns;
  },

  _getColumnEntry(column, isPartitioned, isClustered) {
    return Column.create({
      name: column.name,
      type: this._getType(column.type),
      comment: column.comment,
      precision: column.precision,
      scale: column.scale,
      isPartitioned: isPartitioned,
      isClustered: isClustered,
      editing: !(isPartitioned || isClustered),
      newColumn: false
    });
  },

  _getType(typeString) {
    return datatypes.find((item) => item.label.toLowerCase() === typeString.toLowerCase());
  },

  _isClustered(tableInfo, columnName) {
    if (!Ember.isEmpty(tableInfo.get('storageInfo.bucketCols'))) {
      return tableInfo.get('storageInfo.bucketCols').contains(columnName);
    } else {
      return false;
    }
  },

  _extractParameters() {
    if (!Ember.isEmpty(this.get('table.detailedInfo.parameters'))) {
      let tableProperties = this.get('table.detailedInfo.parameters');
      return Object.keys(tableProperties)
        .filter((item) => item !== 'transactional')
        .map((item) => {
          return TableProperty.create({
            key: item,
            value: tableProperties[item],
            editing: false,
            newProperty: false
          });
        });
    } else {
      return [];
    }
  },

  _extractSettings() {
    let settings = {};
    let tableInfo = this.get('table');

    // filter out transaction parameter to set if transactional
    if (!Ember.isEmpty(this.get('table.detailedInfo.parameters'))) {
      let tableProperties = this.get('table.detailedInfo.parameters');
      let transactional = Object.keys(tableProperties)
        .filter((item) => item === 'transactional');
      if (!Ember.isEmpty(transactional)) {
        settings.transactional = true;
      }
    }

    // Find if already clustered, then set number of buckets
    if (!Ember.isEmpty(tableInfo.get('storageInfo.bucketCols'))) {
      settings.numBuckets = parseInt(tableInfo.get('storageInfo.numBuckets'));
      this.set('shouldAddBuckets', true);
    }

    return settings;
  },

  validate() {
    if (!(this.checkColumnUniqueness() &&
      this.validateColumns())) {
      this.selectTab("edit.table.columns");
      return false;
    }

    if(!(this.validateNumBuckets())) {
      this.selectTab("edit.table.advanced");
      return false;
    }

    if (!(this.validateTableProperties())) {
      this.selectTab("edit.table.properties");
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
    if(clusteredColumns.get('length') > 0 &&
      (Ember.isEmpty(this.get('settings.numBuckets')) ||
      !Helper.isInteger(this.get('settings.numBuckets')))) {
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
