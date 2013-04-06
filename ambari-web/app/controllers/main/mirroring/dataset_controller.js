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

App.MainMirroringDataSetController = Ember.Controller.extend({
  name: 'mainMirroringDataSetController',

  model: Ember.Object.create(
    {
      newDataSet: null,
      listOfTargetClusterNames: function () {
        var listOfClusterNames = [];
        var listOfTargetClusters = App.TargetCluster.find();
        if (listOfTargetClusters && listOfTargetClusters.content.length) {
          listOfTargetClusters.forEach(function (tcluster) {
            listOfClusterNames.push(tcluster.get('clusterName'));
          });
        }
        return listOfClusterNames;
      }.property('newDataSet.targetCluster'),  // this property will be set when someone clicks the save button

      originalRecord: null
    }

  ),

  /**
   * Popup with add/edit form
   */
  popup: null,

  /**
   * true - popup with edit form
   * false - popup with add form
   */
  isPopupForEdit: false,

  createNewDataSet: function () {
    var newDataSet = Ember.Object.create({
      name: null,
      sourceDir: null,
      targetCluster: Ember.Object.create(),
      targetDir: null,
      schedule: Ember.Object.create()
    });
    this.set('model.newDataSet', newDataSet);
    return newDataSet;
  },

  setDataSet: function (dataset) {
    var newDataSet = Ember.Object.create({
      name: dataset.get('name'),
      sourceDir: dataset.get('sourceDir'),
      targetCluster: dataset.get('targetCluster'),
      targetDir: dataset.get('targetDir'),
      schedule: dataset.get('schedule')
    });
    this.set('model.newDataSet', newDataSet);
  },

  setOriginalDataSetRecord: function (datasetRecord) {
    this.set('model.originalRecord', datasetRecord);
  },


  getNewDataSet: function () {
    return this.get('model.newDataSet');
  },

  createTargetCluster: function () {
    var controller = App.router.get('mainMirroringTargetClusterController');
    controller.set('returnRoute', App.get('router.currentState.path'));
    App.router.transitionTo('addTargetClusterRoute');
  },

  /**
   * Set old values for all properties in the dataset
   */
  undoChanges: function () {
    this.set('model.newDataSet', this.get('rawDataSet'));
  },

  /**
   * Delete created dataset and its schedule
   */
  deleteNewDataSet: function () {
    var originalRecordSchedule = this.get('model.originalRecord.schedule');
    originalRecordSchedule.deleteRecord();
    originalRecordSchedule.get("transaction").commit();

    var originalRecord = this.get('model.originalRecord');

    originalRecord.deleteRecord();
    originalRecord.get("transaction").commit();
  },

  /**
   * "Delete" button handler
   */
  deleteDatasetClick: function () {
    var self = this;
    App.showConfirmationPopup(function () {
      self.deleteNewDataSet();
      self.get('popup').hide();
      App.router.transitionTo('main.mirroring.index');
    });
  }

});