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

require('views/common/export_metrics_menu_view');
var stringUtils = require('utils/string_utils');
var fileUtils = require('utils/file_utils');

App.ExportMetricsMixin = Em.Mixin.create({

  /**
   * Used as argument passed from template to indicate that resulting format is CSV instead of JSON
   */
  exportToCSVArgument: true,

  isExportMenuHidden: true,

  isExportButtonHidden: false,

  exportMetricsMenuView: App.ExportMetricsMenuView.extend(),

  hideMenuForNoData: function () {
    if (this.get('isExportButtonHidden')) {
      this.set('isExportMenuHidden', true);
    }
  }.observes('isExportButtonHidden'),

  toggleFormatsList: function () {
    this.toggleProperty('isExportMenuHidden');
  },

  exportGraphData: function () {
    this.set('isExportMenuHidden', true);
  },

  exportGraphDataSuccessCallback: function (response, request, params) {
    var hasData = response && response.metrics && Em.keys(response.metrics).length;
    if (!hasData) {
      App.showAlertPopup(Em.I18n.t('graphs.noData.title'), Em.I18n.t('graphs.noData.tooltip.title'));
    } else {
      var fileType = params.isCSV ? 'csv' : 'json',
        fileName = 'data.' + fileType,
        data = params.isCSV ? this.prepareCSV(response) : this.prepareJSON(response);
      fileUtils.downloadTextFile(data, fileType, fileName);
    }
  },

  exportGraphDataErrorCallback: function (jqXHR, ajaxOptions, error, opt) {
    App.ajax.defaultErrorHandler(jqXHR, opt.url, opt.method, jqXHR.status);
  },

  /**
   * Take metrics from any depth level in JSON response
   * @method setMetricsArrays
   * @param data
   * @param metrics
   * @param titles
   */
  setMetricsArrays: function (data, metrics, titles) {
    Em.keys(data).forEach(function (key) {
      if (Em.isArray(data[key])) {
        titles.push(key);
        metrics.push(data[key]);
      } else {
        this.setMetricsArrays(data[key], metrics, titles);
      }
    }, this);
  },

  prepareCSV: function (data) {
    var metrics = [],
      getMetricsItem = function (i, j, k) {
        var item;
        if (data.metrics) {
          item = metrics[j][i][k];
        } else if (Em.isArray(data)) {
          item = data[j].data[i][k];
        }
        return item;
      },
      titles,
      ticksNumber,
      metricsNumber,
      metricsArray;
    if (data.metrics) {
      titles = [Em.I18n.t('common.timestamp')];
      this.setMetricsArrays(data.metrics, metrics, titles);
      ticksNumber = metrics[0].length;
      metricsNumber = metrics.length
    } else if (Em.isArray(data)) {
      titles = data.mapProperty('name');
      titles.unshift(Em.I18n.t('common.timestamp'));
      ticksNumber = data[0].data.length;
      metricsNumber = data.length;
    }
    metricsArray = [titles];
    for (var i = 0; i < ticksNumber; i++) {
      metricsArray.push([getMetricsItem(i, 0, 1)]);
      for (var j = 0; j < metricsNumber; j++) {
        metricsArray[i + 1].push(getMetricsItem(i, j, 0));
      };
    }
    return stringUtils.arrayToCSV(metricsArray);
  },

  prepareJSON: function (data) {
    var fileData;
    if (data.metrics) {
      fileData = JSON.stringify(data.metrics, null, 4);
    } else if (Em.isArray(data)) {
      fileData = JSON.stringify(data, ['name', 'data'], 4);
    }
    return fileData;
  }

});
