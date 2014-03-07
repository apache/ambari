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

  clustersToDelete: [],

  clustersToCreate: [],

  queriesCount: 0,

  // array of error messages
  queryErrors: [],

  isLoaded: function () {
    return App.router.get('mainMirroringController.isLoaded');
  }.property('App.router.mainMirroringController.isLoaded'),

  onLoad: function () {
    if (this.get('isLoaded')) {
      var clusters = [];
      App.TargetCluster.find().forEach(function (cluster) {
        var newCluster = {
          name: cluster.get('name'),
          execute: cluster.get('execute'),
          workflow: cluster.get('workflow'),
          write: cluster.get('write'),
          readonly: cluster.get('readonly'),
          staging: cluster.get('staging'),
          working: cluster.get('working'),
          temp: cluster.get('temp')
        };
        // Source cluster should be shown on top
        if (cluster.get('name') === App.get('clusterName')) {
          clusters.unshift(Ember.Object.create(newCluster));
        } else {
          clusters.push(Ember.Object.create(newCluster));
        }
      }, this);
      this.set('clusters', clusters);
      this.get('clustersToDelete').clear();
      this.get('clustersToCreate').clear();
    }
  }.observes('isLoaded'),

  selectedCluster: null,

  // Disable input fields for already created clusters
  isEditDisabled: function () {
    return !this.get('clustersToCreate').mapProperty('name').contains(this.get('selectedCluster.name'));
  }.property('selectedCluster.name', 'clustersToCreate.@each.name'),

  addCluster: function () {
    var self = this;
    App.showPromptPopup(Em.I18n.t('mirroring.manageClusters.specifyName'),
        function (clusterName) {
          var newCluster = Ember.Object.create({
            name: clusterName,
            execute: '',
            workflow: '',
            write: '',
            readonly: '',
            staging: '',
            working: '',
            temp: ''
          });
          self.get('clusters').pushObject(newCluster);
          self.get('clustersToCreate').pushObject(newCluster);
        }
    );
  },

  removeCluster: function () {
    var self = this;
    App.showConfirmationPopup(function () {
      if (self.get('clustersToCreate').mapProperty('name').contains(self.get('selectedCluster.name'))) {
        self.set('clustersToCreate', self.get('clustersToCreate').without(self.get('selectedCluster')));
      } else {
        self.get('clustersToDelete').push(self.get('selectedCluster'));
      }
      self.set('clusters', self.get('clusters').without(self.get('selectedCluster')));
    });
  },

  save: function () {
    // define clusters need to be deleted, modified or created
    var clusters = this.get('clusters');
    var clustersToCreate = this.get('clustersToCreate');
    var clustersToDelete = this.get('clustersToDelete');
    var queriesCount = clustersToCreate.length + clustersToDelete.length;
    this.set('queriesCount', queriesCount);

    // send request to delete, modify or create cluster
    if (queriesCount) {
      this.get('queryErrors').clear();
      clustersToDelete.forEach(function (cluster) {
        App.ajax.send({
          name: 'mirroring.delete_entity',
          sender: this,
          data: {
            name: cluster.get('name'),
            type: 'cluster',
            falconServer: App.get('falconServerURL')
          },
          success: 'onQueryResponse',
          error: 'onQueryResponse'
        });
      }, this);
      clustersToCreate.forEach(function (cluster) {
        App.ajax.send({
          name: 'mirroring.submit_entity',
          sender: this,
          data: {
            type: 'cluster',
            entity: this.formatClusterXML(cluster),
            falconServer: App.get('falconServerURL')
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
    return '<?xml version="1.0"?><cluster colo="local" description="" name="' + cluster.get('name') +
        '" xmlns="uri:falcon:cluster:0.1"><interfaces><interface type="readonly" endpoint="' + cluster.get('readonly') +
        '" version="2.2.0" /><interface type="execute" endpoint="' + cluster.get('execute') +
        '" version="2.2.0" /><interface type="workflow" endpoint="' + cluster.get('workflow') +
        '" version="4.0.0" />' + '<interface type="messaging" endpoint="tcp://' + App.get('falconServerURL') + ':61616?daemon=true" version="5.1.6" />' +
        '<interface type="write" endpoint="' + cluster.get('write') + '" version="2.2.0" />' +
        '</interfaces><locations><location name="staging" path="' + cluster.get('staging') +
        '" /><location name="temp" path="' + cluster.get('temp') +
        '" /><location name="working" path="' + cluster.get('working') +
        '" /></locations></cluster>';
  }
});
