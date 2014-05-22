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
  index: DS.attr('string'),

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

  /**
   * Global configs
   * @type {{key: string, value: *}[]}
   */
  globals: function() {
    var c = this.get('configs.global');
    return this.mapObject(c);
  }.property('configs.global'),

  /**
   * HBase-Site configs
   * @type {{key: string, value: *}[]}
   */
  hbaseSite: function() {
    var c = this.get('configs.hbase-site');
    return this.mapObject(c);
  }.property('configs.hbase-site'),

  /**
   * Configs which are not in global or hbase-site
   * @type {{key: string, value: *}[]}
   */
  otherConfigs: function() {
    var c = this.get('configs'),
      ret = [],
      self = this;
    if (Ember.typeOf(c) !== 'object') return [];
    Ember.keys(c).forEach(function(key) {
      if (['hbase-site', 'global'].contains(key)) return;
      ret = ret.concat(self.mapObject(c[key]));
    });
    return ret;
  }.property('configs'),

  /**
   * Map object to array
   * @param {object} o
   * @returns {{key: string, value: *}[]}
   */
  mapObject: function(o) {
    if (Ember.typeOf(o) !== 'object') return [];
    return Ember.keys(o).map(function(key) {
      return {key: key, value: o[key]};
    });
  }

});

App.SliderApp.FIXTURES = [
  {
    id: 1,
    index: 'indx1',
    yarnId: 'y1',
    name: 'name1',
    status: 'FROZEN',
    user: 'u1',
    started: 1400132912,
    ended: 1400152912,
    appType: 1,
    diagnostics: 'd1',
    components: [3, 4, 5],
    quickLinks: [1, 2, 6],
    runtimeProperties: [1, 2, 3],
    configs: {
      global: {
        config1: 'value1',
        config2: 'value2',
        config3: 'value3',
        config4: 'value4'
      },
      'hbase-site': {
        config1: 'value1',
        config2: 'value2',
        config3: 'value3',
        config4: 'value4',
        config5: 'value5'
      },
      another: {
        config6: 'value6',
        config7: 'value7',
        config8: 'value8',
        config9: 'value9'
      },
      another2: {
        config10: 'value10',
        config11: 'value11',
        config12: 'value12',
        config13: 'value13'
      }
    }
  },
  {
    id: 2,
    index: 'indx2',
    yarnId: 'y2',
    name: 'name2',
    status: 'RUNNING',
    user: 'u2',
    started: 1400132912,
    ended: 1400152912,
    appType: 2,
    diagnostics: 'd2',
    components: [1, 3],
    quickLinks: [4, 5, 6],
    runtimeProperties: [3, 4]
  },
  {
    id: 3,
    index: 'indx3',
    yarnId: 'y3',
    name: 'name3',
    status: 'Running',
    user: 'u3',
    started: 1400132912,
    ended: 1400152912,
    appType: 3,
    diagnostics: 'd3',
    components: [1],
    quickLinks: [1, 2, 4, 5, 6],
    runtimeProperties: [2, 3]
  },
  {
    id: 4,
    index: 'indx4',
    yarnId: 'y4',
    name: 'name4',
    status: 'Running',
    user: 'u4',
    started: 1400132912,
    ended: 1400152912,
    appType: 4,
    diagnostics: 'd4',
    components: [1, 2, 3, 4, 5],
    quickLinks: [4, 6],
    runtimeProperties: [1, 2, 3, 4, 5]
  },
  {
    id: 5,
    index: 'indx5',
    yarnId: 'y5',
    name: 'name5',
    status: 'Running',
    user: 'u5',
    started: 1400132912,
    ended: 1400152912,
    appType: 5,
    diagnostics: 'd5',
    components: [2, 5],
    quickLinks: [3, 4, 6],
    runtimeProperties: [1, 2, 3]
  }
];

App.SliderApp.Status = {
  running: "Running",
  frozen: "Frozen",
  destroyed: "Destroyed",
  initialized: "Initialized"
};