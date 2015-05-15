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
   * @type {string}
   * @const
   */
  CLONE_SUFFIX: '(Copy)',

  /**
   * @type {number|null}
   */
  timeoutId: null,

  /**
   * common metrics container
   * @type {Array}
   */
  metrics: [],

  /**
   *
   */
  aggregatorFunc: ['._sum', '._avg', '._min', '._max'],

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
   * color of content calculated by thresholds
   * @type {string}
   */
  contentColor: function () {
    return this.get('value') ? 'green' : 'grey';
  }.property('value'),

  beforeRender: function () {
    this.get('metrics').clear();
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
      if (this.get('content.widgetType') === 'HEATMAP') {
        if (request.service_name === 'STACK') {
          this.getHostsMetrics(request).complete(function () {
            requestCounter--;
            if (requestCounter === 0) self.onMetricsLoaded();
          });
        } else {
          this.getHostComponentsMetrics(request).complete(function () {
            requestCounter--;
            if (requestCounter === 0) self.onMetricsLoaded();
          });
        }
      } else if (request.host_component_criteria) {
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
  }.observes('customTimeRange', 'content.properties.time_range'),

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
      case 'RESOURCEMANAGER':
        if (metric.host_component_criteria === 'host_components/HostRoles/ha_state=ACTIVE') {
          //if (metric.host_component_criteria)
          var yarn = App.YARNService.find().objectAt(0);
          if (!yarn.get('isRMHaEnabled')) {
            metric.host_component_criteria = 'host_components/HostRoles/component_name=RESOURCEMANAGER';
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
   * make GET call to server in order to fetch specifc host-component metrics
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
        }).done(function (metricData) {
          self.getMetricsSuccessCallback(metricData);
          dfd.resolve();
        }).fail(function (data) {
          dfd.reject();
        });
      }
    }).fail(function (data) {
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
    if (this.get('content.metrics')) {
      this.get('content.metrics').forEach(function (_metric) {
        var metric_path = _metric.metric_path;
        var isAggregatorFunc = false;
        var metric_data = Em.get(data, metric_path.replace(/\//g, '.'));
        if (Em.isNone(metric_data)) {
          this.aggregatorFunc.forEach(function (_item) {
            if (metric_path.endsWith(_item) && !isAggregatorFunc) {
              isAggregatorFunc = true;
              var metricBeanProperty = metric_path.split("/").pop();
              var metricBean;
              metric_path = metric_path.substring(0, metric_path.indexOf(metricBeanProperty));
              if (metric_path.endsWith("/")) {
                metric_path = metric_path.slice(0, -1);
              }
              metricBean = Em.get(data, metric_path.replace(/\//g, '.'));
              if (!Em.isNone(metricBean)) {
                metric_data = metricBean[metricBeanProperty];
              }
            }
          }, this);
        }
        if (!Em.isNone(metric_data)) {
          _metric.data = metric_data;
          this.get('metrics').pushObject(_metric);
        }
      }, this);
    }
  },

  /**
   * make GET call to get host component metrics accross
   * @param {object} request
   * @return {$.ajax}
   */
  getHostComponentsMetrics: function (request) {
    request.metric_paths.forEach(function (_metric, index) {
      request.metric_paths[index] = "host_components/" + _metric;
    });
    return App.ajax.send({
      name: 'widgets.serviceComponent.metrics.get',
      sender: this,
      data: {
        serviceName: request.service_name,
        componentName: request.component_name,
        metricPaths: request.metric_paths.join(',')
      },
      success: 'getHostComponentsMetricsSuccessCallback'
    });
  },


  getHostComponentsMetricsSuccessCallback: function (data) {
    var metrics = this.get('content.metrics');
    data.host_components.forEach(function (item) {
      metrics.forEach(function (_metric) {
        if (!Em.isNone(Em.get(item, _metric.metric_path.replace(/\//g, '.')))) {
          var metric = $.extend({}, _metric, true);
          metric.data = Em.get(item, _metric.metric_path.replace(/\//g, '.'));
          metric.hostName = item.HostRoles.host_name;
          this.get('metrics').pushObject(metric);
        }
      }, this);
    }, this);
  },

  getHostsMetrics: function (request) {
    return App.ajax.send({
      name: 'widgets.hosts.metrics.get',
      sender: this,
      data: {
        metricPaths: request.metric_paths.join(',')
      },
      success: 'getHostsMetricsSuccessCallback'
    });
  },

  getHostsMetricsSuccessCallback: function (data) {
    var metrics = this.get('content.metrics');
    data.items.forEach(function (item) {
      metrics.forEach(function (_metric, index) {
        if (!Em.isNone(Em.get(item, _metric.metric_path.replace(/\//g, '.')))) {
          var metric = $.extend({}, _metric, true);
          metric.data = Em.get(item, _metric.metric_path.replace(/\//g, '.'));
          metric.hostName = item.Hosts.host_name;
          this.get('metrics').pushObject(metric);
        }
      }, this);
    }, this);
  },

  /**
   * callback on metrics loaded
   */
  onMetricsLoaded: function () {
    var self = this;
    if (!this.get('isLoaded')) this.set('isLoaded', true);
    this.drawWidget();
    clearTimeout(this.get('timeoutId'));
    this.set('timeoutId', setTimeout(function () {
      self.loadMetrics();
    }, App.contentUpdateInterval));
  },


  /**
   * draw widget
   */
  drawWidget: function () {
    if (this.get('isLoaded')) {
      this.calculateValues();
      this.set('value', (this.get('content.values')[0]) ? this.get('content.values')[0].computedValue : '');
    }
  },

  /**
   * initialize tooltips
   */
  initTooltip: function () {
    var self = this;

    if (this.get('isLoaded')) {
      Em.run.next(function(){
        App.tooltip(self.$(".corner-icon > .icon-copy"), {title: Em.I18n.t('common.clone')});
        App.tooltip(self.$(".corner-icon > .icon-edit"), {title: Em.I18n.t('common.edit')});
      });
    }
  }.observes('isLoaded'),

  /**
   * calculate series datasets for graph widgets
   */
  calculateValues: function () {
    this.get('content.values').forEach(function (value) {
      var computeExpression = this.computeExpression(this.extractExpressions(value), this.get('metrics'));
      value.computedValue = value.value.replace(this.get('EXPRESSION_REGEX'), function (match) {
        var float = parseFloat(computeExpression[match]);
        if (isNaN(float)) {
          return computeExpression[match] || "";
        } else {
          return String((float % 1 !== 0) ? float.toFixed(2) : float);
        }
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
        if (window.isNaN(match)) {
          if (metrics.someProperty('name', match)) {
            return metrics.findProperty('name', match).data;
          } else {
            validExpression = false;
            console.error('Metrics with name "' + match + '" not found to compute expression');
          }
        } else {
          return match;
        }
      });

      //check for correct math expression
      if (!(validExpression && this.get('MATH_EXPRESSION_REGEX').test(beforeCompute))) {
        validExpression = false;
        console.error('Value for metric is not correct mathematical expression: ' + beforeCompute);
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
      function () {
        self.postWidgetDefinition(true);
      },
      Em.I18n.t('widget.clone.body').format(self.get('content.widgetName')),
      null,
      null,
      Em.I18n.t('common.clone')
    );
  },

  /**
   * collect all needed data to create new widget
   * @returns {{WidgetInfo: {widget_name: *, widget_type: *, description: *, scope: *, metrics: *, values: *, properties: *}}}
   */
  collectWidgetData: function () {
    return {
      WidgetInfo: {
        widget_name: this.get('content.widgetName'),
        widget_type: this.get('content.widgetType'),
        description: this.get('content.widgetDescription'),
        scope: this.get('content.scope'),
        "metrics": this.get('content.metrics').map(function (metric) {
          return {
            "name": metric.name,
            "service_name": metric.service_name,
            "component_name": metric.component_name,
            "host_component_criteria":  metric.host_component_criteria,
            "metric_path": metric.metric_path
          }
        }),
        values: this.get('content.values'),
        properties: this.get('content.properties')
      }
    };
  },

  /**
   * post widget definition to server
   * @param {boolean} isClone
   *  * @param {boolean} isEditClonedWidget
   * @returns {$.ajax}
   */
  postWidgetDefinition: function (isClone, isEditClonedWidget) {
    var data = this.collectWidgetData();
    if (isClone) {
      data.WidgetInfo.widget_name += this.get('CLONE_SUFFIX');
      data.WidgetInfo.scope = 'USER';
    }
    var successCallback =  isEditClonedWidget ? 'editNewClonedWidget' :  'postWidgetDefinitionSuccessCallback';
    return App.ajax.send({
      name: 'widgets.wizard.add',
      sender: this,
      data: {
        data: data
      },
      success: successCallback
    });
  },

  postWidgetDefinitionSuccessCallback: function (data) {
    var widgets = this.get('content.layout.widgets').toArray();
    widgets.pushObject(Em.Object.create({
      id: data.resources[0].WidgetInfo.id
    }));
    var mainServiceInfoSummaryController =  App.router.get('mainServiceInfoSummaryController');
    mainServiceInfoSummaryController.saveWidgetLayout(widgets).done(function(){
      mainServiceInfoSummaryController.updateActiveLayout();
    });
  },

  /*
   * enter edit wizard of the newly cloned widget
   */
  editNewClonedWidget: function (data) {
    var controller = this.get('controller');
    var widgets = this.get('content.layout.widgets').toArray();
    var id = data.resources[0].WidgetInfo.id;
    widgets.pushObject(Em.Object.create({
      id: id
    }));
    var mainServiceInfoSummaryController =  App.router.get('mainServiceInfoSummaryController');
    mainServiceInfoSummaryController.saveWidgetLayout(widgets).done(function() {
      mainServiceInfoSummaryController.getActiveWidgetLayout().done(function() {
        var newWidget = App.Widget.find().findProperty('id', id);
        controller.editWidget(newWidget);
      });
    });
  },

  /*
   * make call when clicking on "edit icon" on widget
   */
  editWidget: function (event) {
    var self = this;
    var isShared = this.get('content.scope') == 'CLUSTER';
    if (!isShared) {
      self.get('controller').editWidget(self.get('content'));
    } else {
      return App.ModalPopup.show({
        header: Em.I18n.t('common.warning'),
        bodyClass: Em.View.extend({
          template: Ember.Handlebars.compile('{{t widget.edit.body}}')
        }),
        primary: Em.I18n.t('widget.edit.button.primary'),
        secondary: Em.I18n.t('widget.edit.button.secondary'),
        third: Em.I18n.t('common.cancel'),
        onPrimary: function () {
          this.hide();
          self.get('controller').editWidget(self.get('content'));
        },
        onSecondary: function () {
          this.hide();
          self.postWidgetDefinition(true, true);
        },
        onThird: function () {
          this.hide();
        }
      });
    }
  }

});
App.WidgetPreviewMixin = Ember.Mixin.create({
  beforeRender: Em.K,
  isLoaded: true,
  metrics: [],
  content: Em.Object.create({
    id: 1,
    values: []
  }),
  loadMetrics: function () {
    this.get('content').setProperties({
      'values': this.get('controller.widgetValues'),
      'properties': this.get('controller.widgetProperties'),
      'widgetName': this.get('controller.widgetName'),
      'metrics': this.get('controller.widgetMetrics')
    });
    this._super();
  }.observes('controller.widgetProperties', 'controller.widgetValues', 'controller.widgetMetrics', 'controller.widgetName'),
  onMetricsLoaded: function () {
    this.drawWidget();
  }
});