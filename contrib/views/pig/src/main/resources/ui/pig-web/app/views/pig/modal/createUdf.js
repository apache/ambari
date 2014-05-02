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

App.CreateUdfView = App.PigModalView.extend({
  templateName: 'pig/modal/createUdf',
  actions:{
    createUdf: function(udf) {
      $(this.get('element')).find('.modal').modal('hide');
      return this.controller.send('createUdf',udf);
    },
    close:function (udf) {
      udf.deleteRecord();
      return this._super();
    }
  },
  udfInvalid:function (argument) {
    var udf = this.get('controller.content');
    var invalid = !udf.get('name') || !udf.get('path');
    return invalid;
  }.property('controller.content.name','controller.content.path'),
});
