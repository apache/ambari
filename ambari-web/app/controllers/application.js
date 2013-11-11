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

App.ApplicationController = Em.Controller.extend({

  name: 'applicationController',

  clusterName: function () {
    return (App.router.get('clusterController.clusterName') || 'My Cluster');
  }.property('App.router.clusterController.clusterName'),

  clusterDisplayName: function () {
    var name = this.get('clusterName');
    var displayName = name.length > 13 ? name.substr(0, 10) + "..." : name;
    return displayName;
  }.property('clusterName'),

  isClusterDataLoaded: function() {
    return App.router.get('clusterController.isLoaded') && App.router.get('loggedIn');
  }.property('App.router.clusterController.isLoaded','App.router.loggedIn'),

  init: function(){
    this._super();
  },

  loadShowBgChecked: function () {
    if (App.testMode) {
      return true;
    } else {
      this.getUserPref(this.persistKey());
      var currentPrefObject = this.get('currentPrefObject');
      if (currentPrefObject != null) {
        return currentPrefObject;
      } else {
        // post persist
        this.postUserPref(this.persistKey(), true);
        return true;
      }
    }
  },
  persistKey: function () {
    var loginName = App.router.get('loginName');
    return 'admin-settings-show-bg-' + loginName;
  },
  currentPrefObject: null,
  /**
   * get persist value from server with persistKey
   */
  getUserPref: function (key) {
    var self = this;
    var url = App.apiPrefix + '/persist/' + key;
    jQuery.ajax(
      {
        url: url,
        context: this,
        async: false,
        success: function (response) {
          if (response) {
            var value = jQuery.parseJSON(response);
            console.log('Got persist value from server with key: ' + key + '. Value is: ' + response);
            self.set('currentPrefObject', value);
            return value;
          }
        },
        error: function (xhr) {
          // this user is first time login
          if (xhr.status == 404) {
            console.log('Persist did NOT find the key: '+ key);
            self.set('currentPrefObject', null);
            return null;
          }
        },
        statusCode: require('data/statusCodes')
      }
    );
  },
  /**
   * post persist key/value to server, value is object
   */
  postUserPref: function (key, value) {
    var url = App.apiPrefix + '/persist/';
    var keyValuePair = {};
    keyValuePair[key] = JSON.stringify(value);
    jQuery.ajax({
      async: false,
      context: this,
      type: "POST",
      url: url,
      data: JSON.stringify(keyValuePair),
      beforeSend: function () {
        console.log('BeforeSend to persist: persistKeyValues', keyValuePair);
      }
    });
  },
  showSettingsPopup: function() {
    var self = this;
    var initValue = this.loadShowBgChecked();
    var curValue = null;
    App.ModalPopup.show({
      header: Em.I18n.t('common.userSettings'),
      bodyClass: Em.View.extend({
        templateName: require('templates/common/settings'),
        isShowBgChecked: initValue,
        updateValue: function () {
          curValue = this.get('isShowBgChecked');
        }.observes('isShowBgChecked')
      }),
      primary: Em.I18n.t('common.save'),
      onPrimary: function() {
        if (curValue == null) {
          curValue = initValue;
        }
        var key = self.persistKey();
        if (!App.testMode) {
          self.postUserPref(key, curValue);
        }
        this.hide();
      },
      onSecondary: function() {
        this.hide();
      }
    })
  }


});