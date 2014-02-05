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

App.MainMirroringManageClusterstView = Em.View.extend({
  name: 'mainMirroringManageClustersView',
  templateName: require('templates/main/mirroring/manage_clusters'),

  clusterSelect: Ember.Select.extend({
    classNames: ['cluster-select'],
    multiple: true,
    content: function () {
      var clusters = this.get('controller.clusters').slice();
      clusters.unshift(App.get('clusterName'));
      return clusters;
    }.property('controller.clusters.@each', 'App.clusterName'),

    onSelect: function () {
      if (this.get('selection.length')) {
        if (this.get('selection').length === 1) {
          this.set('controller.selectedCluster', this.get('selection')[0]);
        } else {
          this.set('selection', [this.get('controller.selectedCluster')]);
        }
      } else {
        this.set('controller.selectedCluster', null);
      }
    }.observes('selection')
  }),

  removeDisabled: function () {
    var selectedCluster = this.get('controller.selectedCluster');
    return !selectedCluster || selectedCluster === App.get('clusterName');
  }.property('controller.selectedCluster', 'App.clusterName')
});


