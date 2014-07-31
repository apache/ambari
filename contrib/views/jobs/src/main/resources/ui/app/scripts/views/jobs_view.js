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

App.JobsView = App.TableView.extend({

  templateName: 'jobs',

  content: [],


  /**
   * If no jobs table rows to show.
   */
  noDataToShow: true,

  filterCondition:[],

  /**
   * If no jobs to display set noDataToShow to true, else set emptyData to false.
   * @method noDataToShowObserver
   */
  noDataToShowObserver: function () {
    this.set("noDataToShow", this.get("controller.content.length") === 0);
  }.observes("controller.content.length"),

  willInsertElement: function () {
    this._super();
    this.clearFilters();
    this.onApplyIdFilter();
    this.set('tableFilteringComplete', true);
  },

  didInsertElement: function () {
    if(!this.get('controller.sortingColumn')){
      var columns = this.get('childViews')[0].get('childViews');
      if(columns && columns.findProperty('name', 'startTime')){
        columns.findProperty('name','startTime').set('status', 'sorting_desc');
        this.get('controller').set('sortingColumn', columns.findProperty('name','startTime'))
      }
    }
  },

  /**
   * Handler for id-filter applying
   * @method onApplyIdFilter
   */
  onApplyIdFilter: function() {
    var isIdFilterApplied = this.get('controller.filterObject.isIdFilterApplied');
    this.get('childViews').forEach(function(childView) {
      if (childView['clearFilter'] && childView.get('column') != 1) {
        if(isIdFilterApplied){
          childView.clearFilter();
        }
        var childOfChild = childView.get('childViews')[0];
        if(childOfChild){
          Em.run.next(function() {
            childOfChild.set('disabled', isIdFilterApplied);
          })
        }
      }
    });
  }.observes('controller.filterObject.isIdFilterApplied'),

  /**
   * Save filter when filtering is complete
   * @method saveFilter
   */
  saveFilter: function () {
    if(this.get('tableFilteringComplete')){
      this.updateFilter(1, this.get('controller.filterObject.id'), 'string');
      this.updateFilter(2, this.get('controller.filterObject.user'), 'string');
      this.updateFilter(4, this.get('controller.filterObject.windowEnd'), 'date');
    }
  }.observes(
      'controller.filterObject.id',
      'controller.filterObject.user',
      'controller.filterObject.windowEnd'
    ),

  sortView: App.Sorts.wrapperView,

  idSort: App.Sorts.fieldView.extend({
    column: 1,
    name: 'id',
    displayName: Em.I18n.t('jobs.column.id'),
    type: 'string'
  }),

  userSort: App.Sorts.fieldView.extend({
    column: 2,
    name: 'user',
    displayName: Em.I18n.t('jobs.column.user'),
    type: 'string'
  }),

  startTimeSort: App.Sorts.fieldView.extend({
    column: 3,
    name: 'startTime',
    displayName: Em.I18n.t('jobs.column.start.time'),
    type: 'number'
  }),

  endTimeSort: App.Sorts.fieldView.extend({
    column: 4,
    name: 'endTime',
    displayName: Em.I18n.t('jobs.column.end.time'),
    type: 'number'
  }),

  durationSort: App.Sorts.fieldView.extend({
    column: 5,
    name: 'duration',
    displayName: Em.I18n.t('jobs.column.duration'),
    type: 'number'
  }),

  /**
   * Select View with list of "rows-per-page" options
   * @type {Ember.View}
   */
  rowsPerPageSelectView: Ember.Select.extend({
    content: ['10', '25', '50', '100', "250", "500"],
    valueBinding: "controller.filterObject.jobsLimit",
    attributeBindings: ['disabled'],
    disabled: false,
    disabledObserver: function () {
      this.set('disabled', !!this.get("parentView.hasBackLinks"));
    }.observes('parentView.hasBackLinks'),
    change: function () {
      this.get('controller').set('navIDs.nextID', '');
    }
  }),

  /**
   * return filtered number of all content number information displayed on the page footer bar
   * @returns {String}
   * @method filteredJobs
   */
  filteredJobs: function () {
    return Em.I18n.t('jobs.filtered.jobs').fmt(this.get('controller.content.length'));
  }.property('controller.content.length', 'controller.totalOfJobs'),

  /**
   * Manage tooltips for jobs
   * @method pageContentObserver
   */
  pageContentObserver: function () {
    if (!this.get('controller.loading')) {
      var tooltip = $('.tooltip');
      if (tooltip.length) {
        Ember.run.later(this, function() {
          if (tooltip.length > 1) {
            tooltip.first().remove();
          }
        }, 500);
      }
    }
  }.observes('controller.loading'),

  init: function() {
    this._super();
    App.tooltip($('body'), {
      selector: '[rel="tooltip"]'
    });
  },

  willDestroyElement : function() {
    $('.tooltip').remove();
  },

  /**
   * Filter-field for Jobs ID.
   * Based on <code>filters</code> library
   */
  jobsIdFilterView: App.Filters.createTextView({
    column: 1,
    showApply: true,
    setPropertyOnApply: 'controller.filterObject.id'
  }),

  /**
   * Filter-list for User.
   * Based on <code>filters</code> library
   */
  userFilterView: App.Filters.createTextView({
    column: 2,
    fieldType: 'input-small',
    showApply: true,
    setPropertyOnApply: 'controller.filterObject.user'
  }),

  /**
   * Filter-field for Start Time.
   * Based on <code>filters</code> library
   */
  startTimeFilterView: App.Filters.createSelectView({
    fieldType: 'input-120',
    column: 3,
    content: ['Any', 'Past 1 hour',  'Past 1 Day', 'Past 2 Days', 'Past 7 Days', 'Past 14 Days', 'Past 30 Days', 'Custom'],
    valueBinding: "controller.filterObject.startTime",
    onChangeValue: function () {
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'date');
    }
  }),

  /**
   * View for job's name
   * @type {Em.View}
   */
  jobNameView: Em.View.extend({

    /**
     * Classname for link
     * @type {string}
     */
    isLink: 'is-not-link',

    /**
     * Update link-status (enabled/disabled) after sorting is complete
     */
    isLinkObserver: function () {
      this.refreshLinks();
    }.observes('controller.sortingDone'),

    /**
     * Update <code>isLink</code> according to <code>job.hasTezDag<code>
     * @method refreshLinks
     */
    refreshLinks: function () {
      this.set('isLink', this.get('job.hasTezDag') ? "" : "is-not-link");
    },

    templateName: 'jobs/jobs_name',

    /**
     * Click-handler.
     * Go to Jobs details page if current job has Tez Dag
     * @returns {null|boolean}
     */
    click: function() {
      if (this.get('job.hasTezDag')) {
        this.get('controller').transitionToRoute('job', this.get('job'));
      }
      return false;
    },

    didInsertElement: function () {
      this.refreshLinks();
    }
  }),

  /**
   * Associations between content (jobs list) property and column index
   * @type {string[]}
   */
  colPropAssoc: function () {
    var associations = [];
    associations[1] = 'id';
    associations[2] = 'user';
    associations[3] = 'startTime';
    associations[4] = 'endTime';
    return associations;
  }.property(),

  clearFilters: function() {
    this.get('childViews').forEach(function(childView) {
      if (childView['clearFilter']) {
        childView.clearFilter();
      }
    });
  },

  jobFailMessage: function() {
    return Em.I18n.t('jobs.table.job.fail');
  }.property(),

  /**
   * @type {Em.View}
   */
  jobsPaginationLeft: Ember.View.extend({
    tagName: 'a',
    templateName: 'table/navigation/pagination_left',
    classNameBindings: ['class'],
    class: function () {
      if (this.get("parentView.hasBackLinks") && !this.get('controller.filterObject.isAnyFilterApplied')) {
        return "paginate_previous";
      }
      return "paginate_disabled_previous";
    }.property('parentView.hasBackLinks', 'controller.filterObject.isAnyFilterApplied'),

    click: function () {
      if (this.get("parentView.hasBackLinks") && !this.get('controller.filterObject.isAnyFilterApplied')) {
        this.get('controller').navigateBack();
      }
    }
  }),

  /**
   * @type {Em.View}
   */
  jobsPaginationRight: Ember.View.extend({
    tagName: 'a',
    templateName: 'table/navigation/pagination_right',
    classNameBindings: ['class'],
    class: function () {
      if (this.get("parentView.hasNextJobs") && !this.get('controller.filterObject.isAnyFilterApplied')) {
        return "paginate_next";
      }
      return "paginate_disabled_next";
    }.property("parentView.hasNextJobs", 'controller.filterObject.isAnyFilterApplied'),

    click: function () {
      if (this.get("parentView.hasNextJobs") && !this.get('controller.filterObject.isAnyFilterApplied')) {
        this.get('controller').navigateNext();
      }
    }
  }),

  /**
   * Enable/disable "next"-arrow
   * @type {bool}
   */
  hasNextJobs: function() {
    return (this.get("controller.navIDs.nextID.length") > 1);
  }.property('controller.navIDs.nextID'),

  /**
   * Enable/disable "back"-arrow
   * @type {bool}
   */
  hasBackLinks: function() {
    return (this.get("controller.navIDs.backIDs").length > 1);
  }.property('controller.navIDs.backIDs.[].length')

});
