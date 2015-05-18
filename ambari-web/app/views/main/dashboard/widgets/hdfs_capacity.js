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

App.NameNodeCapacityPieChartView = App.PieChartDashboardWidgetView.extend({

  title: Em.I18n.t('dashboard.widgets.HDFSDiskUsage'),
  id: '2',

  model_type: 'hdfs',
  modelFieldMax: 'capacityTotal',
  /**
   * HDFS model has 'remaining' value, but not 'used'
   */
  modelFieldUsed: 'capacityRemaining',
  widgetHtmlId: 'widget-nn-capacity',
  hiddenInfoClass: "hidden-info-six-line",

  didInsertElement: function() {
    this._super();
    this.calc();
  },

  calcHiddenInfo: function () {
    var total = this.get('model').get(this.get('modelFieldMax')) + 0;
    var remaining = this.get('model.capacityRemaining');
    var dfsUsed = this.get('model.capacityUsed');
    var nonDfsUsed = this.get('model.capacityNonDfsUsed');
    var dfsPercent = total > 0 ? ((dfsUsed * 100) / total).toFixed(2) : 0;
    var nonDfsPercent = total > 0 ? ((nonDfsUsed * 100) / total).toFixed(2) : 0;
    var remainingPercent = total > 0 ? ((remaining * 100) / total).toFixed(2) : 0;
    if (dfsPercent == "NaN" || dfsPercent < 0) {
      dfsPercent = Em.I18n.t('services.service.summary.notAvailable') + " ";
    }
    if (nonDfsPercent == "NaN" || nonDfsPercent < 0) {
      nonDfsPercent = Em.I18n.t('services.service.summary.notAvailable') + " ";
    }
    if (remainingPercent == "NaN" || remainingPercent < 0) {
      remainingPercent = Em.I18n.t('services.service.summary.notAvailable') + " ";
    }
    var result = [];
    result.pushObject(Em.I18n.t('dashboard.widgets.HDFSDiskUsage.DFSused'));
    result.pushObject(Em.I18n.t('dashboard.widgets.HDFSDiskUsage.info').format(numberUtils.bytesToSize(dfsUsed, 1, 'parseFloat'), dfsPercent));
    result.pushObject(Em.I18n.t('dashboard.widgets.HDFSDiskUsage.nonDFSused'));
    result.pushObject(Em.I18n.t('dashboard.widgets.HDFSDiskUsage.info').format(numberUtils.bytesToSize(nonDfsUsed, 1, 'parseFloat'), nonDfsPercent));
    result.pushObject(Em.I18n.t('dashboard.widgets.HDFSDiskUsage.remaining'));
    result.pushObject(Em.I18n.t('dashboard.widgets.HDFSDiskUsage.info').format(numberUtils.bytesToSize(remaining, 1, 'parseFloat'), remainingPercent));
    return result;
  },

  calcDataForPieChart: function() {
    var total = this.get('model').get(this.get('modelFieldMax')) * 1024 * 1024;
    var used = total - this.get('model').get(this.get('modelFieldUsed')) * 1024 * 1024;
    var percent = total > 0 ? ((used)*100 / total).toFixed() : 0;
    var percent_precise = total > 0 ? ((used)*100 / total).toFixed(1) : 0;
    return [percent, percent_precise];
  }

});
