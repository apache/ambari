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
var date = require('utils/date');
var misc = require('utils/misc');

App.App2 = Ember.Object.extend({

  app_id: "", //string
  app_name: "", //string
  app_type: "", //string
  workflow_entity_name: "", //string
  user_name: "", //string
  queue: "", //string
  submit_time: 0, //number
  launch_time: 0, //number
  finish_time: 0, //number
  num_stages: 0, //number
  stages: [], //number
  status: "", //string
});
