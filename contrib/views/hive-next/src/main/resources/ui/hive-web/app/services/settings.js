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

export default Ember.Service.extend({

  notifyService: Ember.inject.service('notify'),

  settings: Ember.ArrayProxy.create({ content: [] }),
  predefinedSettings: constants.hiveParameters,
  isDefaultSettingsLoaded :false,

  _createSetting: function(name, value) {
    var setting = Ember.Object.createWithMixins({
      valid     : true,
      value     : Ember.computed.alias('selection.value'),
      selection : Ember.Object.create()
    });

    if (name) {
      setting.set('key', Ember.Object.create({ name: name }));
    }

    if (value) {
      setting.set('selection.value', value);
    }

    return setting;
  },

  _createDefaultSettings: function(settings) {
    if (!settings) {
      return;
    }

    for (var key in settings) {
      this.get('settings').pushObject(this._createSetting(key, settings[key]));
    }
  },

  _validate: function () {
    var settings = this.get('settings');
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
  }.observes('settings.@each.value', 'settings.@each.key'),

  add: function() {
    this.get('settings').pushObject(this._createSetting());
  },

  createKey: function(name) {
    var key = { name: name };
    this.get('predefinedSettings').pushObject(key);

    this.get('settings').findBy('key', null).set('key', key);
  },

  remove: function(setting) {
    this.get('settings').removeObject(setting);
  },

  removeAll: function() {
    this.get('settings').clear();
  },

  loadDefaultSettings: function() {
    var isDefaultSettingsLoaded = this.get('isDefaultSettingsLoaded');
    if (isDefaultSettingsLoaded == true) {
      return false;
    }
    var adapter       = this.container.lookup('adapter:application');
    var url           = adapter.buildURL() + '/savedQueries/defaultSettings';
    var self = this;

    adapter.ajax(url)
      .then(function(response) {
        self.set('isDefaultSettingsLoaded',true);
        self._createDefaultSettings(response.settings);
      })
      .catch(function(error) {
        self.get('notifyService').error(error);
      });
  },

  saveDefaultSettings: function() {
    var self     = this;
    var data     = {};
    var adapter  = this.container.lookup('adapter:application');
    var url      = adapter.buildURL() + '/savedQueries/defaultSettings';
    var settings = this.get('settings');

    var settingException = {};

    try {
      settings.forEach(function(setting) {

        settingException['value'] = Ember.isEmpty(setting.get('value'));

        if(settingException['value']) {
          settingException['name'] = setting.get('key.name');
          throw settingException
        }
        data[setting.get('key.name')] = setting.get('value');

      });
    } catch(e) {
      if (e!==settingException) throw e;
    }


    if(settingException['value']){
      self.get('notifyService').error('Please enter the value for '+ settingException['name'] );
      return;
    }

    adapter.ajax(url, 'POST', {
        data: {settings: data }
      })
      .then(function(response) {
        if (response && response.settings) {
          self.get('notifyService').success(Ember.I18n.t('alerts.success.settings.saved'));
        } else {
          self.get('notifyService').error(response);
        }
      });
  },

  getSettings: function() {
    var settings = this.get('settings');
    var asString = "";

    if (!settings.get('length')) {
      return asString;
    }

    settings.forEach(function(setting) {
      asString += "set %@=%@;\n".fmt(setting.get('key.name'), setting.get('value'));
    });

    return asString;
  },

  parseGlobalSettings: function(query, model) {
    if (!query || !model || !model.get('globalSettings')) {
      return;
    }

    var globals = model.get('globalSettings');
    var content = query.get('fileContent');

    if (globals !== this.getSettings()) {
      return;
    }

    query.set('fileContent', content.replace(globals, ''));
  }

});
