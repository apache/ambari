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

App.MainMirroringManageClustersController = Em.ArrayController.extend({
  name: 'mainMirroringManageClustersController',

  // link to popup object
  popup: null,

  clusters: [],

  // array of original clusters to compare with changed ones
  originalClusters: [],

  queriesCount: 0,

  // array of error messages
  queryErrors: [],

  isLoaded: function () {
    return App.router.get('mainMirroringController.isLoaded');
  }.property('App.router.mainMirroringController.isLoaded'),

  onLoad: function () {
    if (this.get('isLoaded')) {
      var clusters = [];
      var originalClusters = [];
      App.TargetCluster.find().forEach(function (cluster) {
        var newCluster = {
          name: cluster.get('name'),
          execute: cluster.get('execute'),
          workflow: cluster.get('workflow'),
          readonly: cluster.get('readonly'),
          staging: cluster.get('staging'),
          working: cluster.get('working'),
          temp: cluster.get('temp')
        };
        clusters.push(Ember.Object.create(newCluster));
        originalClusters.push(Ember.Object.create(newCluster));
      }, this);
      this.set('clusters', clusters);
      this.set('originalClusters', originalClusters);
    }
  }.observes('isLoaded'),

  selectedCluster: null,

  addCluster: function () {
    var self = this;
    App.showPromptPopup(Em.I18n.t('mirroring.manageClusters.specifyName'),
        function (clusterName) {
          self.get('clusters').pushObject(Ember.Object.create({
            name: clusterName,
            execute: '',
            workflow: '',
            readonly: '',
            staging: '',
            working: '',
            temp: ''
          }));
        }
    );
  },

  removeCluster: function () {
    var self = this;
    App.showConfirmationPopup(function () {
      self.set('clusters', self.get('clusters').without(self.get('selectedCluster')));
    });
  },

  save: function () {
    // define clusters need to be deleted, modified or created
    var clusters = this.get('clusters');
    var originalClusters = this.get('originalClusters');
    var originalClustersNames = originalClusters.mapProperty('name');
    var clustersToModify = [];
    var clustersToCreate = [];
    clusters.forEach(function (cluster) {
      var clusterName = cluster.get('name');
      if (originalClustersNames.contains(clusterName)) {
        if (JSON.stringify(cluster) !== JSON.stringify(originalClusters.findProperty('name', clusterName))) {
          clustersToModify.push(clusterName);
        }
        originalClustersNames = originalClustersNames.without(clusterName);
      } else {
        clustersToCreate.push(clusterName);
      }
    }, this);
    var clustersToDelete = originalClustersNames;
    var queriesCount = clustersToCreate.length + clustersToDelete.length + clustersToModify.length;
    this.set('queriesCount', queriesCount);

    // send request to delete, modify or create cluster
    if (queriesCount) {
      this.get('queryErrors').clear();
      clustersToDelete.forEach(function (cluster) {
        App.ajax.send({
          name: 'mirroring.delete_instance',
          sender: this,
          data: {
            name: cluster,
            type: 'cluster'
          },
          success: 'onQueryResponse',
          error: 'onQueryResponse'
        });
      }, this);
      clustersToCreate.forEach(function (cluster) {
        App.ajax.send({
          name: 'mirroring.submit_instance',
          sender: this,
          data: {
            type: 'cluster',
            instance: this.formatClusterXML(clusters.findProperty('name', cluster))
          },
          success: 'onQueryResponse',
          error: 'onQueryResponse'
        });
      }, this);
      clustersToModify.forEach(function (cluster) {
        App.ajax.send({
          name: 'mirroring.update_instance',
          sender: this,
          data: {
            name: cluster,
            type: 'cluster',
            instance: this.formatClusterXML(clusters.findProperty('name', cluster))
          },
          success: 'onQueryResponse',
          error: 'onQueryResponse'
        });
      }, this);
    } else {
      this.get('popup').hide();
    }
  },

  // close popup after getting response from all queries or show popup with errors
  onQueryResponse: function () {
    var queryErrors = this.get('queryErrors');
    if (arguments.length === 4) {
      queryErrors.push(arguments[2]);
    }
    var queriesCount = this.get('queriesCount');
    this.set('queriesCount', --queriesCount);
    if (queriesCount < 1) {
      if (queryErrors.length) {
        App.showAlertPopup(Em.I18n.t('common.error'), Em.I18n.t('mirroring.manageClusters.error') + ': ' + queryErrors.join(', '));
      } else {
        this.get('popup').hide();
      }
    }
  },

  /**
   * Return XML-formatted string made from cluster object
   * @param {Object} cluster - object with cluster data
   * @return {String}
   */
  formatClusterXML: function (cluster) {
    return '<?xml version="1.0"?><cluster colo="default" description="" name="' + cluster.get('name') +
        '" xmlns="uri:falcon:cluster:0.1" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"><interfaces><interface type="readonly" endpoint="' + cluster.get('readonly') +
        '" version="2.2.0.2.0.6.0-76" /><interface type="execute" endpoint="' + cluster.get('execute') +
        '" version="2.2.0.2.0.6.0-76" /><interface type="workflow" endpoint="' + cluster.get('workflow') +
        '" version="3.1.4" /></interfaces><locations><location name="staging" path="' + cluster.get('staging') +
        '" /><location name="temp" path="' + cluster.get('temp') +
        '" /><location name="working" path="' + cluster.get('working') +
        '" /></locations></cluster>';
  }
});
