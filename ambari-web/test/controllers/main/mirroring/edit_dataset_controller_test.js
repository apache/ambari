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

describe('App.MainMirroringEditDataSetController', function () {

  describe('#clearStep', function () {
    it('should clear all fields, error flags and messages', function () {
      var mainMirroringEditDataSetController = App.MainMirroringEditDataSetController.create();
      var errors = mainMirroringEditDataSetController.get('errors');
      var errorMessages = mainMirroringEditDataSetController.get('errorMessages');
      var formFeilds = mainMirroringEditDataSetController.get('formFields');
      formFeilds.set('datasetName', 'test');
      sinon.spy(mainMirroringEditDataSetController, 'clearErrors');
      mainMirroringEditDataSetController.clearStep();
      expect(mainMirroringEditDataSetController.clearErrors.calledOnce).to.be.true;
      Em.keys(formFeilds).forEach(function (field) {
        expect(formFeilds[field]).to.be.null;
      });
      mainMirroringEditDataSetController.clearErrors.restore();
    });
  });

  describe('#clearErrors', function () {
    it('should clear all error messages and flags', function () {
      var mainMirroringEditDataSetController = App.MainMirroringEditDataSetController.create();
      var errors = mainMirroringEditDataSetController.get('errors');
      var errorMessages = mainMirroringEditDataSetController.get('errorMessages');
      Em.keys(errors).forEach(function (error) {
        errors[error] = true;
      }, this);
      Em.keys(errorMessages).forEach(function (errorMessage) {
        errorMessages[errorMessage] = 'test';
      }, this);
      mainMirroringEditDataSetController.clearErrors();
      Em.keys(errors).forEach(function (error) {
        expect(errors[error]).to.be.false;
      });
      Em.keys(errorMessages).forEach(function (errorMessage) {
        expect(errorMessages[errorMessage]).to.be.empty;
      });
    });
  });

  describe('#showAddPopup', function () {
    it('should show popup and set isEdit as false', function () {
      var mainMirroringEditDataSetController = App.MainMirroringEditDataSetController.create();
      sinon.spy(App.ModalPopup, 'show');
      mainMirroringEditDataSetController.showAddPopup();
      expect(App.ModalPopup.show.calledOnce).to.be.true;
      expect(mainMirroringEditDataSetController.get('isEdit')).to.be.false;
      App.ModalPopup.show.restore();
    });
  });

  describe('#showEditPopup', function () {
    it('should show popup,set isEdit as true and set dataset id', function () {
      var mainMirroringEditDataSetController = App.MainMirroringEditDataSetController.create();
      var dataset = Ember.Object.create({
        id: 'test'
      });
      sinon.spy(App.ModalPopup, 'show');
      mainMirroringEditDataSetController.showEditPopup(dataset);
      expect(App.ModalPopup.show.calledOnce).to.equal(true);
      expect(mainMirroringEditDataSetController.get('isEdit')).to.be.true;
      expect(mainMirroringEditDataSetController.get('datasetIdToEdit')).to.equal('test');
      App.ModalPopup.show.restore();
    });
  });

  describe('#showPopup', function () {
    it('should show dataset popup and save its view', function () {
      var mainMirroringEditDataSetController = App.MainMirroringEditDataSetController.create();
      sinon.spy(App.ModalPopup, 'show');
      mainMirroringEditDataSetController.showPopup();
      expect(App.ModalPopup.show.calledOnce).to.equal(true);
      expect(mainMirroringEditDataSetController.get('popup')).to.not.be.empty;
      App.ModalPopup.show.restore();
    });
  });

  describe('#applyValidation', function () {
    it('should add observers to all fields to validate form', function () {
      var mainMirroringEditDataSetController = App.MainMirroringEditDataSetController.create();
      sinon.spy(mainMirroringEditDataSetController, 'validate');
      mainMirroringEditDataSetController.applyValidation();
      expect(mainMirroringEditDataSetController.validate.calledOnce).to.be.true;
      Em.keys(mainMirroringEditDataSetController.get('formFields')).forEach(function (field) {
        expect(mainMirroringEditDataSetController.hasObserverFor('formFields.' + field)).to.be.true;
      });
      mainMirroringEditDataSetController.validate.restore();
    });
  });

  var testCases = [
    {
      day: '01/01/2001',
      hours: '00',
      minutes: '00',
      middayPeriod: 'AM',
      result: new Date('01/01/2001 00:00 AM'),
      message: 'should return date object'
    },
    {
      day: '06/05/2014',
      hours: '12',
      minutes: '59',
      middayPeriod: 'PM',
      result: new Date('06/05/2014 12:59 PM'),
      message: 'should return date object'
    },
    {
      day: '',
      hours: '00',
      minutes: '00',
      middayPeriod: 'AM',
      result: null,
      message: 'should return null if there are empty fields'
    }
  ];

  describe('#scheduleStartDate', function () {
    testCases.forEach(function (test) {
      it(test.message, function () {
        var mainMirroringEditDataSetController = App.MainMirroringEditDataSetController.create({
          formFields: Ember.Object.create({
            datasetStartDate: test.day,
            hoursForStart: test.hours,
            minutesForStart: test.minutes,
            middayPeriodForStart: test.middayPeriod
          })
        });
        expect(mainMirroringEditDataSetController.get('scheduleStartDate')).to.deep.equal(test.result);
      });
    });
  });

  describe('#scheduleEndDate', function () {
    testCases.forEach(function (test) {
      it(test.message, function () {
        var mainMirroringEditDataSetController = App.MainMirroringEditDataSetController.create({
          formFields: Ember.Object.create({
            datasetEndDate: test.day,
            hoursForEnd: test.hours,
            minutesForEnd: test.minutes,
            middayPeriodForEnd: test.middayPeriod
          })
        });
        expect(mainMirroringEditDataSetController.get('scheduleEndDate')).to.deep.equal(test.result);
      });
    });
  });

  var formFields = Ember.Object.create({
    datasetName: 'test',
    datasetTargetClusterName: 'test',
    datasetSourceDir: '/test',
    datasetTargetDir: '/test',
    datasetStartDate: '01/19/2038',
    hoursForStart: '03',
    minutesForStart: '15',
    middayPeriodForStart: 'AM',
    datasetEndDate: '01/19/2039',
    hoursForEnd: '03',
    minutesForEnd: '15',
    middayPeriodForEnd: 'AM',
    datasetFrequency: '1',
    repeatOptionSelected: 'days'
  })

  describe('#validate', function () {
    it('should set an error for empty fields', function () {
      var mainMirroringEditDataSetController = App.MainMirroringEditDataSetController.create();
      mainMirroringEditDataSetController.validate();
      var errors = mainMirroringEditDataSetController.get('errors');
      var errorMessages = mainMirroringEditDataSetController.get('errorMessages');
      Em.keys(errors).forEach(function (error) {
        expect(errors[error]).to.be.true;
      });
      Em.keys(errorMessages).forEach(function (errorMessage) {
        expect(errorMessages[errorMessage]).to.equal(Em.I18n.t('mirroring.required.error'));
      });
    });
    it('should set an error if start date is after end date', function () {
      var mainMirroringEditDataSetController = App.MainMirroringEditDataSetController.create();
      var formFields = mainMirroringEditDataSetController.get('formFields');
      formFields.set('datasetStartDate', '04/07/2014');
      formFields.set('hoursForStart', '11');
      formFields.set('minutesForStart', '00');
      formFields.set('middayPeriodForStart', 'PM');
      formFields.set('datasetEndDate', '04/07/2014');
      formFields.set('hoursForEnd', '11');
      formFields.set('minutesForEnd', '00');
      formFields.set('middayPeriodForEnd', 'AM');
      mainMirroringEditDataSetController.validate();
      expect(mainMirroringEditDataSetController.get('errors.isEndDateError')).to.be.true;
      expect(mainMirroringEditDataSetController.get('errorMessages.endDate')).to.equal(Em.I18n.t('mirroring.dateOrder.error'));
    });
    it('should set an error if start date is in the past', function () {
      var mainMirroringEditDataSetController = App.MainMirroringEditDataSetController.create();
      var formFields = mainMirroringEditDataSetController.get('formFields');
      formFields.set('datasetStartDate', '04/07/2014');
      formFields.set('hoursForStart', '11');
      formFields.set('minutesForStart', '00');
      formFields.set('middayPeriodForStart', 'AM');
      mainMirroringEditDataSetController.validate();
      expect(mainMirroringEditDataSetController.get('errors.isStartDateError')).to.be.true;
      expect(mainMirroringEditDataSetController.get('errorMessages.startDate')).to.equal(Em.I18n.t('mirroring.startDate.error'));
    });
    it('should set an error if repeat field value consist not only form digits', function () {
      var mainMirroringEditDataSetController = App.MainMirroringEditDataSetController.create();
      var formFields = mainMirroringEditDataSetController.get('formFields');
      formFields.set('datasetFrequency', 'test');
      mainMirroringEditDataSetController.validate();
      expect(mainMirroringEditDataSetController.get('errors.isFrequencyError')).to.be.true;
      expect(mainMirroringEditDataSetController.get('errorMessages.frequency')).to.equal(Em.I18n.t('mirroring.required.invalidNumberError'));
      formFields.set('datasetFrequency', '100test');
      mainMirroringEditDataSetController.validate();
      expect(mainMirroringEditDataSetController.get('errors.isFrequencyError')).to.be.true;
      expect(mainMirroringEditDataSetController.get('errorMessages.frequency')).to.equal(Em.I18n.t('mirroring.required.invalidNumberError'));
    });
    it('should not set errors if all fields are filled correctly', function () {
      var mainMirroringEditDataSetController = App.MainMirroringEditDataSetController.create({
        formFields: formFields
      });
      mainMirroringEditDataSetController.validate();
      var errors = mainMirroringEditDataSetController.get('errors');
      var errorMessages = mainMirroringEditDataSetController.get('errorMessages');
      Em.keys(errors).forEach(function (error) {
        expect(errors[error]).to.be.false;
      });
      Em.keys(errorMessages).forEach(function (errorMessage) {
        expect(errorMessages[errorMessage]).to.be.empty;
      });
    });
  });

  describe('#addZero', function () {
    it('should add 0 for numbers less than 10', function () {
      var mainMirroringEditDataSetController = App.MainMirroringEditDataSetController.create();
      expect(mainMirroringEditDataSetController.addZero(1)).to.equal('01');
      expect(mainMirroringEditDataSetController.addZero(9)).to.equal('09');
      expect(mainMirroringEditDataSetController.addZero(0)).to.equal('00');
    });
    it('should not add 0 for numbers greater than 9', function () {
      var mainMirroringEditDataSetController = App.MainMirroringEditDataSetController.create();
      expect(mainMirroringEditDataSetController.addZero(10)).to.equal('10');
      expect(mainMirroringEditDataSetController.addZero(99)).to.equal('99');
    });
  });

  describe('#toTZFormat', function () {
    it('should convert date to TZ format', function () {
      var mainMirroringEditDataSetController = App.MainMirroringEditDataSetController.create();
      expect(mainMirroringEditDataSetController.toTZFormat(new Date(Date.UTC(2014, 0, 1, 1, 1)))).to.equal('2014-01-01T01:01Z');
      expect(mainMirroringEditDataSetController.toTZFormat(new Date(Date.UTC(2014, 11, 31, 23, 59)))).to.equal('2014-12-31T23:59Z');
    });
  });

  describe('#toAMPMHours', function () {
    it('should convert time to 12-hours format', function () {
      var mainMirroringEditDataSetController = App.MainMirroringEditDataSetController.create();
      expect(mainMirroringEditDataSetController.toAMPMHours(13)).to.equal('01');
      expect(mainMirroringEditDataSetController.toAMPMHours(20)).to.equal('08');
      expect(mainMirroringEditDataSetController.toAMPMHours(24)).to.equal('12');
      expect(mainMirroringEditDataSetController.toAMPMHours(0)).to.equal('12');
    });
    it('should not convert time if argument is less than 12', function () {
      var mainMirroringEditDataSetController = App.MainMirroringEditDataSetController.create();
      expect(mainMirroringEditDataSetController.toAMPMHours(1)).to.equal('01');
      expect(mainMirroringEditDataSetController.toAMPMHours(8)).to.equal('08');
      expect(mainMirroringEditDataSetController.toAMPMHours(11)).to.equal('11');
    });
  });

  describe('#save', function () {
    it('should create XML and send it to server', function () {
      var mainMirroringEditDataSetController = App.MainMirroringEditDataSetController.create({
        popup: Ember.Object.create({
          isSaving: false
        })
      });
      sinon.spy(mainMirroringEditDataSetController, 'createDatasetXML');
      sinon.spy(mainMirroringEditDataSetController, 'sendDatasetToServer');
      mainMirroringEditDataSetController.save();
      expect(mainMirroringEditDataSetController.createDatasetXML.calledOnce).to.be.true;
      expect(mainMirroringEditDataSetController.sendDatasetToServer.calledOnce).to.be.true;
      expect(mainMirroringEditDataSetController.get('popup.isSaving')).to.be.true;
      mainMirroringEditDataSetController.createDatasetXML.restore();
      mainMirroringEditDataSetController.sendDatasetToServer.restore();
    });
  });

  describe('#createDatasetXML', function () {
    it('should create XML-fromatted data', function () {
      var mainMirroringEditDataSetController = App.MainMirroringEditDataSetController.create({
        formFields: formFields
      });
      var startDate = new Date('01/19/2038 03:15 AM').toISOString().replace(/\:\d{2}\.\d{3}/, '');
      var endDate = new Date('01/19/2039 03:15 AM').toISOString().replace(/\:\d{2}\.\d{3}/, '');
      var expectedResult = '<?xml version="1.0"?><feed description="" name="' + App.mirroringDatasetNamePrefix + 'test" xmlns="uri:falcon:feed:0.1"><frequency>days(1)' +
        '</frequency><clusters><cluster name="' + App.get('clusterName') + '" type="source"><validity start="' + startDate + '" end="' + endDate +
        '"/><retention limit="days(7)" action="delete"/></cluster><cluster name="test" type="target"><validity start="' + startDate + '" end="' + endDate +
        '"/><retention limit="months(1)" action="delete"/><locations><location type="data" path="/test" /></locations></cluster></clusters><locations><location type="data" path="' +
        '/test" /></locations><ACL owner="hue" group="users" permission="0755" /><schema location="/none" provider="none"/></feed>';
      var result = mainMirroringEditDataSetController.createDatasetXML();
      expect(result).to.equal(expectedResult);
    });
  });

  describe('#sendDatasetToServer', function () {
    var mainMirroringEditDataSetController = App.MainMirroringEditDataSetController.create({
      formFields: formFields
    });
    beforeEach(function () {
      sinon.stub(App.ajax, 'send', Em.K);
    });
    afterEach(function () {
      App.ajax.send.restore();
    });
    it('should send data with correct dataset name', function () {
      mainMirroringEditDataSetController.sendDatasetToServer('test');
      expect(App.ajax.send.args[0][0].data.name).to.equal(App.mirroringDatasetNamePrefix + formFields.datasetName);
    });
    it('should send data from param', function () {
      mainMirroringEditDataSetController.sendDatasetToServer('test');
      expect(App.ajax.send.args[0][0].data.entity).to.equal('test');
    });
    it('should use edit request if isEdit is true', function () {
      mainMirroringEditDataSetController.set('isEdit', true);
      mainMirroringEditDataSetController.sendDatasetToServer('test');
      expect(App.ajax.send.args[0][0].name).to.equal('mirroring.update_entity');
    });
    it('should use create request if isEdit is false', function () {
      mainMirroringEditDataSetController.set('isEdit', false);
      mainMirroringEditDataSetController.sendDatasetToServer('test');
      expect(App.ajax.send.args[0][0].name).to.equal('mirroring.create_new_dataset');
    });
  });

  describe('#onSaveSuccess', function () {
    it('should hide popup and load data', function () {
      var mainMirroringEditDataSetController = App.MainMirroringEditDataSetController.create({
        popup: Ember.Object.create({
          isSaving: true,
          hide: function () {
          }
        })
      });
      App.router.set('mainMirroringController', Ember.Object.create({
        loadData: function () {
        }
      }));
      sinon.spy(mainMirroringEditDataSetController.get('popup'), 'hide');
      sinon.spy(App.router.get('mainMirroringController'), 'loadData');
      mainMirroringEditDataSetController.onSaveSuccess();
      expect(mainMirroringEditDataSetController.get('popup.isSaving')).to.be.false;
      expect(App.router.get('mainMirroringController').loadData.calledOnce).to.be.true;
      expect(mainMirroringEditDataSetController.get('popup').hide.calledOnce).to.be.true;
      mainMirroringEditDataSetController.get('popup').hide.restore();
      App.router.get('mainMirroringController').loadData.restore();
    });
  });

  describe('#onSaveError', function () {
    var mainMirroringEditDataSetController;
    beforeEach(function () {
      mainMirroringEditDataSetController = App.MainMirroringEditDataSetController.create({
        popup: Ember.Object.create({
          isSaving: true
        })
      });
      sinon.stub(App, 'showAlertPopup', Em.K);
    });
    afterEach(function () {
      App.showAlertPopup.restore();
    });
    it('shouldn\'t show error popup and enable button', function () {
      mainMirroringEditDataSetController.onSaveError(null);
      expect(App.showAlertPopup.calledOnce).to.be.false;
      expect(mainMirroringEditDataSetController.get('popup.isSaving')).to.be.false;
    });
    it('should show error popup and enable button', function () {
      mainMirroringEditDataSetController.onSaveError({responseText: '<message>test</message>'});
      expect(App.showAlertPopup.args[0][1]).to.be.equal(Em.I18n.t('mirroring.manageClusters.error') + ': test');
      expect(mainMirroringEditDataSetController.get('popup.isSaving')).to.be.false;
    });
  });

  describe('#saveDisabled', function () {
    it('should return true if there are some errors', function () {
      var mainMirroringEditDataSetController = App.MainMirroringEditDataSetController.create();
      mainMirroringEditDataSetController.set('errors.isNameError', true);
      expect(mainMirroringEditDataSetController.get('saveDisabled')).to.be.true;
    });
  });
});
