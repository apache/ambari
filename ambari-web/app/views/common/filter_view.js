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

var wrapperView = Ember.View.extend({
  classNames: ['view-wrapper'],
  layout: Ember.Handlebars.compile('<a href="#" {{action "clearFilter" target="view"}} class="ui-icon ui-icon-circle-close ui-name"></a> {{yield}}'),
  template: Ember.Handlebars.compile('{{#if view.fieldId}}<input type="hidden" id="{{unbound view.fieldId}}" value="" />{{/if}} {{view view.filterView}}'),

  value: null,

  clearFilter: function(){
    this.set('value', this.get('emptyValue'));
    return false;
  },

  emptyValue: '',

  isEmpty: function(){
    if(this.get('value') === null){
      return true;
    }
    return this.get('value').toString() === this.get('emptyValue').toString();
  },

  showClearFilter: function(){
    if(!this.get('parentNode')){
      return;
    }

    if(this.isEmpty()){
      this.get('parentNode').addClass('notActive');
    } else {
      this.get('parentNode').removeClass('notActive');
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

  filterView: Em.View,

  init: function(){
    this.set('value', this.get('emptyValue'));
    this._super();
  },

  didInsertElement: function(){
    var parent = this.$().parent();
    this.set('parentNode', parent);
    parent.addClass('notActive');
  }
});

var textFieldView = Ember.TextField.extend({
  type:'text',
  placeholder: 'Any',
  valueBinding: "parentView.value"
});

var componentFieldView = Ember.View.extend({
  templateName: require('templates/main/host/component_filter'),
  classNames: ['btn-group'],
  classNameBindings: ['open'],
  multiple: true,

  isFilterOpen: false,

  btnGroupClass:function () {
    return this.get('isFilterOpen') ? 'btn-group open' : 'btn-group';
  }.property('isFilterOpen'),

  valueBinding: 'parentView.value',
  masterComponentsBinding: 'controller.masterComponents',
  slaveComponentsBinding: 'controller.slaveComponents',
  clientComponentsBinding: 'controller.clientComponents',

  allComponentsChecked:false,
  toggleAllComponents:function () {
    var checked = this.get('allComponentsChecked');
    this.set('masterComponentsChecked', checked);
    this.set('slaveComponentsChecked', checked);
    this.set('clientComponentsChecked', checked);
  }.observes('allComponentsChecked'),

  masterComponentsChecked:false,
  toggleMasterComponents:function () {
    this.get('masterComponents').setEach('checkedForHostFilter', this.get('masterComponentsChecked'));
  }.observes('masterComponentsChecked'),

  slaveComponentsChecked:false,
  toggleSlaveComponents:function () {
    this.get('slaveComponents').setEach('checkedForHostFilter', this.get('slaveComponentsChecked'));
  }.observes('slaveComponentsChecked'),

  clientComponentsChecked: false,
  toggleClientComponents: function() {
    this.get('clientComponents').setEach('checkedForHostFilter', this.get('clientComponentsChecked'));
  }.observes('clientComponentsChecked'),

  /**
   * Clear filter to initial state
   */
  clearFilter:function() {
    this.set('allComponentsChecked', false);
    this.set('masterComponentsChecked', false);
    this.set('slaveComponentsChecked', false);
    this.set('clientComponentsChecked', false);

    this.get('masterComponents').setEach('checkedForHostFilter', false);
    this.get('slaveComponents').setEach('checkedForHostFilter', false);
    this.get('clientComponents').setEach('checkedForHostFilter', false);
    this.set('value', []);
  },

  /**
   * Onclick handler for <code>cancel filter</code> button
   */
  closeFilters:function () {
    $(document).unbind('click');
    this.set('isFilterOpen', false);
  },

  /**
   * Onclick handler for <code>apply filter</code> button
   */
  applyFilter:function() {
    this.closeFilters();

    var chosenComponents = [];

    this.get('masterComponents').filterProperty('checkedForHostFilter', true).forEach(function(item){
      chosenComponents.push(item.get('displayName'));
    });
    this.get('slaveComponents').filterProperty('checkedForHostFilter', true).forEach(function(item){
      chosenComponents.push(item.get('displayName'));
    });
    this.get('clientComponents').filterProperty('checkedForHostFilter', true).forEach(function(item){
      chosenComponents.push(item.get('displayName'));
    });
    this.set('value', chosenComponents);
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
  },

  didInsertElement:function () {
    if (this.get('controller.comeWithFilter')) {
      this.applyFilter();
      this.set('controller.comeWithFilter', false);
    } else {
      this.clearFilter();
    }
  }
});

module.exports = {
  wrapperView : wrapperView,

  createTextView : function(config){

    config.fieldType = config.fieldType || 'input-medium';
    config.filterView = textFieldView.extend({
      classNames : [ config.fieldType ]
    });

    return wrapperView.extend(config);
  },
  createComponentView : function(config){
    config.filterView = componentFieldView;
    config.emptyValue = [];
    config.clearFilter = function(){
      this.forEachChildView(function(item){
        if(item.clearFilter){
          item.clearFilter();
        }
      });
      return false;
    };

    return wrapperView.extend(config);
  }
};