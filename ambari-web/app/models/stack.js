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
  id: DS.attr('string'), //  ${stackName}-${stackVersion}.
  stackName: DS.attr('string'),
  stackVersion: DS.attr('string'),
  active: DS.attr('boolean'),  // All of the instances should have this value to true. We should map only those stacks that has active flag set to true
  parentStackVersion: DS.attr('string'),
  minUpgradeVersion: DS.attr('string'),
  minJdkVersion: DS.attr('string'),
  maxJdkVersion: DS.attr('string'),
  configTypes: DS.attr('object'),
  operatingSystems: DS.hasMany('App.OperatingSystem'),
  isSelected: DS.attr('boolean', {defaultValue: false}),

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
  }.property('id'),

  /**
   * @return: {Array} App.StackService instances for selected stack instance. For non-selected stack instance returns empty array
   */
  services: function () {
    var result = [];
    var isStackSelected = this.get('isSelected');
    var stackServices = App.StackService.find().get('length');
    if (isStackSelected && stackServices) {
      result = App.StackService.find();
    }
    return result;
  }.property('isSelected'),

  /**
   * Right now there ambari-web is not fetching this information from the server as it does not need as of present.
   * @TODO: This should return stack level configurations for selected stack instance i.e properties of cluster-env file
   */
  configurations: function() {
    return [];
  }.property('isSelected')

});


App.Stack.FIXTURES = [];
