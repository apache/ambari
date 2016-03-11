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
var dateUtil = require('utils/date/date');


App.ServiceConfigVersion = DS.Model.extend({
  MAX_AUTHOR_LENGTH: 20,
  MAX_NOTES_LENGTH: 80,
  serviceName: DS.attr('string'),
  displayName: Em.computed.formatRole('serviceName', true),
  groupName: DS.attr('string'),
  groupId: DS.attr('number'),
  version: DS.attr('number'),
  createTime: DS.attr('number'),
  rawCreateTime: DS.attr('number'),
  author: DS.attr('string'),
  notes: DS.attr('string'),
  service: DS.belongsTo('App.Service'),
  hosts: DS.attr('array'),
  index: DS.attr('number'),
  isCurrent: DS.attr('boolean'),
  isDisplayed: DS.attr('boolean'),
  stackVersion: DS.attr('string'),
  isCompatible: DS.attr('boolean'),
  canBeMadeCurrent: Em.computed.and('isCompatible', '!isCurrent'),
  isDefault: Em.computed.equal('groupName', 'default'),
  currentTooltip: Em.computed.i18nFormat('dashboard.configHistory.table.current.tooltip', 'displayName', 'configGroupName'),

  /**
   * @type {string}
   */
  configGroupName: function () {
    return this.get('isDefault') ? Em.I18n.t('common.default') : this.get('groupName');
  }.property('groupName','isDefault'),

  /**
   * @type {string}
   */
  authorFormatted: function () {
    var author = this.get('author');
    if (author) {
      return author.length > this.get('MAX_AUTHOR_LENGTH') ? author.slice(0, this.get('MAX_AUTHOR_LENGTH')) + '...' : author;
    }
  }.property('author'),

  /**
   * @type {string}
   */
  fullNotes: function () {
    return (typeof this.get('notes') === 'string') ?
           (this.get('notes') || Em.I18n.t('dashboard.configHistory.table.notes.no')) :
           Em.I18n.t('dashboard.configHistory.table.notes.no');
  }.property('notes'),

  /**
   * @type {string}
   */
  briefNotes: function () {
    return this.get('fullNotes').slice(0, (this.get('MAX_NOTES_LENGTH') + 1));
  }.property('fullNotes'),

  /**
   * @type {boolean}
   */
  moreNotesExists: function () {
    return (typeof this.get('notes') === 'string') && this.get('notes').length > this.get('MAX_NOTES_LENGTH');
  }.property('notes'),

  /**
   * @type {string}
   */
  versionText: Em.computed.i18nFormat('dashboard.configHistory.table.version.versionText', 'version'),

  /**
   * @type {string}
   */
  makeCurrentButtonText: Em.computed.i18nFormat('dashboard.configHistory.info-bar.revert.versionButton', 'versionText'),

  /**
   * @type {string}
   */
  createdDate: function () {
    return dateUtil.dateFormat(this.get('createTime'));
  }.property('createTime'),

  /**
   * @type {string}
   */
  timeSinceCreated: function () {
    return $.timeago(this.get('rawCreateTime'));
  }.property('rawCreateTime'),

  /**
   * determine whether ServiceConfigVersion is requested from server
   */
  isRequested: DS.attr('boolean'),

  /**
   * @type {boolean}
   */
  isRestartRequired: function () {
    if (this.get('service.isRestartRequired') && this.get('isCurrent')) {
      var hostNames = this.get('hosts');
      if (!hostNames.length) return false;
      for (var i = 0; i < hostNames.length; i++) {
        if (Object.keys(this.get('service.restartRequiredHostsAndComponents')).contains(hostNames[i])) {
          return true;
        }
      }
    }
    return false;
  }.property('service.isRestartRequired','isDefault', 'isCurrent', 'hosts', 'service.restartRequiredHostsAndComponents', 'router.mainServiceInfoConfigsController.configGroups'),

  /**
   * {{view: string, compare: string, revert: string}} disabledActionMessages
   */
  disabledActionMessages: function () {
    return {
      view: (this.get('isDisplayed')) ? Em.I18n.t('dashboard.configHistory.info-bar.view.button.disabled') : '',
      compare: (this.get('isDisplayed')) ? Em.I18n.t('dashboard.configHistory.info-bar.compare.button.disabled') : '',
      revert: (this.get('isCurrent')) ? Em.I18n.t('dashboard.configHistory.info-bar.revert.button.disabled') : ''
    }
  }.property('isDisplayed', 'isCurrent'),

  /**
   * {{view: (string|boolean), compare: (string|boolean), revert: (string|boolean)}} disabledActionAttr
   */
  disabledActionAttr: function () {
    return {
      view: (this.get('isDisplayed')) ? 'disabled' : false,
      compare: (this.get('isDisabled') || this.get('isDisplayed')) ? 'disabled' : false,
      revert: (this.get('isDisabled') || this.get('isCurrent')) ? 'disabled' : false
    }
  }.property('isDisplayed', 'isCurrent', 'isDisabled')
});

App.ServiceConfigVersion.FIXTURES = [];
