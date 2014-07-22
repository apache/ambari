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

App.MainConfigHistoryController = Em.ArrayController.extend({
  name: 'mainConfigHistoryController',

  dataSource: App.ServiceConfigVersion.find(),
  content: function () {
    return this.get('dataSource').toArray();
  }.property('dataSource.@each.isLoaded'),
  isLoaded: false,
  isPolling: false,

  /**
   * initial data load
   */
  load: function () {
    var self = this;

    this.set('isLoaded', false);
    this.loadHistoryToModel().done(function () {
      self.set('isLoaded', true);
      self.doPolling();
    });
  },

  /**
   * get data from server and push it to model
   * @return {*}
   */
  loadHistoryToModel: function () {
    var dfd = $.Deferred();

    var url = '/data/configurations/service_versions.json';

    App.HttpClient.get(url, App.serviceConfigVersionsMapper, {
      complete: function () {
        dfd.resolve();
      }
    });
    return dfd.promise();
  },

  /**
   * request latest data from server and update content
   */
  doPolling: function () {
    var self = this;

    setTimeout(function () {
      if (self.get('isPolling')) {
        self.loadHistoryToModel().done(function () {
          self.doPolling();
        })
      }
    }, App.componentsUpdateInterval);
  }
});
