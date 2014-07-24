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

/**
 * Wrapper View for all filter components. Layout template and common actions are located inside of it.
 * Logic specific for data component(input, select, or custom multi select, which fire any changes on interface) are
 * located in inner view - <code>filterView</code>.
 *
 * If we want to have input filter, put <code>textFieldView</code> to it.
 * All inner views implemented below this view.
 * @type {*}
 */

var wrapperView = Ember.View.extend({
  classNames: ['view-wrapper'],
  layoutName: 'wrapper_layout',
  templateName: 'wrapper_template',

  value: null,

  /**
   * Column index
   */
  column: null,

  /**
   * If this field is exists we dynamically create hidden input element and set value there.
   * Used for some cases, where this values will be used outside of component
   */
  fieldId: null,

  clearFilter: function(){
    this.set('value', this.get('emptyValue'));
    if(this.get('setPropertyOnApply')){
      this.setValueOnApply();
    }
    return false;
  },

  setValueOnApply: function() {
    if(this.get('value') == null){
      this.set('value', '')
    }
    this.set(this.get('setPropertyOnApply'), this.get('value'));
    return false;
  },

  actions: {
    actionSetValueOnApply: function() {
      this.setValueOnApply();
    },
    actionClearFilter: function() {
      this.clearFilter();
    }
  },

  /**
   * Use to determine whether filter is clear or not. Also when we want to set empty value
   */
  emptyValue: '',

  /**
   * Whether our <code>value</code> is empty or not
   * @return {Boolean}
   */
  isEmpty: function(){
    if(this.get('value') === null){
      return true;
    }
    return this.get('value').toString() === this.get('emptyValue').toString();
  },

  /**
   * Show/Hide <code>Clear filter</code> button.
   * Also this method updates computed field related to <code>fieldId</code> if it exists.
   * Call <code>onChangeValue</code> callback when everything is done.
   */
  showClearFilter: function(){
    if(!this.get('parentNode')){
      return;
    }
    // get the sort view element in the same column to current filter view to highlight them together
    var relatedSort = $(this.get('element')).parents('thead').find('.sort-view-' + this.get('column'));
    if(this.isEmpty()){
      this.get('parentNode').removeClass('active-filter');
      this.get('parentNode').addClass('notActive');
      relatedSort.removeClass('active-sort');
    } else {
      this.get('parentNode').removeClass('notActive');
      this.get('parentNode').addClass('active-filter');
      relatedSort.addClass('active-sort');
    }

    if(this.get('fieldId')){
      this.$('> input').eq(0).val(this.get('value'));
    }

    this.onChangeValue();
  }.observes('value'),

  /**
   * Callback for value changes
   */
  onChangeValue: function(){

  },

  /**
   * Filter components is located here. Should be redefined
   */
  filterView: Em.View,

  /**
   * Update class of parentNode(hide clear filter button) on page load
   */
  didInsertElement: function(){
    var parent = this.$().parent();
    this.set('parentNode', parent);
    parent.addClass('notActive');
  }
});

/**
 * Simple input control for wrapperView
 */
var textFieldView = Ember.TextField.extend({
  type:'text',
  placeholder: Em.I18n.t('any'),
  valueBinding: "parentView.value"
});

/**
 * Simple multiselect control for wrapperView.
 * Used to render blue button and popup, which opens on button click.
 * All content related logic should be implemented manually outside of it
 */
var componentFieldView = Ember.View.extend({
  classNames: ['btn-group'],
  classNameBindings: ['isFilterOpen:open:'],

  /**
   * Whether popup is shown or not
   */
  isFilterOpen: false,

  /**
   * We have <code>value</code> property similar to inputs <code>value</code> property
   */
  valueBinding: 'parentView.value',

  /**
   * Clear filter to initial state
   */
  clearFilter: function(){
    this.set('value', '');
  },

  /**
   * Onclick handler for <code>cancel filter</code> button
   */
  closeFilter:function () {
    $(document).unbind('click');
    this.set('isFilterOpen', false);
  },

  /**
   * Onclick handler for <code>apply filter</code> button
   */
  applyFilter:function() {
    this.closeFilter();
  },

  /**
   * Onclick handler for <code>show component filter</code> button.
   * Also this function is used in some other places
   */
  clickFilterButton:function () {
    var self = this;
    this.set('isFilterOpen', !this.get('isFilterOpen'));
    if (this.get('isFilterOpen')) {

      var dropDown = this.$('.filter-components');
      var firstClick = true;
      $(document).bind('click', function (e) {
        if (!firstClick && $(e.target).closest(dropDown).length == 0) {
          self.set('isFilterOpen', false);
          $(document).unbind('click');
        }
        firstClick = false;
      });
    }
  }
});

