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

App.TemplateWidgetView = Em.View.extend(App.WidgetMixin, {
  templateName: require('templates/common/widget/template_widget'),

  /**
   * @type {string}
   */
  title: '',

  /**
   * @type {string}
   */
  value: '',

  /**
   * common metrics container
   * @type {Array}
   */
  metrics: [],

  drawWidget: function () {
    if (this.get('isLoaded')) {
      this.calculateValues();
      this.set('value', this.get('content.values')[0].computedValue);
      this.set('title', this.get('content.values')[0].name);
    }
  }.observes('isLoaded'),

  /**
   * calculate series datasets for graph widgets
   */
  calculateValues: function () {
    var metrics = this.get('metrics');
    var displayUnit = this.get('content.properties.display_unit');

    this.get('content.values').forEach(function (value) {
      var computeExpression = this.computeExpression(this.extractExpressions(value), metrics);
      value.computedValue = value.value.replace(this.get('EXPRESSION_REGEX'), function (match) {
        return (computeExpression[match]) ? computeExpression[match] + displayUnit : Em.I18n.t('common.na');
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
  }
});