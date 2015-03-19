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

  predefinedSettings: constants.hiveParameters,

  currentSettings: function () {
    var currentId = this.get('index.model.id');
    var targetSettings = this.findBy('id', currentId);

   if (!targetSettings) {
      targetSettings = this.pushObject(Ember.Object.create({
        id: currentId,
        settings: []
      }));
    }

    return targetSettings;
  }.property('index.model.id'),

  updateSettingsId: function (oldId, newId) {
    this.filterBy('id', oldId).setEach('id', newId);
  },

  getSettingsString: function () {
    var currentId = this.get('index.model.id');

    var querySettings = this.findBy('id', currentId);

    if (!querySettings) {
      return "";
    }

    var settings = querySettings.get('settings').map(function (setting) {
      return 'set %@ = %@;'.fmt(setting.get('key.name'), setting.get('value'));
    });

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
    var self = this;

    var regex = new RegExp(/^set\s+[\w-.]+(\s+|\s?)=(\s+|\s?)[\w-.]+(\s+|\s?);/gim);
    var settings = content.match(regex);

    if (!settings) {
      return;
    }

    query.set('fileContent', content.replace(regex, '').trim());
    settings = settings.map(function (setting) {
      var KV = setting.split('=');
      var obj = {
        key: {
          name: KV[0].replace('set', '').trim()
        },
        value: KV[1].replace(';', '').trim()
      };

      if (!self.get('predefinedSettings').findBy('name', obj.key.name)) {
        self.get('predefinedSettings').pushObject({
          name: obj.key.name
        });
      }

      return obj;
    });

    this.setSettingForQuery(id, settings);
  }.observes('openQueries.currentQuery', 'openQueries.tabUpdated'),

  setSettingForQuery: function (id, settings) {
    var querySettings = this.findBy('id', id);

    if (!querySettings) {
      this.pushObject(Ember.Object.create({
        id: id,
        settings: settings
      }));
    } else {
      querySettings.setProperties({
        'settings': settings
      });
    }
  },

  validate: function() {
    var settings = this.get('currentSettings.settings') || [];
    var predefinedSettings = this.get('predefinedSettings');

    settings.forEach(function(setting) {
      var predefined = predefinedSettings.filterProperty('name', setting.get('key.name'));
      if (!predefined.length) {
        return;
      } else {
        predefined = predefined[0];
      }

      if (predefined.values && predefined.values.contains(setting.get('value'))) {
        setting.set('valid', true);
        return;
      }

      if (predefined.validate && predefined.validate.test(setting.get('value'))) {
        setting.set('valid', true);
        return;
      }

      setting.set('valid', false);
    });
  }.observes('currentSettings.[]', 'currentSettings.settings.@each.value', 'currentSettings.settings.@each.key'),

  currentSettingsAreValid: function() {
    var currentSettings = this.get('currentSettings.settings');
    var invalid = currentSettings.filterProperty('valid', false);

    return invalid.length ? false : true;
  }.property('currentSettings.settings.@each.value', 'currentSettings.settings.@each.key'),

  actions: {
    add: function () {
      var currentId = this.get('index.model.id'),
          querySettings = this.findBy('id', currentId);

      var Setting = Ember.Object.extend({
        valid: true,
        selection: Ember.Object.create(),
        value: Ember.computed.alias('selection.value')
      });

      querySettings.get('settings').pushObject(Setting.create({}));
    },

    remove: function (setting) {
      this.findBy('id', this.get('index.model.id')).settings.removeObject(setting);
    },

    addKey: function (param) {
      var newKey = this.get('predefinedSettings').pushObject({
        name: param
      });

      this.get('currentSettings.settings').findBy('key', null).set('key', newKey);
    }
  }
});
