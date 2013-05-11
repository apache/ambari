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
    targetCluster: null,
    originalRecord: null,
    isPopupForEdit: false, // set the default to add scenario
    isNameNodeWebUrlError: function (key, value) {
      if (value) {
        return value;
      }
      var controller = App.router.get('mainMirroringTargetClusterController');
      var isNameNodeWebUrlError = controller.checkNameNodeWebUrlErrors();
      return isNameNodeWebUrlError;
    }.property('targetCluster.nameNodeWebUrl', 'model.targetCluster.nameNodeWebUrl'),
    isNameNodeRpcUrlError: function (key, value) {
      if (value) {
        return value;
      }
      var controller = App.router.get('mainMirroringTargetClusterController');
      var isNameNodeRpcUrlError = controller.checkNameNodeRpcUrlErrors();
      return isNameNodeRpcUrlError;
    }.property('targetCluster.nameNodeRpcUrl', 'model.targetCluster.nameNodeRpcUrl'),

    isOozieServerUrlError: function (key, value) {
      if (value) {
        return value;
      }
      var controller = App.router.get('mainMirroringTargetClusterController');
      var isOozieServerUrlError = controller.checkOozieServerUrlErrors();
      return isOozieServerUrlError;
    }.property('targetCluster.oozieServerUrl', 'model.targetCluster.oozieServerUrl'),

    isClusterNameError: function (key, value) {
      if (value) {
        return value;
      }
      var controller = App.router.get('mainMirroringTargetClusterController');
      var isClusterNameError = controller.checkClusterNameErrors();
      return isClusterNameError;
    }.property('targetCluster.clusterName', 'model.targetCluster.clusterName'),

    nameNodeWebUrlErrorMessage: null,
    nameNodeRpcUrlErrorMessage: null,
    oozieServerUrlErrorMessage: null,
    clusterNameErrorMessage: null
  }),

  isSubmitted1: null,
  isSubmitted2: null,

  validate1: function () {
    var isNameNodeWebUrlError = this.checkNameNodeWebUrlErrors();
    var isNameNodeRpcUrlError = this.checkNameNodeRpcUrlErrors();
    var isOozieServerUrlError = this.checkOozieServerUrlErrors();

    if (isNameNodeWebUrlError || isNameNodeRpcUrlError || isOozieServerUrlError) {
      return false;
    }
    return true;
  },

  validate2: function () {
    var isClusterNameError = this.checkClusterNameErrors();

    if (isClusterNameError) {
      return false;
    }
    return true;
  },

  checkNameNodeWebUrlErrors: function () {
    if (!this.get('isSubmitted1')){
      this.set('model.nameNodeWebUrlErrorMessage', "");
      return false;
    }
    var nameNodeWebUrl = this.get('model.targetCluster.nameNodeWebUrl');
    if (!nameNodeWebUrl || nameNodeWebUrl.trim() === "") {
      this.set('model.isNameNodeWebUrlError', true);
      this.set('model.nameNodeWebUrlErrorMessage', Em.I18n.t('mirroring.required.error'));
      return true;
    }
    else {
      this.set('model.nameNodeWebUrlErrorMessage', "");
      return false;
    }

  },

  checkNameNodeRpcUrlErrors: function () {
    if (!this.get('isSubmitted1')){
      this.set('model.nameNodeRpcUrlErrorMessage', "");
      return false;
    }
    var nameNodeRpcUrl = this.get('model.targetCluster.nameNodeRpcUrl');
    if (!nameNodeRpcUrl || nameNodeRpcUrl.trim() === "") {
      this.set('model.isNameNodeRpcUrlError', true);
      this.set('model.nameNodeRpcUrlErrorMessage', Em.I18n.t('mirroring.required.error'));
      return true;
    }
    else {
      this.set('model.nameNodeRpcUrlErrorMessage', "");
      return false;
    }

  },

  checkOozieServerUrlErrors: function () {
    if (!this.get('isSubmitted1')){
      this.set('model.oozieServerUrlErrorMessage', "");
      return false;
    }
    var oozieServerUrl = this.get('model.targetCluster.oozieServerUrl');
    if (!oozieServerUrl || oozieServerUrl.trim() === "") {
      this.set('model.isOozieServerUrlError', true);
      this.set('model.oozieServerUrlErrorMessage', Em.I18n.t('mirroring.required.error'));
      return true;
    }
    else {
      this.set('model.oozieServerUrlErrorMessage', "");
      return false;
    }

  },

  checkClusterNameErrors: function () {
    if (!this.get('isSubmitted1')){
      this.set('model.clusterNameErrorMessage', "");
      return false;
    }
    var clusterName = this.get('model.targetCluster.clusterName');
    if (!clusterName || clusterName.trim() === "") {
      this.set('model.isClusterNameError', true);
      this.set('model.clusterNameErrorMessage', Em.I18n.t('mirroring.required.error'));
      return true;
    }
    else {
      this.set('model.clusterNameErrorMessage', "");
      return false;
    }

  },

  setOriginalRecord: function (targetClusterRecord) {
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

  popup: null,

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