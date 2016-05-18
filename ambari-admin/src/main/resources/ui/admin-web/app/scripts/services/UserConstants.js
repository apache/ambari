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

angular.module('ambariAdminConsole').constant('UserConstants', {
  /**
   * Available user_types 'values' and 'labels' map.
   */
  TYPES: {
    LOCAL: {
      VALUE: 'LOCAL',
      LABEL_KEY: 'common.local'
    },
    LDAP: {
      VALUE: 'LDAP',
      LABEL_KEY: 'common.ldap'
    },
    JWT: {
      VALUE: 'JWT',
      LABEL_KEY: 'common.jwt'
    }
  }
});
