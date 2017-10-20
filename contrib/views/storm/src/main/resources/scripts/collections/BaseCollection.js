/**
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at
*
     http://www.apache.org/licenses/LICENSE-2.0
*
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/

define(['require',
  'utils/Globals',
  'backbone.paginator'
  ], function (require, Globals) {
  'use strict';

  var BaseCollection = Backbone.PageableCollection.extend(
    /** @lends BaseCollection.prototype */
    {
      /**
       * BaseCollection's initialize function
       * @augments Backbone.PageableCollection
       * @constructs
       */

      initialize: function () {

      },
      bindErrorEvents: function () {
        this.bind("error", function(model, error) {
          if (error.status == 401) {
            throw new Error("ERROR 401 occured.");
          }
        });
      },
      search : function(letters){
        var self = this;
        if(letters === ""){
          if(this.unfilteredCollection){
            this.getFirstPage().fullCollection.reset(this.unfilteredCollection.models);
          }
        } else {
          if(this.unfilteredCollection){
            this.getFirstPage().fullCollection.reset(this.unfilteredCollection.models);
          }
          var results = _.filter(this.fullCollection.models,function(model) {
              var pattern = letters;
              if(pattern.indexOf('\\') > -1)
                pattern = pattern.replace(/\\/g, '\\\\');
              if(pattern.indexOf('*') > -1)
                pattern = pattern.replace(/\*/g, '\\*');
              if(pattern.indexOf('$') > -1)
                pattern = pattern.replace(/\$/g, '\\$');
              if(pattern.indexOf('^') > -1)
                pattern = pattern.replace(/\^/g, '\\^');
              if(pattern.indexOf('+') > -1)
                pattern = pattern.replace(/\+/g, '\\+');
              if(pattern.indexOf('?') > -1)
                pattern = pattern.replace(/\?/g, '\\?');
              if(pattern.indexOf('(') > -1)
                pattern = pattern.replace(/\(/g, '\\(');
              if(pattern.indexOf(')') > -1)
                pattern = pattern.replace(/\)/g, '\\)');
              if(pattern.indexOf('[') > -1)
                pattern = pattern.replace(/\[/g, '\\[');
              if(pattern.indexOf(']') > -1)
                pattern = pattern.replace(/\]/g, '\\]');

             var regexTest = new RegExp(pattern,"i");
             var result = false;
              _.each(this.searchFields, function(field) {
                if(regexTest.test(model.get(field))) {
                  result = true;
                }
             });
             return result;
          }.bind(this));
          if(!this.unfilteredCollection) {
            this.unfilteredCollection = this.fullCollection.clone();
          }

          this.getFirstPage().fullCollection.reset(results);
        }
      },
      /**
       * state required for the PageableCollection
       */
      state: {
        // firstPage: 0,
        pageSize: Globals.settings.PAGE_SIZE
      },

      mode: 'client',

      /**
       * override the parseState of PageableCollection for our use
       */
      parse: function (resp, options) {
        var newState = this.parseState(resp, _.clone(this.queryParams), _.clone(this.state), options);
        try {
          if (newState) {
            this.state = this._checkState(_.extend({}, this.state, newState));
          }
        } catch (error) {
          if (error.name === 'RangeError') {
            this.state.currentPage = 0;
            this.state.startIndex = 0;
            this.fetch({
              reset: true
            });
          }
        }
        return this.parseRecords(resp, options);
      },
      parseRecords: function (resp, options) {
        // try {
        //   if (!this.modelAttrName) {
        //     throw new Error("this.modelAttrName not defined for " + this);
        //   }
        //   return Globalize.byString(resp, this.modelAttrName);
        // } catch (e) {
        //   console.log(e);
        // }
      },

      ////////////////////////////////////////////////////////////
      // Overriding backbone-pageable page handlers methods   //
      ////////////////////////////////////////////////////////////
      getFirstPage: function (options) {
        return this.getPage("first", _.extend({
          reset: true
        }, options));
      },

      getPreviousPage: function (options) {
        return this.getPage("prev", _.extend({
          reset: true
        }, options));
      },

      getNextPage: function (options) {
        return this.getPage("next", _.extend({
          reset: true
        }, options));
      },

      getLastPage: function (options) {
        return this.getPage("last", _.extend({
          reset: true
        }, options));
      },

      getParticularPage: function (pageNumber, options){
        return this.getPage(pageNumber, _.extend({
          reset: true
        }, options));
      }
        /////////////////////////////
        // End overriding methods //
        /////////////////////////////
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