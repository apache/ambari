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

App.MainJobsView = App.TableView.extend({
  templateName: require('templates/main/jobs'),

  content: [],


  /**
   * If no jobs table rows to show.
   */
  noDataToShow:true,

  filterCondition:[],

  /*
   If no jobs to display set noDataToShow to true, else set emptyData to false.
   */
  noDataToShowObserver: function () {
    if(this.get("controller.content.length") > 0){
      this.set("noDataToShow",false);
    }else{
      this.set("noDataToShow",true);
    }
  }.observes("controller.content.length"),

  willInsertElement: function () {
    var self = this;
    var name = this.get('controller.name');
    var colPropAssoc = this.get('colPropAssoc');
    var filterConditions = App.db.getFilterConditions(name);
    if (filterConditions) {
      this.set('filterConditions', filterConditions);
      var childViews = this.get('childViews');

      filterConditions.forEach(function(condition) {
        var view = childViews.findProperty('column', condition.iColumn);
        if (view) {
          //self.get('controller.filterObject').set(colPropAssoc[condition.iColumn], condition.value);
          view.set('value', condition.value);
          if(view.get('setPropertyOnApply')){
            view.setValueOnApply();
          }
          Em.run.next(function() {
            view.showClearFilter();
          });
        }
      });
    } else {
      this.clearFilters();
    }
    this.onApplyIdFilter();
    this.set('tableFilteringComplete', true);
  },

  didInsertElement: function () {
    if(!this.get('controller.sortingColumn')){
      var columns = this.get('childViews')[0].get('childViews')
      if(columns && columns.findProperty('name', 'startTime')){
        columns.findProperty('name','startTime').set('status', 'sorting_desc');
        this.get('controller').set('sortingColumn', columns.findProperty('name','startTime'))
      }
    }
  },

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

  sortView: sort.wrapperView,
  idSort: sort.fieldView.extend({
    column: 1,
    name: 'id',
    displayName: Em.I18n.t('jobs.column.id'),
    type: 'string'
  }),
  userSort: sort.fieldView.extend({
    column: 2,
    name: 'user',
    displayName: Em.I18n.t('jobs.column.user'),
    type: 'string'
  }),
  startTimeSort: sort.fieldView.extend({
    column: 3,
    name: 'startTime',
    displayName: Em.I18n.t('jobs.column.start.time'),
    type: 'number'
  }),
  endTimeSort: sort.fieldView.extend({
    column: 4,
    name: 'endTime',
    displayName: Em.I18n.t('jobs.column.end.time'),
    type: 'number'
  }),
  durationSort: sort.fieldView.extend({
    column: 5,
    name: 'duration',
    displayName: Em.I18n.t('jobs.column.duration'),
    type: 'number'
  }),

  /**
   * Select View with list of "rows-per-page" options
   * @type {Ember.View}
   */
  rowsPerPageSelectView: Em.Select.extend({
    content: ['10', '25', '50', '100', "250", "500"],
    valueBinding: "controller.filterObject.jobsLimit",
    attributeBindings: ['disabled'],
    disabled: false,
    disabledObserver: function () {
      if (this.get("parentView.hasBackLinks")) {
        this.set('disabled', true);
      }else{
        this.set('disabled', false);
      }
    }.observes('parentView.hasBackLinks'),
    change: function () {
      this.get('controller').set('navIDs.nextID', '');
      this.get('parentView').saveDisplayLength();
    }
  }),

  /**
   * return filtered number of all content number information displayed on the page footer bar
   * @returns {String}
   */
  filteredJobs: function () {
    return Em.I18n.t('jobs.filtered.jobs').format(this.get('controller.content.length'));
  }.property('controller.content.length', 'controller.totalOfJobs'),

  pageContentObserver: function () {
    if (!this.get('controller.loading')) {
      if ($('.tooltip').length) {
        Ember.run.later(this, function() {
          if ($('.tooltip').length > 1) {
            $('.tooltip').first().remove();
          };
        }, 500);
      };
    };
  }.observes('controller.loading'),

  init: function() {
    this._super();
    App.tooltip($('body'), {
      selector: '[rel="tooltip"]',
      template: '<div class="tooltip jobs-tooltip"><div class="tooltip-arrow"></div><div class="tooltip-inner"></div></div>',
      placement: 'bottom'
    });
  },

  willDestroyElement : function() {
    $('.tooltip').remove();
  },

  /**
   * Filter-field for Jobs ID.
   * Based on <code>filters</code> library
   */
  jobsIdFilterView: filters.createTextView({
    column: 1,
    showApply: true,
    setPropertyOnApply: 'controller.filterObject.id'
  }),

  /**
   * Filter-list for User.
   * Based on <code>filters</code> library
   */
  userFilterView: filters.createTextView({
    column: 2,
    fieldType: 'input-small',
    showApply: true,
    setPropertyOnApply: 'controller.filterObject.user'
  }),

  /**
   * Filter-field for Start Time.
   * Based on <code>filters</code> library
   */
  startTimeFilterView: filters.createSelectView({
    fieldType: 'input-120',
    column: 3,
    content: ['Any', 'Past 1 hour',  'Past 1 Day', 'Past 2 Days', 'Past 7 Days', 'Past 14 Days', 'Past 30 Days', 'Custom'],
    valueBinding: "controller.filterObject.startTime",
    onChangeValue: function () {
      this.get('parentView').updateFilter(this.get('column'), this.get('value'), 'date');
    }
  }),

  jobNameView: Em.View.extend({
    classNames: ['job-link'],
    tagName: 'a',
    classNameBindings: ['isLink'],
    attributeBindings: ['rel', 'job.queryText:data-original-title', 'href'],
    rel: 'tooltip',
    href: '#',
    isLink: 'is-not-link',
    isLinkObserver: function () {
      if (this.get('job.hasTezDag')) {
        this.set('isLink', "");
      }else{
        this.set('isLink', "is-not-link");
      }
    }.observes('controller.sortingDone'),
    template: Ember.Handlebars.compile('{{job.name}}'),
    click: function(event) {
      if (this.get('job.hasTezDag')) {
        App.router.transitionTo('main.jobs.jobDetails', this.get('job'));
      };
      return false;
    }
  }),

  /**
   * associations between content (jobs list) property and column index
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

  jobsPaginationLeft: Ember.View.extend({
    tagName: 'a',
    template: Ember.Handlebars.compile('<i class="icon-arrow-left"></i>'),
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

  jobsPaginationRight: Ember.View.extend({
    tagName: 'a',
    template: Ember.Handlebars.compile('<i class="icon-arrow-right"></i>'),
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

  hasNextJobs: function() {
    return (this.get("controller.navIDs.nextID.length") > 0);
  }.property('controller.navIDs.nextID'),

  hasBackLinks: function() {
    return (this.get("controller.navIDs.backIDs").length > 1);
  }.property('controller.navIDs.backIDs.[].length')

})
