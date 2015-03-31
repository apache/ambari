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

App.GaugeWidgetView = Em.View.extend(App.WidgetMixin, {
  templateName: require('templates/common/widget/gauge_widget'),

  /**
   * @type {string}
   */
  title: '',

  /**
   * @type {string}
   */
  value: '',

  /**
   * common metrics container
   * @type {Array}
   */
  metrics: [],

  drawWidget: function () {
    if (this.get('isLoaded')) {
      this.calculateValues();
      this.set('title', this.get('content.values')[0].name);
      this.set('value', this.get('content.values')[0].computedValue);
    }
  },

  chartView: App.ChartPieView.extend({
    stroke: '#D6DDDF', //light grey
    innerR: 25,

    FACTOR: 100,

    MAX_VALUE: 100,

    warningThreshold: function(){
      return this.get('parentView.content.properties.warning_threshold');
    }.property('parentView.content.properties.warning_threshold'),

    errorThreshold: function(){
      return this.get('parentView.content.properties.error_threshold');
    }.property('parentView.content.properties.error_threshold'),

    id: function() {
      return this.get('parentView.content.widgetName');
    }.property('parentView.content.widgetName'),

    existCenterText: true,
    centerTextColor: function () {
      return this.get('contentColor');
    }.property('contentColor'),

    palette: new Rickshaw.Color.Palette({
      scheme: [ '#FFFFFF', '#D6DDDF'].reverse()
    }),

    data: function () {
      var data = parseFloat(this.get('parentView.value')) * this.get('FACTOR');
      if (isNaN(data)) return [0, this.get('MAX_VALUE')];
      return [data, this.get('MAX_VALUE') - data];
    }.property('parentView.value'),

    contentColor: function () {
      var used = parseFloat(this.get('parentView.value')) * this.get('FACTOR');
      var thresh1 = parseFloat(this.get('warningThreshold')) * this.get('FACTOR');
      var thresh2 = parseFloat(this.get('errorThreshold')) * this.get('FACTOR');
      var color_green = App.healthStatusGreen;
      var color_red = App.healthStatusRed;
      var color_orange = App.healthStatusOrange;
      if (used <= thresh1) {
        this.set('palette', new Rickshaw.Color.Palette({
          scheme: [ '#FFFFFF', color_green  ].reverse()
        }));
        return color_green;
      } else if (used <= thresh2) {
        this.set('palette', new Rickshaw.Color.Palette({
          scheme: [ '#FFFFFF', color_orange  ].reverse()
        }));
        return color_orange;
      } else {
        this.set('palette', new Rickshaw.Color.Palette({
          scheme: [ '#FFFFFF', color_red  ].reverse()
        }));
        return color_red;
      }
    }.property('parentView.value', 'warningThreshold', 'errorThreshold'),

    // refresh text and color when data in model changed
    refreshSvg: function () {
      // remove old svg
      var old_svg =  $("#" + this.get('id'));
      if(old_svg){
        old_svg.remove();
      }

      // draw new svg
      this.appendSvg();
    }.observes('parentView.value', 'warningThreshold', 'errorThreshold')
  })
});