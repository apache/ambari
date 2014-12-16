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

App.MainStackVersionsController = Em.ArrayController.extend({
  name: 'mainStackVersionsController',

  content: App.StackVersion.find(),
  timeoutRef: null,
  isPolling: false,
  dataIsLoaded: false,

  /**
   *  path to the mock json
   * @type {String}
   */
  mockUrl: '/data/stack_versions/stack_version_all.json',

  /**
   * api to get ClusterStackVersions with repository_versions (use to init data load)
   * @type {String}
   */
  realUrl: function () {
    return App.apiPrefix + '/clusters/' + App.get('clusterName') + '/stack_versions?fields=*,repository_versions/*,repository_versions/operatingSystems/repositories/*';
  }.property('App.clusterName'),

  /**
   * api to get ClusterStackVersions without repository_versions (use to update data)
   * @type {String}
   */
  realUpdateUrl: function () {
    return App.apiPrefix + '/clusters/' + App.get('clusterName') + '/stack_versions?fields=ClusterStackVersions/*';
  }.property('App.clusterName'),

  /**
   * request latest data from server and update content
   * @method doPolling
   */
  doPolling: function () {
    var self = this;

    this.set('timeoutRef', setTimeout(function () {
      if (self.get('isPolling')) {
        self.loadStackVersionsToModel(self.get('dataIsLoaded')).done(function () {
          self.doPolling();
        })
      }
    }, App.componentsUpdateInterval));
  },
  /**
   * load all data components required by stack version table
   * @return {*}
   * @method load
   */
  load: function () {
    var dfd = $.Deferred();
    var self = this;
    this.loadStackVersionsToModel().done(function () {
      App.get('router.repoVersionsController').loadRepoVersionsToModel().done(function() {
        self.set('dataIsLoaded', true);
        dfd.resolve();
      });

    });
    return dfd.promise();
  },

  /**
   * get stack versions from server and push it to model
   * @return {*}
   * @method loadStackVersionsToModel
   */
  loadStackVersionsToModel: function (isUpdate) {
    var dfd = $.Deferred();

    App.HttpClient.get(this.getUrl(isUpdate), App.stackVersionMapper, {
      complete: function () {
        dfd.resolve();
      }
    });
    return dfd.promise();
  },

  /**
   * returns api url to get clusteStackVersion wirh repositoryVersion
   * or just clustrerStackVersion if only updates are requested
   * or mock json if testmode is on
   * @param isUpdate true if data needs to be updated
   * @returns {String}
   * @method getUrl
   */
  getUrl: function (isUpdate) {
    return App.get('testMode') ? this.get('mockUrl') :
      isUpdate ? this.get('realUpdateUrl') : this.get('realUrl');
  },

  /**
   * goes to the hosts page with content filtered by repo_version_name and repo_version_state
   * @param version
   * @param state
   * @method filterHostsByStack
   */
  filterHostsByStack: function (version, state) {
    if (!version || !state)
      return;
    App.router.get('mainHostController').filterByStack(version, state);
    App.router.get('mainHostController').set('showFilterConditionsFirstLoad', true);
    App.router.transitionTo('hosts.index');
  },

  /**
   * shows popup with listed hosts wich has current state of hostStackVersion
   * @param event
   * @returns {*|void}
   * @method showHosts
   */
  showHosts: function(event) {
    var self = this;
    var status = event.currentTarget.title.toCapital();
    var version = event.contexts[0];
    var hosts = event.contexts[1];
    if (hosts.length) {
      return App.ModalPopup.show({
        bodyClass: Ember.View.extend({
          title: Em.I18n.t('admin.stackVersions.hosts.popup.title').format(version, status, hosts.length),
          template: Em.Handlebars.compile('<h4>{{view.title}}</h4><span class="limited-height-2">'+ hosts.join('<br/>') + '</span>')
        }),
        header: Em.I18n.t('admin.stackVersions.hosts.popup.header').format(status),
        primary: Em.I18n.t('admin.stackVersions.hosts.popup.primary'),
        secondary: Em.I18n.t('common.close'),
        onPrimary: function() {
          this.hide();
          self.filterHostsByStack(version, status);
        }
      });
    }
  }

});
