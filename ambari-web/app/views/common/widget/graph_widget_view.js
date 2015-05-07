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

App.GraphWidgetView = Em.View.extend(App.WidgetMixin, {
  templateName: require('templates/common/widget/graph_widget'),

  /**
   * common metrics container
   * @type {Array}
   */
  metrics: [],

  /**
   * 3600 sec in 1 hour
   * @const
   */
  TIME_FACTOR: 3600,

  /**
   * custom time range, set when graph opened in popup
   * @type {number|null}
   */
  customTimeRange: null,

  /**
   * value in seconds
   * @type {number}
   */
  timeRange: function () {
    var timeRange = parseInt(this.get('content.properties.time_range'));
    if (isNaN(timeRange)) {
      //1h - default time range
      timeRange = 1;
    }
    return this.get('customTimeRange') || timeRange * this.get('TIME_FACTOR');
  }.property('content.properties.time_range', 'customTimeRange'),

  /**
   * value in ms
   * @type {number}
   */
  timeStep: 15,

  /**
   * @type {Array}
   */
  data: [],

  drawWidget: function () {
    if (this.get('isLoaded')) {
      this.set('data', this.calculateValues());
    }
  },

  /**
   * calculate series datasets for graph widgets
   */
  calculateValues: function () {
    var metrics = this.get('metrics');
    var seriesData = [];
    if (this.get('content.values')) {
      this.get('content.values').forEach(function (value) {
        var expression = this.extractExpressions(value)[0];
        var computedExpressions;

        if (expression) {
          computedExpressions = this.computeExpression(expression, metrics);
          seriesData.push({
            name: value.name,
            data: computedExpressions[value.value.match(this.get('EXPRESSION_REGEX'))[0]]
          });
        }
      }, this);
    }
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
      dataLength = -1,
      beforeCompute,
      result = {},
      isDataCorrupted = false;

    //replace values with metrics data
    expression.match(this.get('VALUE_NAME_REGEX')).forEach(function (match) {
      if (metrics.someProperty('name', match)) {
        dataLinks[match] = metrics.findProperty('name', match).data;
        if (!isDataCorrupted) {
          isDataCorrupted = (dataLength !== -1 && dataLength !== dataLinks[match].length);
        }
        dataLength = (dataLinks[match].length > dataLength) ? dataLinks[match].length : dataLength;
      } else {
        validExpression = false;
        console.error('Metrics with name "' + match + '" not found to compute expression');
      }
    });

    if (validExpression) {
      if (isDataCorrupted) {
        this.adjustData(dataLinks, dataLength);
      }
      for (var i = 0, timestamp; i < dataLength; i++) {
        beforeCompute = expression.replace(this.get('VALUE_NAME_REGEX'), function (match) {
          timestamp = dataLinks[match][i][1];
          return dataLinks[match][i][0];
        });
        var dataLinkPointValue = window.isNaN(Number(window.eval(beforeCompute))) ? 0 : Number(window.eval(beforeCompute));
        value.push([dataLinkPointValue, timestamp]);
      }
    }

    result['${' + expression + '}'] = value;
    return result;
  },

  /**
   *  add missing points, with zero value, to series
   *
   * @param {object} dataLinks
   * @param {number} length
   */
  adjustData: function(dataLinks, length) {
    //series with full data taken as original
    var original = [];
    var substituteValue = 0;

    for (var i in dataLinks) {
      if (dataLinks[i].length === length) {
        original = dataLinks[i];
        break;
      }
    }
    original.forEach(function(point, index) {
      for (var i in dataLinks) {
        if (!dataLinks[i][index] || dataLinks[i][index][1] !== point[1]) {
          dataLinks[i].splice(index, 0, [substituteValue, point[1]]);
        }
      }
    }, this);
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
        metricPaths: this.addTimeProperties(request.metric_paths).join(',')
      },
      success: 'getMetricsSuccessCallback'
    });
  },

  /**
   * make GET call to server in order to fetch host-component metrics
   * @param {object} request
   * @returns {$.ajax}
   */
  getHostComponentMetrics: function (request) {
    var dfd;
    var self = this;
    dfd = $.Deferred();
    this.getHostComponentName(request).done(function (data) {
      if (data) {
        request.host_name = data.host_components[0].HostRoles.host_name;
        App.ajax.send({
          name: 'widgets.hostComponent.metrics.get',
          sender: self,
          data: {
            componentName: request.component_name,
            hostName: request.host_name,
            metricPaths: self.addTimeProperties(request.metric_paths).join(',')
          }
        }).done(function(metricData) {
          self.getMetricsSuccessCallback(metricData);
          dfd.resolve();
        }).fail(function(data){
          dfd.reject();
        });
      }
    }).fail(function(data){
      dfd.reject();
    });
    return dfd.promise();
  },

  /**
   * add time properties
   * @param {Array} metricPaths
   * @returns {Array} result
   */
  addTimeProperties: function (metricPaths) {
    var toSeconds = Math.round(App.dateTime() / 1000);
    var fromSeconds = toSeconds - this.get('timeRange');
    var step = this.get('timeStep');
    var result = [];

    metricPaths.forEach(function (metricPath) {
      result.push(metricPath + '[' + fromSeconds + ',' + toSeconds + ',' + step + ']');
    }, this);

    return result;
  },

  /**
   * @type {Em.View}
   * @class
   */
  graphView: App.ChartLinearTimeView.extend({

    noTitleUnderGraph: true,
    inWidget: true,

    /**
     * set custom time range for graph widget
     */
    setTimeRange: function () {
      if (this.get('isPopup')) {
        this.set('parentView.customTimeRange', this.get('timeUnitSeconds'));
      } else {
        this.set('parentView.customTimeRange', null);
      }
    }.observes('isPopup', 'timeUnitSeconds'),

    /**
     * graph height
     * @type {number}
     */
    height: 95,

    /**
     * @type {string}
     */
    id: function () {
      return 'widget_'+ this.get('parentView.content.id') + '_graph';
    }.property('parentView.content.id'),

    /**
     * @type {string}
     */
    renderer: function () {
      return this.get('parentView.content.properties.graph_type') === 'STACK' ? 'area' : 'line';
    }.property('parentView.content.properties.graph_type'),

    title: function () {
      return this.get('parentView.content.widgetName');
    }.property('parentView.content.widgetName'),

    transformToSeries: function (seriesData) {
      var seriesArray = [];

      seriesData.forEach(function (_series) {
        seriesArray.push(this.transformData(_series.data, _series.name));
      }, this);
      return seriesArray;
    },

    loadData: function () {
      var self = this;
      Em.run.next(function () {
        self._refreshGraph(self.get('parentView.data'))
      });
    },

    didInsertElement: function () {
      this.loadData();
    }.observes('parentView.data')
  })
});