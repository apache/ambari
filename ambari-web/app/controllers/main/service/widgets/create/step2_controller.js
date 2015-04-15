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

App.WidgetWizardStep2Controller = Em.Controller.extend({
  name: "widgetWizardStep2Controller",

  /**
   * views of properties
   * @type {Array}
   */
  widgetPropertiesViews: [],

  /**
   * actual values of properties in API format
   * @type {object}
   */
  widgetProperties: {},

  /**
   * @type {Array}
   */
  widgetValues: [],

  /**
   * @type {Array}
   */
  widgetMetrics: [],

  propertiesMap: {
    "warning_threshold": {
      name: 'threshold',
      property: 'smallValue'
    },
    "error_threshold": {
      name: 'threshold',
      property: 'bigValue'
    },
    "display_unit": {
      name: 'display-unit',
      property: 'value'
    },
    "graph_type": {
      name: 'graph_type',
      property: 'value'
    },
    "time_range": {
      name: 'time_range',
      property: 'value'
    }
  },

  /**
   * metrics filtered by type
   * @type {Array}
   */
  filteredMetrics: function () {
    var type = this.get('content.widgetType');
    return this.get('content.allMetrics').filter(function (metric) {
      if (type === 'GRAPH') {
        return metric.temporal;
      } else {
        return metric.point_in_time;
      }
    }, this);
  }.property('content.allMetrics'),

  /**
   * update preview widget with latest expression data
   * @param {Em.View} view
   */
  updateExpressions: function (view) {
    var widgetType = this.get('content.widgetType');
    var expressionData = {
      values: [],
      metrics: []
    };
    if (view.get('expressions').length > 0 && view.get('dataSets').length > 0) {
      switch (widgetType) {
        case 'GAUGE':
        case 'NUMBER':
          expressionData = this.parseExpression(view.get('expressions')[0]);
          expressionData.values = [
            {
              value: expressionData.value
            }
          ];
          break;
        case 'TEMPLATE':
          expressionData = this.parseTemplateExpression(view);
          break;
        case 'GRAPH':
          expressionData = this.parseGraphDataset(view);
          break;
      }
    }
    this.set('widgetValues', expressionData.values);
    this.set('widgetMetrics', expressionData.metrics);
  },

  /**
   * parse Graph data set
   * @param {Ember.View} view
   * @returns {{metrics: Array, values: Array}}
   */
  parseGraphDataset: function (view) {
    var metrics = [];
    var values = [];

    view.get('dataSets').forEach(function (dataSet) {
      var result = this.parseExpression(dataSet.get('expression'));
      metrics.pushObjects(result.metrics);
      values.push({
        name: dataSet.get('label'),
        value: result.value
      });
    }, this);

    return {
      metrics: metrics,
      values: values
    };
  },

  /**
   * parse expression from template
   * @param {Ember.View} view
   * @returns {{metrics: Array, values: {value: *}[]}}
   */
  parseTemplateExpression: function (view) {
    var metrics = [];
    var self = this;
    var expression = view.get('templateValue').replace(/\{\{Expression[\d]\}\}/g, function (exp) {
      var result;
      if (view.get('expressions').someProperty('alias', exp)) {
        result = self.parseExpression(view.get('expressions').findProperty('alias', exp));
        metrics.pushObjects(result.metrics);
        return result.value;
      }
      return exp;
    });
    return {
      metrics: metrics,
      values: [
        {
          value: expression
        }
      ]
    };
  },

  /**
   *
   * @param {object} expression
   * @returns {{metrics: Array, value: string}}
   */
  parseExpression: function (expression) {
    var value = '';
    var metrics = [];

    if (expression.data.length > 0) {
      value = '${';
      expression.data.forEach(function (element) {
        if (element.isMetric) {
          metrics.push({
            name: element.name,
            componentName: element.componentName,
            serviceName: element.serviceName
          });
        }
        value += element.name;
      }, this);
      value += '}';
    }

    return {
      metrics: metrics,
      value: value
    };
  },

  /**
   * update properties of preview widget
   */
  updateProperties: function () {
    var propertiesMap = this.get('propertiesMap');
    var result = {};
    var widgetProperty;

    for (var i in propertiesMap) {
      widgetProperty = this.get('widgetPropertiesViews').findProperty('name', propertiesMap[i].name);
      if (widgetProperty && widgetProperty.get('isValid')) {
        result[i] = widgetProperty.get(propertiesMap[i].property);
      }
    }
    this.set('widgetProperties', result);
  }.observes('widgetPropertiesViews.@each.value', 'widgetPropertiesViews.@each.bigValue', 'widgetPropertiesViews.@each.smallValue'),

  /*
   * Generate the thresholds, unit, time range.etc object based on the widget type selected in previous step.
   */
  renderProperties: function () {
    var widgetType = this.get('content.widgetType');
    var widgetProperties = App.WidgetType.find().findProperty('name', widgetType).get('properties');
    var properties = [];

    switch (widgetType) {
      case 'GAUGE':
        properties = this.renderGaugeProperties(widgetProperties);
        break;
      case 'NUMBER':
        properties = this.renderNumberProperties(widgetProperties);
        break;
      case 'GRAPH':
        properties = this.renderGraphProperties(widgetProperties);
        break;
      case 'TEMPLATE':
        properties = this.renderTemplateProperties(widgetProperties);
        break;
      default:
        console.error('Incorrect Widget Type: ', widgetType);
    }
    this.set('widgetPropertiesViews', properties);
  },

  /**
   * Render properties for gauge-type widget
   * @method renderGaugeProperties
   * @returns {App.WidgetProperties[]}
   */
  renderGaugeProperties: function () {
    return [
      App.WidgetProperties.Thresholds.PercentageThreshold.create({
        smallValue: '0.7',
        bigValue: '0.9',
        isRequired: true
      })
    ];
  },

  /**
   * Render properties for number-type widget
   * @method renderNumberProperties
   * @returns {App.WidgetProperties[]}
   */
  renderNumberProperties: function () {
    return [
      App.WidgetProperties.Threshold.create({
        smallValue: '10',
        bigValue: '20',
        isRequired: false
      }),
      App.WidgetProperties.Unit.create({
        value: 'MB',
        isRequired: false
      })
    ];
  },

  /**
   * Render properties for template-type widget
   * @method renderTemplateProperties
   * @returns {App.WidgetProperties[]}
   */
  renderTemplateProperties: function (widgetProperties) {
    return [
      App.WidgetProperties.Unit.create({
        value: 'MB',
        isRequired: false
      })
    ];
  },

  /**
   * Render properties for graph-type widget
   * @method renderGraphProperties
   * @returns {App.WidgetProperties[]}
   */
  renderGraphProperties: function (widgetProperties) {
    return [
      App.WidgetProperties.GraphType.create({
        value: 'LINE',
        isRequired: true
      }),
      App.WidgetProperties.TimeRange.create({
        value: 'Last 1 hour',
        isRequired: true
      }),
      App.WidgetProperties.Unit.create({
        value: 'MB',
        isRequired: false
      })
    ];
  },

  next: function () {
    if (!this.get('isSubmitDisabled')) {
      App.router.send('next');
    }
  }
});

