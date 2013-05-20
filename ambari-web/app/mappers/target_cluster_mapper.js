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


App.targetClusterMapper = App.QuickDataMapper.create({
  model: App.TargetCluster,
  config: {
    id: 'id',
    cluster_name: 'Clusters.name',
    name_node_web_url: 'name_node_web_url',
    name_node_rpc_url: 'name_node_rpc_url',
    oozie_server_url: 'oozie_server_url'
  },
  map: function (json) {
    if (!this.get('model')) {
      return;
    }
    if (json && json.items && json.items.length > 0) {
      var target_cluster_results = [];
      json.items.forEach(function (item) {
        try {
          item.name_node_web_url = (item.Clusters.interfaces.interface.findProperty("type", "readonly")).endpoint;
          item.name_node_rpc_url = (item.Clusters.interfaces.interface.findProperty("type", "write")).endpoint;
          item.oozie_server_url = (item.Clusters.interfaces.interface.findProperty("type","workflow")).endpoint;
          item.id = item.Clusters.name;

          var re = new RegExp(" ", "g");
          item.id = item.id.replace(re, "_");


          item = this.parseIt(item, this.config);
          target_cluster_results.push(item);
        } catch (ex) {
          console.error('Exception occurred: ' + ex);
        }
      }, this);
      App.store.loadMany(this.get('model'), target_cluster_results);
    }
  }
});
