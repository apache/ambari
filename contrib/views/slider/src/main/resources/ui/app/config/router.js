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

'use strict';

module.exports = App.Router.map(function () {
  this.resource("slider_apps", { path: "/" }, function () {
    this.resource('createAppWizard', function(){
      this.route('step1');
      this.route('step2');
      this.route('step3');
      this.route('step4');
    });
  });
  this.resource('slider_app', { path: 'apps/:slider_app_id' }, function() {
    this.route('configs');
    this.route('summary');
  });
});

