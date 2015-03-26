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

App.GraphWidgetView = App.ChartLinearTimeView.extend({

  /**
   * @type {string}
   */
  id: function () {
    return this.get('content.id');
  }.property('content.id'),

  /**
   * @type {string}
   */
  title: function () {
    return this.get('content.displayName');
  }.property('content.displayName'),

  /**
   * @type {string}
   */
  renderer: function () {
    return this.get('properties.graph_type') === 'STACK' ? 'area' : 'line';
  }.property('properties.graph_type'),

  /**
   * @type {RegExp}
   * @const
   */
  EXPRESSION_REGEX: /\$\{([\w\.\+\-\*\/\(\)]*)\}/g,

  /**
   * @type {RegExp}
   * @const
   */
  MATH_EXPRESSION_REGEX: /^[\d\+\-\*\/\(\)\.]+$/,

  /**
   * @type {RegExp}
   * @const
   */
  VALUE_NAME_REGEX: /[\w\.]+/g,

  /**
   * value in ms
   * @type {number}
   */
  timeRange: 3600,

  /**
   * value in ms
   * @type {number}
   */
  timeStep: 15,

  /**
   * common metrics container
   * @type {Array}
   */
  metrics: [],

  /**
   * @type {boolean}
   */
  isLoaded: false,

  /**
   * @type {App.Widget}
   * @default null
   */
  content: null,

  /**
   * load metrics
   */
  beforeRender: function () {
    var requestData = this.getRequestData(this.get('content.metrics')),
      request,
      requestCounter = 0,
      self = this;

    for (var i in requestData) {
      request = requestData[i];
      requestCounter++;
      if (request.host_component_criteria) {
        this.getHostComponentMetrics(request).complete(function () {
          requestCounter--;
          if (requestCounter === 0) self.set('isLoaded', true);
        });
      } else {
        this.getServiceComponentMetrics(request).complete(function () {
          requestCounter--;
          if (requestCounter === 0) self.set('isLoaded', true);
        });
      }
    }
  },

  didInsertElement: Em.K,

  transformToSeries: function (seriesData) {
    return seriesData;
  },

  drawWidget: function () {
    if (this.get('isLoaded')) {
      this._refreshGraph(this.calculateSeries())
    }
  }.observes('isLoaded'),

  /**
   * calculate series datasets for graph widgets
   */
  calculateSeries: function () {
    var metrics = this.get('metrics');
    var widgetType = this.get('content.widgetType');
    var seriesData = [];

    this.get('content.values').forEach(function (value) {
      var computedExpressions = this.computeExpression(this.extractExpressions(value)[0], metrics);
      seriesData.push(this.transformData(computedExpressions[value.value.match(this.get('EXPRESSION_REGEX'))[0]], value.name));
    }, this);

    return seriesData;
  },

  /**
   * compute expression
   *
   * @param {string} expression
   * @param {object} metrics
   * @returns {object}
   */
  computeExpression: function (expression, metrics) {
    var validExpression = true,
      value = [],
      dataLinks = {},
      dataLength = 0,
      beforeCompute,
      result = {};

    //replace values with metrics data
    expression.match(this.get('VALUE_NAME_REGEX')).forEach(function (match) {
      if (metrics.someProperty('name', match)) {
        dataLinks[match] = metrics.findProperty('name', match).data;
        dataLength = metrics.findProperty('name', match).data.length;
      } else {
        validExpression = false;
        console.warn('Metrics not found to compute expression');
      }
    });

    if (validExpression) {
      for (var i = 0, timestamp; i < dataLength; i++) {
        beforeCompute = expression.replace(this.get('VALUE_NAME_REGEX'), function (match) {
          timestamp = dataLinks[match][i][1];
          return dataLinks[match][i][0];
        });
        value.push([Number(window.eval(beforeCompute)), timestamp]);
      }
    }

    result['${' + expression + '}'] = value;
    return result;
  },

  /**
   * TODO should be used for simple type of widgets
   * @param expressions
   * @param metrics
   * @returns {object}
   *//*
   computeExpressions: function (expressions, metrics) {
   var result = {};

   expressions.forEach(function (_expression) {
   var validExpression = true;
   var value = "";

   //replace values with metrics data
   var beforeCompute = _expression.replace(this.get('VALUE_NAME_REGEX'), function (match) {
   if (metrics.someProperty('name', match)) {
   return metrics.findProperty('name', match).data;
   } else {
   validExpression = false;
   console.warn('Metrics not found to compute expression');
   }
   });

   if (validExpression) {
   //check for correct math expression
   validExpression = this.get('MATH_EXPRESSION_REGEX').test(beforeCompute);
   !validExpression && console.warn('Value is not correct mathematical expression');
   }

   result['${' + _expression + '}'] = (validExpression) ? Number(window.eval(beforeCompute)).toString() : value;
   }, this);
   return result;
   },*/

  /**
   * extract expressions
   * @param {object} value
   * @returns {Array}
   */
  extractExpressions: function (value) {
    var pattern = this.get('EXPRESSION_REGEX'),
      expressions = [],
      match;

    while (match = pattern.exec(value.value)) {
      expressions.push(match[1]);
    }
    return expressions;
  },

  /**
   * make GET call to server in order to fetch service-component metrics
   * @param {object} request
   * @returns {$.ajax}
   */
  getServiceComponentMetrics: function (request) {
    return App.ajax.send({
      name: 'widgets.serviceComponent.metrics.get',
      sender: this,
      data: {
        serviceName: request.service_name,
        componentName: request.component_name,
        widgetIds: this.addTimeProperties(request.widget_ids).join(',')
      },
      success: 'getServiceComponentMetricsSuccessCallback'
    });
  },

  getServiceComponentMetricsSuccessCallback: function (data, opt, params) {
    var metrics = [];
    var metricsData = data.metrics[params.serviceName.toLowerCase()];

    this.get('content.metrics').forEach(function (_metric) {
      if (Em.get(metricsData, _metric.name)) {
        _metric.data = Em.get(metricsData, _metric.name);
        this.get('metrics').pushObject(_metric);
      }
    }, this);
  },

  /**
   * make GET call to server in order to fetch host-component metrics
   * @param {object} request
   * @returns {$.ajax}
   */
  getHostComponentMetrics: function (request) {
    return App.ajax.send({
      name: 'widgets.hostComponent.metrics.get',
      sender: this,
      data: {
        serviceName: request.service_name,
        componentName: request.component_name,
        widgetIds: this.addTimeProperties(request.widget_ids).join(','),
        hostComponentCriteria: 'host_components/HostRoles/' + request.host_component_criteria
      },
      success: 'getHostComponentMetricsSuccessCallback'
    });
  },

  getHostComponentMetricsSuccessCallback: function () {
    //TODO push data to metrics after response structure approved
  },

  /**
   * get data formatted for request
   * @param {Array} metrics
   */
  getRequestData: function (metrics) {
    var requestsData = {};

    metrics.forEach(function (metric) {
      var key = metric.service_name + '_' + metric.component_name + '_' + metric.host_component_criteria;

      if (requestsData[key]) {
        requestsData[key]["widget_ids"].push(metric["widget_id"]);
      } else {
        metric["widget_ids"] = [metric["widget_id"]];
        delete metric["widget_id"];
        requestsData[key] = metric;
      }
    }, this);
    return requestsData;
  },

  /**
   * add time properties
   * @param {Array} widgetIds
   * @returns {Array} result
   */
  addTimeProperties: function (widgetIds) {
    var startDate = App.dateTime();
    var endDate = startDate + this.get('timeRange');
    var step = this.get('timeStep');
    var result = [];

    widgetIds.forEach(function (ambariId) {
      result.push(ambariId + '[' + startDate + ',' + endDate + ',' + step + ']');
    }, this);

    return result;
  }
});