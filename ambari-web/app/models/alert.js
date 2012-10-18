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
  title: DS.attr('string', {key: 'service_description'}),
  serviceType: DS.attr('string', {key: 'service_type'}),
  date: DS.attr('date', {key: 'last_hard_state_change'}),
  status: DS.attr('string', {key: 'current_state'}),
  message: DS.attr('string', {key: 'plugin_output'}),
  primaryKey: 'last_hard_state_change',
  alerts: DS.hasMany('App.Alert'),

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
    return '';
  }.property('serviceType'),

  /**
   * Used to provide appropriate service link in UI
   */
  serviceLink: function () {
    if (this.get('serviceType')) {
      var type = this.get('serviceType').toLowerCase();
      switch (type) {
        case 'mapreduce':
          return '#/main/services/2';
        case 'hdfs':
          return '#/main/services/1';
        case 'hbase':
          return '#/main/services/3';
        case 'zookeeper':
          return '#/main/services/4';
        case 'oozie':
          return '#/main/services/5';
        case 'hive':
          return '#/main/services/6';
      }
    }
    return '';
  }.property('serviceType')

});

/*
 * App.Alert.reopenClass() has to be called as opposed
 * to DS.Model.extend() containing URL. Only then will
 * the 'url' property show up for the instance and the 
 * RESTAdapter will contact server.
 */
App.Alert.reopenClass({
  url: "http://nagiosserver/hdp/nagios/nagios_alerts.php?q1=alerts&alert_type=all"
});

App.Alert.FIXTURES = [
  {
    id: 1,
    title: 'Corrupt/Missing Block',
    service_id: 1,
    date: 'August 29, 2012 17:00',
    status: 'corrupt',
    message: 'message'
  },
  {
    id: 2,
    title: 'Corrupt/Missing Block',
    service_id: 1,
    date: 'August 30, 2012 17:00',
    status: 'ok',
    message: 'message'
  },
  {
    id: 3,
    title: 'Corrupt/Missing Block',
    service_id: 2,
    date: 'August 29, 2012 17:00',
    status: 'corrupt',
    message: 'message'
  },
  {
    id: 4,
    title: 'Corrupt/Missing Block',
    service_id: 2,
    date: 'August 30, 2012 17:00',
    status: 'ok',
    message: 'message'
  },
  {
    id: 5,
    title: 'Corrupt/Missing Block',
    service_id: 3,
    date: 'August 29, 2012 17:00',
    status: 'corrupt',
    message: 'message'
  },
  {
    id: 6,
    title: 'Corrupt/Missing Block',
    service_id: 3,
    date: 'August 30, 2012 17:00',
    status: 'ok',
    message: 'message'
  },
  {
    id: 7,
    title: 'Corrupt/Missing Block',
    service_id: 4,
    date: 'August 29, 2012 17:00',
    status: 'corrupt',
    message: 'message'
  },
  {
    id: 8,
    title: 'Corrupt/Missing Block',
    service_id: 4,
    date: 'August 30, 2012 17:00',
    status: 'ok',
    message: 'message'
  },
  {
    id: 9,
    title: 'Corrupt/Missing Block',
    service_id: 5,
    date: 'August 29, 2012 17:00',
    status: 'corrupt',
    message: 'message'
  },
  {
    id: 10,
    title: 'Corrupt/Missing Block',
    service_id: 5,
    date: 'August 30, 2012 17:00',
    status: 'ok',
    message: 'message'
  },
  {
    id: 11,
    title: 'Corrupt/Missing Block',
    service_id: 6,
    date: 'August 29, 2012 17:00',
    status: 'corrupt',
    message: 'message'
  },
  {
    id: 12,
    title: 'Corrupt/Missing Block',
    service_id: 6,
    date: 'August 30, 2012 17:00',
    status: 'ok',
    message: 'message'
  }
];
