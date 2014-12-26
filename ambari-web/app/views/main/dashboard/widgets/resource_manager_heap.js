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

App.ResourceManagerHeapPieChartView = App.PieChartDashboardWidgetView.extend({

  title: Em.I18n.t('dashboard.widgets.ResourceManagerHeap'),
  id: '17',

  model_type: 'yarn',
  modelFieldMax: 'jvmMemoryHeapMax',
  modelFieldUsed: 'jvmMemoryHeapUsed',
  widgetHtmlId: 'widget-rm-heap',

  getUsed: function() {
    return (this.get('model').get(this.get('modelFieldUsed')) / (1024 * 1024)) || 0;
  },

  getMax: function() {
    return (this.get('model').get(this.get('modelFieldMax')) / (1024 * 1024)) || 0;
  },

  didInsertElement: function() {
    this._super();
    this.calc();
  }

});