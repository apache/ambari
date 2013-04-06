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

App.MainMirroringTargetClusterController = Ember.Controller.extend({
  name: 'mainMirroringTargetClusterController',
  model: Ember.Object.create({
    targetCluster : null,
    originalRecord : null,
    isPopupForEdit : false // set the default to add scenario
  }),
  setOriginalRecord : function(targetClusterRecord){
    this.set('model.originalRecord', targetClusterRecord);
  },

  setTargetCluster: function (targetClusterRecord) {
    var targetCluster = Ember.Object.create({
      id: targetClusterRecord.get('id'),
      clusterName: targetClusterRecord.get('clusterName'),
      nameNodeWebUrl: targetClusterRecord.get('nameNodeWebUrl'),
      nameNodeRpcUrl: targetClusterRecord.get('nameNodeRpcUrl'),
      oozieServerUrl: targetClusterRecord.get('oozieServerUrl')
    });

    this.set('model.targetCluster', targetCluster);
  },


  createTargetCluster: function () {
    var targetCluster = Ember.Object.create({
      clusterName: null,
      nameNodeWebUrl: null,
      nameNodeRpcUrl: null,
      oozieServerUrl: null
    });
    this.set('model.targetCluster', targetCluster);
    return targetCluster;

    /* For future (but on record objects , not on pojos):
     targetCluster.on('didUpdate', function() {
     console.log("------Updated!");
     });
     targetCluster.on('didDelete', function() {
     console.log("------Deleted!");
     });
     targetCluster.on('didCreate', function() {
     console.log("------Created!");
     });
     */

  },
  getTargetCluster: function () {
    return this.get('content.targetCluster');
  },

  popup : null,

  /**
   * "Delete" button handler.
   * A DataSet
   */
  deleteTargetCluster: function () {
    var self = this;
    App.showConfirmationPopup(function () {
      var originalRecord = self.get('model.originalRecord');
      originalRecord.deleteRecord();
      originalRecord.get("transaction").commit();
      self.get('popup').hide();
      App.router.transitionTo('main.mirroring.index');
    });
  }



});