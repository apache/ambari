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

App.Tab = DS.Model.extend({
  id: DS.attr('string'),
  name: DS.attr('string'),
  displayName: DS.attr('string'),
  columns: DS.attr('number', {defaultValue: 1}),
  rows: DS.attr('number', {defaultValue: 1}),
  isAdvanced: DS.attr('boolean', {defaultValue: false}),
  serviceName: DS.attr('string'),
  sections: DS.hasMany('App.Section'),
  isAdvancedHidden: DS.attr('boolean', {defaultValue: false}),
  isRendered: DS.attr('boolean', {defaultValue: false}),

  /**
   * Determines if all <code>configs</code> were attached to tab.
   */
  isConfigsPrepared: DS.attr('boolean', {defaultValue: false}),

  /**
   * Number of the errors in all sections in the current tab
   * @type {number}
   */
  errorsCount: function () {
    var errors = this.get('sections').mapProperty('errorsCount');
    return errors.length ? errors.reduce(Em.sum) : 0;
  }.property('sections.@each.errorsCount'),

  /**
   * Class name used for tab switching
   *
   * @type {String}
   * @property headingClass
   */
  headingClass: function() {
    return '.' + this.get('id');
  }.property('id'),

  /**
   * tooltip message.
   * for now used when tab is disabled
   * @type {String}
   */
  tooltipMsg: function() {
    return this.get('isHiddenByFilter') ? Em.I18n.t('services.service.config.nothing.to.display') : '';
  }.property('isHiddenByFilter'),

  /**
   * Determines if tab is filtered out (all it's sections should be hidden)
   * If it's an Advanced Tab it can't be hidden
   * @type {boolean}
   */
  isHiddenByFilter: function () {
    return this.get('isAdvanced') ? this.get('isAdvancedHidden') : this.get('sections').everyProperty('isHiddenByFilter', true);
  }.property('isAdvanced', 'sections.@each.isHiddenByFilter', 'isAdvancedHidden')

});


App.Tab.FIXTURES = [];
