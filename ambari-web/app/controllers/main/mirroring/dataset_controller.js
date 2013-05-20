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

      originalRecord: null,

      isNameError: function (key, value) {
        if (value) {
          return value;
        }
        var controller = App.router.get('mainMirroringDataSetController');
        var isNameError = controller.checkNameErrors();
        return isNameError;
      }.property('newDataSet.name', 'model.newDataSet.name'),

      isSourceDirError: function (key, value) {
        if (value) {
          return value;
        }
        var controller = App.router.get('mainMirroringDataSetController');
        var isSourceDirError = controller.checkSourceDirErrors();
        return isSourceDirError;
      }.property('newDataSet.sourceDir', 'model.newDataSet.sourceDir'),


      isTargetClusterError: function (key, value) {
        if (value) {
          return value;
        }
        var controller = App.router.get('mainMirroringDataSetController');
        var isTargetClusterError = controller.checkTargetClusterErrors();
        return isTargetClusterError;
      }.property('newDataSet.targetCluster', 'model.newDataSet.targetCluster'),

      isTargetDirError: function (key, value) {
        if (value) {
          return value;
        }
        var controller = App.router.get('mainMirroringDataSetController');
        var isTargetDirError = controller.checkTargetDirErrors();
        return isTargetDirError;
      }.property('newDataSet.targetDir', 'model.newDataSet.targetDir'),

      isStartDateError: function (key, value) {
        if (value) {
          return value;
        }
        var controller = App.router.get('mainMirroringDataSetController');
        var isStartDateError = controller.checkStartDateErrors();
        return isStartDateError;
      }.property('newDataSet.schedule.startDate', 'model.newDataSet.schedule.startDate'),

      isEndDateError: function (key, value) {
        if (value) {
          return value;
        }
        var controller = App.router.get('mainMirroringDataSetController');
        var isEndDateError = controller.checkEndDateErrors();
        return isEndDateError;
      }.property('newDataSet.schedule.endDate', 'model.newDataSet.schedule.endDate'),

      isFrequencyError: function (key, value) {
        if (value) {
          return value;
        }
        var controller = App.router.get('mainMirroringDataSetController');
        var isFrequencyError = controller.checkFrequencyErrors();
        return isFrequencyError;
      }.property('newDataSet.schedule.frequency', 'model.newDataSet.schedule.frequency')


    }
  ),

  isSubmitted: null,

  validate: function () {
    var isNameError = this.checkNameErrors();
    var isSourceDirError = this.checkSourceDirErrors();
    var isTargetClusterError = this.checkTargetClusterErrors();
    var isTargetDirError = this.checkTargetDirErrors();
    var isStartDateError = this.checkStartDateErrors();
    var isEndDateError = this.checkEndDateErrors();
    var isFrequencyError = this.checkFrequencyErrors();

    if (isNameError || isSourceDirError || isTargetClusterError || isTargetDirError || isStartDateError || isEndDateError || isFrequencyError) {
      return false;
    }
    return true;
  },

  checkNameErrors: function () {
    if (!this.get('isSubmitted')){
      this.set('nameErrorMessage', "");
      return false;
    }
    var name = this.get('model.newDataSet.name');
    if (!name || name.trim() === "") {
      this.set('model.isNameError', true);
      this.set('nameErrorMessage', Em.I18n.t('mirroring.required.error'));
      return true;
    }
    else {
      this.set('nameErrorMessage', "");
      return false;
    }

  },

  checkSourceDirErrors: function () {
    if (!this.get('isSubmitted')){
      this.set('sourceDirErrorMessage', "");
      return false;
    }
    var sourceDir = this.get('model.newDataSet.sourceDir');
    if (!sourceDir || sourceDir.trim() === "") {
      this.set('model.isSourceDirError', true);
      this.set('sourceDirErrorMessage', Em.I18n.t('mirroring.required.error'));
      return true;
    }
    else {
      this.set('sourceDirErrorMessage', "");
      return false;
    }

  },

  checkTargetClusterErrors: function () {
    if (!this.get('isSubmitted')){
      this.set('targetClusterErrorMessage', "");
      return false;
    }
    var targetCluster = this.get('model.newDataSet.targetCluster.clusterName');
    if (!targetCluster || targetCluster.trim() === "") {
      this.set('model.isTargetClusterError', true);
      this.set('targetClusterErrorMessage', Em.I18n.t('mirroring.required.error'));
      return true;
    }
    else {
      this.set('targetClusterErrorMessage', "");
      return false;
    }


  },
  checkTargetDirErrors: function () {
    if (!this.get('isSubmitted')){
      this.set('targetDirErrorMessage', "");
      return false;
    }
    var targetDir = this.get('model.newDataSet.targetDir');
    if (!targetDir || targetDir.trim() === "") {
      this.set('model.isTargetDirError', true);
      this.set('targetDirErrorMessage', Em.I18n.t('mirroring.required.error'));
      return true;
    }
    else {
      this.set('targetDirErrorMessage', "");
      return false;
    }

  },

  checkStartDateErrors: function () {
    if (!this.get('isSubmitted')){
      this.set('startDateErrorMessage', "");
      return false;
    }
    var startDate = this.get('model.newDataSet.schedule.startDate');
    if (!startDate || startDate.trim() === "") {
      this.set('model.isStartDateError', true);
      this.set('startDateErrorMessage', Em.I18n.t('mirroring.required.error'));
      return true;
    }
    else {
      this.set('startDateErrorMessage', "");
      return false;
    }

  },

  checkEndDateErrors: function () {
    if (!this.get('isSubmitted')){
      this.set('endDateErrorMessage', "");
      return false;
    }
    var startDate = this.get('model.newDataSet.schedule.startDate');
    var endDate = this.get('model.newDataSet.schedule.endDate');
    if (!endDate || endDate.trim() === "") {
      this.set('model.isEndDateError', true);
      this.set('endDateErrorMessage', Em.I18n.t('mirroring.required.error'));
      return true;
    }
    else {

      var sDate = new Date(this.get('model.newDataSet.schedule.startDate'));
      var eDate = new Date(this.get('model.newDataSet.schedule.endDate'));
      if(sDate > eDate){
        this.set('model.isEndDateError', true);
        this.set('endDateErrorMessage', Em.I18n.t('mirroring.dateOrder.error'));
        return true;
      }


      this.set('endDateErrorMessage', "");
      return false;
    }

  },

  checkFrequencyErrors: function () {
    if (!this.get('isSubmitted')){
      this.set('frequencyErrorMessage', "");
      return false;
    }
    var frequency = this.get('model.newDataSet.schedule.frequency');

    if (!frequency || frequency.trim() === "") {
      this.set('model.isFrequencyError', true);
      this.set('frequencyErrorMessage', Em.I18n.t('mirroring.required.error'));
      return true;
    }
    else {

      var startParenthesisindex = frequency.indexOf('(');
      var endParenthesisindex = frequency.indexOf(')');

      if (endParenthesisindex - startParenthesisindex == 1) {
        this.set('model.isFrequencyError', true);
        this.set('frequencyErrorMessage', Em.I18n.t('mirroring.required.error'));
        return true;
      }
      else {
        var frequencyNum = frequency.substring(startParenthesisindex + 1, endParenthesisindex);

        frequencyNum = parseInt(frequencyNum);

        if (isNaN(frequencyNum)) {
          this.set('model.isFrequencyError', true);
          this.set('frequencyErrorMessage', Em.I18n.t('mirroring.required.invalidNumberError'));
          return true;
        }

      }

      this.set('frequencyErrorMessage', "");
      return false;
    }

  },

  nameErrorMessage: null,
  sourceDirErrorMessage: null,
  targetClusterErrorMessage: null,
  targetDirErrorMessage: null,
  startDateErrorMessage: null,
  endDateErrorMessage: null,
  frequencyErrorMessage: null,
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
      status : 'SCHEDULED',
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
      schedule: dataset.get('schedule'),
      status: dataset.get('status')

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