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

App.FilesView = Em.View.extend({
    templateName: 'files',
    didInsertElement:function () {
      this.scheduleRebind();
    },
    scheduleRebind:function () {
      Em.run.scheduleOnce('render', this, this.get('reBindTooltips'));
    },
    reBindTooltips:function () {
      this.$().tooltip({selector:'[data-toggle=tooltip]'});
    },
    renameInputView: Em.TextField.extend({
      controller:null,
      didInsertElement:function (argument) {
        var element = $(this.get('element'));
        element.focus().val(this.value)
      },
      keyUp: function(e) {
        var target = this.get('targetObject');
        if (e.keyCode==13) {
          return target.send('rename', 'confirm');
        };

        if (e.keyCode==27) {
          return target.send('rename', 'cancel');
        };
      }
    })
});
