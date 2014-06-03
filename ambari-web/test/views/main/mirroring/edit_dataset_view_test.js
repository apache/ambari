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
require('controllers/main/mirroring/edit_dataset_controller');
require('models/target_cluster');
require('views/main/mirroring/edit_dataset_view');

var mainMirroringEditDataSetView;
describe('App.MainMirroringEditDataSetView', function () {

  beforeEach(function () {
    mainMirroringEditDataSetView = App.MainMirroringEditDataSetView.create({
      controller: App.MainMirroringEditDataSetController.create(),
      isLoaded: true
    });
  });

  describe('targetClusterSelect.content', function () {
    var targetClusterSelect;
    beforeEach(function () {
      targetClusterSelect = mainMirroringEditDataSetView.get('targetClusterSelect').create({
        parentView: mainMirroringEditDataSetView
      });
    });

    it('should be empty if data is not loaded', function () {
      targetClusterSelect.set('parentView.isLoaded', false);
      expect(targetClusterSelect.get('content')).to.be.empty;
    });
    it('should contain list of clusters if data is loaded', function () {
      targetClusterSelect.set('parentView.isLoaded', true);
      targetClusterSelect.set('parentView.targetClusters', [
        {name: 'test1'},
        {name: 'test2'},
        {name: App.get('clusterName')}
      ]);
      expect(targetClusterSelect.get('content')).to.eql([
        'test1',
        'test2',
        Em.I18n.t('mirroring.dataset.addTargetCluster')
      ]);
    });
  });

  describe('targetClusterSelect.change', function () {
    var targetClusterSelect;
    beforeEach(function () {
      targetClusterSelect = mainMirroringEditDataSetView.get('targetClusterSelect').create({
        parentView: mainMirroringEditDataSetView,
        content: ['test1', 'test2', 'test3']
      });
      sinon.stub(targetClusterSelect.parentView, 'manageClusters', Em.K);
    });

    afterEach(function () {
      targetClusterSelect.parentView.manageClusters.restore();
    });

    it('should open manage cluster popup if appropriate option was selected', function () {
      targetClusterSelect.set('selection', Em.I18n.t('mirroring.dataset.addTargetCluster'));
      targetClusterSelect.change();
      expect(targetClusterSelect.get('selection')).to.equal('test1');
      expect(targetClusterSelect.parentView.manageClusters.calledOnce).to.be.true;
      expect(targetClusterSelect.get('parentView.controller.formFields.datasetTargetClusterName')).to.equal('test1');
    });
    it('should not open manage cluster popup if appropriate option was not selected', function () {
      targetClusterSelect.set('selection', 'test3');
      targetClusterSelect.change();
      expect(targetClusterSelect.get('selection')).to.equal('test3');
      expect(targetClusterSelect.parentView.manageClusters.calledOnce).to.be.false;
      expect(targetClusterSelect.get('parentView.controller.formFields.datasetTargetClusterName')).to.equal('test3');
    });
  });

  describe('onTargetClustersChange', function () {

    var testCases = [
      {
        isLoaded: true,
        targetClusters: [1, 2, 3],
        targetClusterName: 'test',
        hasTargetClusters: true
      },
      {
        isLoaded: false,
        targetClusters: [1, 2, 3],
        targetClusterName: null,
        hasTargetClusters: false
      },
      {
        isLoaded: true,
        targetClusters: [1],
        targetClusterName: null,
        hasTargetClusters: false
      }
    ];

    testCases.forEach(function (test) {
      it('should set hasTargetClusters property depending on cluster list', function () {
        mainMirroringEditDataSetView.set('isLoaded', test.isLoaded);
        mainMirroringEditDataSetView.set('targetClusters', test.targetClusters);
        mainMirroringEditDataSetView.set('controller.formFields.datasetTargetClusterName', 'test');
        mainMirroringEditDataSetView.onTargetClustersChange();
        expect(mainMirroringEditDataSetView.get('hasTargetClusters')).to.equal(test.hasTargetClusters);
        expect(mainMirroringEditDataSetView.get('controller.formFields.datasetTargetClusterName')).to.equal(test.targetClusterName);
      });
    });
  });

  describe('fillForm', function () {

    App.store.loadMany(App.Dataset, [
      {
        id: 'test1',
        name: 'test1',
        target_cluster_name: 'testCluster1',
        source_dir: '/testDir1',
        target_dir: '/testDir1',
        frequency: '5',
        frequency_unit: 'days',
        schedule_start_date: new Date('11/29/2014 01:00 AM').toISOString().replace(/\:\d{2}\.\d{3}/, ''),
        schedule_end_date: new Date('11/29/2014 02:00 AM').toISOString().replace(/\:\d{2}\.\d{3}/, '')
      },
      {
        id: 'test2',
        name: 'test2',
        target_cluster_name: 'testCluster2',
        source_dir: '/testDir2',
        target_dir: '/testDir2',
        frequency: '10',
        frequency_unit: 'hours',
        schedule_start_date: new Date('11/20/2014 01:00 AM').toISOString().replace(/\:\d{2}\.\d{3}/, ''),
        schedule_end_date: new Date('11/21/2014 02:00 PM').toISOString().replace(/\:\d{2}\.\d{3}/, '')
      },
      {
        id: 'test3',
        name: 'test3',
        target_cluster_name: 'testCluster3',
        source_dir: '/testDir3',
        target_dir: '/testDir3',
        frequency: '1',
        frequency_unit: 'minutes',
        schedule_start_date: new Date('10/29/2014 01:00 AM').toISOString().replace(/\:\d{2}\.\d{3}/, ''),
        schedule_end_date: new Date('11/29/2015 02:00 AM').toISOString().replace(/\:\d{2}\.\d{3}/, '')
      }
    ]);

    var testCases = [
      {
        datasetName: 'test1',
        datasetSourceDir: '/testDir1',
        datasetTargetDir: '/testDir1',
        datasetTargetClusterName: 'testCluster1',
        datasetFrequency: '5',
        repeatOptionSelected: 'days',
        datasetStartDate: '11/29/14',
        datasetEndDate: '11/29/14',
        hoursForStart: '01',
        hoursForEnd: '02',
        minutesForStart: '00',
        minutesForEnd: '00',
        middayPeriodForStart: 'AM',
        middayPeriodForEnd: 'AM'
      },
      {
        datasetName: 'test2',
        datasetSourceDir: '/testDir2',
        datasetTargetDir: '/testDir2',
        datasetTargetClusterName: 'testCluster2',
        datasetFrequency: '10',
        repeatOptionSelected: 'hours',
        datasetStartDate: '11/20/14',
        datasetEndDate: '11/21/14',
        hoursForStart: '01',
        hoursForEnd: '02',
        minutesForStart: '00',
        minutesForEnd: '00',
        middayPeriodForStart: 'AM',
        middayPeriodForEnd: 'PM'
      },
      {
        datasetName: 'test3',
        datasetSourceDir: '/testDir3',
        datasetTargetDir: '/testDir3',
        datasetTargetClusterName: 'testCluster3',
        datasetFrequency: '1',
        repeatOptionSelected: 'minutes',
        datasetStartDate: '10/29/14',
        datasetEndDate: '11/29/15',
        hoursForStart: '01',
        hoursForEnd: '02',
        minutesForStart: '00',
        minutesForEnd: '00',
        middayPeriodForStart: 'AM',
        middayPeriodForEnd: 'AM'
      }
    ];

    it('should not set form fields if isLoaded is false', function () {
      mainMirroringEditDataSetView.set('isLoaded', false);
      mainMirroringEditDataSetView.fillForm();
      Em.keys(mainMirroringEditDataSetView.get('controller.formFields')).forEach(function (field) {
        expect(mainMirroringEditDataSetView.get('controller.formFields.' + field)).to.be.null;
      });
    });

    it('should not set form fields if controller.isEdit is false', function () {
      mainMirroringEditDataSetView.set('controller.isEdit', false);
      mainMirroringEditDataSetView.fillForm();
      Em.keys(mainMirroringEditDataSetView.get('controller.formFields')).forEach(function (field) {
        expect(mainMirroringEditDataSetView.get('controller.formFields.' + field)).to.be.null;
      });
    });

    testCases.forEach(function (test) {
      it('set appropriate form fields from dataset model', function () {
        mainMirroringEditDataSetView.set('controller.datasetIdToEdit', test.datasetName);
        mainMirroringEditDataSetView.set('controller.isEdit', true);
        mainMirroringEditDataSetView.fillForm();
        Em.keys(mainMirroringEditDataSetView.get('controller.formFields')).forEach(function (field) {
          expect(mainMirroringEditDataSetView.get('controller.formFields.' + field)).to.equal(test[field]);
        });
      });
    });
  });
});
