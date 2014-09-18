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

App.SliderAppAlert = DS.Model.extend({
  /**
   * @type {string}
   */
  title: DS.attr('string'),

  /**
   * @type {string}
   */
  serviceName: DS.attr('string'),

  /**
   * @type {string}
   */
  status: DS.attr('string'),

  /**
   * @type {string}
   */
  message: DS.attr('string'),

  /**
   * @type {string}
   */
  hostName: DS.attr('string'),

  /**
   * @type {number}
   */
  lastTime: DS.attr('number'),

  /**
   * @type {number}
   */
  lastCheck: DS.attr('number'),

  /**
   * @type {App.SliderApp}
   */
  appId: DS.belongsTo('sliderApp'),

  /**
   * @type {string}
   */
  iconClass: function () {
    var statusMap = Em.Object.create({
      'OK': 'icon-ok',
      'WARNING': 'icon-warning-sign',
      'CRITICAL': 'icon-remove',
      'PASSIVE': 'icon-medkit'
    });
    return statusMap.getWithDefault(this.get('status'), 'icon-question-sign');
  }.property('status'),

  /**
   * @type {object}
   */
  date: function () {
    return DS.attr.transforms.date.from(this.get('lastTime'));
  }.property('lastTime'),

  /**
   * Provides how long ago this alert happened.
   *
   * @type {String}
   */
  timeSinceAlert: function () {
    var d = this.get('date');
    var timeFormat;
    var statusMap = Em.Object.create({
      'OK': 'OK',
      'WARNING': 'WARN',
      'CRITICAL': 'CRIT',
      'PASSIVE': 'MAINT'
    });
    var messageKey = statusMap.getWithDefault(this.get('status'), 'UNKNOWN');

    if (d) {
      timeFormat = Em.I18n.t('sliderApp.alerts.' + messageKey + '.timePrefix');
      var prevSuffix = $.timeago.settings.strings.suffixAgo;
      $.timeago.settings.strings.suffixAgo = '';
      var since = timeFormat.format($.timeago(this.makeTimeAtleastMinuteAgo(d)));
      $.timeago.settings.strings.suffixAgo = prevSuffix;
      return since;
    } else if (d == 0) {
      timeFormat = Em.I18n.t('sliderApp.alerts.' + messageKey + '.timePrefixShort');
      return timeFormat;
    } else {
      return "";
    }
  }.property('date', 'status'),

  /**
   *
   * @param d
   * @return {object}
   */
  makeTimeAtleastMinuteAgo: function (d) {
    var diff = (new Date).getTime() - d.getTime();
    if (diff < 60000) {
      diff = 60000 - diff;
      return new Date(d.getTime() - diff);
    }
    return d;
  },

  /**
   * Provides more details about when this alert happened.
   *
   * @type {String}
   */
  timeSinceAlertDetails: function () {
    var details = "";
    var date = this.get('date');
    if (date) {
      var dateString = date.toDateString();
      dateString = dateString.substr(dateString.indexOf(" ") + 1);
      dateString = Em.I18n.t('sliderApp.alerts.occurredOn').format(dateString, date.toLocaleTimeString());
      details += dateString;
    }
    var lastCheck = this.get('lastCheck');
    if (lastCheck) {
      lastCheck = new Date(lastCheck * 1000);
      details = details ? details + Em.I18n.t('sliderApp.alerts.brLastCheck').format($.timeago(lastCheck)) : Em.I18n.t('sliderApp.alerts.lastCheck').format($.timeago(lastCheck));
    }
    return details;
  }.property('lastCheck', 'date')

});

App.SliderAppAlert.FIXTURES = [];