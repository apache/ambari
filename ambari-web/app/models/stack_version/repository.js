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

App.Repository = DS.Model.extend({
  id: DS.attr('string'),
  baseUrl : DS.attr('string'),
  defaultBaseUrl : DS.attr('string'),
  latestBaseUrl : DS.attr('string'),
  mirrorsList : DS.attr('array'),
  osType : DS.attr('string'),
  repoId : DS.attr('string'),
  repoName : DS.attr('string'),
  stackName : DS.attr('string'),
  stackVersion : DS.attr('string'),
  operatingSystem: DS.belongsTo('App.OS')
});

App.Repository.FIXTURES = [];


