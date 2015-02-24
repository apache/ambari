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

App.JobsController = Ember.ArrayController.extend(App.RunPeriodically, {

  name: 'mainJobsController',

  /**
   * Sorted ArrayProxy
   * @type {App.HiveJob[]}
   */
  sortedContent: [],

  navIDs: {
    backIDs: [],
    nextID: ''
  },

  /**
   * ID of the last job
   * @type {string}
   */
  lastJobID: '',

  hasNewJobs: false,

  /**
   * Are jobs already loaded
   * @type {bool}
   */
  loaded: false,

  /**
   * Are jobs loading
   * @type {bool}
   */
  loading: false,

  /**
   * Should pagination be reset
   * Used when jobs list is updated
   * @type {bool}
   */
  resetPagination: false,

  /**
   * Column which is used to sort jobs now
   * @type {Em.Object}
   */
  sortingColumn: null,

  /**
   * Property-name used to sort jobs
   * @type {string}
   */
  sortProperty: 'id',

  /**
   * Is sorting doing in asc order
   * @type {bool}
   */
  sortAscending: true,

  /**
   * Is sorting complete
   * @type {bool}
   */
  sortingDone: true,

  /**
   * Diagnostic message shown on jobs table when no data present
   * @type {string}
   */
  jobsMessage: Em.I18n.t('jobs.loadingTasks'),

  totalOfJobs: 0,

  /**
   * Buttons for custom-date popup
   * @type {Em.Object[]}
   */
  customDatePopupButtons: [
    Ember.Object.create({title: Em.I18n.t('ok'), clicked: 'submitCustomDate'}),
    Ember.Object.create({title: Em.I18n.t('cancel'), dismiss: 'modal', clicked: 'dismissCustomDate'})
  ],

  actions: {

    /**
     * Click-handler for "New jobs available on server"-link
     * @method updateJobsByClick
     */
    updateJobsByClick: function () {
      this.set('navIDs.backIDs', []);
      this.set('navIDs.nextID', '');
      this.set('filterObject.nextFromId', '');
      this.set('filterObject.backFromId', '');
      this.set('filterObject.fromTs', '');
      this.set('hasNewJobs', false);
      this.set('resetPagination', true);
      this.loadJobs();
    },

    /**
     * Custom-date submit handler
     * @method submitCustomDate
     */
    submitCustomDate: function () {
      if(this.get('filterObject').submitCustomDate())
        Bootstrap.ModalManager.close('customDate');
    },

    /**
     * Custom-date dismiss handler
     * @method dismissCustomDate
     */
    dismissCustomDate: function() {
      this.set('filterObject.startTime', 'Any');
    }

  },

  /**
   * Observer for content and sorting indicators
   * @method contentAndSortObserver
   */
  contentAndSortObserver: function () {
    Ember.run.once(this, 'contentAndSortUpdater');
  }.observes(
      'content.length',
      'content.@each.id',
      'content.@each.startTime',
      'content.@each.endTime',
      'sortProperties',
      'sortAscending'
    ),

  /**
   * Update <code>sortedContent</code>
   * Called once from <code>contentAndSortObserver</code>
   * @method contentAndSortUpdater
   */
  contentAndSortUpdater: function () {
    this.set('sortingDone', false);
    var content = this.get('content');
    var sortedContent = content.toArray();
    var sortProperty = this.get('sortProperty');
    var sortAscending = this.get('sortAscending');
    sortedContent.sort(function (r1, r2) {
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
    sortedContent.forEach(function (sortedJob) {
      if (sortedArray.length <= count) {
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
      count++;
    });
    if (sortedArray.length > count) {
      for (var c = sortedArray.length - 1; c >= count; c--) {
        sortedArray.removeObject(sortedArray[c]);
      }
    }
    sortedContent.length = 0;
    this.set('sortingDone', true);
  },

  /**
   * Filters-processor
   * @type {Em.Object}
   */
  filterObject: Ember.Object.create({

    /**
     * @type {string}
     */
    id: "",

    /**
     * Does user filter jobs by ID
     * @type {bool}
     */
    isIdFilterApplied: false,

    /**
     * Number of jobs shown on the page
     * @type {string}
     */
    jobsLimit: '10',

    /**
     * Username used to filter
     * @type {string}
     */
    user: "",

    /**
     * Custom start date
     * @type {string}
     */
    windowStart: "",

    /**
     * Custom end date
     * @type {string}
     */
    windowEnd: "",

    /**
     * @type {string}
     */
    nextFromId: "",

    /**
     * @type {string}
     */
    backFromId: "",

    /**
     * @type {string}
     */
    fromTs: "",

    /**
     * Is user using any filter now
     * @type {bool}
     */
    isAnyFilterApplied: false,

    /**
     * Set <code>isIdFilterApplied</code> according to <code>id</code> value
     * @type {bool}
     */
    onApplyIdFilter: function () {
      this.set('isIdFilterApplied', this.get('id') != "");
    }.observes('id'),

    /**
     * Direct binding to startTime filter field
     * @type {string}
     */
    startTime: "",

    /**
     * Fields values from Select Custom Dates form
     * @type {Em.Object}
     */
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

    /**
     * Error-flags for custom start/end dates
     * @type {Em.Object}
     */
    errors: Ember.Object.create({
      isStartDateError: false,
      isEndDateError: false
    }),

    /**
     * Error-messages for custom start/end dates
     * @type {Em.Object}
     */
    errorMessages: Ember.Object.create({
      startDate: '',
      endDate: ''
    }),

    columnsName: Ember.ArrayController.create({
      content: [
        { name: Em.I18n.t('jobs.column.id'), index: 0 },
        { name: Em.I18n.t('jobs.column.user'), index: 1 },
        { name: Em.I18n.t('jobs.column.start.time'), index: 2 },
        { name: Em.I18n.t('jobs.column.end.time'), index: 3 },
        { name: Em.I18n.t('jobs.column.duration'), index: 4 }
      ],
      columnsCount: 6
    }),

    /**
     * Submit custom dates handler
     * @returns {boolean}
     * @method submitCustomDate
     */
    submitCustomDate: function() {
      this.validate();
      if (this.get('errors.isStartDateError') || this.get('errors.isEndDateError')) {
        return false;
      }
      var windowStart = this.createCustomStartDate(),
        windowEnd = this.createCustomEndDate();
      this.set("windowStart", windowStart.getTime());
      this.set("windowEnd", windowEnd.getTime());
      return true;
    },

    /**
     * Create custom start date according to provided in popup data
     * @returns {Date|null}
     * @method createCustomStartDate
     */
    createCustomStartDate: function () {
      var startDate = this.get('customDateFormFields.startDate'),
        hoursForStart = this.get('customDateFormFields.hoursForStart'),
        minutesForStart = this.get('customDateFormFields.minutesForStart'),
        middayPeriodForStart = this.get('customDateFormFields.middayPeriodForStart');
      if (startDate && hoursForStart && minutesForStart && middayPeriodForStart) {
        return new Date(startDate + ' ' + hoursForStart + ':' + minutesForStart + ' ' + middayPeriodForStart);
      }
      return null;
    },

    /**
     * Create custom end date according to provided in popup data
     * @returns {Date|null}
     * @method createCustomStartDate
     */
    createCustomEndDate: function () {
      var endDate = this.get('customDateFormFields.endDate'),
        hoursForEnd = this.get('customDateFormFields.hoursForEnd'),
        minutesForEnd = this.get('customDateFormFields.minutesForEnd'),
        middayPeriodForEnd = this.get('customDateFormFields.middayPeriodForEnd');
      if (endDate && hoursForEnd && minutesForEnd && middayPeriodForEnd) {
        return new Date(endDate + ' ' + hoursForEnd + ':' + minutesForEnd + ' ' + middayPeriodForEnd);
      }
      return null;
    },

    /**
     * Clear <code>errorMessages</code> and <code>errors</code>
     * @method clearErrors
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
     * Validation for every field in customDateFormFields
     * @method validate
     */
    validate: function () {
      var formFields = this.get('customDateFormFields'),
        errors = this.get('errors'),
        errorMessages = this.get('errorMessages');
      this.clearErrors();
      // Check if feild is empty
      Em.keys(errorMessages).forEach(function (key) {
        if (!formFields.get(key)) {
          errors.set('is' + key.capitalize() + 'Error', true);
          errorMessages.set(key, Em.I18n.t('jobs.customDateFilter.error.required'));
        }
      }, this);
      // Check that endDate is after startDate
      var startDate = this.createCustomStartDate(),
        endDate = this.createCustomEndDate();
      if (startDate && endDate && (startDate.getTime() > endDate.getTime())) {
        errors.set('isEndDateError', true);
        errorMessages.set('endDate', Em.I18n.t('jobs.customDateFilter.error.date.order'));
      }
    },

    /**
     * Create link for server request
     * @return {String}
     * @method createJobsFiltersLink
     */
    createJobsFiltersLink: function () {
      // The filters "TEZ:true" are needed because ATS is case sensitive,
      // and in HDP 2.1, "tez" was used, while in HDP 2.2, "TEZ" was used.
      var link = "?fields=events,primaryfilters,otherinfo&secondaryFilter=TEZ:true",
        numberOfAppliedFilters = 0;

      if (this.get("id") !== "") {
        link = "/" + this.get("id") + link;
        numberOfAppliedFilters++;
      }

      link += "&limit=" + (parseInt(this.get("jobsLimit")) + 1);

      if (this.get("user") !== "") {
        link += "&primaryFilter=user:" + this.get("user");
        numberOfAppliedFilters++;
      }
      if (this.get("backFromId") != "") {
        link += "&fromId=" + this.get("backFromId");
      }
      if (this.get("nextFromId") != "") {
        link += "&fromId=" + this.get("nextFromId");
      }
      if (this.get("fromTs") != "") {
        link += "&fromTs=" + this.get("fromTs");
      }
      if (this.get("startTime") !== "" && this.get("startTime") !== "Any") {
        link += this.get("windowStart") !== "" ? ("&windowStart=" + this.get("windowStart")) : "";
        link += this.get("windowEnd") !== "" ? ("&windowEnd=" + this.get("windowEnd")) : "";
        numberOfAppliedFilters++;
      }

      this.set('isAnyFilterApplied', numberOfAppliedFilters > 0);

      return link;
    }

  }),

  sortingColumnObserver: function () {
    if (this.get('sortingColumn')) {
      this.set('sortProperty', this.get('sortingColumn').get('name'));
      this.set('sortAscending', this.get('sortingColumn').get('status') !== "sorting_desc");
    }
  }.observes('sortingColumn.name', 'sortingColumn.status'),

  setTotalOfJobs: function () {
    if (this.get('totalOfJobs') < this.get('content.length')) {
      this.set('totalOfJobs', this.get('content.length'));
    }
  }.observes('content.length'),

  /**
   * Observer for <code>startTime</code>
   * Calculates value for <code>filterObject.windowStart</code> and <code>filterObject.windowEnd</code> or
   * shows Custom Date popup
   * @method startTimeObserver
   */
  startTimeObserver: function () {
    var time = "",
      curTime = new Date().getTime();
    switch (this.get('filterObject.startTime')) {
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
    if (this.get('filterObject.startTime') != "Custom") {
      this.set("filterObject.windowStart", time);
      this.set("filterObject.windowEnd", "");
    }
  },

  /**
   * Show popup with fields for custom start/end dates
   * @method showCustomDatePopup
   */
  showCustomDatePopup: function () {
    Bootstrap.ModalManager.open(
      'customDate',
      Em.I18n.t('jobs.table.custom.date.header'),
      App.JobsCustomDatesSelectView,
      this.get('customDatePopupButtons'),
      this
    );
  },

  /**
   * Success-callback for "jobs-lastID" request
   * Updates <code>hasNewJobs</code>-flag
   * @param {object} data
   * @method lastIDSuccessCallback
   */
  lastIDSuccessCallback: function (data) {
    if (!data.entities[0]) {
      return;
    }
    var lastReceivedID = data.entities[0].entity;
    if (this.get('lastJobID') == '') {
      this.set('lastJobID', lastReceivedID);
      if (this.get('loaded') && App.HiveJob.store.all('hiveJob').get('length') < 1) {
        this.set('hasNewJobs', true);
      }
    }
    else {
      if (this.get('lastJobID') !== lastReceivedID) {
        this.set('lastJobID', lastReceivedID);
        if (!App.HiveJob.store.getById('hiveJob', lastReceivedID)) {
          this.set('hasNewJobs', true);
        }
      }
    }
  },

  /**
   * Error-callback for "jobs-lastID" request
   * @method lastIDErrorCallback
   */
  lastIDErrorCallback: function (data, jqXHR) {
    console.debug(jqXHR);
  },

  /**
   * Check, why jobs weren't loaded and set <code>jobsMessage</code>
   * @param {object|null} jqXHR
   * @method checkDataLoadingError
   */
  checkDataLoadingError: function (jqXHR) {
    var atsComponent = App.HiveJob.store.getById('component', 'APP_TIMELINE_SERVER');
    if (!App.get('atsURL') && atsComponent && atsComponent.get('workStatus') != "STARTED") {
      this.set('jobsMessage', Em.I18n.t('jobs.error.ats.down'));
    }
    else {
      if (jqXHR && (jqXHR.status == 400 || jqXHR.status == 404)) {
        this.set('jobsMessage', Em.I18n.t('jobs.error.400'));
      }
      else {
        if ((!jqXHR && this.get('loaded') && !this.get('loading')) || (jqXHR && jqXHR.status == 500)) {
          this.set('jobsMessage', Em.I18n.t('jobs.nothingToShow'));
        }
        else {
          this.set('jobsMessage', Em.I18n.t('jobs.loadingTasks'));
        }
      }
    }
  },

  /**
   * Do request to load jobs and check last job id
   * @method loadJobs
   */
  loadJobs: function () {
    var yarnService = App.HiveJob.store.getById('service', 'YARN'),
      atsComponent = App.HiveJob.store.getById('component', 'APP_TIMELINE_SERVER'),
      atsInValidState = !!atsComponent && atsComponent.get('workStatus') === "STARTED";
    this.checkDataLoadingError();
    if (App.get('atsURL') || (!Em.isNone(yarnService) && atsInValidState)) {
      this.set('loading', true);
      var atsURL = App.get('atsURL') || 'http://' + atsComponent.get('hostName') + ':' + yarnService.get('ahsWebPort');
      App.ajax.send({
        name: 'jobs_lastID',
        sender: this,
        data: {
          atsURL: atsURL,
          view: App.get("view"),
          version: App.get("version"),
          instanceName: App.get("instanceName")
        },
        success: 'lastIDSuccessCallback',
        error : 'lastIDErrorCallback'
      });
      App.ajax.send({
        name: 'load_jobs',
        sender: this,
        data: {
          atsURL: atsURL,
          filtersLink: this.get('filterObject').createJobsFiltersLink(),
          view: App.get("view"),
          version: App.get("version"),
          instanceName: App.get("instanceName")
        },
        success: 'loadJobsSuccessCallback',
        error: 'loadJobsErrorCallback'
      });
    }
  },

  /**
   * Success callback for jobs-request
   * Call mapper to save jobs to the models
   * @param {object} data
   * @method loadJobsSuccessCallback
   */
  loadJobsSuccessCallback: function (data) {
    App.hiveJobsMapper.map(data);
    this.set('loading', false);
    if (this.get('loaded') == false || this.get('resetPagination') == true) {
      this.initializePagination();
      this.set('resetPagination', false);
    }
    this.set('loaded', true);
  },

  /**
   * Error callback for jobs-request
   * @param {object} jqXHR
   * @method loadJobsErrorCallback
   */
  loadJobsErrorCallback: function (jqXHR) {
    App.hiveJobsMapper.map({entities: []});
    this.checkDataLoadingError(jqXHR);
  },

  /**
   * Update <code>filterObject</code> fields
   * @method initializePagination
   */
  initializePagination: function () {
    var back_link_IDs = this.get('navIDs.backIDs.[]');
    if (!back_link_IDs.contains(this.get('lastJobID'))) {
      back_link_IDs.push(this.get('lastJobID'));
    }
    this.set('filterObject.backFromId', this.get('lastJobID'));
    this.get('filterObject').set('fromTs', new Date().getTime());
  },

  /**
   * Go to next page
   * @method navigateNext
   */
  navigateNext: function () {
    this.set("filterObject.backFromId", '');
    var back_link_IDs = this.get('navIDs.backIDs.[]');
    var lastBackID = this.get('navIDs.nextID');
    if (!back_link_IDs.contains(lastBackID)) {
      back_link_IDs.push(lastBackID);
    }
    this.set('navIDs.backIDs.[]', back_link_IDs);
    this.set("filterObject.nextFromId", this.get('navIDs.nextID'));
    this.set('navIDs.nextID', '');
    this.loadJobs();
  },

  /**
   * Go to previous page
   * @method navigateBack
   */
  navigateBack: function () {
    this.set("filterObject.nextFromId", '');
    var back_link_IDs = this.get('navIDs.backIDs.[]');
    back_link_IDs.pop();
    var lastBackID = back_link_IDs[back_link_IDs.length - 1];
    this.set('navIDs.backIDs.[]', back_link_IDs);
    this.set("filterObject.backFromId", lastBackID);
    this.loadJobs();
  },

  /**
   * Load jobs when <code>filterObject</code> fields were changed
   * @method refreshLoadedJobs
   */
  refreshLoadedJobs: function () {
    this.loadJobs();
  }.observes(
    'filterObject.id',
    'filterObject.jobsLimit',
    'filterObject.user',
    'filterObject.windowStart',
    'filterObject.windowEnd'
  )

});
