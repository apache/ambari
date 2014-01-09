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

App.MainMirroringController = Em.ArrayController.extend({
  name: 'mainMirroringController',

  datasets: function () {
    return App.Dataset.find();
  }.property(),

  targetClusters: function () {
    return App.TargetCluster.find();
  }.property(),

  manageClusters: function () {
    App.ModalPopup.show({
      header: Em.I18n.t('mirroring.dataset.manageClusters'),
      bodyClass: App.MainMirroringManageClusterstView.extend({
        controllerBinding: 'App.router.mainMirroringManageClustersController'
      }),
      primary: Em.I18n.t('common.save'),
      secondary: null,
      onPrimary: function () {
        this.hide();
      },
      didInsertElement: function () {
        this._super();
        this.fitHeight();
      }
    });
  }

});
