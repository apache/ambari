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
var numberUtils = require('utils/number_utils');

App.HBaseMasterHeapPieChartView = App.PieChartDashboardWidgetView.extend({

  title: Em.I18n.t('dashboard.widgets.HBaseMasterHeap'),
  id: '20',

  model_type: 'hbase',

  modelFieldMax: 'heapMemoryMax',
  modelFieldUsed: 'heapMemoryUsed',
  hiddenInfo: function () {
    var heapUsed = this.get('model').get('heapMemoryUsed');
    var heapMax = this.get('model').get('heapMemoryMax');
    var percent = heapMax > 0 ? 100 * heapUsed / heapMax : 0;
    var result = [];
    result.pushObject(percent.toFixed(1) + '% used');
    result.pushObject(numberUtils.bytesToSize(heapUsed, 1, "parseFloat") + ' of ' + numberUtils.bytesToSize(heapMax, 1, "parseFloat"));
    return result;
  }.property('model.heapMemoryUsed', 'model.heapMemoryMax'),

  widgetHtmlId: 'widget-hbase-heap',

  didInsertElement: function() {
    this._super();
    this.calc();
  },

  calcDataForPieChart: function() {
    var used = this.get('model').get(this.get('modelFieldUsed'));
    var total = this.get('model').get(this.get('modelFieldMax'));
    var percent = total > 0 ? ((used)*100 / total).toFixed() : 0;
    return [ percent, 100 - percent];
  }
});