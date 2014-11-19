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

module.exports = Em.Object.create({

  // Fields values from Select Custom Dates form
  customDateFormFields: Ember.Object.create({
    startDate: null,
    hoursForStart: null,
    minutesForStart: null,
    middayPeriodForStart: null,
    endDate: null,
    hoursForEnd: null,
    minutesForEnd: null,
    middayPeriodForEnd: null
  }),

  errors: Ember.Object.create({
    isStartDateError: false,
    isEndDateError: false
  }),

  errorMessages: Ember.Object.create({
    startDate: '',
    endDate: ''
  }),

  showCustomDatePopup: function (context) {
    var self = this;

    return App.ModalPopup.show({
      header: Em.I18n.t('jobs.table.custom.date.header'),
      onPrimary: function () {
        self.validate();
        if(self.get('errors.isStartDateError') || self.get('errors.isEndDateError')) {
          return false;
        }

        var windowStart = self.createCustomStartDate();
        var windowEnd = self.createCustomEndDate();
        context.set('actualValues', {
          endTime: windowEnd.getTime(),
          startTime: windowStart.getTime()
        });
        this.hide();
      },
      onSecondary: function () {
        context.cancel();
        this.hide();
      },
      bodyClass: App.JobsCustomDatesSelectView.extend({
        controller: self,
        validationErrors: self.get('errorMessages'),
        isValid: self.get('errors')
      })
    });
  },

  createCustomStartDate : function () {
    var startDate = this.get('customDateFormFields.startDate');
    var hoursForStart = this.get('customDateFormFields.hoursForStart');
    var minutesForStart = this.get('customDateFormFields.minutesForStart');
    var middayPeriodForStart = this.get('customDateFormFields.middayPeriodForStart');
    if (startDate && hoursForStart && minutesForStart && middayPeriodForStart) {
      return new Date(startDate + ' ' + hoursForStart + ':' + minutesForStart + ' ' + middayPeriodForStart);
    }
    return null;
  },

  createCustomEndDate : function () {
    var endDate = this.get('customDateFormFields.endDate');
    var hoursForEnd = this.get('customDateFormFields.hoursForEnd');
    var minutesForEnd = this.get('customDateFormFields.minutesForEnd');
    var middayPeriodForEnd = this.get('customDateFormFields.middayPeriodForEnd');
    if (endDate && hoursForEnd && minutesForEnd && middayPeriodForEnd) {
      return new Date(endDate + ' ' + hoursForEnd + ':' + minutesForEnd + ' ' + middayPeriodForEnd);
    }
    return null;
  },

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

  // Validation for every field in customDateFormFields
  validate: function () {
    var formFields = this.get('customDateFormFields');
    var errors = this.get('errors');
    var errorMessages = this.get('errorMessages');
    this.clearErrors();
    // Check if feild is empty
    Em.keys(errorMessages).forEach(function (key) {
      if (!formFields.get(key)) {
        errors.set('is' + key.capitalize() + 'Error', true);
        errorMessages.set(key, Em.I18n.t('jobs.customDateFilter.error.required'));
      }
    }, this);
    // Check that endDate is after startDate
    var startDate = this.createCustomStartDate();
    var endDate = this.createCustomEndDate();
    if (startDate && endDate && (startDate > endDate)) {
      errors.set('isEndDateError', true);
      errorMessages.set('endDate', Em.I18n.t('jobs.customDateFilter.error.date.order'));
    }
  }
});
