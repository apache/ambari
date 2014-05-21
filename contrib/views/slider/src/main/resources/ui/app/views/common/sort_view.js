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

var App = require('config/app');

/**
 * Wrapper View for all sort components. Layout template and common actions are located inside of it.
 * Logic specific for sort fields
 * located in inner view - <code>fieldView</code>.
 *
 * @type {*}
 */
var wrapperView = Em.View.extend({
  tagName: 'tr',

  classNames: ['sort-wrapper'],

  willInsertElement:function () {
    if(this.get('parentView.tableFilteringComplete')){
      this.get('parentView').set('filteringComplete', true);
    }
  },

  /**
   * Load sort statuses from local storage
   * Works only after finish filtering in the parent View
   */
  loadSortStatuses: function() {
    var statuses = App.db.getSortingStatuses(this.get('controller.name'));
    if (!this.get('parentView.filteringComplete')) return;
    if (statuses) {
      var childViews = this.get('childViews');
      var self = this;
      statuses.forEach(function(st) {
        if (st.status != 'sorting') {
          var sortOrder = false;
          if(st.status == 'sorting_desc') {
            sortOrder = true;
          }
          self.sort(childViews.findProperty('name', st.name), sortOrder);
          childViews.findProperty('name', st.name).set('status', (sortOrder)?'sorting_desc':'sorting_asc');
          self.get('controller').set('sortingColumn', childViews.findProperty('name', st.name));
        }
        else {
          childViews.findProperty('name', st.name).set('status', st.status);
        }
      });
    }
    this.get('parentView').showProperPage();
  }.observes('parentView.filteringComplete'),

  /**
   * Save sort statuses to local storage
   * Works only after finish filtering in the parent View
   */
  saveSortStatuses: function() {
    if (!this.get('parentView.filteringComplete')) return;
    var statuses = [];
    this.get('childViews').forEach(function(childView) {
      statuses.push({
        name: childView.get('name'),
        status: childView.get('status')
      });
    });
    App.db.setSortingStatuses(this.get('controller.name'), statuses);
  }.observes('childViews.@each.status'),

  /**
   * sort content by property
   * @param property
   * @param order true - DESC, false - ASC
   */
  sort: function(property, order, returnSorted){
    returnSorted = returnSorted ? true : false;
    var content = this.get('content').toArray();
    var sortFunc = this.getSortFunc(property, order);
    this.resetSort();
    content.sort(sortFunc);
    if(returnSorted){
      return content;
    }else{
      this.set('content', content);
    }
  },

  isSorting: false,

  onContentChange: function () {
    if (!this.get('isSorting') && this.get('content.length')) {
      this.get('childViews').forEach(function (view) {
        if (view.status !== 'sorting') {
          var status = view.get('status');
          this.set('isSorting', true);
          this.sort(view, status == 'sorting_desc');
          this.set('isSorting', false);
          view.set('status', status);
        }
      }, this);
    }
  }.observes('content.length'),

  /**
   * reset all sorts fields
   */
  resetSort: function(){
    this.get('childViews').setEach('status', 'sorting');
  },
  /**
   * determines sort function depending on the type of sort field
   * @param property
   * @param order
   * @return {*}
   */
  getSortFunc: function(property, order){
    var func;
    switch (property.get('type')){
      case 'number':
        func = function (a, b) {
          var a = parseFloat(Em.get(a, property.get('name')));
          var b = parseFloat(Em.get(b, property.get('name')));
          if (order) {
            return b - a;
          } else {
            return a - b;
          }
        };
        break;
      default:
        func = function(a,b){
          if(order){
            if (Em.get(a, property.get('name')) > Em.get(b, property.get('name')))
              return -1;
            if (Em.get(a, property.get('name')) < Em.get(b, property.get('name')))
              return 1;
            return 0;
          } else {
            if (Em.get(a, property.get('name')) < Em.get(b, property.get('name')))
              return -1;
            if (Em.get(a, property.get('name')) > Em.get(b, property.get('name')))
              return 1;
            return 0;
          }
        }
    }
    return func;
  }
});
/**
 * particular view that contain sort field properties:
 * name - name of property in content table
 * type(optional) - specific type to sort
 * displayName - label to display
 * @type {*}
 */
var fieldView = Em.View.extend({
  template:Em.Handlebars.compile('<span {{bind-attr class="view.status :column-name"}}>{{view.displayName}}</span>'),
  classNameBindings: ['viewNameClass'],
  tagName: 'th',
  name: null,
  displayName: null,
  status: 'sorting',
  viewNameClass: function () {
    return 'sort-view-' + this.get('column');
  }.property(),
  type: null,
  column: 0,
  /**
   * callback that run sorting and define order of sorting
   * @param event
   */
  click: function(event){
    if(this.get('status') === 'sorting_desc'){
      this.get('parentView').sort(this, false);
      this.set('status', 'sorting_asc');
    }
    else {
      this.get('parentView').sort(this, true);
      this.set('status', 'sorting_desc');
    }
    this.get('controller').set('sortingColumn', this);
  }
});

/**
 * Result object, which will be accessible outside
 * @type {Object}
 */
module.exports = {
  wrapperView: wrapperView,
  fieldView: fieldView
};