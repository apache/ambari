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

App.PopoverDeleteComponent = Em.Component.extend({
  popover:Em.computed.alias('childViews.firstObject'),
  layoutName:'components/deletePopover',
  deleteForever:false,
  actions:{
    confirm:function (deleteForever) {
      this.sendAction('confirm',this.get('deleteForever'));
    },
    close:function () {
      this.set('popover.isVisible',false);
    }
  },
  didInsertElement:function () {
    $('body').on('click.popover', Em.run.bind(this,this.hideMultiply));
  },
  hideMultiply:function (e) {
    if (!this.$()) {
      return;
    }
    if (!this.$().is(e.target)
        && this.$().has(e.target).length === 0
        && $('.popover').has(e.target).length === 0) {
          this.set('popover.isVisible',false);
    }
  },
  willClearRender:function () {
    this.get('popover').$element.off('click');
    $('body').off('click.popover');
  },
});
