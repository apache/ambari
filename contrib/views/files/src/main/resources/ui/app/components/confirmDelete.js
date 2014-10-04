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

App.DropdownWrapComponent = Em.Component.extend({
  onResetConfirm:function () {
    var childs = this.get('childViews').filter(function (view) {
      return view instanceof App.ConfirmDeleteComponent;
    });
    childs.setEach('isRemoving',false);
  }.on('resetConfirm'),
  didInsertElement:function(){
    this.$().on('hidden.bs.dropdown',Em.run.bind(this,this.onResetConfirm));
  },
});

App.ConfirmDeleteComponent = Em.Component.extend({
  layoutName:'components/deleteBulk',
  tagName:'li',
  deleteForever:false,
  isRemoving:false,
  cancelRemoving:function () {
    this.set('isRemoving',false);
  },
  click:function  (e) {
    if (!$(e.target).hasClass('delete')) {
      e.stopPropagation();
    };
  },
  actions:{
    ask:function () {
      this.get('parentView').trigger('resetConfirm');
      this.set('isRemoving',true);
      return false; 
    },
    cancel:function () {
      this.cancelRemoving();
    },
    confirm:function () {
      this.sendAction('confirm',this.get('deleteForever'));
    }
  }
});
