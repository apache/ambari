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
var dateUtil = require('utils/date');


App.ServiceConfigVersion = DS.Model.extend({
  serviceName: DS.attr('string'),
  displayName: function() {
    return App.format.role(this.get('serviceName'));
  }.property('serviceName'),
  groupName: DS.attr('string'),
  groupId: DS.attr('string'),
  version: DS.attr('number'),
  createTime: DS.attr('number'),
  author: DS.attr('string'),
  notes: DS.attr('string'),
  service: DS.belongsTo('App.Service'),
  index: DS.attr('number'),
  isCurrent: DS.attr('boolean'),
  isDisplayed: DS.attr('boolean'),
  currentTooltip: function () {
    return Em.I18n.t('dashboard.configHistory.table.current.tooltip').format(this.get('displayName'), this.get('configGroupName'));
  }.property('displayName', 'configGroupName'),
  configGroupName: function () {
    return (this.get('groupName') === 'default') ? (this.get('displayName') + ' ' + Em.I18n.t('common.default')) : this.get('groupName');
  }.property('groupName'),
  briefNotes: function () {
    return (typeof this.get('notes') === 'string') ? this.get('notes').slice(0, 100) : "";
  }.property('notes'),
  versionText: function () {
    return Em.I18n.t('dashboard.configHistory.table.version.versionText').format(this.get('version'));
  }.property('version'),
  makeCurrentButtonText: function() {
    return Em.I18n.t('dashboard.configHistory.info-bar.revert.versionButton').format(this.get('versionText'));
  }.property('versionText'),
  modifiedDate: function () {
    return dateUtil.dateFormat(this.get('createTime'));
  }.property('createTime'),
  shortModifiedDate: function () {
    return dateUtil.dateFormat(this.get('createTime'), 'MMM DD, YYYY');
  }.property('createTime'),
  /**
   * determine whether ServiceConfigVersion is requested from server
   */
  isRequested: DS.attr('boolean'),
  isRestartRequired: function () {
    return this.get('service.isRestartRequired') && this.get('isCurrent');
  }.property('service.isRestartRequired', 'isCurrent'),
  disabledActionMessages: function () {
    return {
      view: (this.get('isDisplayed')) ? Em.I18n.t('dashboard.configHistory.info-bar.view.button.disabled') : '',
      compare: (this.get('isDisplayed')) ? Em.I18n.t('dashboard.configHistory.info-bar.compare.button.disabled') : '',
      revert: (this.get('isCurrent')) ? Em.I18n.t('dashboard.configHistory.info-bar.revert.button.disabled') : ''
    }
  }.property('isDisplayed', 'isCurrent'),
  disabledActionAttr: function () {
    return {
      view: (this.get('isDisplayed')) ? 'disabled' : false,
      compare: (this.get('isDisabled') || this.get('isDisplayed')) ? 'disabled' : false,
      revert: (this.get('isDisabled') || this.get('isCurrent')) ? 'disabled' : false
    }
  }.property('isDisplayed', 'isCurrent', 'isDisabled')
});

App.ServiceConfigVersion.FIXTURES = [];
