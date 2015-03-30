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

App.WidgetMixin = Ember.Mixin.create({

  /**
   * @type {RegExp}
   * @const
   */
  EXPRESSION_REGEX: /\$\{([\w\.\+\-\*\/\(\)\:\=\[\]]*)\}/g,

  /**
   * @type {RegExp}
   * @const
   */
  MATH_EXPRESSION_REGEX: /^[\d\+\-\*\/\(\)\.]+$/,

  /**
   * @type {RegExp}
   * @const
   */
  VALUE_NAME_REGEX: /[\w\.\:\=\[\]]+/g,

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

  /**
   * extract expressions
   * Example:
   *  input: "${a/b} equal ${b+a}"
   *  expressions: ['a/b', 'b+a']
   *
   * @param {object} input
   * @returns {Array}
   */
  extractExpressions: function (input) {
    var pattern = this.get('EXPRESSION_REGEX'),
      expressions = [],
      match;

    while (match = pattern.exec(input.value)) {
      expressions.push(match[1]);
    }
    return expressions;
  },

  /**
   * calculate series datasets for graph widgets
   */
  calculateValues: function () {
    var metrics = this.get('metrics');
    var displayUnit = this.get('content.properties.display_unit');

    this.get('content.values').forEach(function (value) {
      var computeExpression = this.computeExpression(this.extractExpressions(value), metrics);
      value.computedValue = value.value.replace(this.get('EXPRESSION_REGEX'), function (match) {
        return (computeExpression[match]) ? computeExpression[match] + (displayUnit || "") : Em.I18n.t('common.na');
      });
    }, this);
  },

  /**
   * compute expression
   * @param expressions
   * @param metrics
   * @returns {object}
   */
  computeExpression: function (expressions, metrics) {
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
  },

  /**
   * get data formatted for request
   * @param {Array} metrics
   */
  getRequestData: function (metrics) {
    var requestsData = {};

    metrics.forEach(function (metric) {
      var key = metric.service_name + '_' + metric.component_name + '_' + metric.host_component_criteria;
      var requestMetric = $.extend({}, metric);

      if (requestsData[key]) {
        requestsData[key]["widget_ids"].push(requestMetric["widget_id"]);
      } else {
        requestMetric["widget_ids"] = [requestMetric["widget_id"]];
        delete requestMetric["widget_id"];
        requestsData[key] = requestMetric;
      }
    }, this);
    return requestsData;
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
        widgetIds: request.widget_ids.join(',')
      },
      success: 'getServiceComponentMetricsSuccessCallback'
    });
  },

  getServiceComponentMetricsSuccessCallback: function (data, opt, params) {
    var metrics = [];

    this.get('content.metrics').forEach(function (_metric) {
      if (Em.get(data, _metric.widget_id.replace(/\//g, '.'))) {
        _metric.data = Em.get(data, _metric.widget_id.replace(/\//g, '.'));
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
        widgetIds: request.widget_ids.join(','),
        hostComponentCriteria: 'host_components/HostRoles/' + request.host_component_criteria
      },
      success: 'getHostComponentMetricsSuccessCallback'
    });
  },

  getHostComponentMetricsSuccessCallback: function () {
    //TODO push data to metrics after response structure approved
  }

});