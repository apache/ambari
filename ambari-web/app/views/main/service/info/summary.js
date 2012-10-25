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
    hdfs: false,
    mapreduce: false,
    hbase: false
  },

  alerts: function(){
    var serviceId = this.get('service.id');
    if(serviceId) {
      return App.Alert.find({'service_id':serviceId });
    }
    return [];
//    return App.router.get('mainServiceInfoSummaryController.content.alerts');
  }.property('App.router.mainServiceInfoSummaryController.content.alerts'),

  controller: function(){
    return App.router.get('mainServiceInfoSummaryController');
  }.property(),

  service: function(){
    return this.get('controller.content');
  }.property('controller.content'),

  isHide: true,
  moreStatsView: Em.View.extend({
    tagName: "a",
    template: Ember.Handlebars.compile('{{t services.service.summary.moreStats}}'),
    attributeBindings: ['href'],
    classNames: ['more-stats'],
    click: function(event) {
      this._parentView._parentView.set('isHide', false);
      this.remove();
    },
    href: 'javascript:void(null)'
  }),
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
    var serviceStatus = summaryView.get('serviceStatus');
    $.each(serviceStatus, function(key, value) {
      if (key == serviceName) {
        summaryView.set('serviceStatus.' + key, true);
      } else {
        summaryView.set('serviceStatus.' + key, false);
      }
    });

    jQuery.getJSON('data/services/summary/' + serviceName + '.json',
      function (data) {
        if (data[serviceName]) {
          var summary = data[serviceName];
          if(serviceName == 'hdfs') {
            summary['start_time'] = summary['start_time'].toDaysHoursMinutes();
            summary['memory_heap_percent_used'] = summary['memory_heap_used'].countPercentageRatio(summary['memory_heap_max']);
            summary['memory_heap_used'] = summary['memory_heap_used'].bytesToSize(2, 'parseFloat');
            summary['memory_heap_max'] = summary['memory_heap_max'].bytesToSize(2, 'parseFloat');
            summary['dfs_percent_disk_used'] = parseFloat((100 - summary['dfs_percent_remaining']).toFixed(2)) + "%";
            summary['used_bytes'] = (summary['dfs_used_bytes'] + summary['nondfs_used_bytes']).bytesToSize(2, 'parseFloat');
            summary['dfs_total_bytes'] = summary['dfs_total_bytes'].bytesToSize(2, 'parseFloat');
            summary['metricGraphViews'] = [App.ChartServiceMetricsHDFS_JVMThreads.extend(), 
                                           App.ChartServiceMetricsHDFS_JVMHeap.extend(), 
                                           App.ChartServiceMetricsHDFS_IO.extend(), 
                                           App.ChartServiceMetricsHDFS_RPC.extend(), 
                                           App.ChartServiceMetricsHDFS_FileOperations.extend(), 
                                           App.ChartServiceMetricsHDFS_GC.extend()];
          } else if (serviceName == 'mapreduce') {
            summary['start_time'] = summary['start_time'].toDaysHoursMinutes();
            summary['memory_heap_percent_used'] = summary['memory_heap_used'].countPercentageRatio(summary['memory_heap_max']);
            summary['memory_heap_used'] = summary['memory_heap_used'].bytesToSize(2, 'parseFloat');
            summary['memory_heap_max'] = summary['memory_heap_max'].bytesToSize(2, 'parseFloat');
          } else if (serviceName == 'hbase') {
            summary['memory_heap_percent_used'] = summary['memory_heap_used'].countPercentageRatio(summary['memory_heap_max']);
            summary['memory_heap_used'] = summary['memory_heap_used'].bytesToSize(2, 'parseFloat');
            summary['memory_heap_max'] = summary['memory_heap_max'].bytesToSize(2, 'parseFloat');
            summary['start_time'] = summary['start_time'].toDaysHoursMinutes();
            summary['active_time'] = summary['active_time'].toDaysHoursMinutes();
          }
          summaryView.set('attributes', summary);
        }
      }
    )
  }
});