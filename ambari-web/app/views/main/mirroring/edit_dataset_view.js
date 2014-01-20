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
var filters = require('views/common/filter_view');
var sort = require('views/common/sort_view');
var date = require('utils/date');

App.MainMirroringEditDataSetView = Em.View.extend({
  name: 'mainMirroringEditDataSetView',
  templateName: require('templates/main/mirroring/edit_dataset'),

  datasetTypeOptions: [Em.I18n.t('mirroring.dataset.type.HDFS'), Em.I18n.t('mirroring.dataset.type.Hive')],

  targetClusterSelect: Em.Select.extend({
    classNames: ['target-cluster-select'],

    content: function () {
      return [App.get('clusterName'), Em.I18n.t('mirroring.dataset.addTargetCluster')]
    }.property(),

    change: function () {
      if (this.get('selection') === Em.I18n.t('mirroring.dataset.addTargetCluster')) {
        this.set('selection', this.get('content')[0]);
        App.router.get('mainMirroringController').manageClusters();
      }
      this.set('parentView.controller.formFields.datasetTargetClusterName', this.get('selection'))
    }
  }),

  repeatOptions: [Em.I18n.t('mirroring.dataset.repeat.minutes'), Em.I18n.t('mirroring.dataset.repeat.hours'), Em.I18n.t('mirroring.dataset.repeat.days'), Em.I18n.t('mirroring.dataset.repeat.months')],

  middayPeriodOptions: [Em.I18n.t('mirroring.dataset.middayPeriod.am'), Em.I18n.t('mirroring.dataset.middayPeriod.pm')],

  hourOptions: ['01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12'],

  minuteOptions: ['00', '05', '10', '15', '20', '25', '30', '35', '40', '45', '50', '55'],

  didInsertElement: function () {
    $('.datepicker').datepicker({
      format: 'mm/dd/yyyy'
    });
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
