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

function _shake (element) {
  var l = 5;
  for ( var i = 0; i < 4; i++ ) {
    element.animate( (l>0) ? {'margin-left':(l=-l)+'px','padding-left':0}:{'padding-left':+(l=-l)+'px','margin-left':0}, 50, function (el) {
      element.css({'padding-left':0,'margin-left':0});
    });
  }
}

App.ToggleContextComponent = Em.Component.extend({
  didInsertElement:function () {
    var fileRow = this.$().parents('tr'),
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
    }
    this.set('targetObject.parentController.targetContextMenu',this.get('targetObject'));
    return true;
  },
  itemHandler:function (t,e) {
    if (e.target.dataset.disabled) {
      return false;
    }
  },
  openOnClick:function (e) {
    if($(e.target).is('td') || $(e.target).hasClass('allow-open')){
      if (this.get('targetObject.content.readAccess')) {
        this.get('targetObject').send('open');
      } else {
        _shake(this.$().parents('.file-row').find('.file-name span').first());
      }
    }
  },
  willClearRender:function () {
    var fileRow = this.$().parents('tr');
    fileRow.off('click');
    fileRow.data('context').closemenu();
    fileRow.data('context').destroy();
  }
});

App.FileShakerComponent = Em.Component.extend({
  action:'',
  isValid:false,
  click:function () {
    if (this.get('isValid')) {
      this.sendAction('action');
    } else {
      _shake(this.$());
    }
  }
});
