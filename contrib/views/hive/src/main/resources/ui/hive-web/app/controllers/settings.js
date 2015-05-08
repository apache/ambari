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
import utils from 'hive/utils/functions';

export default Ember.ArrayController.extend({
  needs: [
    constants.namingConventions.index,
    constants.namingConventions.openQueries
  ],

  index: Ember.computed.alias('controllers.' + constants.namingConventions.index),
  openQueries: Ember.computed.alias('controllers.' + constants.namingConventions.openQueries),
  sessionTag: Ember.computed.alias('index.model.sessionTag'),
  sessionActive: Ember.computed.alias('index.model.sessionActive'),

  canInvalidateSession: Ember.computed.and('sessionTag', 'sessionActive'),

  predefinedSettings: constants.hiveParameters,

  selectedSettings: function () {
    var predefined = this.get('predefinedSettings');
    var current = this.get('currentSettings.settings');

    return predefined.filter(function (setting) {
      return current.findBy('key.name', setting.name);
    });
  }.property('currentSettings.settings.@each.key'),

  currentSettings: function () {
    var currentId = this.get('index.model.id');
    var targetSettings = this.findBy('id', currentId);

   if (!targetSettings && currentId) {
      targetSettings = this.pushObject(Ember.Object.create({
        id: currentId,
        settings: []
      }));
    }

    return targetSettings;
  }.property('openQueries.currentQuery'),

  updateSettingsId: function (oldId, newId) {
    this.filterBy('id', oldId).setEach('id', newId);
  },

  getCurrentValidSettings: function () {
    var currentSettings = this.get('currentSettings');
    var validSettings = [];

    if (!currentSettings) {
      return '';
    }

    currentSettings.get('settings').map(function (setting) {
      if (setting.get('valid')) {
        validSettings.pushObject('set %@ = %@;'.fmt(setting.get('key.name'), setting.get('value')));
      }
    });

    return validSettings;
  },

  hasSettings: function (id) {
    var settings;
    var settingId = id ? id : this.get('index.model.id');

    settings = this.findBy('id', settingId);

    return settings && settings.get('settings.length');
  },

  parseQuerySettings: function () {
    var query = this.get('openQueries.currentQuery');
    var content = query.get('fileContent');
    var self = this;
    var regex = new RegExp(utils.regexes.setSetting);
    var settings = content.match(regex) || [];
    var targetSettings = this.findBy('id', this.get('index.model.id'));

    if (!query || !targetSettings) {
      return;
    }

    settings = settings.map(function (setting) {
      var KeyValue = setting.split('=');
      var name     = KeyValue[0].replace('set', '').trim();
      var value    = KeyValue[1].replace(';', '').trim();

      if (!self.get('predefinedSettings').findBy('name', name)) {
        self.get('predefinedSettings').pushObject({
          name: name
        });
      }

      var settingObj = Ember.Object.createWithMixins({
        key: Ember.Object.create({ name: 'nam' }),
        selection : Ember.Object.create({ value: 'val'}),

        value: Ember.computed.alias('selection.value'),
        valid: true
      });

      settingObj.set('key.name', name);
      settingObj.set('selection.value', value);

      return settingObj;
    });

    targetSettings.set('settings', settings);
  }.observes('openQueries.currentQuery', 'openQueries.currentQuery.fileContent', 'openQueries.tabUpdated'),

  validate: function () {
    var settings = this.get('currentSettings.settings') || [];
    var predefinedSettings = this.get('predefinedSettings');

    settings.forEach(function (setting) {
      var predefined = predefinedSettings.findBy('name', setting.get('key.name'));

      if (!predefined) {
        return;
      }

      if (predefined.values && predefined.values.contains(setting.get('value'))) {
        setting.set('valid', true);
        return;
      }

      if (predefined.validate && predefined.validate.test(setting.get('value'))) {
        setting.set('valid', true);
        return;
      }

      if (!predefined.validate) {
        setting.set('valid', true);
        return;
      }

      setting.set('valid', false);
    });
  }.observes('currentSettings.[]', 'currentSettings.settings.[]', 'currentSettings.settings.@each.value', 'currentSettings.settings.@each.key'),

  currentSettingsAreValid: function () {
    var currentSettings = this.get('currentSettings.settings');
    var invalid = currentSettings.filterProperty('valid', false);

    return invalid.length ? false : true;
  }.property('currentSettings.settings.@each.value', 'currentSettings.settings.@each.key'),

  loadSessionStatus: function () {
    var model         = this.get('index.model');
    var sessionActive = this.get('sessionActive');
    var sessionTag    = this.get('sessionTag');
    var adapter       = this.container.lookup('adapter:application');
    var url           = adapter.buildURL() + '/jobs/sessions/' + sessionTag;

    if (sessionTag && sessionActive === undefined) {
      adapter.ajax(url, 'GET')
        .then(function (response) {
          model.set('sessionActive', response.session.actual);
        })
        .catch(function () {
          model.set('sessionActive', false);
        });
    }
  }.observes('index.model', 'index.model.status'),

  actions: {
    add: function () {
      var setting = Ember.Object.createWithMixins({
        valid: true,
        selection: Ember.Object.create(),
        value: Ember.computed.alias('selection.value')
      });

      this.get('currentSettings.settings').pushObject(setting);
    },

    remove: function (setting) {
      var currentQuery = this.get('openQueries.currentQuery');
      var currentQueryContent = currentQuery.get('fileContent');
      var keyValue = 'set %@ = %@;\n'.fmt(setting.get('key.name'), setting.get('value'));

      this.get('currentSettings.settings').removeObject(setting);

      if (currentQueryContent.indexOf(keyValue) > -1) {
        currentQuery.set('fileContent', currentQueryContent.replace(keyValue, ''));
      }
    },

    addKey: function (param) {
      var newKey = this.get('predefinedSettings').pushObject({
        name: param
      });

      this.get('currentSettings.settings').findBy('key', null).set('key', newKey);
    },

    removeAll: function () {
      var currentQuery = this.get('openQueries.currentQuery'),
          currentQueryContent = currentQuery.get('fileContent'),
          regex = new RegExp(utils.regexes.setSetting),
          settings = currentQueryContent.match(regex);

      currentQuery.set('fileContent', currentQueryContent.replace(settings, ''));
      this.get('currentSettings').set('settings', []);
    },

    invalidateSession: function () {
      var self       = this;
      var sessionTag = this.get('sessionTag');
      var adapter    = this.container.lookup('adapter:application');
      var url        = adapter.buildURL() + '/jobs/sessions/' + sessionTag;
      var model = this.get('index.model');

      // @TODO: Split this into then/catch once the BE is fixed
      adapter.ajax(url, 'DELETE').catch(function (response) {
        if ([200, 404].contains(response.status)) {
          model.set('sessionActive', false);
          self.notify.success('alerts.success.sessions.deleted');
        } else {
          self.notify.error(response.responseJSON.message, response.responseJSON.trace);
        }
      });
    }
  }
});