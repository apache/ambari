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
   * @type {RegExp}
   * @const
   */
  EXPRESSION_REGEX: /\$\{([\w\s\.\,\+\-\*\/\(\)\:\=\[\]]*)\}/g,

  /**
   * list of operators that can be used in expression
   * @type {Array}
   * @constant
   */
  OPERATORS: ["+", "-", "*", "/", "(", ")"],

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

  /**
   * @type {boolean}
   */
  isEditWidget: function () {
    return this.get('content.controllerName') === 'widgetEditController';
  }.property('content.controllerName'),

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
        return !this.isExpressionComplete(this.get('expressions')[0]);
      case "GRAPH":
        return !this.isGraphDataComplete(this.get('dataSets'));
      case "TEMPLATE":
        return !this.isTemplateDataComplete(this.get('expressions'), this.get('templateValue'));
    }
    return false;
  }.property(
    'widgetPropertiesViews.@each.isValid',
    'dataSets.@each.label',
    'templateValue'
  ),

  /**
   * check whether data of expression is complete
   * @param {Em.Object} expression
   * @returns {boolean}
   */
  isExpressionComplete: function (expression) {
    return Boolean(expression && !expression.get('isInvalid') && !expression.get('isEmpty'));
  },

  /**
   * check whether data of graph widget is complete
   * @param dataSets
   * @returns {boolean} isComplete
   */
  isGraphDataComplete: function (dataSets) {
    var isComplete = Boolean(dataSets.length);

    for (var i = 0; i < dataSets.length; i++) {
      if (dataSets[i].get('label').trim() === '' || !this.isExpressionComplete(dataSets[i].get('expression'))) {
        isComplete = false;
        break;
      }
    }
    return isComplete;
  },

  /**
   * check whether data of template widget is complete
   * @param {Array} expressions
   * @param {string} templateValue
   * @returns {boolean} isComplete
   */
  isTemplateDataComplete: function (expressions, templateValue) {
    var isComplete = Boolean(expressions.length > 0 && templateValue.trim() !== '');

    if (isComplete) {
      for (var i = 0; i < expressions.length; i++) {
        if (!this.isExpressionComplete(expressions[i])) {
          isComplete = false;
          break;
        }
      }
    }
    return isComplete;
  },

  /**
   * Add data set
   * @param {object|null} event
   * @param {boolean} isDefault
   * @returns {number} id
   */
  addDataSet: function(event, isDefault) {
    var id = (isDefault) ? 1 :(Math.max.apply(this, this.get('dataSets').mapProperty('id')) + 1);

    this.get('dataSets').pushObject(Em.Object.create({
      id: id,
      label: '',
      isRemovable: !isDefault,
      expression: Em.Object.create({
        data: [],
        isInvalid: false,
        isEmpty: function () {
          return (this.get('data.length') === 0);
        }.property('data.length')
      })
    }));
    return id;
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
   * @returns {number} id
   */
  addExpression: function(event, isDefault) {
    var id = (isDefault) ? 1 :(Math.max.apply(this, this.get('expressions').mapProperty('id')) + 1);

    this.get('expressions').pushObject(Em.Object.create({
      id: id,
      isRemovable: !isDefault,
      data: [],
      alias: '{{' + this.get('EXPRESSION_PREFIX') + id + '}}',
      isInvalid: false,
      isEmpty: function () {
        return (this.get('data.length') === 0);
      }.property('data.length')
    }));
    return id;
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
      item.expression = Em.Object.create(item.expression);
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
   * Note: in order to draw widget it should be converted to API format of widget
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
              name: "",
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
          metrics.push(element);
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
    var result = {};

    this.get('widgetPropertiesViews').forEach(function(property){
      for (var key in property.valueMap) {
        result[property.valueMap[key]] = property.get(key);
      }
    });
    this.set('widgetProperties', result);
  }.observes('widgetPropertiesViews.@each.value', 'widgetPropertiesViews.@each.bigValue', 'widgetPropertiesViews.@each.smallValue'),

  /*
   * Generate the thresholds, unit, time range.etc object based on the widget type selected in previous step.
   */
  renderProperties: function () {
    var widgetProperties = App.WidgetType.find(this.get('content.widgetType')).get('properties');
    var propertiesData = this.get('widgetProperties');
    var result = [];

    widgetProperties.forEach(function (property) {
      var definition = App.WidgetPropertyTypes.findProperty('name', property.name);
      property = $.extend({}, property);

      //restore previous values
      for (var key in definition.valueMap) {
        if (propertiesData[definition.valueMap[key]]) {
          property[key] = propertiesData[definition.valueMap[key]];
        }
      }
      result.pushObject(App.WidgetProperty.create($.extend(definition, property)));
    });

    this.set('widgetPropertiesViews', result);
  },

  /**
   * convert data with model format to editable format
   * Note: in order to edit widget expression it should be converted to editable format
   * @param {App.Widget} content
   * @param {Ember.Controller} widgetController
   */
  convertData: function(content, widgetController) {
    var self = this;
    var expressionId = 0;

    this.get('expressions').clear();
    this.get('dataSets').clear();

    switch (content.get('widgetType')) {
      case 'NUMBER':
      case 'GAUGE':
        var id = this.addExpression(null, true);
        this.get('expressions').findProperty('id', id).set('data', this.parseValue(content.get('values')[0].value, content.get('metrics'))[0]);
        break;
      case 'TEMPLATE':
        this.parseValue(content.get('values')[0].value, content.get('metrics')).forEach(function(item, index) {
          var id = this.addExpression(null, (index === 0));
          this.get('expressions').findProperty('id', id).set('data', item);
        }, this);
        this.set('templateValue', content.get('values')[0].value.replace(this.get('EXPRESSION_REGEX'), function(){
          return '{{' + self.get('EXPRESSION_PREFIX') + ++expressionId + '}}';
        }));
        break;
      case 'GRAPH':
        content.get('values').forEach(function (value, index) {
          var id = this.addDataSet(null, (index === 0));
          var dataSet = this.get('dataSets').findProperty('id', id);
          dataSet.set('label', value.name);
          dataSet.set('expression.data', this.parseValue(value.value, content.get('metrics'))[0]);
        }, this);
        break;
    }
    widgetController.save('templateValue', this.get('templateValue'));
    widgetController.save('expressions', this.get('expressions'));
    widgetController.save('dataSets', this.get('dataSets'));
  },

  /**
   * parse value
   * @param value
   * @param metrics
   * @returns {Array}
   */
  parseValue: function(value, metrics) {
    var pattern = this.get('EXPRESSION_REGEX'),
      expressions = [],
      match;

    while (match = pattern.exec(value)) {
      expressions.push(this.getExpressionData(match[1], metrics));
    }

    return expressions;
  },

  /**
   * format values into expression data objects
   * @param {string} expression
   * @param {Array} metrics
   * @returns {Array}
   */
  getExpressionData: function(expression, metrics) {
    var str = '';
    var data = [];
    var id = 0;
    var metric;

    for (var i = 0, l = expression.length; i < l; i++) {
      if (this.get('OPERATORS').contains(expression[i])) {
        if (str.trim().length > 0) {
          metric = metrics.findProperty('name', str.trim());
          data.pushObject(Em.Object.create({
            id: ++id,
            name: str.trim(),
            isMetric: true,
            componentName: metric.component_name,
            serviceName: metric.service_name,
            metricPath: metric.metric_path,
            hostComponentCriteria: metric.host_component_criteria
          }));
          str = '';
        }
        data.pushObject(Em.Object.create({
          id: ++id,
          name: expression[i],
          isOperator: true
        }));
      } else {
        str += expression[i];
      }
    }
    if (str.trim().length > 0) {
      metric = metrics.findProperty('name', str.trim());
      data.pushObject(Em.Object.create({
        id: ++id,
        name: str.trim(),
        isMetric: true,
        componentName: metric.component_name,
        serviceName: metric.service_name,
        metricPath: metric.metric_path,
        hostComponentCriteria: metric.host_component_criteria
      }));
    }
    return data;
  },

  next: function () {
    if (!this.get('isSubmitDisabled')) {
      App.router.send('next');
    }
  }
});

