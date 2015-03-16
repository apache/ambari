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

/**
 * THIS IS NOT USED FOR NOW
 * FOR CONFIG GROUPS WE ARE USING OLD MODELS AND LOGIC
 */

var App = require('app');

App.ServiceConfigGroup = DS.Model.extend({
  /**
   * unique id generated as <code>serviceName<code><code>configGroupId<code>
   * in case default configGroup <code>serviceName<code><code>0<code>
   * @property {string}
   */
  id: DS.attr('string'),

  /**
   * original id for config group that is get from server
   * for default groups "-1"
   * @property {number}
   */
  configGroupId: DS.attr('number'),

  name: DS.attr('string'),
  serviceName: DS.attr('string'),
  description: DS.attr('string'),
  hostNames: DS.attr('array'),
  configVersions: DS.hasMany('App.ConfigVersion'),
  service: DS.belongsTo('App.Service')
});

App.ServiceConfigGroup.FIXTURES = [];
