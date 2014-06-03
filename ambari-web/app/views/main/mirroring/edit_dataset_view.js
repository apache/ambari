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

App.MainMirroringEditDataSetView = Em.View.extend({

  name: 'mainMirroringEditDataSetView',

  templateName: require('templates/main/mirroring/edit_dataset'),

  /**
   * Defines if there are some target clusters defined
   * @type {Boolean}
   */
  hasTargetClusters: false,

  targetClusters: App.TargetCluster.find(),

  targetClusterSelect: Em.Select.extend({
    classNames: ['target-cluster-select'],

    content: function () {
      if (!this.get('parentView.isLoaded')) return [];
      return this.get('parentView.targetClusters').mapProperty('name').without(App.get('clusterName')).concat(Em.I18n.t('mirroring.dataset.addTargetCluster'));
    }.property('parentView.isLoaded', 'parentView.targetClusters.length'),

    change: function () {
      if (this.get('selection') === Em.I18n.t('mirroring.dataset.addTargetCluster')) {
        this.set('selection', this.get('content')[0]);
        this.get('parentView').manageClusters();
      }
      this.set('parentView.controller.formFields.datasetTargetClusterName', this.get('selection'))
    }
  }),

  /**
   * Set <code>hasTargetClusters</code> after clustes load
   */
  onTargetClustersChange: function () {
    if (this.get('isLoaded') && this.get('targetClusters.length') > 1) {
      this.set('hasTargetClusters', true);
    } else {
      this.set('hasTargetClusters', false);
      this.set('controller.formFields.datasetTargetClusterName', null);
    }
  }.observes('isLoaded', 'targetClusters.length'),

  repeatOptions: [Em.I18n.t('mirroring.dataset.repeat.minutes'), Em.I18n.t('mirroring.dataset.repeat.hours'), Em.I18n.t('mirroring.dataset.repeat.days'), Em.I18n.t('mirroring.dataset.repeat.months')],

  middayPeriodOptions: [Em.I18n.t('mirroring.dataset.middayPeriod.am'), Em.I18n.t('mirroring.dataset.middayPeriod.pm')],

  hourOptions: ['01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12'],

  minuteOptions: ['00', '05', '10', '15', '20', '25', '30', '35', '40', '45', '50', '55'],

  isLoaded: function () {
    return App.router.get('mainMirroringController.isLoaded');
  }.property('App.router.mainMirroringController.isLoaded'),

  manageClusters: function () {
    App.router.get('mainMirroringController').manageClusters();
  },

  /**
   * Fill form input fields for selected dataset to edit
   */
  fillForm: function () {
    var isEdit = this.get('controller.isEdit');
    if (this.get('isLoaded') && isEdit) {
      var controller = this.get('controller');
      var dataset = App.Dataset.find().findProperty('id', controller.get('datasetIdToEdit'));
      var scheduleStartDate = new Date(dataset.get('scheduleStartDate'));
      var scheduleEndDate = new Date(dataset.get('scheduleEndDate'));
      var formFields = controller.get('formFields');
      formFields.set('datasetName', dataset.get('name'));
      formFields.set('datasetSourceDir', dataset.get('sourceDir'));
      formFields.set('datasetTargetDir', dataset.get('targetDir'));
      formFields.set('datasetTargetClusterName', dataset.get('targetClusterName'));
      formFields.set('datasetFrequency', dataset.get('frequency'));
      formFields.set('repeatOptionSelected', dataset.get('frequencyUnit'));
      formFields.set('datasetStartDate', controller.addZero(scheduleStartDate.getMonth() + 1) + '/' + controller.addZero(scheduleStartDate.getDate()) + '/' + controller.addZero(scheduleStartDate.getFullYear()));
      formFields.set('datasetEndDate', controller.addZero(scheduleEndDate.getMonth() + 1) + '/' + controller.addZero(scheduleEndDate.getDate()) + '/' + controller.addZero(scheduleEndDate.getFullYear()));
      var startHours = scheduleStartDate.getHours();
      var endHours = scheduleEndDate.getHours();
      formFields.set('hoursForStart', controller.toAMPMHours(startHours));
      formFields.set('hoursForEnd', controller.toAMPMHours(endHours));
      formFields.set('minutesForStart', controller.addZero(scheduleStartDate.getMinutes()));
      formFields.set('minutesForEnd', controller.addZero(scheduleEndDate.getMinutes()));
      formFields.set('middayPeriodForStart', startHours > 11 ? 'PM' : 'AM');
      formFields.set('middayPeriodForEnd', endHours > 11 ? 'PM' : 'AM');
    }
  }.observes('isLoaded', 'App.router.mainMirroringController.selectedDataset', 'controller.isEdit'),

  select: Em.Select.extend({
    attributeBindings: ['disabled']
  }),

  didInsertElement: function () {
    // Initialize datepicker
    $('.datepicker').datepicker({
      format: 'mm/dd/yyyy'
    }).on('changeDate', function (ev) {
          $(this).datepicker('hide');
        });

    // Set default value for Repeat every combo box
    this.set('controller.formFields.repeatOptionSelected', this.get('repeatOptions')[2]);

    this.fillForm();
    this.onTargetClustersChange();
  },

  willDestroyElement: function () {
    var controller = this.get('controller');
    Em.keys(this.get('controller.formFields')).forEach(function (key) {
      controller.removeObserver('formFields.' + key, controller, 'validate');
    }, this);
    this._super();
  },

  init: function () {
    this.get('controller').clearStep();
    this._super();
  }
});
