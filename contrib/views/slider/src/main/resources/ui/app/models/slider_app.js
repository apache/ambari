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
   * @type {string}
   */
  user: DS.attr('string'),

  /**
   * @type {number}
   */
  started: DS.attr('number'),

  /**
   * @type {number}
   */
  ended: DS.attr('number'),

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
  components: DS.hasMany('sliderAppComponent', {async:true}),

  /**
   * @type {App.QuickLink[]}
   */
  quickLinks: DS.hasMany('quickLink', {async:true}),

  /**
   * @type {App.SliderAppAlert[]}
   */
  alerts: DS.hasMany('sliderAppAlert', {async:true}),

  /**
   * @type {App.TypedProperty[]}
   */
  runtimeProperties: DS.hasMany('typedProperty', {async:true}),

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

  // Config categories, that should be hidden on app page
  hiddenCategories: ['yarn-site', 'global'],

  /**
   * Display metrics only for running apps
   * @type {boolean}
   */
  showMetrics: function() {
    var global = this.get('configs')['global'];
    //check whether slider has GANGLIA configured if not metrics should be hidden
    if (!(global['ganglia_server_host'] && global['ganglia_server_id'] && global['ganglia_server_port'])) {
      return false;
    }
    return App.SliderApp.Status.running === this.get('status');
  }.property('status', 'configs'),

  /**
   * Map object to array
   * @param {object} o
   * @returns {{key: string, value: *}[]}
   */
  mapObject: function(o) {
    if (Ember.typeOf(o) !== 'object') return [];
    return Ember.keys(o).map(function(key) {
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
    running: "RUNNING" ,
    submitted:"SUBMITTED"
};