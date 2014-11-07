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
  mockUrl: '/data/stack_versions/stack_version_all.json',
  realUrl: function () {
    return App.apiPrefix + '/clusters/' + App.get('clusterName') + '/stack_versions&minimal_response=true';
  }.property('App.clusterName'),

  /**
   * load all data components required by stack version table
   * @return {*}
   */
  load: function () {
    var dfd = $.Deferred();
    var self = this;
    this.loadStackVersionsToModel().done(function () {
      self.set('dataIsLoaded', true);
      dfd.resolve();
    });
    return dfd.promise();
  },

  /**
   * get stack versions from server and push it to model
   * @return {*}
   */
  loadStackVersionsToModel: function () {
    var dfd = $.Deferred();

    App.HttpClient.get(this.getUrl(), App.stackVersionMapper, {
      complete: function () {
        dfd.resolve();
      }
    });
    return dfd.promise();
  },

  getUrl: function () {
    return App.get('testMode') ? this.get('mockUrl') : this.get('realUrl');
  }

});
