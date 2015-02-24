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

export default Ember.ArrayController.extend({
  needs: [
    constants.namingConventions.index,
    constants.namingConventions.openQueries
  ],

  index: Ember.computed.alias('controllers.' + constants.namingConventions.index),
  openQueries: Ember.computed.alias('controllers.' + constants.namingConventions.openQueries),

  showSettingsOverlay: false,

  querySettings: function () {
    var currentId = this.get('index.model.id');
    return this.findBy('id', currentId);
  }.property('model.[]', 'index.model.id'),

  updateSettingsId: function (oldId, newId) {
    this.filterBy('id', oldId).setEach('id', newId);
  },

  getSettingsString: function (id) {
    var currentId = id ? id : this.get('index.model.id');

    var querySettings = this.findBy('id', currentId);

    if (!querySettings) {
      return "";
    }

    var settings = querySettings.get('settings').map(function (setting) {
      return 'set %@ = %@;'.fmt(setting.key, setting.value);
    });

    if (querySettings.get('runOnTez')) {
      settings.push('set %@ = tez;'.fmt(constants.settings.executionEngine));
    }

    return settings.join("\n");
  },

  hasSettings: function (id) {
    id = id ? id : this.get('index.model.id');
    var settings = this.findBy('id', id);

    return settings && settings.get('settings.length');
  },

  parseQuerySettings: function () {
    var id = this.get('index.model.id');
    var query = this.get('openQueries.currentQuery');
    var content = query.get('fileContent');
    var runOnTez = false;


    var regex = new RegExp(/^set\s+[\w-.]+(\s+|\s?)=(\s+|\s?)[\w-.]+(\s+|\s?);/gim);
    var settings = content.match(regex);

    if (!settings) {
      return;
    }

    query.set('fileContent', content.replace(regex, '').trim());
    settings = settings.map(function (setting) {
      var KV = setting.split('=');

      return {
        key: KV[0].replace('set', '').trim(),
        value: KV[1].replace(';', '').trim()
      };
    });

    // remove runOnTez from settings
    settings = settings.findBy('key', constants.settings.executionEngine).without(false);

    this.setSettingForQuery(id, settings, !!runOnTez);
  }.observes('openQueries.currentQuery', 'openQueries.tabUpdated'),

  setSettingForQuery: function (id, settings, runOnTez) {
    var querySettings = this.findBy('id', id);

    if (!querySettings) {
      this.pushObject(Ember.Object.create({
        id: id,
        settings: settings,
        runOnTez: runOnTez
      }));
    } else {
      querySettings.setProperties({
        'settings': settings,
        'runOnTez': runOnTez
      });
    }
  },

  createSettingsForQuery: function () {
    var currentId = this.get('index.model.id');

    if (!this.findBy('id', currentId)) {
      this.pushObject(Ember.Object.create({
        id: currentId,
        settings: [],
        runOnTez: false
      }));
    }
  },

  actions: {
    toggleOverlay: function () {
      // create a setting object if its not already there
      this.createSettingsForQuery();
      this.toggleProperty('showSettingsOverlay');
    },

    add: function () {
      var currentId = this.get('index.model.id'),
       querySettings = this.findBy('id', currentId);

      querySettings.settings.pushObject(Ember.Object.create({
        key: '',
        value: ''
      }));
    },

    remove: function (setting) {
      var currentId = this.get('index.model.id');
      this.findBy('id', currentId).settings.removeObject(setting);
    }
  }
});
