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
var customDatePopup = require('/views/common/custom_date_popup');

App.MainAlertDefinitionsController = Em.ArrayController.extend({

  name: 'mainAlertDefinitionsController',

  /**
   * Timestamp when <code>App.alertDefinitionsMapper</code> run last time
   * Current <code>content</code> is updated on when it changed
   * @type {number|null}
   */
  mapperTimestamp: null,

  /**
   * List of all <code>App.AlertDefinition</code>
   * Consists of:
   * <ul>
   *   <li>App.PortAlertDefinition</li>
   *   <li>App.MetricsAlertDefinition</li>
   *   <li>App.WebAlertDefinition</li>
   *   <li>App.AggregateAlertDefinition</li>
   *   <li>App.ScriptAlertDefinition</li>
   * </ul>
   * @type {App.AlertDefinition[]}
   */
  content: function() {
    return Array.prototype.concat.call(Array.prototype, App.PortAlertDefinition.find().toArray(),
      App.MetricsAlertDefinition.find().toArray(),
      App.WebAlertDefinition.find().toArray(),
      App.AggregateAlertDefinition.find().toArray(),
      App.ScriptAlertDefinition.find().toArray());
  }.property('mapperTimestamp'),

  toggleState: Em.K

});
