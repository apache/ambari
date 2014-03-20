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

App.MainJobsController = Em.ArrayController.extend({

  name:'mainJobsController',

  content: [],

  loaded : false,
  loading : false,
  loadJobsTimeout: null,
  loadTimeout: null,
  jobsUpdateInterval: 6000,
  jobsUpdate: null,
  sortingColumn: null,

  updateJobs: function (controllerName, funcName) {
    clearInterval(this.get('jobsUpdate'));
    var self = this;
    var interval = setInterval(function () {
      App.router.get(controllerName)[funcName]();
    }, this.jobsUpdateInterval);
    this.set('jobsUpdate', interval);
  },

  totalOfJobs: 0,
  setTotalOfJobs: function () {
    if(this.get('totalOfJobs') < this.get('content.length')){
      this.set('totalOfJobs', this.get('content.length'));
    }
  }.observes('content.length'),

  filterObject: Ember.Object.create({
    id: "",
    isIdFilterApplied: false,
    jobsLimit: -1,
    user: "",
    windowStart: "",
    windowEnd: "",

    onApplyIdFilter: function () {
      if(this.get('id') == ""){
        this.set('isIdFilterApplied', false);
      }else{
        this.set('isIdFilterApplied', true);
      }
    }.observes('id'),

    /**
     * Direct binding to startTime filter field
     */
    startTime: "",
    onStartTimeChange:function(){
      var time = "";
      var curTime = new Date().getTime();
      switch (this.get('startTime')) {
        case 'Past 1 hour':
          time = curTime - 3600000;
          break;
        case 'Past 1 Day':
          time = curTime - 86400000;
          break;
        case 'Past 2 Days':
          time = curTime - 172800000;
          break;
        case 'Past 7 Days':
          time = curTime - 604800000;
          break;
        case 'Past 14 Days':
          time = curTime - 1209600000;
          break;
        case 'Past 30 Days':
          time = curTime - 2592000000;
          break;
        case 'Custom':
          this.showCustomDatePopup();
          break;
        case 'Any':
          time = "";
          break;
      }
      if(this.get('startTime') != "Custom"){
        this.set("windowStart", time);
        this.set("windowEnd", "");
      }
    }.observes("startTime"),

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

    showCustomDatePopup: function () {
      var self = this;
      var windowEnd = "";
      var windowStart = "";
      App.ModalPopup.show({
        header: Em.I18n.t('jobs.table.custom.date.header'),
        onPrimary: function () {
          self.validate();
          if(self.get('errors.isStartDateError') || self.get('errors.isEndDateError')){
            return false;
          }

          var windowStart = self.createCustomStartDate();
          var windowEnd = self.createCustomEndDate();

          self.set("windowStart", windowStart.getTime());
          self.set("windowEnd", windowEnd.getTime());
          this.hide();
        },
        onSecondary: function () {
          self.set('startTime','Any');
          this.hide();
        },
        bodyClass: App.JobsCustomDatesSelectView.extend({
          controller: self
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
    },

    /**
     * Create link for server request
     * @return {String}
     */
    createJobsFiltersLink: function() {
      var link = "?fields=events,primaryfilters,otherinfo";

      if(this.get("id") !== "") {
        link = "/" + this.get("id") + link;
      }
      if(this.get("jobsLimit") != -1){
        link += "&limit=" + this.get("jobsLimit");
      }
      if(this.get("user") !== ""){
        link += "&primaryFilter=user:" + this.get("user");
      }
      if(this.get("startTime") !== ""){
        link += this.get("windowStart") !== "" ? ("&windowStart=" + this.get("windowStart")) : "";
        link += this.get("windowEnd") !== "" ? ("&windowEnd=" + this.get("windowEnd")) : "";
      }
      return link;
    }
  }),

  columnsName: Ember.ArrayController.create({
    content: [
      { name: Em.I18n.t('jobs.column.id'), index: 0 },
      { name: Em.I18n.t('jobs.column.user'), index: 1 },
      { name: Em.I18n.t('jobs.column.start.time'), index: 2 },
      { name: Em.I18n.t('jobs.column.end.time'), index: 3 },
      { name: Em.I18n.t('jobs.column.duration'), index: 4 }
    ]
  }),

  loadJobs : function() {
    var self = this;
    var timeout = this.get('loadTimeout');
    var yarnService = App.YARNService.find().objectAt(0);
    if (yarnService != null) {
      this.set('loading', true);
      var historyServerHostName = yarnService.get('appTimelineServerNode.hostName');
      var filtersLink = this.get('filterObject').createJobsFiltersLink();
      var hiveQueriesUrl = App.testMode ? "/data/jobs/hive-queries.json" : "/proxy?url=http://" + historyServerHostName
          + ":" + yarnService.get('ahsWebPort') + "/ws/v1/timeline/HIVE_QUERY_ID" + filtersLink;
      App.HttpClient.get(hiveQueriesUrl, App.hiveJobsMapper, {
        complete : function(jqXHR, textStatus) {
          var sortColumn = self.get('sortingColumn');
          if(sortColumn && sortColumn.get('status')){
            var sortColumnStatus = sortColumn.get('status');
            sortColumn.get('parentView').set('content', self.get('content'));
            sortColumn.get('parentView').sort(sortColumn, sortColumnStatus === "sorting_desc");
            sortColumn.set('status', sortColumnStatus);
            self.set('content',sortColumn.get('parentView').get('content'));
          }
          self.set('loading', false);
          self.set('loaded', true);
        }
      }, function (jqXHR, textStatus) {
        App.hiveJobsMapper.map({entities : []});
      });
    }else{
      clearTimeout(timeout);
      timeout = setTimeout(function(){
        self.loadJobs();
      }, 300);
    }
  },

  refreshLoadedJobs : function() {
    var timeout = this.get('loadJobsTimeout');
    var self = this;

    clearTimeout(timeout);
    timeout = setTimeout(function(){
      self.loadJobs();
    }, 300);

    this.set('loadJobsTimeout', timeout);
  }.observes(
      'filterObject.id',
      'filterObject.jobsLimit',
      'filterObject.user',
      'filterObject.windowStart',
      'filterObject.windowEnd'
  )
})
