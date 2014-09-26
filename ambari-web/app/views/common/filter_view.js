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

var App = require('app');

var wrapperView = Ember.View.extend({
  classNames: ['view-wrapper'],
  layout: Ember.Handlebars.compile('<a href="#" {{action "clearFilter" target="view"}} class="ui-icon ui-icon-circle-close"></a> {{yield}}'),
  template: Ember.Handlebars.compile(
    '{{#if view.fieldId}}<input type="hidden" id="{{unbound view.fieldId}}" value="" />{{/if}}' +
    '{{view view.filterView}}' +
    '{{#if view.showApply}}<button {{action "setValueOnApply" target="view"}} class="apply-btn btn"><span>Apply</span></button>{{/if}} '
  ),

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

  /**
   * This option is useful for Em.Select, if you want get events when same option selected more than one time a raw.
   * This property is Object[] (for example see App.MainConfigHistoryView.modifiedFilterView),
   * that have followed structure:
   *
   * <code>
   * [
   *  {
   *    values: ['DefinedValue1', 'DefinedValue2'], // this properties should be defined in `content` property
   *    displayAs: 'Choose me' // this value will be displayed
   *  }
   * ]
   * </code>
   * @type {Array}
   *
   **/
  triggeredOnSameValue: null,

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

  /**
   * Use to determine whether filter is clear or not. Also when we want to set empty value
   */
  emptyValue: '',

  /**
   * empty value that actually applied to filtering on server,
   * used with Select filter where displayed and actual filter value are different
   */
  appliedEmptyValue: "",

  /**
   * reset value to empty string if emptyValue selected
   */
  actualValue: function () {
    return this.get('value') === this.get('emptyValue') ? "" : this.get('value');
  }.property('value'),

  /**
   * Whether our <code>value</code> is empty or not
   * @return {Boolean}
   */
  isEmpty: function(){
    if (Em.isNone(this.get('value'))) {
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
    this.checkSelectSpecialOptions();
  },

  /**
   * Check for Em.Select that should use dispatching event when option with same value selected more than one time.
   **/
  checkSelectSpecialOptions: function() {
    // check predefined property
    if (!this.get('triggeredOnSameValue') || !this.get('triggeredOnSameValue').length) return;
    // add custom additional observer that will handle property changes
    this.addObserver('value', this, 'valueCustomObserver');
    // get the full class attribute to find our select
    var classInlineAttr = this.get('fieldType').split(',')
      .map(function(className) {
        return '.' + className.trim();
      }).join('');
    this.set('classInlineAttr', classInlineAttr);
    this.get('triggeredOnSameValue').forEach(function(triggeredValue) {
      triggeredValue.values.forEach(function(value, index) {
        // option with property `value`
        var $optionEl = $(this.get('element')).find(classInlineAttr)
          .find('option[value="' + value + '"]');
        // should be displayed with `displayAs` caption
        $optionEl.text(triggeredValue.displayAs);
        // the second one option should be hidden
        // as the result, on init stage we show only one option that could be selected
        if (index == 1) {
          $optionEl.css('display', 'none');
        }
      }, this);
    }, this);
  },
  /**
   *
   * Custom observer that used for special case of Em.Select related to dispatching event
   * when option with same value selected more than one time.
   *
   **/
  valueCustomObserver: function() {
    var hiddenValue;
    this.get('triggeredOnSameValue').forEach(function(triggeredValue) {
      var values = triggeredValue.values;
      // find current selected value from `values` list
      var currentValueIndex = values.indexOf(this.get('value'));
      if (currentValueIndex < 0) return;
      // value assigned to hidden option
      hiddenValue = values[Number(currentValueIndex == 0)];
    }, this);
    if (hiddenValue) {
      // our select
      var $select = $(this.get('element')).find(this.get('classInlineAttr'));
      // now hide option with current value
      $select.find('option[value="{0}"]'.format(this.get('value'))).css('display', 'none');
      // and show option that was hidden
      $select.find('option[value="{0}"'.format(hiddenValue)).css('display', 'block');
    }
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
module.exports = {
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
      classNames : config.fieldType.split(','),
      attributeBindings: ['disabled','multiple'],
      disabled: false
    });
    config.emptyValue = config.emptyValue || 'Any';

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
          var options = compareValue.split(',');
          if(typeof (origin) === "string"){
            var rowValue = origin;
          }else{
            var rowValue = origin.mapProperty('componentName').join(" ");
          }
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
      case 'select':
        return function (origin, compareValue){
          //TODO add filter by select value
          return true;
        };
        break;
      case 'range':
        return function (origin, compareValue){
          if (compareValue[1] && compareValue[0]) {
            return origin >= compareValue[0] && origin <= compareValue[1];
          } else if (compareValue[0]){
            return origin >= compareValue[0];
          } else if (compareValue[1]) {
            return origin <= compareValue[1]
          }
          return true;
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
