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
  dataIsLoaded: false,
  mockUrl: '/data/stack_versions/repo_versions_all.json',
  realUrl: function () {
    return App.get('apiPrefix') + App.get('stackVersionURL') + '/repository_versions?fields=*,operatingSystems/*,operatingSystems/repositories/*';
  }.property('App.stackVersionURL'),
  /**
   * load all data components required by repo version table
   * @return {*}
   */
  load: function () {
    this.set('dataIsLoaded', false);
    var dfd = $.Deferred();
    var self = this;
    this.loadRepoVersionsToModel().done(function () {
      self.set('dataIsLoaded', true);
      dfd.resolve();
    });
    return dfd.promise();
  },

  /**
   * get repo versions from server and push it to model
   * @return {*}
   */
  loadRepoVersionsToModel: function (isUpdate) {
    var dfd = $.Deferred();
    var self = this;
    App.get('router.mainStackVersionsController').loadStackVersionsToModel().done(function () {
      App.HttpClient.get(self.getUrl(isUpdate), App.repoVersionMapper, {
        complete: function () {
          dfd.resolve();
        }
      });
    });

    return dfd.promise();
  },

  getUrl: function (isUpdate) {
    return App.get('testMode') ? this.get('mockUrl') :
      isUpdate ? this.get('realUpdateUrl') : this.get('realUrl');
  },

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
    App.ajax.send({
      name: 'admin.stack_version.install.repo_version',
      sender: this,
      data: data,
      success: 'installStackVersionSuccess'
    });
  },

  installStackVersionSuccess: function (data, opt, params) {
    var stackVersion = App.StackVersion.find().findProperty('repositoryVersion.id', params.id);
    App.router.transitionTo('main.admin.adminStackVersions.version', stackVersion);
  }
});
