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
var date = require('utils/date');
var misc = require('utils/misc');

App.App = DS.Model.extend({

  run: DS.belongsTo('App.Run'),

  appId: DS.attr('string'),
  appName: DS.attr('string'),
  appType: DS.attr('string'),
  workflowEntityName: DS.attr('string'),
  userName: DS.attr('string'),
  queue: DS.attr('string'),
  submitTime: DS.attr('number'),
  launchTime: DS.attr('number'),
  finishTime: DS.attr('number'),
  numStages: DS.attr('number'),
  stages: DS.attr('object'),
  status: DS.attr('string'),
});

App.App.FIXTURES = [];
