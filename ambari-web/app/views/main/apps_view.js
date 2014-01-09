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

App.MainAppsView = Em.View.extend({
  templateName: require('templates/main/apps'),
  paginationInfo: function() {
    return this.t('tableView.filters.paginationInfo').format(this.get('controller.paginationObject.startIndex'), this.get('controller.paginationObject.endIndex'), this.get('controller.paginationObject.iTotalDisplayRecords'));
  }.property('controller.paginationObject.startIndex', 'controller.paginationObject.endIndex', 'controller.paginationObject.iTotalDisplayRecords'),
  //Pagination left/right buttons css class
  paginationLeft: Ember.View.extend({
    tagName: 'a',
    template: Ember.Handlebars.compile('<i class="icon-arrow-left"></i>'),
    classNameBindings: ['class'],
    class: function () {
      if (parseInt(this.get("controller.paginationObject.startIndex")) > 1) {
        return "paginate_previous";
      } else {
        return "paginate_disabled_previous";
      }
    }.property("controller.paginationObject.startIndex"),
    click: function (event) {
      if (this.get('class') == "paginate_previous") {
        var startIndex = parseInt(this.get("controller.paginationObject.startIndex")) - 1;
        var showRows = parseInt(this.get("controller.filterObject.iDisplayLength"));
        var startDisplayValue = Math.max(0, startIndex - showRows);
        this.set("controller.filterObject.iDisplayStart", startDisplayValue);
      }
    }
  }),
  paginationRight: Ember.View.extend({
    tagName: 'a',
    template: Ember.Handlebars.compile('<i class="icon-arrow-right"></i>'),
    classNameBindings: ['class'],
    class: function () {
      if ((parseInt(this.get("controller.paginationObject.endIndex"))) < parseInt(this.get("controller.paginationObject.iTotalDisplayRecords"))) {
        return "paginate_next";
      } else {
        return "paginate_disabled_next";
      }
    }.property("controller.paginationObject.endIndex"),
    click: function (event) {
      if (this.get('class') == "paginate_next") {
        var startDisplayValue = parseInt(this.get("controller.paginationObject.endIndex"));
        this.set("controller.filterObject.iDisplayStart", startDisplayValue);
      }
    }
  }),

  /**
   * If there are table rows with runs.
   */
  emptyData:true,

  /*
   If no runs to display set emptyData to true and reset Avg table data, else to set emptyData to false.
   */
  emptyDataObserver:function(){
    if(this.get("controller.paginationObject.iTotalRecords") != null && this.get("controller.paginationObject.iTotalDisplayRecords")>0){
      this.set("emptyData",false);
    }else{
      this.set("emptyData",true);
      this.set("controller.serverData",null);
    }
  }.observes("controller.paginationObject.iTotalDisplayRecords","controller.paginationObject.iTotalRecords"),


  /**
   * View for RunPerPage select component
   */
  runPerPageSelectView: Em.Select.extend({
    selected: '10',
    content: ['10', '25', '50', '100']
  }),

  wrapSorting: Ember.View.extend({
    tagName: 'tr'
  }),

  sortingColumns: Ember.View.extend({
    tagName: 'th',
    classNameBindings: ['class', 'widthClass'],
    class: "sorting",
    widthClass: "",
    content: null,
    defaultColumn: 8,

    didInsertElement: function () {
      this.set("widthClass", "col" + this.get('content.index'));
      if (this.get('content.index') == this.get('defaultColumn')) {
        this.setControllerObj(this.content.index, "DESC");
        this.set("class", "sorting_desc");
      }
    },
    click: function (event) {
      console.log(this.get('class'));
      if (this.get('class') == "sorting") {
        this.resetSortClass();
        this.setControllerObj(this.get('content.index'), "ASC");
        this.set("class", "sorting_asc");
      } else if (this.get('class') == "sorting_asc") {
        this.setControllerObj(this.get('content.index'), "DESC");
        this.set("class", "sorting_desc");
      } else if (this.get('class') == "sorting_desc") {
        this.setControllerObj(this.get('content.index'), "ASC");
        this.set("class", "sorting_asc");
      }
    },
    resetSortClass: function () {
      this.get("parentView.childViews").map(function (a, e) {
        a.get("childViews")[0].set("class", "sorting")
      });
    },
    setControllerObj: function (col, dir) {
      this.set("controller.filterObject.iSortCol_0", col);
      this.set("controller.filterObject.sSortDir_0", dir);
    }
  }),

  /**
   * Filter-field for Search
   */
  appSearchThrough: Em.TextField.extend({
    classNames: ['input-medium'],
    type: 'text',
    placeholder: Em.I18n.t('common.search')
  }),
  /**
   * Filter-field for App ID.
   * Based on <code>filters</code> library
   */
  appIdFilterView: filters.createTextView({
    valueBinding: "controller.filterObject.sSearch_0"
  }),
  /**
   * Filter-field for name.
   * Based on <code>filters</code> library
   */
  nameFilterView: filters.createTextView({
    valueBinding: "controller.filterObject.sSearch_1",
    fieldType: 'input-small'
  }),
  /**
   * Filter-field for type.
   * Based on <code>filters</code> library
   */
  typeFilterView: filters.createSelectView({
    fieldType: 'input-small',
    valueBinding: "controller.filterObject.runType",
    content: ['Any', 'Pig', 'Hive', 'MapReduce']
  }),

  /**
   * Filter-list for User.
   * Based on <code>filters</code> library
   */
  userFilterView: filters.createComponentView({
    /**
     * Inner FilterView. Used just to render component. Value bind to <code>mainview.value</code> property
     * Base methods was implemented in <code>filters.componentFieldView</code>
     */
    filterView: filters.componentFieldView.extend({
      templateName:require('templates/main/apps/user_filter'),

      usersBinding: 'controller.users',

      allComponentsChecked:false,
      toggleAllComponents:function () {
        var checked = this.get('allComponentsChecked');
        this.get('users').setEach('checked', checked);
      }.observes('allComponentsChecked'),

      clearFilter:function() {
        this.set('allComponentsChecked', false);

        this.get('users').setEach('checked', false);

        this._super();
      },

      applyFilter:function() {
        this._super();

        var chosenUsers = this.get('users').filterProperty('checked', true).mapProperty('name');
        this.set('value', chosenUsers.toString());
      }
    }),

    valueBinding: 'controller.filterObject.sSearch_3'
  }),
  /**
   * Filter-field for jobs.
   * Based on <code>filters</code> library
   */
  jobsFilterView: filters.createTextView({
    fieldType: 'input-super-mini',
    valueBinding: "controller.filterObject.jobs"
  }),
  /**
   * Filter-field for Input.
   * Based on <code>filters</code> library
   */
  inputFilterView: filters.createTextView({
    fieldType: 'input-super-mini',
    valueBinding: "controller.filterObject.input"
  }),
  /**
   * Filter-field for Output.
   * Based on <code>filters</code> library
   */
  outputFilterView: filters.createTextView({
    fieldType: 'input-super-mini',
    valueBinding: "controller.filterObject.output"
  }),
  /**
   * Filter-field for Duration.
   * Based on <code>filters</code> library
   */
  durationFilterView: filters.createTextView({
    fieldType: 'input-super-mini',
    valueBinding: "controller.filterObject.duration"
  }),
  /**
   * Filter-field for RunDate.
   * Based on <code>filters</code> library
   */
  runDateFilterView: filters.createSelectView({
    fieldType: 'input-medium',
    valueBinding: "controller.filterObject.runDate",
    content: ['Any', 'Past 1 Day', 'Past 2 Days', 'Past 7 Days', 'Past 14 Days', 'Past 30 Days']
  }),

  /**
   * Onclick handler for Show All/Filtered buttons
   */
  clickViewType: function (event) {
    this.set("controller.filterObject.viewTypeClickEvent", true);
    if ($(event.target).hasClass("filtered") || $(event.target.parentNode).hasClass("filtered")) {
      this.set("controller.filterObject.viewType", "filtered");
    } else {
      this.set("controller.filterObject.allFilterActivated", true);
      this.set("controller.filterObject.viewType", "all");
    }
  },
  /**
   * Clears up last job ID when coming in fresh to page.
   * Not doing this will result in failure to load job
   * data, and subsequently the popup dialog.
   */
  didInsertElement: function(){
    var self = this;
    Em.run.next(function() {
      self.get('_childViews').forEach(function(childView) {
        if(childView['showClearFilter']) {
          childView.showClearFilter();
        }
      });
    });
    this.get('controller').set('lastJobId', null);
    this.onChangeViewType();
  },
  /**
   *
   */
  onChangeViewType: function () {
    var tmpViewType = this.get('controller.filterObject.viewType');
    var filterButtons = $("#filter_buttons").children();
    filterButtons.each(function (index, element) {
      $(element).removeClass('selected');
      if (tmpViewType == $(element).data('view-type')) {
        $(element).addClass('selected');
      }
    });
    this.set("controller.filterObject.viewTypeClickEvent", false);
  }.observes("controller.filterObject.viewType"),

  /**
   * This Container View is used to render static table row(appTableRow) and additional dynamic content
   */
  containerRow: Em.ContainerView.extend({

    /**
     * Unique row id
     */
    id: function () {
      return this.get('run.id');
    }.property("run.id"),

    /**
     * Show/hide additional content appropriated for this row
     */
    expandToggle: function () {
      //App.router.get('mainAppsItemController').set('jobsLoaded', false);
      App.router.get('mainAppsItemController').set('content', this.get('run'));
      App.ModalPopup.show({
        classNames: ['big-modal'],
        header: Em.I18n.t('apps.dagCharts.popup'),
        bodyClass: App.MainAppsItemView.extend({
          controllerBinding: 'App.router.mainAppsItemController'
        }),
        secondary: null
      });
    }
  }),
  /**
   * Table-row view
   */
  appTableRow: Em.View.extend({
    templateName: require('templates/main/apps/list_row'),
    classNames: ['app-table-row'],
    tagName: "tr",
    onLoad: function() {
      var run = this.get('parentView.run');
      if (run.index) {
        var strip = (run.index % 2) ? 'even' : 'odd';
        this.$().addClass(strip);
      }
    }.observes('parentView.run'),

    didInsertElement: function() {
      this.onLoad();
    },
    mouseEnter: function (event, view) {
      $(event.currentTarget).addClass("hover")
    },
    mouseLeave: function (event, view) {
      $(event.currentTarget).removeClass("hover");
    },
    click: function (event, view) {
      this.get('parentView').expandToggle();
    }

  })

});