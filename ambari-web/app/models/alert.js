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

App.AlertStatus = {
  negative: 'corrupt',
  positive: 'ok'
}

/**
 * Defines structure for App.Alert class. Keys mentioned here are for JSON data
 * which comes back from NAGIOS server.
 */
App.Alert = DS.Model.extend({
  alertId: DS.attr('string'),
  primaryKey: 'alertId',
  title: DS.attr('string'),//service_description in ajax response
  serviceType: DS.attr('string'),
  date: DS.attr('date'),
  status: DS.attr('string'),//current_state in ajax response
  message: DS.attr('string'),//plugin_output in ajax response
  hostName: DS.attr('string'),
  currentAttempt: DS.attr('string'),
  lastHardStateChange: DS.attr('number'),
  lastHardState: DS.attr('number'),
  lastTimeOk: DS.attr('number'),
  lastTimeWarning: DS.attr('number'),
  lastTimeUnknown: DS.attr('number'),
  lastTimeCritical: DS.attr('number'),
  isFlapping: DS.attr('number'),
  lastCheck: DS.attr('number'),

  /**
   * Used to show correct icon in UI
   */
  isOk: function () {
    return this.get('status') == "0";
  }.property('status'),

  /**
   * Used to show correct icon in UI
   */
  isWarning: function () {
    return this.get('status') == "1";
  }.property('status'),

  /**
   * Used to show correct icon in UI
   */
  isCritical: function() {
    return this.get('status') == '2';
  }.property('status'),

  /**
   * Used to show only required alerts at the service level
   */
  ignoredForServices: function() {
    return ['NodeManager health', 'NodeManager process', 'TaskTracker process', 'RegionServer process', 'DataNode process', 'DataNode space', 'ZooKeeper Server process'].contains(this.get('title'));
  }.property('title'),

  /**
   * Used to show only required alerts at the host level
   */
  ignoredForHosts: function() {
    return this.get('title').indexOf('Percent') != -1;
  }.property('title'),

  /**
   * Provides how long ago this alert happened.
   * 
   * @type {String}
   */
  timeSinceAlert: function () {
    var d = this.get('date');
    if (d) {
      var prefix = this.t('services.alerts.OK.timePrefix');
      switch (this.get('status')) {
        case "1":
          prefix = this.t('services.alerts.WARN.timePrefix');
          break;
        case "2":
          prefix = this.t('services.alerts.CRIT.timePrefix');
          break;
        case "3":
          prefix = this.t('services.alerts.UNKNOWN.timePrefix');
          break;
      }
      var prevSuffix = $.timeago.settings.strings.suffixAgo;
      $.timeago.settings.strings.suffixAgo = '';
      var since = prefix + $.timeago(this.makeTimeAtleastMinuteAgo(d));
      $.timeago.settings.strings.suffixAgo = prevSuffix;
      return since;
    }
    return "";
  }.property('date', 'status'),
  
  makeTimeAtleastMinuteAgo: function(d){
    var diff = new Date().getTime() - d.getTime();
    if (diff < 60000) {
      diff = 60000 - diff;
      var newD = new Date(d.getTime() - diff );
      //console.log("Making time more than 1 minute. New time=",newD,", Old time=",d);
      return newD;
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
      dateString = "Occurred on " + dateString + ", " + date.toLocaleTimeString();
      details += dateString;
    }
    var lastCheck = this.get('lastCheck');
    if (lastCheck) {
      lastCheck = new Date(lastCheck * 1000);
      details = details + "<br>Last checked " + $.timeago(lastCheck);
    }
    return details;
  }.property('lastCheck', 'date'),

  /**
   * Used to show appropriate service label in UI
   */
  serviceName: function () {
    if (this.get('serviceType')) {
      var type = this.get('serviceType').toLowerCase();
      switch (type) {
        case 'mapreduce':
          return 'MapReduce';
        case 'hdfs':
          return 'HDFS';
        case 'hbase':
          return "HBase";
        case 'zookeeper':
          return "Zookeeper";
        case 'oozie':
          return "Oozie";
        case 'hive':
          return 'Hive';
      }
    }
    return null;
  }.property('serviceType'),

  /**
   * Used to provide appropriate service link in UI
   */
  serviceLink: function () {
    if (this.get('serviceType')) {
      var type = this.get('serviceType').toLowerCase();
      switch (type) {
        case 'mapreduce':
          return '#/main/services/MAPREDUCE/summary';
        case 'hdfs':
          return '#/main/services/HDFS/summary';
        case 'hbase':
          return '#/main/services/HBASE/summary';
        case 'zookeeper':
          return '#/main/services/ZOOKEEPER/summary';
        case 'oozie':
          return '#/main/services/OOZIE/summary';
        case 'hive':
          return '#/main/services/HIVE/summary';
      }
    }
    return null;
  }.property('serviceType')

});

App.Alert.FIXTURES = [
];
