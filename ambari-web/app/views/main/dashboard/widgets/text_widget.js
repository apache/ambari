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
var date = require('utils/date/date');

App.TextDashboardWidgetView = App.DashboardWidgetView.extend({

  templateName: require('templates/main/dashboard/widgets/simple_text'),

  classNameBindings: ['isRed', 'isOrange', 'isGreen', 'isNA'],

  isRed: function () {
    return (this.get('data') <= this.get('thresh1'));
  }.property('data','thresh1'),

  isOrange: function () {
    return (this.get('data') <= this.get('thresh2') && this.get('data') > this.get('thresh1'));
  }.property('data','thresh1','thresh2'),

  isGreen: function () {
    return (this.get('data') > this.get('thresh2'));
  }.property('data','thresh2'),

  isNA: function () {
    return this.get('data') === null;
  }.property('data'),

  hiddenInfo: [],
  thresh1: null,
  thresh2: null,
  maxValue: null,
  updateColors: function(handlers, colors) {
    var colorstops = colors[0] + ", "; // start with the first color
    for (var i = 0; i < handlers.length; i++) {
      colorstops += colors[i] + " " + handlers[i] + "%,";
      colorstops += colors[i+1] + " " + handlers[i] + "%,";
    }
    colorstops += colors[colors.length - 1];
    var cssForChromeAndSafari = '-webkit-linear-gradient(left,' + colorstops + ')'; // chrome & safari
    var slider = $('#slider-range');
    slider.css('background-image', cssForChromeAndSafari);
    var cssForIE = '-ms-linear-gradient(left,' + colorstops + ')'; // IE 10+
    slider.css('background-image', cssForIE);
    //$('#slider-range').css('filter', 'progid:DXImageTransform.Microsoft.gradient( startColorStr= ' + colors[0] + ', endColorStr= ' + colors[2] +',  GradientType=1 )' ); // IE 10-
    var cssForFireFox = '-moz-linear-gradient(left,' + colorstops + ')'; // Firefox
    slider.css('background-image', cssForFireFox);

    slider.find('.ui-widget-header').css({'background-color': '#FF8E00', 'background-image': 'none'}); // change the  original ranger color
  }
});
