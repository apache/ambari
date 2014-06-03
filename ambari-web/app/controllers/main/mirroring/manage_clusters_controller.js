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

  executeTooltip: Em.I18n.t('mirroring.manageClusters.executeTooltip'),
  readonlyTooltip: Em.I18n.t('mirroring.manageClusters.readonlyTooltip'),
  workflowTooltip: Em.I18n.t('mirroring.manageClusters.workflowTooltip'),
  writeTooltip: Em.I18n.t('mirroring.manageClusters.writeTooltip'),

  clusters: [],

  newCluster: null,

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
    }
  }.observes('isLoaded'),

  selectedCluster: null,

  addCluster: function () {
    var self = this;
    var newClusterPopup = App.ModalPopup.show({
      header: Em.I18n.t('mirroring.manageClusters.create.cluster.popup'),
      bodyClass: Em.View.extend({
        controller: self,
        templateName: require('templates/main/mirroring/create_new_cluster')
      }),
      classNames: ['create-target-cluster-popup'],
      primary: Em.I18n.t('common.save'),
      secondary: Em.I18n.t('common.cancel'),
      onPrimary: function () {
        this.set('disablePrimary', true);
        self.createNewCluster();
      },
      willInsertElement: function () {
        var clusterName = App.get('clusterName');
        var newCluster = Ember.Object.create({
          name: '',
          execute: '',
          workflow: '',
          write: '',
          readonly: '',
          staging: '/apps/falcon/<cluster-name>/staging',
          working: '/apps/falcon/<cluster-name>/working',
          temp: '/tmp'
        });
        self.set('newCluster', newCluster);
      },
      didInsertElement: function () {
        this._super();
        this.fitHeight();
      }
    });
    this.set('newClusterPopup', newClusterPopup);
  },

  removeCluster: function () {
    var self = this;
    var selectedClusterName = self.get('selectedCluster.name');
    App.showConfirmationPopup(function () {
      App.ajax.send({
        name: 'mirroring.delete_entity',
        sender: self,
        data: {
          name: selectedClusterName,
          type: 'cluster',
          falconServer: App.get('falconServerURL')
        },
        success: 'onRemoveClusterSuccess',
        error: 'onError'
      });
    }, Em.I18n.t('mirroring.manageClusters.remove.confirmation').format(selectedClusterName));
  },

  onRemoveClusterSuccess: function () {
    this.set('clusters', this.get('clusters').without(this.get('selectedCluster')));
  },

  onError: function (response) {
    if (response && response.responseText) {
      var errorMessage = /(?:\<message\>)((.|\n)+)(?:\<\/message\>)/.exec(response.responseText);
      if (errorMessage.length > 1) {
        App.showAlertPopup(Em.I18n.t('common.error'), Em.I18n.t('mirroring.manageClusters.error') + ': ' + errorMessage[1]);
      }
    }
  },

  createNewCluster: function () {
    App.ajax.send({
      name: 'mirroring.submit_entity',
      sender: this,
      data: {
        type: 'cluster',
        entity: this.formatClusterXML(this.get('newCluster')),
        falconServer: App.get('falconServerURL')
      },
      success: 'onCreateClusterSuccess',
      error: 'onCreateClusterError'
    });
  },

  onCreateClusterSuccess: function () {
    this.get('clusters').pushObject(this.get('newCluster'));
    this.get('newClusterPopup').hide();
  },

  onCreateClusterError: function (response) {
    this.set('newClusterPopup.disablePrimary', false);
    this.onError(response);
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
