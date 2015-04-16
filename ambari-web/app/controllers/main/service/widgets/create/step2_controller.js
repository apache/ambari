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

  EXPRESSION_PREFIX: 'Expression',

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

  /**
   * @type {Array}
   */
  expressions: [],

  /**
   * used only for GRAPH widget
   * @type {Array}
   */
  dataSets: [],

  /**
   * content of template of Template widget
   * @type {string}
   */
  templateValue: '',

  /**
   * views of properties
   * @type {Array}
   */
  widgetPropertiesViews: [],

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
   * @type {boolean}
   */
  isSubmitDisabled: function() {
    if (this.get('widgetPropertiesViews').someProperty('isValid', false)) {
      return true;
    }
    switch (this.get('content.widgetType')) {
      case "NUMBER":
      case "GAUGE":
        return this.get('expressions')[0] &&
          (this.get('expressions')[0].get('editMode') ||
          this.get('expressions')[0].get('data.length') === 0);
      case "GRAPH":
        return this.get('dataSets.length') > 0 &&
          (this.get('dataSets').someProperty('expression.editMode') ||
          this.get('dataSets').someProperty('expression.data.length', 0));
      case "TEMPLATE":
        return !this.get('templateValue') ||
          this.get('expressions.length') > 0 &&
          (this.get('expressions').someProperty('editMode') ||
          this.get('expressions').someProperty('data.length', 0));
    }
    return false;
  }.property('widgetPropertiesViews.@each.isValid',
    'expressions.@each.editMode',
    'dataSets.@each.expression'),

  /**
   * Add data set
   * @param {object|null} event
   * @param {boolean} isDefault
   */
  addDataSet: function(event, isDefault) {
    var id = (isDefault) ? 1 :(Math.max.apply(this, this.get('dataSets').mapProperty('id')) + 1);

    this.get('dataSets').pushObject(Em.Object.create({
      id: id,
      label: '',
      isRemovable: !isDefault,
      expression: {
        data: [],
        editMode: false
      }
    }));
  },

  /**
   * Remove data set
   * @param {object} event
   */
  removeDataSet: function(event) {
    this.get('dataSets').removeObject(event.context);
  },

  /**
   * Add expression
   * @param {object|null} event
   * @param {boolean} isDefault
   */
  addExpression: function(event, isDefault) {
    var id = (isDefault) ? 1 :(Math.max.apply(this, this.get('expressions').mapProperty('id')) + 1);

    this.get('expressions').pushObject(Em.Object.create({
      id: id,
      isRemovable: !isDefault,
      data: [],
      alias: '{{' + this.get('EXPRESSION_PREFIX') + id + '}}',
      editMode: false
    }));
  },

  /**
   * Remove expression
   * @param {object} event
   */
  removeExpression: function(event) {
    this.get('expressions').removeObject(event.context);
  },

  /**
   * initialize data
   * widget should have at least one expression or dataSet
   */
  initWidgetData: function() {
    this.set('widgetProperties', this.get('content.widgetProperties'));
    this.set('widgetValues', this.get('content.widgetValues'));
    this.set('widgetMetrics', this.get('content.widgetMetrics'));
    this.set('expressions', this.get('content.expressions').map(function (item) {
      return Em.Object.create(item);
    }, this));
    this.set('dataSets', this.get('content.dataSets').map(function (item) {
      return Em.Object.create(item);
    }, this));
    this.set('templateValue', this.get('content.templateValue'));
    if (this.get('expressions.length') === 0) {
      this.addExpression(null, true);
    }
    if (this.get('dataSets.length') === 0) {
      this.addDataSet(null, true);
    }
  },

  /**
   * update preview widget with latest expression data
   * @param {Em.View} view
   */
  updateExpressions: function () {
    var widgetType = this.get('content.widgetType');
    var expressionData = {
      values: [],
      metrics: []
    };
    if (this.get('expressions').length > 0 && this.get('dataSets').length > 0) {
      switch (widgetType) {
        case 'GAUGE':
        case 'NUMBER':
          expressionData = this.parseExpression(this.get('expressions')[0]);
          expressionData.values = [
            {
              value: expressionData.value
            }
          ];
          break;
        case 'TEMPLATE':
          expressionData = this.parseTemplateExpression(this);
          break;
        case 'GRAPH':
          expressionData = this.parseGraphDataset(this);
          break;
      }
    }
    this.set('widgetValues', expressionData.values);
    this.set('widgetMetrics', expressionData.metrics);
  }.observes('templateValue', 'dataSets.@each.label'),

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
        smallValue: this.get('widgetProperties.warning_threshold') || '0.7',
        bigValue: this.get('widgetProperties.error_threshold') || '0.9',
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
        smallValue: this.get('widgetProperties.warning_threshold') || '10',
        bigValue: this.get('widgetProperties.error_threshold') || '20',
        isRequired: false
      }),
      App.WidgetProperties.Unit.create({
        value: this.get('widgetProperties.display_unit') || 'MB',
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
        value: this.get('widgetProperties.display_unit') || 'MB',
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
        value: this.get('widgetProperties.graph_type') || 'LINE',
        isRequired: true
      }),
      App.WidgetProperties.TimeRange.create({
        value: this.get('widgetProperties.time_range') || 'Last 1 hour',
        isRequired: true
      }),
      App.WidgetProperties.Unit.create({
        value: this.get('widgetProperties.display_unit') || 'MB',
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

