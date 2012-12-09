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

App.MainDashboardView = Em.View.extend({
  templateName: require('templates/main/dashboard'),
  content : [],
  services:function(){
    var services = App.Service.find();
    services.forEach(function(item){
      var vName;
      var item2;
      switch(item.get('serviceName')) {
        case "HDFS":
          vName = App.MainDashboardServiceHdfsView;
          item2 = App.HDFSService.find(item.get('id'));
          break;
        case "MAPREDUCE":
          vName = App.MainDashboardServiceMapreduceView;
          item2 = App.MapReduceService.find(item.get('id'));
          break;
        case "HBASE":
          vName = App.MainDashboardServiceHbaseView;
          item2 = App.HBaseService.find(item.get('id'));
          break;
        case "HIVE":
          vName = App.MainDashboardServiceHiveView ;
          break;
        case "ZOOKEEPER":
          vName = App.MainDashboardServiceZookeperView;
          break;
        case "OOZIE":
          vName = App.MainDashboardServiceOozieView;
          break;
        default:
          vName = Em.View;
      }
      this.get('content').pushObject({
        viewName : vName,
        model: item2 || item
      })
    }, this);

  }.observes('App.router.clusterController.dataLoadList.services')
});