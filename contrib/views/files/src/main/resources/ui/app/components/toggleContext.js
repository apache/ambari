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

App.ToggleContextComponent = Em.Component.extend({
  didInsertElement:function () {
    var fileRow = this.$().parents('.file-row'),
        beforeHandler = Ember.run.bind(this, this.setContext),
        itemHandler = Ember.run.bind(this, this.itemHandler);

    fileRow.on('click',Ember.run.bind(this, this.openOnClick));

    fileRow.contextmenu({
      target:'#context-menu',
      before:beforeHandler,
      onItem:itemHandler
    });
  },
  setContext:function(e) {
    if (this.get('targetObject.isMoving')) {
      return false;
    };
    this.set('targetObject.parentController.targetContextMenu',this.get('targetObject'));
    return true;
  },
  itemHandler:function (t,e) {
    if (e.target.dataset.disabled) {
      return false;
    };
  },
  openOnClick:function (e) {
    if($(e.target).is('td') || $(e.target).hasClass('allow-open')){
      this.get('targetObject').send('open');
    }
  },
  willClearRender:function () {
    this.$().parents('.file-row').off('click');
    this.$().parents('.file-row').off('.context.data-api').removeData('context');
  }
});
