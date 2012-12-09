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
   * Used to show appropriate date in UI
   */
  dateDisplay: function () {
    var d = this.get('date');
    if (d) {
      var dateString = d.toDateString() + ". " + d.toLocaleTimeString();
      dateString = dateString.substr(dateString.indexOf(" ") + 1);
      return dateString;
    }
    return "";
  }.property('date'),

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
