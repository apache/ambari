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

App.SubSection = DS.Model.extend({

  id: DS.attr('string'),

  /**
   * @type {string}
   */
  name: DS.attr('string'),

  /**
   * @type {string}
   */
  displayName: DS.attr('string'),

  /**
   * @type {boolean}
   */
  border: DS.attr('boolean', {defaultValue: false}),

  /**
   * @type {number}
   */
  rowIndex: DS.attr('number', {defaultValue: 1}),

  /**
   * @type {number}
   */
  columnIndex: DS.attr('number', {defaultValue: 1}),

  /**
   * @type {number}
   */
  rowSpan: DS.attr('number', {defaultValue: 1}),

  /**
   * @type {number}
   */
  columnSpan: DS.attr('number', {defaultValue: 1}),

  /**
   * @type {App.Section}
   */
  section: DS.belongsTo('App.Section'),

  /**
   * @type {App.StackConfigProperty[]}
   */
  configProperties: DS.hasMany('App.StackConfigProperty'),

  /**
   * @type {App.SubSectionTab[]}
   */
  subSectionTabs: DS.hasMany('App.SubSectionTab'),


  dependsOn: DS.attr('array', {defaultValue: []}),

  /**
   * @type {boolean}
   */
  leftVerticalSplitter: DS.attr('boolean', {defaultValue: true}),

  /**
   * @type {App.ServiceConfigProperty[]}
   */
  configs: [],

  /**
   * @type {boolean}
   */
  hasTabs: function() {
    return this.get('subSectionTabs.length');
  }.property('subSectionTabs.length'),

  showTabs: function() {
    return this.get('hasTabs')  && this.get('subSectionTabs').someProperty('isVisible');
  }.property('hasTabs','subSectionTabs.@each.isVisible'),

  /**
   * Number of the errors in all configs
   * @type {number}
   */
  errorsCount: function () {
    var visibleTabs = this.get('subSectionTabs').filterProperty('isVisible');
    var subSectionTabsErrors = visibleTabs.length ? visibleTabs.mapProperty('errorsCount').reduce(function(p, c) { return p + c; }) : 0;
    return subSectionTabsErrors + this.get('configs').filter(function(config) {
      return config.get('isVisible') && (!config.get('isValid') || (config.get('overrides') || []).someProperty('isValid', false));
    }).length;
  }.property('configs.@each.isValid', 'configs.@each.isVisible', 'configs.@each.overrideErrorTrigger', 'subSectionTabs.@each.isVisible', 'subSectionTabs.@each.errorsCount'),

  /**
   * @type {boolean}
   */
  addLeftVerticalSplitter: function() {
    return !this.get('isFirstColumn') && this.get('leftVerticalSplitter');
  }.property('isFirstColumn', 'leftVerticalSplitter'),

  /**
   * @type {boolean}
   */
  addRightVerticalSplitter: function() {
    return !this.get('isLastColumn');
  }.property('isLastColumn'),

  /**
   * @type {boolean}
   */
  showTopSplitter: function() {
    return !this.get('isFirstRow') && !this.get('border');
  }.property('isFirstRow', 'border'),

  /**
   * @type {boolean}
   */
  isFirstRow: function () {
    return this.get('rowIndex') == 0;
  }.property('rowIndex'),

  /**
   * @type {boolean}
   */
  isMiddleRow: function () {
    return this.get('rowIndex') != 0 && (this.get('rowIndex') + this.get('rowSpan') < this.get('section.sectionRows'));
  }.property('rowIndex', 'rowSpan', 'section.sectionRows'),

  /**
   * @type {boolean}
   */
  isLastRow: function () {
    return this.get('rowIndex') + this.get('rowSpan') == this.get('section.sectionRows');
  }.property('rowIndex', 'rowSpan', 'section.sectionRows'),

  /**
   * @type {boolean}
   */
  isFirstColumn: function () {
    return this.get('columnIndex') == 0;
  }.property('columnIndex'),

  /**
   * @type {boolean}
   */
  isMiddleColumn: function () {
    return this.get('columnIndex') != 0 && (this.get('columnIndex') + this.get('columnSpan') < this.get('section.sectionColumns'));
  }.property('columnIndex', 'columnSpan', 'section.sectionColumns'),

  /**
   * @type {boolean}
   */
  isLastColumn: function () {
    return this.get('columnIndex') + this.get('columnSpan') == this.get('section.sectionColumns');
  }.property('columnIndex', 'columnSpan', 'section.sectionColumns'),

  /**
   * If the visibility of subsection is dependent on a value of some config
   */
  isHiddenByConfig: false,

  /**
   * Determines if subsection is filtered by checking it own configs
   * If there is no configs, subsection can't be hidden
   * @type {boolean}
   */
  isHiddenByFilter: function () {
    var configs = this.get('configs').filter(function(c) {
      return !c.get('hiddenBySection') && c.get('isVisible');
    });
    return configs.length ? configs.everyProperty('isHiddenByFilter', true) : false;
  }.property('configs.@each.isHiddenByFilter'),

  /**
   * Determines if subsection is visible
   * @type {boolean}
   */
  isSectionVisible: function () {
    return !this.get('isHiddenByFilter') && !this.get('isHiddenByConfig') && this.get('configs').someProperty('isVisible', true);
  }.property('isHiddenByFilter', 'configs.@each.isVisible', 'isHiddenByConfig')
});


App.SubSection.FIXTURES = [];

