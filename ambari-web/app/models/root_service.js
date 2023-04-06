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

/**
 * This model loads the rootService (Ambari)
 * The model maps to the  http://localhost:8080/api/v1/services/AMBARI?fields=components/RootServiceComponents
 * @type {*}
 */
App.RootService = DS.Model.extend({
  serviceName: DS.attr('string'),
  displayName: Em.computed.formatRole('serviceName', true),
  components: DS.hasMany('App.RootServiceComponents')
});

App.RootService.FIXTURES = [];

App.RootServiceComponents = DS.Model.extend({
  componentName: DS.attr('string'),
  displayName: Em.computed.formatRole('componentName', false),
  componentVersion: DS.attr('string'),
  serverClock: DS.attr('number'),
  serviceName: DS.attr('string'),
  properties: DS.attr('object')
});

App.RootServiceComponents.FIXTURES = [];

