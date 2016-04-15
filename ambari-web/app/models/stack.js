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

App.Stack = DS.Model.extend({
  id: DS.attr('string'), //  ${stackName}-${stackVersion}-${repoVersion}.
  stackName: DS.attr('string'),
  stackVersion: DS.attr('string'),
  repositoryVersion: DS.attr('string'),
  showAvailable: DS.attr('boolean'),  // All of the instances should have this value to true. We should map only those stacks that has this flag set to true
  type: DS.attr('string'), // ["PATCH", "STANDARD"]
  stackServices: DS.hasMany('App.ServiceSimple'),
  operatingSystems: DS.hasMany('App.OperatingSystem'),
  isSelected: DS.attr('boolean', {defaultValue: false}),

  stackNameVersion: function () {
    //${stackName}-${stackVersion}.
    return this.get('stackName') + '-' + this.get('stackVersion');
  }.property('stackName', 'stackVersion'),

  isPatch: function () {
    return this.get('type') == "PATCH";
  }.property('type'),
  displayName: function () {
    //${stackName}-${repositoryVersion}.
    return this.get('stackName') + '-' + this.get('repositoryVersion');
  }.property('stackName', 'repositoryVersion'),

  /**
   * @return: {Array} returns supported repositories for all OperatingSystem's supported by a stack instance
   */
  repositories: function () {
    var operatingSystems = this.get('operatingSystems');
    var repositories = [];
    operatingSystems.forEach(function (os) {
      os.get('repositories').forEach(function (repository) {
        repositories.pushObject(repository);
      }, this);
    }, this);
    return repositories;
  }.property('id')
});


App.Stack.FIXTURES = [];