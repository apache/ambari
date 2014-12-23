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

App.RepoVersionsController = App.RepoVersionsManagementController.extend({
  name: 'repoVersionsController',

  content: function () {
    return App.RepositoryVersion.find().filterProperty('stackVersion', null);
  }.property('dataIsLoaded'),

  /**
   * installs repoversion to the cluster by running <code>installRepoVersion<code> method
   * of <code>mainStackVersionsDetailsController<code> controller
   * @param event
   * @method installRepoVersion
   */
  installRepoVersion: function(event) {
    App.get('router.mainStackVersionsDetailsController').installRepoVersion(event);
  }
});
