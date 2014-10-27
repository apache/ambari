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

App.AlertDefinition = DS.Model.extend({

  name: DS.attr('string'),
  label: DS.attr('string'),
  serviceName: DS.attr('string'),
  componentName: DS.attr('string'),
  enabled: DS.attr('boolean'),
  scope: DS.attr('string'),
  interval: DS.attr('number'),
  type: DS.attr('string')
});

App.AlertReportDefinition = DS.Model.extend({
  type: DS.attr('string'),
  text: DS.attr('string'),
  value: DS.attr('number')
});

App.AlertMetricsSourceDefinition = DS.Model.extend({
  propertyList: [],
  value: DS.attr('string')
});

App.PortAlertDefinition = App.AlertDefinition.extend({
  defaultPort: DS.attr('number'),
  uri: DS.attr('string'),
  reporting: DS.hasMany('App.AlertReportDefinition')
});

App.MetricsAlertDefinition = App.AlertDefinition.extend({
  jmx: DS.belongsTo('App.AlertMetricsSourceDefinition'),
  ganglia: DS.belongsTo('App.AlertMetricsSourceDefinition'),
  uri: DS.attr('string'),
  reporting: DS.hasMany('App.AlertReportDefinition')
});

App.AggregateAlertDefinition = App.AlertDefinition.extend({
  alertName: DS.attr('string'),
  reporting: DS.hasMany('App.AlertReportDefinition')
});

App.ScriptAlertDefinition = App.AlertDefinition.extend({
  location: DS.attr('string'),
  reporting: DS.hasMany('App.AlertReportDefinition')
});
