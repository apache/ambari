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

App.MainMirroringEditDataSetController = Ember.Controller.extend({
  name: 'mainMirroringEditDataSetController',

  /**
   * Defines to show Edit Dataset or Create New Dataset popup
   * @type {Boolean}
   */
  isEdit: false,

  /**
   * Contains Dataset id if <code>isEdit</code> is true
   * @type {Boolean}
   */
  datasetIdToEdit: null,

  /**
   * Fields values from Edit DataSet form
   * @type {Object}
   */
  formFields: Ember.Object.create({
    datasetName: null,
    datasetTargetClusterName: null,
    datasetSourceDir: null,
    datasetTargetDir: null,
    datasetStartDate: null,
    hoursForStart: null,
    minutesForStart: null,
    middayPeriodForStart: null,
    datasetEndDate: null,
    hoursForEnd: null,
    minutesForEnd: null,
    middayPeriodForEnd: null,
    datasetFrequency: null,
    repeatOptionSelected: null
  }),

  /**
   * Messages for errors occurred during Edit DataSet form validation
   * @type {Object}
   */
  errorMessages: Ember.Object.create({
    name: '',
    sourceDir: '',
    targetDir: '',
    startDate: '',
    endDate: '',
    frequency: '',
    targetClusterName: ''
  }),

  /**
   * Flags with errors related to each field in Edit/Create Dataset form
   * @type {Object}
   */
  errors: Ember.Object.create({
    isNameError: false,
    isSourceDirError: false,
    isTargetDirError: false,
    isStartDateError: false,
    isEndDateError: false,
    isFrequencyError: false,
    isTargetClusterNameError: false
  }),

  /**
   * Clear all fields in Edit/Create Dataset form and clears all errors
   */
  clearStep: function () {
    var formFields = this.get('formFields');
    Em.keys(formFields).forEach(function (key) {
      formFields.set(key, null);
    }, this);
    this.clearErrors();
  },

  /**
   * Clear all error flags and messages
   */
  clearErrors: function () {
    var errorMessages = this.get('errorMessages');
    Em.keys(errorMessages).forEach(function (key) {
      errorMessages.set(key, '');
    }, this);
    var errors = this.get('errors');
    Em.keys(errors).forEach(function (key) {
      errors.set(key, false);
    }, this);
  },

  /**
   * Show Create New Dataset popup
   * @return {Object} popup view
   */
  showAddPopup: function () {
    var popup = this.showPopup(Em.I18n.t('mirroring.dataset.newDataset'));
    this.set('isEdit', false);
    return popup;
  },

  /**
   * Show Edit Dataset popup
   * @return {Object} popup view
   */
  showEditPopup: function (dataset) {
    this.set('datasetIdToEdit', dataset.get('id'));
    var popup = this.showPopup(Em.I18n.t('mirroring.dataset.editDataset'));
    this.set('isEdit', true);
    return popup;
  },

  /**
   * Show popup with Dataset form fields
   * @return {Object} popup view
   */
  showPopup: function (header) {
    var self = this;
    var popup = App.ModalPopup.show({
      classNames: ['sixty-percent-width-modal'],
      header: header,
      primary: Em.I18n.t('mirroring.dataset.save'),
      secondary: Em.I18n.t('common.cancel'),
      showCloseButton: false,
      isSaving: false,
      saveDisabled: function () {
        return self.get('saveDisabled');
      }.property('App.router.' + self.get('name') + '.saveDisabled'),
      disablePrimary: function () {
        return this.get('saveDisabled') || this.get('isSaving');
      }.property('saveDisabled', 'isSaving'),
      onPrimary: function () {
        // Apply form validation for first click
        if (!this.get('primaryWasClicked')) {
          this.toggleProperty('primaryWasClicked');
          self.applyValidation();
          if (this.get('saveDisabled')) {
            return false;
          }
        }
        self.save();
        App.router.transitionTo('main.mirroring.index');
      },
      primaryWasClicked: false,
      onSecondary: function () {
        this.hide();
        App.router.send('gotoShowJobs');
      },
      bodyClass: App.MainMirroringEditDataSetView.extend({
        controller: self
      })
    });
    this.set('popup', popup);
    return popup;
  },

  /**
   * Set observer to call validate method if any property from formFields will change
   */
  applyValidation: function () {
    Em.keys(this.get('formFields')).forEach(function (key) {
      this.addObserver('formFields.' + key, this, 'validate');
    }, this);
    this.validate();
  },

  /**
   * Return date object calculated from appropriate fields
   * @type {Date}
   */
  scheduleStartDate: function () {
    var startDate = this.get('formFields.datasetStartDate');
    var hoursForStart = this.get('formFields.hoursForStart');
    var minutesForStart = this.get('formFields.minutesForStart');
    var middayPeriodForStart = this.get('formFields.middayPeriodForStart');
    if (startDate && hoursForStart && minutesForStart && middayPeriodForStart) {
      return new Date(startDate + ' ' + hoursForStart + ':' + minutesForStart + ' ' + middayPeriodForStart);
    }
    return null;
  }.property('formFields.datasetStartDate', 'formFields.hoursForStart', 'formFields.minutesForStart', 'formFields.middayPeriodForStart'),

  /**
   * Return date object calculated from appropriate fields
   * @type {Date}
   */
  scheduleEndDate: function () {
    var endDate = this.get('formFields.datasetEndDate');
    var hoursForEnd = this.get('formFields.hoursForEnd');
    var minutesForEnd = this.get('formFields.minutesForEnd');
    var middayPeriodForEnd = this.get('formFields.middayPeriodForEnd');
    if (endDate && hoursForEnd && minutesForEnd && middayPeriodForEnd) {
      return new Date(endDate + ' ' + hoursForEnd + ':' + minutesForEnd + ' ' + middayPeriodForEnd);
    }
    return null;
  }.property('formFields.datasetEndDate', 'formFields.hoursForEnd', 'formFields.minutesForEnd', 'formFields.middayPeriodForEnd'),


  /**
   * Validation for every field in Edit DataSet form
   */
  validate: function () {
    var formFields = this.get('formFields');
    var errors = this.get('errors');
    var errorMessages = this.get('errorMessages');
    this.clearErrors();
    // Check if feild is empty
    Em.keys(errorMessages).forEach(function (key) {
      if (!formFields.get('dataset' + key.capitalize())) {
        errors.set('is' + key.capitalize() + 'Error', true);
        errorMessages.set(key, Em.I18n.t('mirroring.required.error'));
      }
    }, this);
    // Check that endDate is after startDate
    var scheduleStartDate = this.get('scheduleStartDate');
    var scheduleEndDate = this.get('scheduleEndDate');
    if (scheduleStartDate && scheduleEndDate && (scheduleStartDate > scheduleEndDate)) {
      errors.set('isEndDateError', true);
      errorMessages.set('endDate', Em.I18n.t('mirroring.dateOrder.error'));
    }
    // Check that startDate is after current date
    if (scheduleStartDate && !this.get('isEdit') && new Date(App.dateTime()) > scheduleStartDate) {
      errors.set('isStartDateError', true);
      errorMessages.set('startDate', Em.I18n.t('mirroring.startDate.error'));
    }
    // Check that repeat field value consists only from digits
    if (isNaN(this.get('formFields.datasetFrequency'))) {
      errors.set('isFrequencyError', true);
      errorMessages.set('frequency', Em.I18n.t('mirroring.required.invalidNumberError'));
    }
  },


  /**
   * Add '0' for numbers less than 10
   * @param {Number|String} number
   * @return {String}
   */
  addZero: function (number) {
    return ('0' + number).slice(-2);
  },

  /**
   * Convert date to TZ format
   * @param {Date} date
   * @return {String}
   */
  toTZFormat: function (date) {
    return date.toISOString().replace(/\:\d{2}\.\d{3}/,'');
  },

  /**
   * Converts hours value from 24-hours format to AM/PM format
   * @param {Number|String} hours
   * @return {String}
   */
  toAMPMHours: function (hours) {
    var result = hours % 12;
    result = result ? result : 12;
    return this.addZero(result);
  },

  /**
   * Save data from dataset form to server
   */
  save: function () {
    this.set('popup.isSaving', true);
    var datasetXML = this.createDatasetXML();
    this.sendDatasetToServer(datasetXML);
  },

  /**
   * Compose XML-object from populated dataset form fields
   * @return {String}
   */
  createDatasetXML: function () {
    var datasetNamePrefix = App.get('mirroringDatasetNamePrefix');
    var datasetName = this.get('formFields.datasetName');
    var prefixedDatasetName = datasetNamePrefix + datasetName;
    var sourceCluster = App.get('clusterName');
    var targetCluster = this.get('formFields.datasetTargetClusterName');
    var sourceDir = this.get('formFields.datasetSourceDir');
    var targetDir = this.get('formFields.datasetTargetDir');
    var datasetFrequency = this.get('formFields.datasetFrequency');
    var repeatOptionSelected = this.get('formFields.repeatOptionSelected');
    var startDate = this.get('scheduleStartDate');
    var endDate = this.get('scheduleEndDate');
    var scheduleStartDateFormatted = this.toTZFormat(startDate);
    var scheduleEndDateFormatted = this.toTZFormat(endDate);

    return '<?xml version="1.0"?><feed description="" name="' + prefixedDatasetName + '" xmlns="uri:falcon:feed:0.1"><frequency>' + repeatOptionSelected + '(' + datasetFrequency + ')' +
        '</frequency><clusters><cluster name="' + sourceCluster + '" type="source"><validity start="' + scheduleStartDateFormatted + '" end="' + scheduleEndDateFormatted +
        '"/><retention limit="days(7)" action="delete"/></cluster><cluster name="' + targetCluster + '" type="target"><validity start="' + scheduleStartDateFormatted + '" end="' + scheduleEndDateFormatted +
        '"/><retention limit="months(1)" action="delete"/><locations><location type="data" path="' + targetDir + '" /></locations></cluster></clusters><locations><location type="data" path="' +
        sourceDir + '" /></locations><ACL owner="hue" group="users" permission="0755" /><schema location="/none" provider="none"/></feed>';
  },

  /**
   * Send dataset XML-data to server
   * @param {String} datasetXML
   */
  sendDatasetToServer: function (datasetXML) {
    var datasetNamePrefix = App.get('mirroringDatasetNamePrefix');
    var datasetName = this.get('formFields.datasetName');
    var prefixedDatasetName = datasetNamePrefix + datasetName;
    return App.ajax.send({
      name: this.get('isEdit') ? 'mirroring.update_entity' : 'mirroring.create_new_dataset',
      sender: this,
      data: {
        name: prefixedDatasetName,
        type: 'feed',
        entity: datasetXML,
        falconServer: App.get('falconServerURL')
      },
      success: 'onSaveSuccess',
      error: 'onSaveError'
    });
  },

  /**
   * Callback for success saving XML-data on server
   */
  onSaveSuccess: function () {
    this.set('popup.isSaving', false);
    this.get('popup').hide();
    App.router.get('mainMirroringController').loadData();
  },

  /**
   * Callback for error while saving XML-data on server
   */
  onSaveError: function (response) {
    this.set('popup.isSaving', false);
    if (response && response.responseText) {
      var errorMessage = /(?:\<message\>)((.|\n)+)(?:\<\/message\>)/.exec(response.responseText);
      if (errorMessage.length > 1) {
        App.showAlertPopup(Em.I18n.t('common.error'), Em.I18n.t('mirroring.manageClusters.error') + ': ' + errorMessage[1]);
      }
    }
  },

  /**
   * Defines if save button should be disabled
   * @type {Boolean}
   */
  saveDisabled: function () {
    var errors = this.get('errors');
    return errors.get('isNameError') || errors.get('isSourceDirError') || errors.get('isTargetDirError') || errors.get('isStartDateError') || errors.get('isEndDateError') || errors.get('isFrequencyError') || errors.get('isTargetClusterNameError');
  }.property('errors.isNameError', 'errors.isSourceDirError', 'errors.isTargetDirError', 'errors.isStartDateError', 'errors.isEndDateError', 'errors.isFrequencyError', 'errors.isTargetClusterNameError')
});
