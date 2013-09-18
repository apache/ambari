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

App.JobTrackerCpuPieChartView = App.PieChartDashboardWidgetView.extend({

  title: Em.I18n.t('dashboard.widgets.JobTrackerCpu'),
  id: '7',

  model_type: 'mapreduce',

  widgetHtmlId: 'widget-jt-cpu',

  modelFieldUsed: 'jobTrackerCpu',

  didInsertElement: function() {
    this._super();
    this.calc();
  },

  calcHiddenInfo: function() {
    var value = this.get('model').get(this.get('modelFieldUsed'));
    var obj1;
    if( value == null) {
      obj1 = Em.I18n.t('services.service.summary.notAvailable');
    }
    else {
      value = value >= 100 ? 100: value;
      obj1 = (value + 0).toFixed(2) + '%';
    }
    var result = [];
    result.pushObject(obj1);
    result.pushObject('CPU wait I/O');
    return result;
  },

  calcIsPieExists: function() {
    return (this.get('model').get(this.get('modelFieldUsed')) != null);
  },

  calcDataForPieChart: function() {
    var value = this.get('model').get(this.get('modelFieldUsed'));
    value = value >= 100 ? 100: value;
    var percent = (value + 0).toFixed(1);
    var percent_precise = (value + 0).toFixed(2);
    return [ percent, percent_precise];
  }

});