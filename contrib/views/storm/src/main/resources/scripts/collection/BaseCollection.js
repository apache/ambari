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

define(['require',
  'utils/Globals',
  'utils/Utils'
  ], function (require, Globals, Utils) {
  'use strict';

  var BaseCollection = Backbone.Collection.extend(
    /** @lends BaseCollection.prototype */
    {
      initialize: function () {},
      bindErrorEvents: function () {
        this.bind("error", Utils.defaultErrorHandler);
      },

    },
    /** BaseCollection's Static Attributes */
    {
      // Static functions
      getTableCols: function (cols, collection) {
        var retCols = _.map(cols, function (v, k, l) {
          var defaults = collection.constructor.tableCols[k];
          if (!defaults) {
            defaults = {};
          }
          return _.extend({
            'name': k
          }, defaults, v);
        });

        return retCols;
      },

      nonCrudOperation: function (url, requestMethod, options) {
        return Backbone.sync.call(this, null, this, _.extend({
          url: url,
          type: requestMethod
        }, options));
      }

    });

  return BaseCollection;
});