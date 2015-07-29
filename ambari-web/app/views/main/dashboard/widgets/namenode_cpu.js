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

App.NameNodeCpuPieChartView = App.PieChartDashboardWidgetView.extend({

  title: Em.I18n.t('dashboard.widgets.NameNodeCpu'),
  id: '3',

  model_type: 'hdfs',
  widgetHtmlId: 'widget-nn-cpu',
  cpuWio: null,
  nnHostName: "",
  intervalId: null,

  willDestroyElement: function () {
    clearInterval(this.get("intervalId"));
  },

  didInsertElement: function () {
    this._super();
    var self = this,
      intervalId;
    App.router.get('mainController').isLoading.call(App.router.get('clusterController'), 'isServiceContentFullyLoaded').done(function () {
      if (App.get('isHaEnabled')) {
        self.set('nnHostName', self.get('model').get('activeNameNode.hostName'));
      } else {
        self.set('nnHostName', self.get('model').get('nameNode.hostName'));
      }
      if (self.get('nnHostName')) {
        self.getValue();
        intervalId = setInterval(function () {
          self.getValue()
        }, App.componentsUpdateInterval);
        self.set('intervalId', intervalId);
      }
    });
  },

  getValue: function () {
    App.ajax.send({
      name: 'namenode.cpu_wio',
      sender: this,
      data: {
        nnHost: this.get('nnHostName')
      },
      success: 'updateValueSuccess',
      error: 'updateValueError'
    });
  },

  updateValueError: function () {
    this.calc();
  },

  updateValueSuccess: function (response) {
    this.set('cpuWio', Em.get(response, 'metrics.cpu.cpu_wio'));
    this.calc();
  },

  calcHiddenInfo: function () {
    var value = this.get('cpuWio');
    var obj1;
    if (value == null) {
      obj1 = Em.I18n.t('services.service.summary.notAvailable');
    }
    else {
      value = value >= 100 ? 100 : value;
      obj1 = (value + 0).toFixed(2) + '%';
    }
    var result = [];
    result.pushObject(obj1);
    result.pushObject('CPU wait I/O');
    return result;
  },

  calcIsPieExists: function () {
    return (!Em.isNone(this.get('cpuWio')));
  },

  calcDataForPieChart: function () {
    var value = this.get('cpuWio');
    value = value >= 100 ? 100 : value;
    var percent = (value + 0).toFixed(1);
    var percent_precise = (value + 0).toFixed(2);
    return [percent, percent_precise];
  }
});