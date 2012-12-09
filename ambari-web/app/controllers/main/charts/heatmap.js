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

App.MainChartsHeatmapController = Em.Controller.extend({
  name:'mainChartsHeatmapController',
  cluster: App.Cluster.find(1),

  /**
   * return class name for build rack visual schema
   * @this App.MainChartsHeatmapController
   */
  visualSchema: function() {
    var maxHostsPerRack = this.get('cluster.maxHostsPerRack');
    switch(maxHostsPerRack) {
      case 10:
        return 'rack-5-2'
      case 20:
        return 'rack-5-4'
      case 30:
        return 'rack-5-6'
      case 40:
        return 'rack-5-8'
      case 50:
        return 'rack-5-10'
      default:
        return 'rack-5-10'
    }
  }.property('cluster')
})