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

App.MainHostComboSearchBoxView = Em.View.extend({
  templateName: require('templates/main/host/combo_search_box'),
  didInsertElement: function () {
    window.visualSearch = VS.init({
      container: $('#combo_search_box'),
      query: '',
      showFacets: true,
      unquotable: [
        'text'
      ],
      callbacks: {
        search: function (query, searchCollection) {
          var $query = $('#search_query');
          var count = searchCollection.size();
          $query.stop().animate({opacity: 1}, {duration: 300, queue: false});
          $query.html('<span class="raquo">&raquo;</span> You searched for: ' +
          '<b>' + (query || '<i>nothing</i>') + '</b>. ' +
          '(' + count + ' facet' + (count == 1 ? '' : 's') + ')');
          clearTimeout(window.queryHideDelay);
          window.queryHideDelay = setTimeout(function () {
            $query.animate({
              opacity: 0
            }, {
              duration: 1000,
              queue: false
            });
          }, 2000);
        },
        facetMatches: function (callback) {
          console.log('called');
          callback([
            {label: 'name', category: 'Host'},
            {label: 'ip', category: 'Host'},
            {label: 'version', category: 'Host'},
            {label: 'health', category: 'Host'},
            {label: 'service', category: 'Service'},
            {label: 'component', category: 'Service'},
            {label: 'state', category: 'Service'}
          ]);
        },
        valueMatches: function (facet, searchTerm, callback) {
          switch (facet) {
            case 'name':
              callback([
                {value: 'c6401.ambari.apache.org', label: 'c6401.ambari.apache.org'},
                {value: 'c6402.ambari.apache.org', label: 'c6402.ambari.apache.org'},
                {value: 'c6403.ambari.apache.org', label: 'c6403.ambari.apache.org'}
              ]);
              break;
            case 'ip':
              callback(['192.168.64.101', '192.168.64.102', '192.168.64.103']);
              break;
            case 'rack':
              callback(['/default-rack', '/default-rack-1', '/default-rack-2']);
              break;
            case 'version':
              callback([
                'HDP-2.0.0.0-1587',
                'HDP-2.1.2.2-1576',
                'HDP-2.2.4.0-2252',
                'HDP-2.3.4.0-3485'
              ]);
              break;
            case 'health':
              callback([
                'Healthy',
                'Master Down',
                'Slave Down',
                'Lost Heartbeat',
                'Alerts',
                'Restart',
                'Maintainance Mode'
              ]);
              break;
            case 'service':
              callback([
                'HDFS',
                'YARN',
                'HIVE',
                'HBASE',
                'Storm',
                'Oozie',
                'Falcon',
                'Pig',
                'Spark',
                'Zookeeper',
                'AMS',
                'Ranger'
              ]);
              break;
            case 'component':
              callback([
                'NameNode',
                'SNameNode',
                'ZooKeeper Server',
                'DataNode',
                'HDFS Client',
                'Zookeeper Client'
              ], {preserveOrder: true});
              break;
            case 'state':
              callback([
                'Started',
                'Stopped',
                'Install Failed',
                'Decommissioning',
                'Decommissioned'
              ], {preserveOrder: true});
              break;
          }
        }
      }
    });
  }
});