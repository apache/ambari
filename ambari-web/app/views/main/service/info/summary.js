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

App.MainServiceInfoSummaryView = Em.View.extend({
  templateName: require('templates/main/service/info/summary'),
  attributes: null,
  serviceStatus: {
    hdfs: false
  },
  serviceName: function() {
    return App.router.mainServiceInfoSummaryController.get('content.serviceName');
  }.property('App.router.mainServiceInfoSummaryController.content'),
  init: function() {
    this._super();
    if (this.get('serviceName'))
      this.loadServiceSummary(this.get('serviceName'));
  },
  loadServiceSummary: function(serviceName) {
    var summaryView = this;
    jQuery.getJSON('data/services/summary/' + serviceName + '.json',
      function (data) {
        if (data[serviceName]) {
          var summary = data[serviceName];
          if(serviceName == 'hdfs') {
            summaryView.set('serviceStatus.hdfs', true);
            summary['memory_heap_used'] = summaryView.convertByteToMbyte(summary['memory_heap_used']);
            summary['memory_heap_max'] = summaryView.convertByteToMbyte(summary['memory_heap_max']);
            summary['memory_heap_percent_used'] = summaryView.countPercentageRatio(summary['memory_heap_used'], summary['memory_heap_max']);
            summary['used_bytes'] = summaryView.convertByteToGbyte(summary['dfs_used_bytes'] + summary['nondfs_used_bytes']);
            summary['dfs_total_bytes'] = summaryView.convertByteToGbyte(summary['dfs_total_bytes']);
          } else {
            summaryView.set('serviceStatus.hdfs', false);
          }
          summaryView.set('attributes', summary);
        }
      }
    )
  },
  convertByteToMbyte: function(value) {
    var bytesInMbyte = 1048576;
    var newValue = value/bytesInMbyte;
    return parseFloat(newValue.toFixed(1));
  },
  convertByteToGbyte: function(value) {
    var bytesInGbyte = 1073741824;
    var newValue = value/bytesInGbyte;
    return parseFloat(newValue.toFixed(2));
  },
  countPercentageRatio: function(usedValue, maxValue) {
    return Math.round((usedValue/maxValue) * 100);
  }
});