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

App.PieChartDashboardWidgetView = App.DashboardWidgetView.extend({

  templateName: require('templates/main/dashboard/widgets/pie_chart'),

  maxValue: 100,

  modelFieldMax: null,
  modelFieldUsed: null,

  hiddenInfo: null,

  isPieExist: null,

  dataForPieChart: null,

  widgetHtmlId: null,

  getUsed: function() {
    return this.get('model').get(this.get('modelFieldUsed')) || 0;
  },

  getMax: function() {
    return this.get('model').get(this.get('modelFieldMax')) || 0;
  },

  calcHiddenInfo: function() {
    var used = this.getUsed();
    var max = this.getMax();
    var percent = max > 0 ? 100 * used / max : 0;
    var result = [];
    result.pushObject(percent.toFixed(1) + '% used');
    result.pushObject(numberUtils.bytesToSize(used, 1, 'parseFloat', 1024 * 1024) + ' of ' + numberUtils.bytesToSize(max, 1, 'parseFloat', 1024 * 1024));
    return result;
  },

  calcIsPieExists: function() {
    return this.get('model').get(this.get('modelFieldMax')) > 0;
  },

  calcDataForPieChart: function() {
    var used = this.getUsed();
    var total = this.getMax();
    var percent = total > 0 ? (used * 100 / total).toFixed() : 0;
    var percentPrecise = total > 0 ? (used * 100 / total).toFixed(1) : 0;
    return [percent, percentPrecise];
  },

  calc: function() {
    this.set('hiddenInfo', this.calcHiddenInfo());
    var isPieExists = this.calcIsPieExists();
    this.set('isPieExist', isPieExists);
    if (isPieExists) {
      this.set('dataForPieChart', this.calcDataForPieChart());
    }
  },

  didInsertElement: function() {
    this._super();
    this.addObserver('model.' + this.get('modelFieldMax'), this, this.calc);
    this.addObserver('model.' + this.get('modelFieldUsed'), this, this.calc);
  },

  content: App.ChartPieView.extend({
    model: null,  //data bind here
    id: Em.computed.alias('parentView.widgetHtmlId'), // html id
    stroke: '#D6DDDF', //light grey
    thresh1: null, //bind from parent
    thresh2: null,
    innerR: 25,

    existCenterText: true,
    centerTextColor: Em.computed.alias('contentColor'),

    palette: new Rickshaw.Color.Palette({
      scheme: [ '#FFFFFF', '#D6DDDF'].reverse()
    }),

    data: function() {
      var oriData = this.get('parentView.dataForPieChart');
      return [oriData[0], 100 - oriData[0]];
    }.property(),

    setData: function() {
      this.set('data', this.get('parentView.dataForPieChart'));
    }.observes('parentView.dataForPieChart'),

    contentColor: function () {
      var used = parseFloat(this.get('parentView.dataForPieChart')[1]);
      var thresh1 = parseFloat(this.get('thresh1'));
      var thresh2 = parseFloat(this.get('thresh2'));
      if (used <= thresh1) {
        this.set('palette', new Rickshaw.Color.Palette({
          scheme: ['#FFFFFF', App.healthStatusGreen].reverse()
        }));
        return App.healthStatusGreen;
      }
      if (used <= thresh2) {
        this.set('palette', new Rickshaw.Color.Palette({
          scheme: ['#FFFFFF', App.healthStatusOrange].reverse()
        }));
        return App.healthStatusOrange;
      }
      this.set('palette', new Rickshaw.Color.Palette({
        scheme: ['#FFFFFF', App.healthStatusRed].reverse()
      }));
      return App.healthStatusRed;
    }.property('data', 'thresh1', 'thresh2'),

    // refresh text and color when data in model changed
    refreshSvg: function () {
      // remove old svg
      var oldSvg = $("#" + this.get('id'));
      if(oldSvg){
        oldSvg.remove();
      }

      // draw new svg
      this.appendSvg();
    }.observes('data', 'thresh1', 'thresh2')
  })
});
