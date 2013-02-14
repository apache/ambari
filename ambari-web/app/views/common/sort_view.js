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

var misc = require('utils/misc');

/**
 * Wrapper View for all sort components. Layout template and common actions are located inside of it.
 * Logic specific for sort fields
 * located in inner view - <code>fieldView</code>.
 *
 * @type {*}
 */
var wrapperView = Em.View.extend({
  tagName: 'tr',
  /**
   * sort content by property
   * @param property
   * @param order: true - DESC, false - ASC
   */
  sort: function(property, order){
    var content = this.get('content').toArray();
    var sortFunc = this.getSortFunc(property, order);
    this.resetSort();
    content.sort(sortFunc);
    this.set('content', content);
  },
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
      case 'ip':
        func = function (a, b) {
          var a = misc.ipToInt(a.get(property.get('name')));
          var b = misc.ipToInt(b.get(property.get('name')));
          if(order){
            return b - a;
          } else {
            return a - b;
          }
        };
        break;
      default:
        func = function(a,b){
          if(order){
            if (a.get(property.get('name')) > b.get(property.get('name')))
              return -1;
            if (a.get(property.get('name')) < b.get(property.get('name')))
              return 1;
            return 0;
          } else {
            if (a.get(property.get('name')) < b.get(property.get('name')))
              return -1;
            if (a.get(property.get('name')) > b.get(property.get('name')))
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
  template:Em.Handlebars.compile('{{view.displayName}}'),
  classNameBindings: ['status'],
  tagName: 'th',
  name: null,
  displayName: null,
  status: 'sorting',
  type: null,
  /**
   * callback that run sorting and define order of sorting
   * @param event
   */
  click: function(event){
    if(this.get('status') === 'sorting_desc'){
      this.get('parentView').sort(this, true);
      this.set('status', 'sorting_asc');
    } else {
      this.get('parentView').sort(this, false);
      this.set('status', 'sorting_desc');
    }
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