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

var App = require('app');

var dateUtils = require('utils/date');

/**
 * Controller for user settings
 * Allows to get them from persist and update them to the persist
 *
 * @class UserSettingsController
 */
App.UserSettingsController = Em.Controller.extend(App.UserPref, {

  name: 'userSettingsController',

  /**
   * @type {object}
   */
  userSettings: {},

  /**
   * Each property type is {name: string, defaultValue: *}
   * @type {object}
   */
  userSettingsKeys: function () {
    var loginName = App.router.get('loginName');
    var prefix = 'admin-settings-';
    return {
      show_bg: {
        name: prefix +'show-bg-' + loginName,
        defaultValue: true
      },
      timezone: {
        name: prefix + 'timezone-' + loginName,
        defaultValue: dateUtils.detectUserTimezone()
      }
    };
  }.property('App.router.loginName'),

  init: function () {
    this.set('timezonesFormatted', this._parseTimezones());
    this._super();
  },

  /**
   *
   * @private
   * @method _parseTimezones
   * @return {{utcOffset: number, label: string, value: string}[]}
   */
  _parseTimezones: function () {
    return dateUtils.getAllTimezoneNames().map(function (timeZoneName) {
      var zone = moment(new Date()).tz(timeZoneName);
      var offset = zone.format('Z');
      return {
        utcOffset: zone.utcOffset(),
        label: '(UTC' + offset + ') ' + timeZoneName,
        value: timeZoneName
      };
    }).sort(function (zoneA, zoneB) {
      if (zoneA.utcOffset === zoneB.utcOffset) {
        if (zoneA.value === zoneB.value) {
          return 0;
        }
        return zoneA.value < zoneB.value ? -1 : 1;
      }
      else {
        if(zoneA.utcOffset === zoneB.utcOffset) {
          return 0;
        }
        return zoneA.utcOffset < zoneB.utcOffset ? -1 : 1;
      }
    });
  },

  /**
   * Load some user's setting from the persist
   * If <code>persistKey</code> is not provided, all settings are loaded
   * @param {string} [persistKey]
   * @method dataLoading
   * @returns {$.Deferred.promise}
   */
  dataLoading: function (persistKey) {
    var key = persistKey ? this.get('userSettingsKeys.' + persistKey + '.name') : '';
    var dfd = $.Deferred();
    var self = this;
    this.getUserPref(key).complete(function () {
      var curPref = self.get('currentPrefObject');
      self.set('currentPrefObject', null);
      dfd.resolve(curPref);
    });
    return dfd.promise();
  },

  getUserPrefSuccessCallback: function (response) {
    if (!Em.isNone(response)) {
      this.updateUserPrefWithDefaultValues(response);
    }
    this.set('currentPrefObject', response);
    return response;
  },

  getUserPrefErrorCallback: function (request) {
    // this user is first time login
    if (404 == request.status) {
      this.updateUserPrefWithDefaultValues();
    }
  },

  /**
   * Load all current user's settings to the <code>userSettings</code>
   * @method getAllUserSettings
   */
  getAllUserSettings: function () {
    var userSettingsKeys = this.get('userSettingsKeys');
    var userSettings = {};
    this.dataLoading().done(function (json) {
      Object.keys(userSettingsKeys).forEach(function (k) {
        userSettings[k] = JSON.parse(json[userSettingsKeys[k].name]);
      });
    });
    this.set('userSettings', userSettings);
  },

  /**
   * If user doesn't have any settings stored in the persist,
   * default values should be populated there
   * @param {object} [response]
   * @method updateUserPrefWithDefaultValues
   */
  updateUserPrefWithDefaultValues: function (response) {
    response = response || {};
    var keys = this.get('userSettingsKeys');
    var self = this;
    Object.keys(keys).forEach(function (key) {
      if (Em.isNone(response[keys[key].name])) {
        self.postUserPref(key, keys[key].defaultValue);
      }
    });
  },

  /**
   * "Short"-key method for post user settings to the persist
   * Example:
   *  real key is something like 'userSettingsKeys.timezone.name'
   *  but user should call this method with 'timezone'
   * @method postUserPref
   * @param {string} key
   * @param {*} value
   * @returns {*}
   */
  postUserPref: function (key, value) {
    return this._super(this.get('userSettingsKeys.' + key + '.name'), value);
  },

  /**
   * Sync <code>userSettingsKeys</code> after each POST-update
   * @returns {*}
   */
  postUserPrefSuccessCallback: function () {
    return this.getAllUserSettings();
  },

  /**
   * Open popup with user settings
   * @method showSettingsPopup
   */
  showSettingsPopup: function() {
    // Settings only for admins
    if (!App.isAccessible('upgrade_ADMIN')) {
      return;
    }
    var self = this;
    var curValue = null;
    var keys = this.get('userSettingsKeys');
    var timezonesFormatted = this.get('timezonesFormatted');

    this.dataLoading().done(function (response) {
      var initValue = JSON.parse(response[keys.show_bg.name]);
      var initTimezone = timezonesFormatted.findProperty('value', JSON.parse(response[keys.timezone.name]));
      return App.ModalPopup.show({

        header: Em.I18n.t('common.userSettings'),

        bodyClass: Em.View.extend({

          templateName: require('templates/common/settings'),

          isNotShowBgChecked: !initValue,

          updateValue: function () {
            curValue = !this.get('isNotShowBgChecked');
          }.observes('isNotShowBgChecked'),

          /**
           * @type {{label: string, value: string}}
           */
          timezonesList: timezonesFormatted

        }),

        /**
         * @type {string}
         */
        selectedTimezone: initTimezone,

        primary: Em.I18n.t('common.save'),

        onPrimary: function() {
          if (Em.isNone(curValue)) {
            curValue = initValue;
          }
          if (!App.get('testMode')) {
            self.postUserPref('show_bg', curValue);
            self.postUserPref('timezone', this.get('selectedTimezone.value'));
          }
          if (this.needsPageRefresh()) {
            location.reload();
          }
          this._super();
        },

        /**
         * Determines if page should be refreshed after user click "Save"
         * @returns {boolean}
         */
        needsPageRefresh: function () {
          return initTimezone !== this.get('selectedTimezone');
        }

      })
    });
  }

});
