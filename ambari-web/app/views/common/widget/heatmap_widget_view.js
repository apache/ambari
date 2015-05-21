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

App.HeatmapWidgetView = Em.View.extend(App.WidgetMixin, {
  templateName: require('templates/common/widget/heatmap_widget'),

  /**
   * common metrics container
   * @type {Array}
   */
  metrics: [],

  /**
   *  racks container  binded in the template
   *  @type {Array}
   */
  racks: [],

  /**
   * draw widget
   */
  drawWidget: function () {
    if (this.get('isLoaded')) {
      var hostToValueMap = this.calculateValues();
      var hostNames = [];
      if (this.get('racks').everyProperty('isLoaded', true)) {
        this.get('racks').forEach(function (rack) {
          hostNames = hostNames.concat(rack.hosts.mapProperty('hostName'));
        });
      }

      var metricObject = App.MainChartHeatmapMetric.create({
        name: this.get('content.displayName'),
        units: this.get('content.properties.display_unit'),
        maximumValue: this.get('content.properties.max_limit'),
        hostNames: hostNames,
        hostToValueMap: hostToValueMap
      });

      this.set('controller.selectedMetric', metricObject);

      this.set('controller.inputMaximum', metricObject.get('maximumValue'));
    }
  },

  /**
   * calculate value for heatmap widgets
   */
  calculateValues: function () {
    var metrics = this.get('metrics');
    var hostToValueMap = this.computeExpression(this.extractExpressions(this.get('content.values')[0]), metrics);
    return hostToValueMap;
  },


  /**
   * compute expression
   * @param expressions
   * @param metrics
   * @returns {object}
   */
  computeExpression: function (expressions, metrics) {
    var hostToValueMap = {};
    var hostNames = metrics.mapProperty('hostName');
    hostNames.forEach(function (_hostName) {
      expressions.forEach(function (_expression) {
        var validExpression = true;

        //replace values with metrics data
        var beforeCompute = _expression.replace(this.get('VALUE_NAME_REGEX'), function (match) {
          var _metric;
          if (window.isNaN(match)) {
            _metric = metrics.filterProperty('name', match).findProperty('hostName', _hostName);
            if (_metric) {
              return _metric.data;
            } else {
              validExpression = false;
              console.warn('Metrics with name "' + match + '" not found to compute expression');
            }
          } else {
            return match;
          }
        });

        if (validExpression && this.get('MATH_EXPRESSION_REGEX').test(beforeCompute)) {
          var value = Number(window.eval(beforeCompute)).toString();
          if (value == "NaN")  {
            value = 0
          }
          hostToValueMap[_hostName] = value;
        } else {
          console.error('Value for metric is not correct mathematical expression: ' + beforeCompute);
        }
      }, this);
    }, this);

    return hostToValueMap;
  }
});