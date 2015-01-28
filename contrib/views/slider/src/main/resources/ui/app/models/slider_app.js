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

App.SliderApp = DS.Model.extend({

  /**
   * @type {string}
   */
  yarnId: DS.attr('string'),

  /**
   * @type {string}
   */
  name: DS.attr('string'),

  /**
   * @type {status}
   */
  status: DS.attr('string'),

  /**
   * Status before performed action
   * @type {string}
   */
  statusBeforeAction: DS.attr('string'),

  /**
   * @type {displayStatus}
   */
  displayStatus: DS.attr('string'),

  /**
   * @type {string}
   */
  user: DS.attr('string'),

  /**
   * @type {number}
   */
  started: DS.attr('number'),

  /**
   * @type {boolean}
   */
  isActionPerformed: DS.attr('boolean'),

  /**
   * @type {boolean}
   */
  isActionFinished: function() {
    return this.get('status') != this.get('statusBeforeAction');
  }.property('statusBeforeAction', 'status'),

  /**
   * @type {String}
   */

  startedToLocalTime: function () {
    var started = this.get('started');
    return started ? moment(started).format('ddd, DD MMM YYYY, HH:mm:ss Z [GMT]') : '-';
  }.property('started'),

  /**
   * @type {number}
   */
  ended: DS.attr('number'),

  /**
   * @type {String}
   */

  endedToLocalTime: function () {
    var ended = this.get('ended');
    return ended ? moment(ended).format('ddd, DD MMM YYYY, HH:mm:ss Z [GMT]') : '-';
  }.property('ended'),

  /**
   * @type {App.SliderAppType}
   */
  appType: DS.belongsTo('sliderAppType'),

  /**
   * @type {string}
   */

  description: DS.attr('string'),
  /**
   * @type {string}
   */
  diagnostics: DS.attr('string'),

  /**
   * @type {App.SliderAppComponent[]}
   */
  components: DS.hasMany('sliderAppComponent', {async: true}),

  /**
   * @type {App.QuickLink[]}
   */
  quickLinks: DS.hasMany('quickLink', {async: true}),

  /**
   * @type {App.SliderAppAlert[]}
   */
  alerts: DS.hasMany('sliderAppAlert', {async: true}),

  /**
   * @type {App.TypedProperty[]}
   */
  runtimeProperties: DS.hasMany('typedProperty', {async: true}),

  /**
   * @type {object}
   * Format:
   * {
   *   site-name1: {
   *      config1: value1,
   *      config2: value2
   *      ...
   *   },
   *   site-name2: {
   *      config3: value5,
   *      config4: value6
   *      ...
   *   },
   *   ...
   * }
   */
  configs: DS.attr('object'),

  jmx: DS.attr('object'),

  supportedMetricNames: DS.attr('string'),

  /**
   * Config categories, that should be hidden on app page
   * @type {string[]}
   */
  hiddenCategories: [],

  /**
   * @type {boolean}
   */
  doNotShowComponentsAndAlerts: function () {
    return this.get('status') == "FROZEN" || this.get('status') == "FAILED";
  }.property('status', 'components', 'alerts'),

  /**
   * Display metrics only for running apps
   * Also don't display if metrics don't exist
   * @type {boolean}
   */
  showMetrics: function () {
    if (!this.get('supportedMetricNames.length')) return false;
    if (App.get('metricsHost') != null) {
      return true;
    }
    return App.SliderApp.Status.running === this.get('status');
  }.property('status', 'configs', 'supportedMetricNames'),

  /**
   * Map object to array
   * @param {object} o
   * @returns {{key: string, value: *}[]}
   */
  mapObject: function (o) {
    if (Ember.typeOf(o) !== 'object') return [];
    return Ember.keys(o).map(function (key) {
      return {
        key: key,
        value: o[key],
        isMultiline: o[key].indexOf("\n") !== -1 || o[key].length > 100
      };
    });
  }

});

App.SliderApp.FIXTURES = [];

App.SliderApp.Status = {
  accepted: "ACCEPTED",
  failed: "FAILED",
  finished: "FINISHED",
  killed: "KILLED",
  new: "NEW",
  new_saving: "NEW_SAVING",
  running: "RUNNING",
  submitted: "SUBMITTED",
  frozen: "FROZEN",
  stopped: "STOPPED"
};
