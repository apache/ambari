/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var App = require('app');

var stringUtils = require('utils/string_utils');

App.alertDefinitionsMapper = App.QuickDataMapper.create({

  model: App.AlertDefinition,
  reportModel: App.AlertReportDefinition,
  metricsSourceModel: App.AlertMetricsSourceDefinition,
  metricsUriModel: App.AlertMetricsUriDefinition,
  parameterModel: App.AlertDefinitionParameter,

  config: {
    id: 'AlertDefinition.id',
    name: 'AlertDefinition.name',
    description: 'AlertDefinition.description',
    label: 'AlertDefinition.label',
    service_id: 'AlertDefinition.service_name',
    service_name: 'AlertDefinition.service_name',
    component_name: 'AlertDefinition.component_name',
    enabled: 'AlertDefinition.enabled',
    scope: 'AlertDefinition.scope',
    interval: 'AlertDefinition.interval',
    type: 'AlertDefinition.source.type',
    reporting_key: 'reporting',
    reporting_type: 'array',
    reporting: {
      item: 'id'
    },
    parameters_key: 'reporting',
    parameters_type: 'array',
    parameters: {
      item: 'id'
    }
  },

  portConfig: {
    default_port: 'AlertDefinition.source.default_port',
    port_uri: 'AlertDefinition.source.uri'
  },

  aggregateConfig: {
    alert_name: 'AlertDefinition.source.alert_name'
  },

  scriptConfig: {
    location: 'AlertDefinition.source.path'
  },
  
  serverConfig: {
  },

  uriConfig: {
    id: 'AlertDefinition.source.uri.id',
    http: 'AlertDefinition.source.uri.http',
    https: 'AlertDefinition.source.uri.https',
    https_property: 'AlertDefinition.source.uri.https_property',
    https_property_value: 'AlertDefinition.source.uri.https_property_value',
    connection_timeout: 'AlertDefinition.source.uri.connection_timeout'
  },

  parameterConfig: {
    id: 'AlertDefinition.source.parameters.id',
    name: 'AlertDefinition.source.parameters.name',
    display_name: 'AlertDefinition.source.parameters.display_name',
    units: 'AlertDefinition.source.parameters.units',
    value: 'AlertDefinition.source.parameters.value',
    description: 'AlertDefinition.source.parameters.description',
    type: 'AlertDefinition.source.parameters.type',
    threshold: 'AlertDefinition.source.parameters.threshold'
  },

  map: function (json) {
    console.time('App.alertDefinitionsMapper execution time');
    if (json && json.items) {
      var self = this,
          alertDefinitions = [],
          alertReportDefinitions = [],
          alertMetricsSourceDefinitions = [],
          alertMetricsUriDefinitions = [],
          alertGroupsMap = App.cache.previousAlertGroupsMap,
          existingAlertDefinitions = App.AlertDefinition.find(),
          existingAlertDefinitionsMap = {},
          alertDefinitionsToDelete = existingAlertDefinitions.mapProperty('id'),
          rawSourceData = {};

      existingAlertDefinitions.forEach(function (d) {
        existingAlertDefinitionsMap[d.get('id')] = d;
      });

      json.items.forEach(function (item) {
        var convertedReportDefinitions = [];
        var reporting = item.AlertDefinition.source.reporting;
        for (var report in reporting) {
          if (reporting.hasOwnProperty(report)) {
            if (report === "units") {
              convertedReportDefinitions.push({
                id: item.AlertDefinition.id + report,
                type: report,
                text: reporting[report]
              });
            } else {
              convertedReportDefinitions.push({
                id: item.AlertDefinition.id + report,
                type: report,
                text: reporting[report].text,
                value: reporting[report].value
              });
            }
          }
        }

        alertReportDefinitions = alertReportDefinitions.concat(convertedReportDefinitions);
        item.reporting = convertedReportDefinitions;

        rawSourceData[item.AlertDefinition.id] = item.AlertDefinition.source;
        item.AlertDefinition.description = item.AlertDefinition.description || '';

        var alertDefinition = this.parseIt(item, this.get('config'));

        if (alertGroupsMap[alertDefinition.id]) {
          alertDefinition.groups = alertGroupsMap[alertDefinition.id];
        }

        var oldAlertDefinition = existingAlertDefinitionsMap[alertDefinition.id];
        if (oldAlertDefinition) {
          // new values will be parsed in the another mapper, so for now just use old values
          alertDefinition.summary = oldAlertDefinition.get('summary');
          alertDefinition.last_triggered = oldAlertDefinition.get('lastTriggered');
          alertDefinition.last_triggered_raw = oldAlertDefinition.get('lastTriggeredRaw');
        }

        alertDefinitionsToDelete = alertDefinitionsToDelete.without(alertDefinition.id);

        // map properties dependent on Alert Definition type
        switch (item.AlertDefinition.source.type) {
          case 'PORT':
            alertDefinitions.push($.extend(alertDefinition, this.parseIt(item, this.get('portConfig'))));
            break;
          case 'METRIC':
            // map App.AlertMetricsSourceDefinition's
            var jmxMetric = item.AlertDefinition.source.jmx;
            var gangliaMetric = item.AlertDefinition.source.ganglia;
            if (jmxMetric) {
              alertDefinition.jmx_id = item.AlertDefinition.id + 'jmx';
              alertMetricsSourceDefinitions.push({
                id: alertDefinition.jmx_id,
                value: jmxMetric.value,
                property_list: jmxMetric.property_list
              });
            }
            if (gangliaMetric) {
              alertDefinition.ganglia_id = item.AlertDefinition.id + 'ganglia';
              alertMetricsSourceDefinitions.push({
                id: alertDefinition.ganglia_id,
                value: gangliaMetric.value,
                property_list: gangliaMetric.property_list
              });
            }

            // map App.AlertMetricsUriDefinition
            alertDefinition.uri_id = item.AlertDefinition.id + 'uri';
            item.AlertDefinition.source.uri.id = alertDefinition.uri_id;
            alertMetricsUriDefinitions.push(this.parseIt(item, this.get('uriConfig')));
            alertDefinitions.push(alertDefinition);
            break;
          case 'WEB':
            // map App.AlertMetricsUriDefinition
            alertDefinition.uri_id = item.AlertDefinition.id + 'uri';
            item.AlertDefinition.source.uri.id = alertDefinition.uri_id;
            alertMetricsUriDefinitions.push(this.parseIt(item, this.get('uriConfig')));
            alertDefinitions.push(alertDefinition);
            break;
          case 'AGGREGATE':
            alertDefinitions.push($.extend(alertDefinition, this.parseIt(item, this.get('aggregateConfig'))));
            break;
          case 'SCRIPT':
            alertDefinitions.push($.extend(alertDefinition, this.parseIt(item, this.get('scriptConfig'))));
            break;
          case 'SERVER':
            alertDefinitions.push($.extend(alertDefinition, this.parseIt(item, this.get('serverConfig'))));
            break;
          case 'RECOVERY':
            alertDefinitions.push($.extend(alertDefinition, this.parseIt(item, this.get('uriConfig'))));
            break;
          default:
            console.error('Incorrect Alert Definition type:', item.AlertDefinition);
        }
      }, this);

      alertDefinitionsToDelete.forEach(function(definitionId) {
        self.deleteRecord(existingAlertDefinitions.findProperty('id', definitionId));
      });

      // load all mapped data to model
      App.store.loadMany(this.get('reportModel'), alertReportDefinitions);
      App.store.loadMany(this.get('metricsSourceModel'), alertMetricsSourceDefinitions);
      this.setMetricsSourcePropertyLists(this.get('metricsSourceModel'), alertMetricsSourceDefinitions);
      App.store.loadMany(this.get('metricsUriModel'), alertMetricsUriDefinitions);
      // this loadMany takes too much time
      App.store.loadMany(this.get('model'), alertDefinitions);
      this.setAlertDefinitionsRawSourceData(rawSourceData);
      App.store.commit();
    }
    console.timeEnd('App.alertDefinitionsMapper execution time');
  },

  /**
   * set propertyList properties from <code>data</code> for records in <code>model</code>
   * @param model
   * @param data
   */
  setMetricsSourcePropertyLists: function (model, data) {
    var modelsMap = {};
    model.find().forEach(function (m) {
      modelsMap[m.get('id')] = m;
    });
    data.forEach(function (record) {
      var m = modelsMap[record.id];
      if (m) {
        m.set('propertyList', record.property_list);
      }
    });
  },

  /**
   * set rawSourceDate properties for <code>App.AlertDefinition</code> records
   * @param rawSourceData
   */
  setAlertDefinitionsRawSourceData: function (rawSourceData) {
    var allDefinitions = App.AlertDefinition.find();
    var allDefinitionsMap = {};
    allDefinitions.forEach(function(d) {
      allDefinitionsMap[d.get('id')] = d;
    });
    for (var alertDefinitionId in rawSourceData) {
      if (rawSourceData.hasOwnProperty(alertDefinitionId)) {
        var m = allDefinitionsMap[+alertDefinitionId];
        if (m) {
          m.set('rawSourceData', rawSourceData[alertDefinitionId]);
        }
      }
    }
  }
});
