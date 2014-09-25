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
var date = require('utils/date');

App.UptimeTextDashboardWidgetView = App.TextDashboardWidgetView.extend({

  templateName: require('templates/main/dashboard/widgets/uptime'),

  timeUnit: null,

  hiddenInfoClass: "hidden-info-three-line",

  thresh1: 5,
  thresh2: 10,
  maxValue: 'infinity',

  component: null,
  /**
   * Model's field that used to calculate data and content
   * Should be defined in every child
   */
  modelField: null,

  data: null,
  content: null,

  isGreen: function () {
    return this.get('data') != null;
  }.property('data'),

  isOrange: function () {
    return false;
  }.property('data'),

  isRed: function () {
    return false;
  }.property('data'),

  timeConverter: function (timestamp) {
    var m = moment((new Date(timestamp)));
    return [m.format('ddd MMM DD YYYY'), m.format('HH:mm:ss')];
  },

  /**
   * All children should have such code
   * <code>
   * didInsertElement: function() {
   *   this._super();
   *   this.calc();
   * }
   * </code>
   */
  didInsertElement: function () {
    this._super();
    this.addObserver('model.' + this.get('modelField'), this, this.calc);
  },

  calc: function () {
    this.set('data', this.calcData());
    this.set('content', this.calcContent());
  },

  uptimeProcessing: function (uptime) {
    var uptimeString = this.timeConverter(uptime);
    var diff = App.dateTime() - uptime;
    if (diff < 0) {
      diff = 0;
    }
    var formatted = date.timingFormat(diff); //17.67 days
    var timeUnit = null;
    if (formatted != null) {
      switch (formatted.split(" ")[1]) {
        case 'secs':
          timeUnit = 's';
          break;
        case 'hours':
          timeUnit = 'hr';
          break;
        case 'days':
          timeUnit = 'd';
          break;
        case 'mins':
          timeUnit = 'min';
          break;
        default:
          timeUnit = formatted.split(" ")[1];
      }
      this.set('timeUnit', timeUnit);
      this.set('hiddenInfo', []);
      this.get('hiddenInfo').pushObject(formatted);
      this.get('hiddenInfo').pushObject(uptimeString[0]);
      this.get('hiddenInfo').pushObject(uptimeString[1]);
    }
    return formatted;
  },

  calcData: function () {
    var field = this.get('modelField');
    var uptime = this.get('model').get(field);
    if (uptime && uptime > 0) {
      var formatted = this.uptimeProcessing(uptime);
      if (formatted != null) {
        return parseFloat(formatted.split(" ")[0]);
      }
    }
    this.set('hiddenInfo', [this.get('component'), 'Not Running']);
    return null;
  },

  calcContent: function () {
    var data = this.get('data');
    if (data) {
      return data.toFixed(1) + ' ' + this.get('timeUnit');
    }
    else {
      return Em.I18n.t('services.service.summary.notAvailable');
    }
  }
});
