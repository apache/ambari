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

App.MainJobsController = Em.Controller.extend({
/*
 * https://github.com/emberjs/ember.js/issues/1221 prevents this controller
 * from being an Ember.ArrayController. Doing so will keep the UI flashing
 * whenever any of the 'sortProperties' or 'sortAscending' properties are set.
 * 
 *  To bypass this issue this controller will be a regular controller. Also,
 *  for memory-leak issues and sorting purposes, we are decoupling the backend
 *  model and the UI model. There will be simple Ember POJOs for the UI which
 *  will be periodically updated from backend Jobs model. 
 */
  
  name:'mainJobsController',

  /**
   * Unsorted ArrayProxy
   */
  content: App.HiveJob.find(),
  
  /**
   * Sorted ArrayProxy
   */
  sortedContent: [],

  contentAndSortObserver : function() {
    Ember.run.once(this, 'contentAndSortUpdater');
  }.observes('content.length', 'content.@each.id', 'content.@each.startTime', 'content.@each.endTime', 'sortProperties', 'sortAscending'),
  
  contentAndSortUpdater: function() {
    this.set('sortingDone', false);
    var content = this.get('content');
    var sortedContent = content.toArray();
    var sortProperty = this.get('sortProperty');
    var sortAscending = this.get('sortAscending');
    sortedContent.sort(function(r1, r2) {
      var r1id = r1.get(sortProperty);
      var r2id = r2.get(sortProperty);
      if (r1id < r2id)
        return sortAscending ? -1 : 1;
      if (r1id > r2id)
        return sortAscending ? 1 : -1;
      return 0;
    });
    var sortedArray = this.get('sortedContent');
    var count = 0;
    sortedContent.forEach(function(sortedJob){
      if(sortedArray.length <= count) {
        sortedArray.pushObject(Ember.Object.create());
      }
      sortedArray[count].set('failed', sortedJob.get('failed'));
      sortedArray[count].set('hasTezDag', sortedJob.get('hasTezDag'));
      sortedArray[count].set('queryText', sortedJob.get('queryText'));
      sortedArray[count].set('name', sortedJob.get('name'));
      sortedArray[count].set('user', sortedJob.get('user'));
      sortedArray[count].set('id', sortedJob.get('id'));
      sortedArray[count].set('startTimeDisplay', sortedJob.get('startTimeDisplay'));
      sortedArray[count].set('endTimeDisplay', sortedJob.get('endTimeDisplay'));
      sortedArray[count].set('durationDisplay', sortedJob.get('durationDisplay'));
      count ++;
    });
    if(sortedArray.length > count) {
      for(var c = sortedArray.length-1; c >= count; c--){
        sortedArray.removeObject(sortedArray[c]);
      }
    }
    sortedContent.length = 0;
    this.set('sortingDone', true);
  },

  navIDs: {
    backIDs: [],
    nextID: ''
  },
  lastJobID: '',
  hasNewJobs: false,
  loaded : false,
  loading : false,
  resetPagination: false,
  loadJobsTimeout: null,
  loadTimeout: null,
  jobsUpdateInterval: 6000,
  jobsUpdate: null,
  sortingColumn: null,
  sortProperty: 'id',
  sortAscending: true,
  sortingDone: true,

  sortingColumnObserver: function () {
    if(this.get('sortingColumn')){
      this.set('sortProperty', this.get('sortingColumn').get('name'));
      this.set('sortAscending', this.get('sortingColumn').get('status') == "sorting_desc" ? false : true );
    }
  }.observes('sortingColumn.name','sortingColumn.status'),

  updateJobsByClick: function () {
    this.set('navIDs.backIDs', []);
    this.set('navIDs.nextID', '');
    this.get('filterObject').set('nextFromId', '');
    this.get('filterObject').set('backFromId', '');
    this.get('filterObject').set('fromTs', '');
    this.set('hasNewJobs', false);
    this.set('resetPagination', true);
    this.loadJobs();
  },

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
    jobsLimit: 10,
    user: "",
    windowStart: "",
    windowEnd: "",
    nextFromId: "",
    backFromId: "",
    fromTs: "",
    isAnyFilterApplied: false,

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
      var numberOfAppliedFilters = 0;

      if(this.get("id") !== "") {
        link = "/" + this.get("id") + link;
        numberOfAppliedFilters++;
      }

      link += "&limit=" + (parseInt(this.get("jobsLimit")) + 1);

      if(this.get("user") !== ""){
        link += "&primaryFilter=user:" + this.get("user");
        numberOfAppliedFilters++;
      }
      if(this.get("backFromId") != ""){
        link += "&fromId=" + this.get("backFromId");
      }
      if(this.get("nextFromId") != ""){
        link += "&fromId=" + this.get("nextFromId");
      }
      if(this.get("fromTs") != ""){
        link += "&fromTs=" + this.get("fromTs");
      }
      if(this.get("startTime") !== "" && this.get("startTime") !== "Any"){
        link += this.get("windowStart") !== "" ? ("&windowStart=" + this.get("windowStart")) : "";
        link += this.get("windowEnd") !== "" ? ("&windowEnd=" + this.get("windowEnd")) : "";
        numberOfAppliedFilters++;
      }

      if(numberOfAppliedFilters > 0){
        this.set('isAnyFilterApplied', true);
      }else{
        this.set('isAnyFilterApplied', false);
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

  lastIDSuccessCallback: function(data, jqXHR, textStatus) {
    var lastReceivedID = data.entities[0].entity;
    if(this.get('lastJobID') == '') {
      this.set('lastJobID', lastReceivedID);
    } else if (this.get('lastJobID') !== lastReceivedID) {
      this.set('lastJobID', lastReceivedID);
      this.set('hasNewJobs', true);
    }
  },

  lastIDErrorCallback: function(data, jqXHR, textStatus) {
    console.debug(jqXHR);
  },

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
      App.ajax.send({
        name: 'jobs.lastID',
        sender: self,
        data: {
          historyServerHostName: historyServerHostName,
          ahsWebPort: yarnService.get('ahsWebPort')
        },
        success: 'lastIDSuccessCallback',
        error : 'lastIDErrorCallback'
      }),
      App.HttpClient.get(hiveQueriesUrl, App.hiveJobsMapper, {
        complete : function(data, jqXHR, textStatus) {
          self.set('loading', false);
          if(self.get('loaded') == false || self.get('resetPagination') == true){
            self.initializePagination();
            self.set('resetPagination', false);
          }
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

  initializePagination: function() {
    var back_link_IDs = this.get('navIDs.backIDs.[]');
    if(!back_link_IDs.contains(this.get('lastJobID'))) {
      back_link_IDs.push(this.get('lastJobID'));
    }
    this.set('filterObject.backFromId', this.get('lastJobID'));
    this.get('filterObject').set('fromTs', App.get('currentServerTime'));
  },

  navigateNext: function() {
    this.set("filterObject.backFromId", '');
    var back_link_IDs = this.get('navIDs.backIDs.[]');
    var lastBackID = this.get('navIDs.nextID');
    if(!back_link_IDs.contains(lastBackID)) {
      back_link_IDs.push(lastBackID);
    }
    this.set('navIDs.backIDs.[]', back_link_IDs);
    this.set("filterObject.nextFromId", this.get('navIDs.nextID'));
    this.set('navIDs.nextID', '');
    this.loadJobs();
  },

  navigateBack: function() {
    this.set("filterObject.nextFromId", '');
    var back_link_IDs = this.get('navIDs.backIDs.[]');
    back_link_IDs.pop();
    var lastBackID = back_link_IDs[back_link_IDs.length - 1]
    this.set('navIDs.backIDs.[]', back_link_IDs);
    this.set("filterObject.backFromId", lastBackID);
    this.loadJobs();
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
