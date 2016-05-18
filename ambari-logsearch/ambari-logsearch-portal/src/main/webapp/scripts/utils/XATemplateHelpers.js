/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

 
	/*
	 * General guidelines while writing helpers:
	 * 
	 * - If returning HTML use return new Handlebars.SafeString();
	 * - If the helper needs optional arguments use the "hash arguments"
	 *   Eg. {{{link . "See more..." story.url class="story"}}}
	 *   NOTE: the first argument after the helper name should be . which will be context in the helper function
	 *   Handlebars.registerHelper('link', function(context, text, url, options) {
	 *   	var attrs = [];
	 * 		
	 *   	for(var prop in options.hash) {
	 *   		attrs.push(prop + '="' + options.hash[prop] + '"');
	 *   	}	
	 *   	return new Handlebars.SafeString("<a " + attrs.join(" ") + ">" + text + "</a>");
	 *   });
	 * 
	 * 
	 * NOTE: Due to some limitations in the require-handlebars-plugin, we cannot have helper that takes zero arguments,
	 *       for such helpers we have to pass a "." as first argument. [https://github.com/SlexAxton/require-handlebars-plugin/issues/72] 
	 */
	

define(['require','Handlebars'],function(require,Handlebars){

	var HHelpers = {};
	
	/**
     * Convert new line (\n\r) to <br>
     * from http://phpjs.org/functions/nl2br:480
     */
	Handlebars.registerHelper('nl2br', function(text) {
        text = Handlebars.XAUtils.escapeExpression(text);
        var nl2br = (text + '').replace(/([^>\r\n]?)(\r\n|\n\r|\r|\n)/g, '$1' + '<br>' + '$2');
        return new Handlebars.SafeString(nl2br);
    });
	
	/*
	 * escapeHtmlChar
	 */
	Handlebars.registerHelper("escapeHtmlChar", function(str) {
		return Util.escapeHtmlChar(str);
	});
	
	Handlebars.registerHelper("nl2brAndEscapeHtmlChar", function(str) {
		return Util.nl2brAndEscapeHtmlChar(str);
	});
	
	/*
	 * required to fetch label for enum value
	 */ 
	Handlebars.registerHelper('convertEnumValueToLabel', function(enumName, enumValue) {
		return Util.enumValueToLabel( Util.getEnum(enumName), enumValue);
	});
	
	/*
	 * Truncate the String till n positions
	 */
	Handlebars.registerHelper('truncStr', function(str, n, useWordBoundary) {
		var len = n || 1;
		var useWordBn = useWordBoundary || false;
		return str.trunc(len, useWordBn);
	});
	
	/*Handlebars.registerHelper('tt', function(str) {
		return localization.tt(str);
	});*/
	
	Handlebars.registerHelper('getCopyrightDate', function() {
		return new Date().getFullYear().toString();
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

	Handlebars.registerHelper('ifCond', function (v1, operator, v2, options) {
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


    /**
     * This helper provides a for i in range loop
     *
     * start and end parameters have to be integers >= 0 or their string representation. start should be <= end.
     * In all other cases, the block is not rendered.
     * Usage:
     *        <ul>
     *            {{#for 0 10}}
     *                <li>{{this}}</li>
     *            {{/for}}
     *        </ul>
     */
    Handlebars.registerHelper('for', function(start, end, options) {
        var fn = options.fn, inverse = options.inverse;
        var isStartValid = (start != undefined && !isNaN(parseInt(start)) && start >= 0);
        var isEndValid = (end != undefined && !isNaN(parseInt(end)) && end >= 0);
        var ret = "";

        if (isStartValid && isEndValid && parseInt(start) <= parseInt(end)) {
            for (var i = start; i <= end; i++) {
                ret = ret + fn(i);
            }
        } else {
            ret = inverse(this);
        }

        return ret;
    });

	Handlebars.registerHelper('dateFormat', function(context, block) {
		if (window.moment) {
			var f = block.hash.format || "MMM Do, YYYY";
			return moment(Date(context)).format(f);
		}else{
			return context;   //  moment plugin not available. return data as is.
		};
	});
	return HHelpers;
});
