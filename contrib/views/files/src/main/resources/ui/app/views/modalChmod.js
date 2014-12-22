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

App.ModalChmodView = Em.View.extend({
  actions:{
    confirm:function (file) {
      this.get('controller.controllers.files').send('confirmChmod',this.get('controller.file'));
      this.$('.chmodal').modal('hide');
    },
    close:function () {
      var file = this.get('controller.file');
      var diff = file.changedAttributes();
      if (diff.permission) {
        file.set('permission',diff.permission[0]);
      }
      this.$('.chmodal').modal('hide');
    }
  },
  didInsertElement:function (argument) {
    this.$('.btn-chmod').each(function () {
      $(this).toggleClass('active',$(this).children('input').is(':checked'));
    });

    this.$('.chmodal').modal();
    this.$('.chmodal').on('hidden.bs.modal',function  () {
      this.get('controller.controllers.files').send('removeChmodModal');
    }.bind(this));
  },
  willClearRender:function  () {
    this.$('.chmodal').off('hidden.bs.modal');
    this.$('.chmodal').modal('hide');
  }
});