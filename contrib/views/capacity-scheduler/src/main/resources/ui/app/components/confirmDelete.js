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

var bound;

bound = function(fnName) {
  return Ember.computed(fnName,function() {
    return this.get(fnName).bind(this);
  });
};

App.ClickElsewhereMixin = Ember.Mixin.create({
  onClickElsewhere: Ember.K,
  clickHandler: bound("elsewhereHandler"),
  elsewhereHandler: function(e) {
    var $target, element, thisIsElement;
    element = this.get("element");
    $target = $(e.target);
    thisIsElement = $target.closest(element).length === 1;
    if (!thisIsElement) {
      return this.onClickElsewhere(event);
    }
  },
  didInsertElement: function() {
    this._super.apply(this, arguments);
    return $(window).on("click", this.get("clickHandler"));
  },
  willDestroyElement: function() {
    $(window).off("click", this.get("clickHandler"));
    return this._super.apply(this, arguments);
  }
});

App.ConfirmDeleteComponent = Em.Component.extend(App.ClickElsewhereMixin,{
  confirm:false,
  onClickElsewhere:function () {
    this.set('confirm',false);
  },
  actions: {
    delete: function() {
      if (this.get('confirm')) {
        this.toggleProperty('confirm');
        this.sendAction('action', this.get('param'));
      } else {
        this.toggleProperty('confirm');
      };
    }
  },
  tagName:'a',
  tooltip:function () {
    var element = this.$();
    if (this.get('confirm')) {
      element.tooltip({
        placement:'left',
        title:'Click again to confirm'
      }).tooltip('show');
    } else {
      element.tooltip('destroy')
    };
  }.observes('confirm'),
  click:function (e) {
    this.send('delete');
  },
  didChange:function () {
    this.set('confirm',false);
  }.observes('param'),

  classNames:['pull-right rm-queue'],
  layout:Em.Handlebars.compile('<span class="fa-stack"> <i class="fa fa-times fa-stack-2x"></i> {{#if confirm}} <i class="fa fa-check fa-stack-1x fa-stack-bottom green"></i> {{/if}} </span> ')
});
