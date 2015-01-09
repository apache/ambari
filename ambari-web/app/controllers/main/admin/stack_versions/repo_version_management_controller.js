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

App.RepoVersionsManagementController = Em.ArrayController.extend({
  name: 'repoVersionsManagementController',

  dataIsLoaded: false,
  timeoutRef: null,
  isPolling: false,
  /**
   * path to the mock json
   * @type {String}
   */
  mockRepoUrl: '/data/stack_versions/repo_versions_all.json',

  /**
   * api to get RepoVersions
   * @type {String}
   */
  realRepoUrl: function () {
    //TODO correct url after api will be fixed
    return App.get('apiPrefix') + App.get('stackVersionURL') +
      '/repository_versions?fields=*,operating_systems/*,operating_systems/repositories/*,operatingSystems/*,operatingSystems/repositories/*';
  }.property('App.stackVersionURL'),

  /**
   * path to the mock json
   * @type {String}
   */
  mockStackUrl: '/data/stack_versions/stack_version_all.json',

  /**
   * api to get ClusterStackVersions with repository_versions (use to init data load)
   * @type {String}
   */
  realStackUrl: function () {
    //TODO correct url after api will be fixed
    return App.apiPrefix + '/clusters/' + App.get('clusterName') +
      '/stack_versions?fields=*,repository_versions/*,repository_versions/operating_systems/repositories/*,repository_versions/operatingSystems/repositories/*';
  }.property('App.clusterName'),

  /**
   * api to get ClusterStackVersions without repository_versions (use to update data)
   * @type {String}
   */
  realUpdateUrl: function () {
    return App.apiPrefix + '/clusters/' + App.get('clusterName') + '/stack_versions?fields=ClusterStackVersions/*';
  }.property('App.clusterName'),

  /**
   * returns url to get data for repoVersion or clusterStackVersion
   * @param {Boolean} stack true if load clusterStackVersion
   * @param {Boolean} fullLoad true if load all data
   * @returns {String}
   * @method getUrl
   */
  getUrl: function(stack, fullLoad) {
    if (App.get('testMode')) {
      return stack ? this.get('mockStackUrl') : this.get('mockRepoUrl')
    } else {
      if (fullLoad) {
        return stack ? this.get('realStackUrl') : this.get('realRepoUrl');
      } else {
        return this.get('realUpdateUrl');
      }
    }
  },

  /**
   * get stack versions from server and push it to model
   * @return {*}
   * @method loadStackVersionsToModel
   */
  loadStackVersionsToModel: function (fullLoad) {
    var dfd = $.Deferred();
    App.HttpClient.get(this.getUrl(true, fullLoad), App.stackVersionMapper, {
      complete: function () {
        dfd.resolve();
      }
    });
    return dfd.promise();
  },

  /**
   * get repo versions from server and push it to model
   * @return {*}
   * @params {Boolean} isUpdate - if true loads part of data that need to be updated
   * @method loadRepoVersionsToModel()
   */
  loadRepoVersionsToModel: function () {
    var dfd = $.Deferred();
    App.HttpClient.get(this.getUrl(false, true), App.repoVersionMapper, {
      complete: function () {
        dfd.resolve();
      }
    });
    return dfd.promise();
  },

  /**
   * loads all needed data
   * @returns {$.Deferred().promise()}
   * @method load
   */
  load: function() {
    var dfd = $.Deferred();
    var self = this;
    self.set('dataIsLoaded', false);
    self.loadStackVersionsToModel(true).done(function () {
      self.loadRepoVersionsToModel().done(function() {
        self.set('dataIsLoaded', true);
        dfd.resolve();
      });
    });
    return dfd.promise();
  },

  /**
   * request latest data from server and update content
   * @method doPolling
   */
  doPolling: function () {
    var self = this;

    this.set('timeoutRef', setTimeout(function () {
      if (self.get('isPolling')) {
        self.loadStackVersionsToModel(false).done(function () {
          self.doPolling();
        })
      }
    }, App.componentsUpdateInterval));
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
   * runs <code>showHostsListPopup<code>
   * @param event
   * @returns {void}
   * @method showHosts
   */
  showHosts: function(event) {
    var status = event.contexts[0];
    var version = event.contexts[1];
    var hosts = event.contexts[2];
    this.showHostsListPopup(status, version, hosts);
  },

  /**
   * shows popup with listed hosts wich has current state of hostStackVersion
   * @param {Object} status - status of repoverion
   *    {id: "string", label: "string"}
   * @param {string} version - repo version name
   * @param {[string]} hosts - array of host containing current repo version in proper state
   * @returns {App.ModalPopup}
   * @method showHostsListPopup
   */
  showHostsListPopup: function(status, version, hosts) {
    var self = this;
    if (hosts.length) {
      return App.ModalPopup.show({
        bodyClass: Ember.View.extend({
          title: Em.I18n.t('admin.stackVersions.hosts.popup.title').format(version, status.label, hosts.length),
          template: Em.Handlebars.compile('<h4>{{view.title}}</h4><span class="limited-height-2">'+ hosts.join('<br/>') + '</span>')
        }),
        header: Em.I18n.t('admin.stackVersions.hosts.popup.header').format(status.label),
        primary: Em.I18n.t('admin.stackVersions.hosts.popup.primary'),
        secondary: Em.I18n.t('common.close'),
        onPrimary: function() {
          this.hide();
          self.filterHostsByStack(version, status.id);
        }
      });
    }
  }
});