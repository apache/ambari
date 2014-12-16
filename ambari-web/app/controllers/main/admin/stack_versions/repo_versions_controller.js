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

App.RepoVersionsController = Em.ArrayController.extend({
  name: 'repoVersionsController',

  content: function () {
    return App.RepositoryVersion.find().filterProperty('stackVersion', null);
  }.property('dataIsLoaded'),

  /**
   * true if content is loaded to model
   * @type {Boolean}
   */
  dataIsLoaded: false,

  /**
   * path to the mock json
   * @type {String}
   */
  mockUrl: '/data/stack_versions/repo_versions_all.json',

  /**
   * api to get RepoVersions
   * @type {String}
   */
  realUrl: function () {
    return App.get('apiPrefix') + App.get('stackVersionURL') + '/repository_versions?fields=*,operatingSystems/*,operatingSystems/repositories/*';
  }.property('App.stackVersionURL'),

  /**
   * load all data components required by repo version table
   * @return {*}
   * @method load()
   */
  load: function () {
    this.set('dataIsLoaded', false);
    var dfd = $.Deferred();
    var self = this;

    App.get('router.mainStackVersionsController').loadStackVersionsToModel().done(function () {
      self.loadRepoVersionsToModel().done(function () {
        self.set('dataIsLoaded', true);
        dfd.resolve();
      });
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
    App.HttpClient.get(this.getUrl(), App.repoVersionMapper, {
      complete: function () {
        dfd.resolve();
      }
    });
    return dfd.promise();
  },

  /**
   * returns api url to get repositoryVersion
   * or mock json if testmode is on
   * @returns {String}
   * @method getUrl
   */
  getUrl: function () {
    return App.get('testMode') ? this.get('mockUrl') : this.get('realUrl');
  },

  /**
   * sends request to install repoVersion to the cluster
   * and create clusterStackVersion resourse
   * @param event
   * @return {$.ajax}
   * @method installRepoVersion
   */
  installRepoVersion: function (event) {
    var repo = event.context;
    var data = {
      ClusterStackVersions: {
        stack: repo.get('stackVersionType'),
        version: repo.get('stackVersionNumber'),
        repository_version: repo.get('repositoryVersion')
      },
      id: repo.get('id')
    };
    return App.ajax.send({
      name: 'admin.stack_version.install.repo_version',
      sender: this,
      data: data,
      success: 'installStackVersionSuccess'
    });
  },

  /**
   * success callback for <code>installRepoVersion()<code>
   * saves request id to the db, and redirect user to the just
   * created clusterStackVersion.
   * @param data
   * @param opt
   * @param params
   * @method installStackVersionSuccess
   */
  installStackVersionSuccess: function (data, opt, params) {
    App.db.set('repoVersion', 'id', [data.Requests.id]);
    if(!App.StackVersion.find().findProperty('repositoryVersion.id', params.id)) {
      App.get('router.mainStackVersionsController').loadStackVersionsToModel().done(function() {
        var stackVersion = App.StackVersion.find().findProperty('repositoryVersion.id', params.id);
        App.router.transitionTo('main.admin.adminStackVersions.version', stackVersion);
      });
    }
  }
});
