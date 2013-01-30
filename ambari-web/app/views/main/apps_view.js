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

App.MainAppsView = Em.View.extend({
  templateName:require('templates/main/apps'),

  /**
   * List of users
   */
  users:function () {
    return this.get('controller.content').mapProperty("userName").uniq();
  }.property('controller.content.length'),

  //Pagination left/right buttons css class
  paginationLeft : Ember.View.extend({
    tagName: 'a',
    template: Ember.Handlebars.compile('<i class="icon-arrow-left"></i>'),
    classNameBindings: ['class'],
    class:"",
    calculateClass: function(){
      if(parseInt(this.get("controller.paginationObject.startIndex"))>1){
        this.set("class","paginate_previous");
      }else{
        this.set("class","paginate_disabled_previous");
      }
    }.observes("controller.paginationObject"),
    click: function(event){
      if(this.class == "paginate_previous"){
        var startIndex=parseInt(this.get("controller.paginationObject.startIndex"))-1;
        var showRows=parseInt(this.get("controller.filterObject.iDisplayLength"));
        var startDisplayValue = Math.max(0,startIndex-showRows);
        this.set("controller.filterObject.iDisplayStart", startDisplayValue);
      }
    }
  }),
  paginationRight : Ember.View.extend({
    tagName: 'a',
    template: Ember.Handlebars.compile('<i class="icon-arrow-right"></i>'),
    classNameBindings: ['class'],
    class:"",
    calculateClass: function(){
      if((parseInt(this.get("controller.paginationObject.endIndex"))+1)<parseInt(this.get("controller.paginationObject.iTotalDisplayRecords"))){
        this.set("class","paginate_next");
      }else{
        this.set("class","paginate_disabled_next");
      }
    }.observes("controller.paginationObject"),
    click: function(event){
      if(this.class == "paginate_next"){
        var startDisplayValue = parseInt(this.get("controller.paginationObject.endIndex"));
        this.set("controller.filterObject.iDisplayStart", startDisplayValue);
      }
    }
  }),

  wrapSorting:Ember.View.extend({
    tagName: 'tr'
  }),

  sortingColumns: Ember.View.extend({
    tagName: 'th',
    classNameBindings: ['class','widthClass'],
    class:"sorting",
    widthClass:"",
    content: null,
    didInsertElement:function(){
      this.set("widthClass","col"+this.content.index);
    },
    click: function(event){
      console.log(this.class);
      if(this.class == "sorting"){
        this.resetSortClass();
        this.setControllerObj(this.content.index,"ASC");
        this.set("class", "sorting_asc");
      }else if(this.class == "sorting_asc"){
        this.setControllerObj(this.content.index,"DESC");
        this.set("class", "sorting_desc");
      }else if(this.class == "sorting_desc"){
        this.setControllerObj("","");
        this.set("class", "sorting");
      }
    },
    resetSortClass:function(){
      this.get("parentView.childViews").map(function(a,e){a.get("childViews")[0].set("class","sorting")});
    },
    setControllerObj:function(col,dir){
      this.set("controller.filterObject.iSortCol_0", col);
      this.set("controller.filterObject.sSortDir_0", dir);
    }
  }),

  /**
   * Filter-field for AppId
   */
  appidFilterView:Em.TextField.extend({
    classNames:['input-small-app'],
    type:'text',
    placeholder:'Any ID',
    elementId:'appid_filter',
    filtering:function () {
      if (this.get('value') == '') {
        this.$().closest('th').addClass('notActive');
      }
      else {
        this.$().closest('th').removeClass('notActive');
      }
    }.observes('value')
  }),
  /**
   * Filter-field for Search
   */
  appSearchThrough:Em.TextField.extend({
    classNames:['input-medium'],
    type:'text',
    placeholder:'Search',
    filtering:function () {
      if (this.get('value') == '') {
        this.$().closest('th').addClass('notActive');
      }
      else {
        this.$().closest('th').removeClass('notActive');
      }
    }.observes('value')
  }),
  /**
   * Filter-field for name
   */
  nameFilterView:Em.TextField.extend({
    classNames:['input-small'],
    type:'text',
    placeholder:'Any Name',
    filtering:function () {
      if (this.get('value') == '') {
        this.$().closest('th').addClass('notActive');
      }
      else {
        this.$().closest('th').removeClass('notActive');
      }
    }.observes('value')
  }),
  /**
   * dataTable filter views
   */
  typeSelectView:Em.Select.extend({
    classNames:['input-small'],
    selected:'Any',
    content:['Any', 'Pig', 'Hive', 'MapReduce'],
    change:function (event) {
      if (this.get('selection') === 'Any') {
        this.$().closest('th').addClass('notActive');
      }
      else {
        this.$().closest('th').removeClass('notActive');
      }
    }
  }),
  /**
   * dataTable filter views
   */
  runPerPageSelectView:Em.Select.extend({

    selected:'10',
    content:['10', '25', '50', '100']
  }),
  /**
   * Filter-list for User
   */
  userFilterView:Em.View.extend({
    classNames:['btn-group'],
    classNameBindings:['open'],
    multiple:true,
    open:false,
    users:function () {
      var users = [];
      for (var i = 0; i < this.get('parentView').get('users').length; i++)
        users.push(Ember.Object.create({
          name:this.get('parentView').get('users')[i],
          checked:false
        }));
      return users;
    }.property('parentView.users'),
    templateName:require('templates/main/apps/user_filter'),
    allComponentsChecked:false,
    toggleAllComponents:function () {
      var checked = this.get('allComponentsChecked');
      this.get('users').forEach(function (item) {
        item.set('checked', checked);
      });
    }.observes('allComponentsChecked'),
    clickFilterButton:function (event) {
      this.set('open', !this.get('open'));
    },
    clearFilter:function (self) {
      self.set('allComponentsChecked', true);
      self.set('allComponentsChecked', false);
      jQuery('#user_filter').val([]);
      self.get('parentView').get('controller.filterObject').set("sSearch_3","");
      jQuery('#user_filter').closest('th').addClass('notActive');
    },
    closeFilter:function () {
      this.set('open', false);
    },
    applyFilter:function () {
      var chosenUsers = new Array();
      this.set('open', !this.get('open'));
      this.get('users').forEach(function (item) {
        if (item.get('checked')) chosenUsers.push(item.get('name'));
      });

      /**
       * Set filterObject
       */
      this.get('parentView').get('controller.filterObject').set("sSearch_3",chosenUsers)

      jQuery('#user_filter').val(chosenUsers);
      if (chosenUsers.length == 0) {
        this.$().closest('th').addClass('notActive');
      }
      else {
        this.$().closest('th').removeClass('notActive');
      }
    }
  }),
  /**
   * Filter-field for jobs
   */
  jobsFilterView:Em.TextField.extend({
    classNames:['input-mini'],
    type:'text',
    placeholder:'Any ',
    elementId:'jobs_filter',
    filtering:function () {
      if (this.get('value') == '') {
        this.$().closest('th').addClass('notActive');
      }
      else {
        this.$().closest('th').removeClass('notActive');
      }
      //this.get('parentView').get('applyFilter')(this.get('parentView'), 6);
    }.observes('value')
  }),

  /**
   * Filter-field for Input
   */
  inputFilterView:Em.TextField.extend({
    classNames:['input-mini'],
    type:'text',
    placeholder:'Any ',
    elementId:'input_filter',
    filtering:function () {
      if (this.get('value') == '') {
        this.$().closest('th').addClass('notActive');
      }
      else {
        this.$().closest('th').removeClass('notActive');
      }
    }.observes('value')
  }),
  /**
   * Filter-field for Output
   */
  outputFilterView:Em.TextField.extend({
    classNames:['input-mini'],
    type:'text',
    placeholder:'Any ',
    elementId:'output_filter',
    filtering:function () {
      if (this.get('value') == '') {
        this.$().closest('th').addClass('notActive');
      }
      else {
        this.$().closest('th').removeClass('notActive');
      }
    }.observes('value')
  }),
  /**
   * Filter-field for Duration
   */
  durationFilterView:Em.TextField.extend({
    classNames:['input-mini'],
    type:'text',
    placeholder:'Any ',
    elementId:'duration_filter',
    filtering:function () {
      if (this.get('value') == '') {
        this.$().closest('th').addClass('notActive');
      }
      else {
        this.$().closest('th').removeClass('notActive');
      }
    }.observes('value')
  }),
  /**
   * Filter-field for RunDate
   */
  rundateSelectView:Em.Select.extend({
    content:['Any', 'Past 1 Day', 'Past 2 Days', 'Past 7 Days', 'Past 14 Days', 'Past 30 Days'],
    selected:'Any',
    classNames:['input-medium'],
    elementId:'rundate_filter',
    change:function (event) {
      if (this.get('selection') == 'Any') {
        this.$().closest('th').addClass('notActive');
      }
      else {
        this.$().closest('th').removeClass('notActive');
      }
    }
  }),

  /**
   * Onclick handler for Show All/Filtered buttons
   */
  clickViewType:function (event) {
    this.set("controller.filterObject.viewTypeClickEvent", true);
    if($(event.target).hasClass("filtered")){
      this.set("controller.filterObject.viewType", "filtered");
    }else{
      this.set("controller.filterObject.allFilterActivated", true);
      this.set("controller.filterObject.viewType", "all");
    }
  },
  /**
   *
   */
  onChangeViewType:function () {
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
   * reset all filters in dataTable
   *
   * @param event
   */
  clearFilters:function (event) {
    this._childViews.forEach(function (item) {
      if(item.get("viewName") === "runPerPageSelectView"){
        return "";
      }
      if (item.get('tagName') === 'input') {
        item.set('value', '');
      }
      if (item.get('tagName') === 'select') {
        item.set('value', 'Any');
        item.change();
      }
      if (item.get('multiple')) {
        item.get('clearFilter')(item);
      }
    });
  },
  /**
   * Clear selected filter
   * @param event
   */
  clearFilterButtonClick:function (event) {
    var viewName = event.target.id.replace('view_', '');
    var elementId = this.get(viewName).get('elementId');
    if (this.get(viewName).get('tagName') === 'input') {
      this.get(viewName).set('value', '');
    }
    if (this.get(viewName).get('tagName') === 'select') {
      this.get(viewName).set('value', 'Any');
      this.get(viewName).change();
    }
    if (this.get(viewName).get('multiple')) {
      this.get(viewName).get('clearFilter')(this.get(viewName));
    }
  },

  /**
   * This Container View is used to render static table row(appTableRow) and additional dynamic content
   */
  containerRow:Em.ContainerView.extend({

    /**
     * Unique row id
     */
    id:function () {
      return this.get('run.id');
    }.property("run.id"),

    /**
     * Show/hide additional content appropriated for this row
     */
    expandToggle:function () {
      //App.router.get('mainAppsItemController').set('jobsLoaded', false);
      App.router.get('mainAppsItemController').set('content', this.get('run'));
      App.ModalPopup.show({
        classNames:['big-modal'],
        header:Em.I18n.t('apps.dagCharts.popup'),
        bodyClass:App.MainAppsItemView.extend({
          controllerBinding:'App.router.mainAppsItemController'
        }),
        onPrimary:function () {
          this.hide();
        },
        secondary:null
      });
    }
  }),
  /**
   * Table-row view
   */
  appTableRow:Em.View.extend({
    templateName:require('templates/main/apps/list_row'),
    classNames:['app-table-row'],
    tagName:"tr",
    mouseEnter:function (event, view) {
      $(event.currentTarget).addClass("hover")
    },
    mouseLeave:function (event, view) {
      $(event.currentTarget).removeClass("hover");
    },
    click:function (event, view) {
      this.get('parentView').expandToggle();
    }

  })

});