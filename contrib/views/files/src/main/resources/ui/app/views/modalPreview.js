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

App.ModalPreviewView = Em.View.extend({
  actions:{
    confirm:function (file) {
      this.get('controller.controllers.file').send('confirmPreview', this.get('controller.file'));
      this.$('.preview').modal('hide');
    },
    close:function () {
      this.$('.preview').modal('hide');
    }
  },
  didInsertElement:function (argument) {
    var self = this;

    this.$('.preview').modal();

    this.$('.preview').on('hidden.bs.modal',function  () {
      this.get('controller.controllers.files').send('removePreviewModal');
    }.bind(this));

    this.$('.preview-content').on('scroll', function() {
      if($(this).scrollTop() + $(this).innerHeight() >= this.scrollHeight) {
        self.get('controller').send('next');
      }
    });

  },
  willClearRender:function  () {
    this.$('.preview').off('hidden.bs.modal');
    this.$('.preview').modal('hide');
  }
});