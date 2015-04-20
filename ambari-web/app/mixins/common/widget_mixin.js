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
  EXPRESSION_REGEX: /\$\{([\w\s\.\,\+\-\*\/\(\)\:\=\[\]]*)\}/g,

  /**
   * @type {RegExp}
   * @const
   */
  MATH_EXPRESSION_REGEX: /^[\d\s\+\-\*\/\(\)\.]+$/,

  /**
   * @type {RegExp}
   * @const
   */
  VALUE_NAME_REGEX: /[\w\.\,\:\=\[\]]+/g,

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

  beforeRender: function () {
    this.loadMetrics();
  },


  /**
   * load metrics
   */
  loadMetrics: function () {
    var requestData = this.getRequestData(this.get('content.metrics')),
      request,
      requestCounter = 0,
      self = this;

    for (var i in requestData) {
      request = requestData[i];
      requestCounter++;
      if (request.host_component_criteria) {
        this.getHostComponentMetrics(request).always(function () {
          requestCounter--;
          if (requestCounter === 0) self.onMetricsLoaded();
        });
      } else {
        this.getServiceComponentMetrics(request).complete(function () {
          requestCounter--;
          if (requestCounter === 0) self.onMetricsLoaded();
        });
      }
    }
  },

  /**
   * get data formatted for request
   * @param {Array} metrics
   */
  getRequestData: function (metrics) {
    var requestsData = {};
    if (metrics) {
      metrics.forEach(function (metric, index) {
        var key;
        if (metric.host_component_criteria) {
          this.tweakHostComponentCriteria(metric);
          key = metric.service_name + '_' + metric.component_name + '_' + metric.host_component_criteria;
        } else {
          key = metric.service_name + '_' + metric.component_name;
        }
        var requestMetric = $.extend({}, metric);

        if (requestsData[key]) {
          requestsData[key]["metric_paths"].push(requestMetric["metric_path"]);
        } else {
          requestMetric["metric_paths"] = [requestMetric["metric_path"]];
          delete requestMetric["metric_path"];
          requestsData[key] = requestMetric;
        }
      }, this);
    }
    return requestsData;
  },

  /**
   * Tweak necessary host component criteria
   * NameNode HA host component criteria is applicable only in HA mode
   */
  tweakHostComponentCriteria: function (metric) {
    switch (metric.component_name) {
      case 'NAMENODE':
        if (metric.host_component_criteria === 'host_components/metrics/dfs/FSNamesystem/HAState=active') {
          //if (metric.host_component_criteria)
          var hdfs = App.HDFSService.find().objectAt(0);
          var activeNNHostName = !hdfs.get('snameNode') && hdfs.get('activeNameNode');
          if (!activeNNHostName) {
            metric.host_component_criteria = 'host_components/HostRoles/component_name=NAMENODE';
          }
        }
        break;
    }
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
        metricPaths: request.metric_paths.join(',')
      },
      success: 'getMetricsSuccessCallback'
    });
  },

  /**
   * make GET call to server in order to fetch service-component metrics
   * @param {object} request
   * @returns {$.Deferred}
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
            metricPaths: request.metric_paths.join(',')
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
   * make GET call to server in order to fetch host-component names
   * @param {object} request
   * @returns {$.ajax}
   */
  getHostComponentName: function (request) {
    return App.ajax.send({
      name: 'widgets.hostComponent.get.hostName',
      sender: this,
      data: {
        serviceName: request.service_name,
        componentName: request.component_name,
        metricPaths: request.metric_paths.join(','),
        hostComponentCriteria: request.host_component_criteria
      }
    });
  },

  /**
   * callback on getting aggregated metrics and host component metrics
   * @param data
   */
  getMetricsSuccessCallback: function (data) {
    var metrics = [];

    this.get('content.metrics').forEach(function (_metric) {
      if (!Em.isNone(Em.get(data, _metric.metric_path.replace(/\//g, '.')))) {
        _metric.data = Em.get(data, _metric.metric_path.replace(/\//g, '.'));
        this.get('metrics').pushObject(_metric);
      }
    }, this);
  },


  /**
   * callback on metrics loaded
   */
  onMetricsLoaded: function () {
    var self = this;
    this.set('isLoaded', true);
    this.drawWidget();
    setTimeout(function () {
      self.loadMetrics();
    }, App.contentUpdateInterval);
  },


  /**
   * draw widget
   */
  drawWidget: function () {
    if (this.get('isLoaded')) {
      this.calculateValues();
      this.set('value', this.get('content.values')[0] && this.get('content.values')[0].computedValue);
    }
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
        return (!Em.isNone(computeExpression[match])) ? computeExpression[match] + (displayUnit || "") : Em.I18n.t('common.na');
      });
    }, this);
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

  /*
   * make call when clicking on "remove icon" on widget
   */
  hideWidget: function (event) {
    this.get('controller').hideWidget(
      {
        context: Em.Object.create({
          id: event.context
        })
      }
    );
  },

  /*
   * make call when clicking on "clone icon" on widget
   */
  cloneWidget: function (event) {
    var self = this;
    return App.showConfirmationPopup(
      function() {
        self.postWidgetDefinition();
      },
      Em.I18n.t('widget.clone.body').format(self.get('content.displayName')),
      null,
      null,
      Em.I18n.t('common.clone')
    );
  },

  /**
   * collect all needed data to create new widget
   * @returns {{WidgetInfo: {widget_name: *, display_name: *, widget_type: *, description: *, scope: *, metrics: *, values: *, properties: *}}}
   */
  collectWidgetData: function () {
    return {
      WidgetInfo: {
        widget_name: this.get('content.widgetName'),
        display_name: this.get('content.displayName'),
        widget_type: this.get('content.widgetType'),
        description: this.get('content.widgetDescription'),
        scope: this.get('content.scope'),
        "metrics": this.get('content.metrics').map(function (metric) {
          return {
            "name": metric.name,
            "service_name": metric.serviceName,
            "component_name": metric.componentName,
            "metric_path": metric.metric_path,
            "category": metric.category
          }
        }),
        values: this.get('content.values'),
        properties: this.get('content.properties')
      }
    };
  },

  /**
   * post widget definition to server
   * @returns {$.ajax}
   */
  postWidgetDefinition: function () {
    return App.ajax.send({
      name: 'widgets.wizard.add',
      sender: this,
      data: {
        data: this.collectWidgetData()
      },
      success: 'postWidgetDefinitionSuccessCallback'
    });
  },

  postWidgetDefinitionSuccessCallback: function() {

  },

  /*
   * make call when clicking on "edit icon" on widget
   */
  editWidget: function (event) {

  }

});
App.WidgetPreviewMixin = Ember.Mixin.create({
  beforeRender: Em.K,
  isLoaded: true,
  metrics: [],
  content: Em.Object.create({
    widgetName: 'mock-widget',
    values: []
  }),
  drawWidget: function () {
    this.loadMetrics();
    this.get('content').setProperties({
      'values': this.get('controller.widgetValues'),
      'properties': this.get('controller.widgetProperties'),
      'displayName': this.get('controller.widgetName')
    });
    this._super();
  }.observes('controller.widgetProperties', 'controller.widgetValues', 'controller.widgetMetrics', 'controller.widgetName'),
  loadMetrics: function () {
    var metrics = [];
    this.get('controller.widgetMetrics').forEach(function (metric) {
      metrics.push({
        name: metric.name,
        data: this.get('MOCK_VALUE')
      });
    }, this);
    this.set('metrics', metrics);
  }
});