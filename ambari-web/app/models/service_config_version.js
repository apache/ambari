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
  version: DS.attr('number'),
  createTime: DS.attr('number'),
  appliedTime: DS.attr('number'),
  author: DS.attr('string'),
  notes: DS.attr('string'),
  service: DS.belongsTo('App.Service'),
  index: DS.attr('number'),
  isCurrent: DS.attr('boolean'),
  briefNotes: function () {
    return (typeof this.get('notes') === 'string') ? this.get('notes').slice(0, 80) : "";
  }.property('notes'),
  versionText: function () {
    return Em.I18n.t('dashboard.configHistory.table.version.versionText').format(this.get('version'));
  }.property('version'),
  modifiedDate: function () {
    return dateUtil.dateFormat(this.get('appliedTime'));
  }.property('createTime'),
  shortModifiedDate: function () {
    return dateUtil.dateFormat(this.get('appliedTime'), 'MMM DD, YYYY');
  }.property('createTime'),
  /**
   * determine whether ServiceConfigVersion is requested from server
   */
  isRequested: DS.attr('boolean'),
  isRestartRequired: function () {
    return this.get('service.isRestartRequired') && this.get('isCurrent');
  }.property('service.isRestartRequired', 'isCurrent')
});

App.ServiceConfigVersion.FIXTURES = [];
