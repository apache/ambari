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
  'handlebars',
  'utils/LangSupport'
  ], function(require, Handlebars, localization) {
  /*
   * General guidelines while writing helpers:
   *
   * - If returning HTML use return new Handlebars.SafeString();
   * - If the helper needs optional arguments use the "hash arguments"
   *   Eg. {{{link . "See more..." story.url class="story"}}}
   *   NOTE: the first argument after the helper name should be . which will be context in the helper function
   *   Handlebars.registerHelper('link', function (context, text, url, options) {
   *    var attrs = [];
   *
   *    for(var prop in options.hash) {
   *      attrs.push(prop + '="' + options.hash[prop] + '"');
   *    }
   *    return new Handlebars.SafeString("<a " + attrs.join(" ") + ">" + text + "</a>");
   *   });
   *
   *
   * NOTE: Due to some limitations in the require-handlebars-plugin, we cannot have helper that takes zero arguments,
   *       for such helpers we have to pass a "." as first argument. [https://github.com/SlexAxton/require-handlebars-plugin/issues/72]
   */

  var HHelpers = {};

  /**
   * Convert new line (\n\r) to <br>
   * from http://phpjs.org/functions/nl2br:480
   */
  HHelpers.nl2br = function(text) {
    text = Handlebars.Utils.escapeExpression(text);
    var nl2br = (text + '').replace(/([^>\r\n]?)(\r\n|\n\r|\r|\n)/g, '$1' + '<br>' + '$2');
    return new Handlebars.SafeString(nl2br);
  };
  Handlebars.registerHelper('nl2br', HHelpers.nl2br);

  Handlebars.registerHelper('toHumanDate', function(val) {
    if (!val) return "";
    return localization.formatDate(val, 'f');
  });

  /*
   * Truncate the String till n positions
   */
  Handlebars.registerHelper('truncStr', function(str, n, useWordBoundary) {
    var len = n || 1;
    var useWordBn = useWordBoundary || false;
    return str.trunc(len, useWordBn);
  });


  Handlebars.registerHelper('tt', function(str) {
    return localization.tt(str);
    return str;
  });

  Handlebars.registerHelper('if_eq', function(context, options) {
    if (context == options.hash.compare)
      return options.fn(this);
    return options.inverse(this);
  });

  Handlebars.registerHelper('if_gt', function(context, options) {
    if (context > options.hash.compare)
      return options.fn(this);
    return options.inverse(this);
  });

  Handlebars.registerHelper('ifCond', function(v1, operator, v2, options) {

    switch (operator) {
      case '==':
        return (v1 == v2) ? options.fn(this) : options.inverse(this);
        break;
      case '===':
        return (v1 === v2) ? options.fn(this) : options.inverse(this);
        break;
      case '<':
        return (v1 < v2) ? options.fn(this) : options.inverse(this);
        break;
      case '<=':
        return (v1 <= v2) ? options.fn(this) : options.inverse(this);
        break;
      case '>':
        return (v1 > v2) ? options.fn(this) : options.inverse(this);
        break;
      case '>=':
        return (v1 >= v2) ? options.fn(this) : options.inverse(this);
        break;
      default:
        return options.inverse(this);
        break;
    }
    //return options.inverse(this);
  });

  //For Example
  /*{{#compare numberone "eq" numbretwo}}
    do something
  {{else}}
    do something else
  {{/compare}}
*/
  Handlebars.registerHelper("compare", function(v1, op, v2, options) {

    var c = {
      "eq": function(v1, v2) {
        return v1 == v2;
      },
      "neq": function(v1, v2) {
        return v1 != v2;
      },

    };

    if (Object.prototype.hasOwnProperty.call(c, op)) {
      return c[op].call(this, v1, v2) ? options.fn(this) : options.inverse(this);
    }
    return options.inverse(this);
  });
  //For Example
  //{{#eachProperty object}}
  //{{property}}: {{value}}<br/>
  //{{/eachProperty }}
  Handlebars.registerHelper('eachProperty', function(context, options) {
    var ret = "";
    for (var prop in context) {
      ret = ret + options.fn({
        property: prop,
        value: context[prop]
      });
    }
    return new Handlebars.SafeString(ret);
  });

  return HHelpers;
});