/**
 * Simple select control for wrapperView
 */
var selectFieldView = Ember.Select.extend({
  selectionBinding: 'parentView.value',
  contentBinding: 'parentView.content'
});

/**
 * Result object, which will be accessible outside
 * @type {Object}
 */
App.Filters = {
  /**
   * You can access wrapperView outside
   */
  wrapperView : wrapperView,

  /**
   * And also controls views if need it
   */
  textFieldView : textFieldView,
  selectFieldView: selectFieldView,
  componentFieldView: componentFieldView,

  /**
   * Quick create input filters
   * @param config parameters of <code>wrapperView</code>
   */
  createTextView : function(config){
    config.fieldType = config.fieldType || 'input-medium';
    config.filterView = textFieldView.extend({
      classNames : [ config.fieldType ]
    });

    return wrapperView.extend(config);
  },

  /**
   * Quick create multiSelect filters
   * @param config parameters of <code>wrapperView</code>
   */
  createComponentView : function(config){
    config.clearFilter = function(){
      this.forEachChildView(function(item){
        if(item.clearFilter){
          item.clearFilter();
        }
      });
      return false;
    };

    return wrapperView.extend(config);
  },

  /**
   * Quick create select filters
   * @param config parameters of <code>wrapperView</code>
   */
  createSelectView: function(config){

    config.fieldType = config.fieldType || 'input-medium';
    config.filterView = selectFieldView.extend({
      classNames : [ config.fieldType ],
      attributeBindings: ['disabled','multiple'],
      disabled: false
    });
    config.emptyValue = Em.I18n.t('any');

    return wrapperView.extend(config);
  },
  /**
   * returns the filter function, which depends on the type of property
   * @param type
   * @param isGlobal check is search global
   * @return {Function}
   */
  getFilterByType: function(type, isGlobal){
    switch (type){
      case 'ambari-bandwidth':
        return function(rowValue, rangeExp){
          var compareChar = isNaN(rangeExp.charAt(0)) ? rangeExp.charAt(0) : false;
          var compareScale = rangeExp.charAt(rangeExp.length - 1);
          var compareValue = compareChar ? parseFloat(rangeExp.substr(1, rangeExp.length)) : parseFloat(rangeExp.substr(0, rangeExp.length));
          var match = false;
          if (rangeExp.length == 1 && compareChar !== false) {
            // User types only '=' or '>' or '<', so don't filter column values
            match = true;
            return match;
          }
          switch (compareScale) {
            case 'g':
              compareValue *= 1073741824;
              break;
            case 'm':
              compareValue *= 1048576;
              break;
            case 'k':
              compareValue *= 1024;
              break;
            default:
              //default value in GB
              compareValue *= 1073741824;
          }
          rowValue = (jQuery(rowValue).text()) ? jQuery(rowValue).text() : rowValue;

          var convertedRowValue;
          if (rowValue === '<1KB') {
            convertedRowValue = 1;
          } else {
            var rowValueScale = rowValue.substr(rowValue.length - 2, 2);
            switch (rowValueScale) {
              case 'KB':
                convertedRowValue = parseFloat(rowValue)*1024;
                break;
              case 'MB':
                convertedRowValue = parseFloat(rowValue)*1048576;
                break;
              case 'GB':
                convertedRowValue = parseFloat(rowValue)*1073741824;
                break;
            }
          }

          switch (compareChar) {
            case '<':
              if (compareValue > convertedRowValue) match = true;
              break;
            case '>':
              if (compareValue < convertedRowValue) match = true;
              break;
            case false:
            case '=':
              if (compareValue == convertedRowValue) match = true;
              break;
          }
          return match;
        };
        break;
      case 'duration':
        return function (rowValue, rangeExp) {
          var compareChar = isNaN(rangeExp.charAt(0)) ? rangeExp.charAt(0) : false;
          var compareScale = rangeExp.charAt(rangeExp.length - 1);
          var compareValue = compareChar ? parseFloat(rangeExp.substr(1, rangeExp.length)) : parseFloat(rangeExp.substr(0, rangeExp.length));
          var match = false;
          if (rangeExp.length == 1 && compareChar !== false) {
            // User types only '=' or '>' or '<', so don't filter column values
            match = true;
            return match;
          }
          switch (compareScale) {
            case 's':
              compareValue *= 1000;
              break;
            case 'm':
              compareValue *= 60000;
              break;
            case 'h':
              compareValue *= 3600000;
              break;
            default:
              compareValue *= 1000;
          }
          rowValue = (jQuery(rowValue).text()) ? jQuery(rowValue).text() : rowValue;

          switch (compareChar) {
            case '<':
              if (compareValue > rowValue) match = true;
              break;
            case '>':
              if (compareValue < rowValue) match = true;
              break;
            case false:
            case '=':
              if (compareValue == rowValue) match = true;
              break;
          }
          return match;
        };
        break;
      case 'date':
        return function (rowValue, rangeExp) {
          var match = false;
          var timePassed = App.dateTime() - rowValue;
          switch (rangeExp) {
            case 'Past 1 hour':
              match = timePassed <= 3600000;
              break;
            case 'Past 1 Day':
              match = timePassed <= 86400000;
              break;
            case 'Past 2 Days':
              match = timePassed <= 172800000;
              break;
            case 'Past 7 Days':
              match = timePassed <= 604800000;
              break;
            case 'Past 14 Days':
              match = timePassed <= 1209600000;
              break;
            case 'Past 30 Days':
              match = timePassed <= 2592000000;
              break;
            case 'Any':
              match = true;
              break;
          }
          return match;
        };
        break;
      case 'number':
        return function(rowValue, rangeExp){
          var compareChar = rangeExp.charAt(0);
          var compareValue;
          var match = false;
          if (rangeExp.length == 1) {
            if (isNaN(parseInt(compareChar))) {
              // User types only '=' or '>' or '<', so don't filter column values
              match = true;
              return match;
            }
            else {
              compareValue = parseFloat(parseFloat(rangeExp).toFixed(2));
            }
          }
          else {
            if (isNaN(parseInt(compareChar))) {
              compareValue = parseFloat(parseFloat(rangeExp.substr(1, rangeExp.length)).toFixed(2));
            }
            else {
              compareValue = parseFloat(parseFloat(rangeExp.substr(0, rangeExp.length)).toFixed(2));
            }
          }
          rowValue = parseFloat((jQuery(rowValue).text()) ? jQuery(rowValue).text() : rowValue);
          match = false;
          switch (compareChar) {
            case '<':
              if (compareValue > rowValue) match = true;
              break;
            case '>':
              if (compareValue < rowValue) match = true;
              break;
            case '=':
              if (compareValue == rowValue) match = true;
              break;
            default:
              if (rangeExp == rowValue) match = true;
          }
          return match;
        };
        break;
      case 'multiple':
        return function(origin, compareValue){
          var options = compareValue.split(','),
            rowValue = (typeof (origin) === "string") ? origin : origin.mapProperty('componentName').join(" ");
          var str = new RegExp(compareValue, "i");
          for (var i = 0; i < options.length; i++) {
            if(!isGlobal) {
              str = new RegExp('(\\W|^)' + options[i] + '(\\W|$)');
            }
            if (rowValue.search(str) !== -1) {
              return true;
            }
          }
          return false;
        };
        break;
      case 'boolean':
        return function (origin, compareValue){
          return origin === compareValue;
        };
        break;
      case 'string':
      default:
        return function(origin, compareValue){
          var regex = new RegExp(compareValue,"i");
          return regex.test(origin);
        }
    }
  }

};
