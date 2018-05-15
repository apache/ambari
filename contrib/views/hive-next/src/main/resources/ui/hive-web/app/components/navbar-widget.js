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

import Ember from 'ember';
import constants from 'hive/utils/constants';

export default Ember.Component.extend({
  tagName: 'navigation-bar',
  title: constants.appTitle,

  items: Ember.A([
    Ember.Object.create({text: 'menus.query',
                         path: constants.namingConventions.routes.index}),

    Ember.Object.create({text: 'menus.savedQueries',
                         path: constants.namingConventions.routes.queries}),

    Ember.Object.create({text: 'menus.history',
                         path: constants.namingConventions.routes.history}),

    Ember.Object.create({text: 'menus.udfs',
                         path: constants.namingConventions.routes.udfs}),

    Ember.Object.create({text: 'menus.uploadTable',
      path: constants.namingConventions.routes.uploadTable})
  ])
});
