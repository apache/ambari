/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

jQuery.extend(jQuery.fn.dataTableExt.oSort, {
  // @see utils/date.js
  "ambari-date-pre":function (date_string) {
    date_string = $(date_string).text(); // strip Ember script tags
    var date = date_string.substring(4);
    var month = date.substring(1, 4);
    var day = date.substring(5, 7);
    var year = date.substring(9, 13);
    var hours = date.substring(14, 16);
    var minutes = date.substring(17, 19);

    var months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
    month = months.indexOf(month);
    if (month < 10) month = '0' + month;
    return year + month + day + hours + minutes;
  },

  "ambari-date-asc":function (a, b) {
    return a - b;
  },

  "ambari-date-desc":function (a, b) {
    return b - a;
  }
});

jQuery.extend(jQuery.fn.dataTableExt.oApi, {
    "fnFilterClear":function ( oSettings )
    {
        /* Remove global filter */
        oSettings.oPreviousSearch.sSearch = "";

        /* Remove the text of the global filter in the input boxes */
        if ( typeof oSettings.aanFeatures.f != 'undefined' )
        {
            var n = oSettings.aanFeatures.f;
            for ( var i=0, iLen=n.length ; i<iLen ; i++ )
            {
                $('input', n[i]).val( '' );
            }
        }

        /* Remove the search text for the column filters - NOTE - if you have input boxes for these
         * filters, these will need to be reset
         */
        for ( var i=0, iLen=oSettings.aoPreSearchCols.length ; i<iLen ; i++ )
        {
            oSettings.aoPreSearchCols[i].sSearch = "";
        }

        /* Redraw */
        oSettings.oApi._fnReDraw( oSettings );
    }
});

jQuery.extend(jQuery.fn.dataTableExt.oApi, {
    "fnGetColumnData":function ( oSettings, iColumn, bUnique, bFiltered, bIgnoreEmpty ) {
        // check that we have a column id
        if ( typeof iColumn == "undefined" ) return [];

        // by default we only wany unique data
        if ( typeof bUnique == "undefined" ) bUnique = true;

        // by default we do want to only look at filtered data
        if ( typeof bFiltered == "undefined" ) bFiltered = true;

        // by default we do not wany to include empty values
        if ( typeof bIgnoreEmpty == "undefined" ) bIgnoreEmpty = true;

        // list of rows which we're going to loop through
        var aiRows;

        // use only filtered rows
        if (bFiltered == true) aiRows = oSettings.aiDisplay;
        // use all rows
        else aiRows = oSettings.aiDisplayMaster; // all row numbers

        // set up data array
        var asResultData = new Array();

        for (var i=0,c=aiRows.length; i<c; i++) {
            iRow = aiRows[i];
            var sValue = this.fnGetData(iRow, iColumn);

            // ignore empty values?
            if (bIgnoreEmpty == true && sValue.length == 0) continue;

            // ignore unique values?
            else if (bUnique == true && jQuery.inArray(sValue, asResultData) > -1) continue;

            // else push the value onto the result data array
            else asResultData.push(sValue);
        }

        return asResultData;
    }
});

jQuery.extend($.fn.dataTableExt.afnFiltering.push(
    function( oSettings, aData, iDataIndex ) {
        var inputFilters = [
            {iColumn:'3', elementId: 'user_filter', type:'multiple'},
            {iColumn:'4', elementId: 'jobs_filter', type:'number' },
            {iColumn:'5', elementId: 'input_filter', type:'number' },
            {iColumn:'6', elementId: 'output_filter', type:'number' },
            {iColumn:'7', elementId: 'duration_filter', type:'number' },
            {iColumn:'8', elementId: 'rundate_filter', type:'date' }
        ];
        var match = true;
        for(i = 0; i < inputFilters.length; i++){
            switch(inputFilters[i].type){
                case 'date':
                    if(jQuery('#'+inputFilters[i].elementId).val() !== 'Any' && match) {
                        dateFilter(jQuery('#'+inputFilters[i].elementId).val(), aData[inputFilters[i].iColumn]);
                    }
                break;
                case 'number':
                    if(jQuery('#'+inputFilters[i].elementId).val() && match){
                        numberFilter(jQuery('#'+inputFilters[i].elementId).val(), aData[inputFilters[i].iColumn]);
                    }
                    break;
                case 'multiple':
                    if(jQuery('#'+inputFilters[i].elementId).val() && match){
                        multipleFilter(jQuery('#'+inputFilters[i].elementId).val(), aData[inputFilters[i].iColumn]);
                    }
            }
        }
        function multipleFilter(condition, rowValue){
            var options = condition.split(',');
            match = false;
            rowValue = (jQuery(rowValue).text()) ? jQuery(rowValue).text() : rowValue;
            for(var i = 0; i < options.length; i++) {
                if (options[i] === rowValue) match = true;
            }
        }

        function dateFilter(condition, rowValue) {
            var nowTime = new Date().getTime();
            var oneDayPast = nowTime - 86400000;
            var twoDaysPast = nowTime - 172800000;
            var sevenDaysPast = nowTime - 604800000;
            var fourteenDaysPast = nowTime - 1209600000;
            var thirtyDaysPast = nowTime - 2592000000;
            rowValue = (jQuery(rowValue).text()) ? jQuery(rowValue).text() : rowValue;
            rowValue = new Date(rowValue).getTime();
            match = false;
            switch (condition) {
                case 'Any':
                    match = true;
                    break;
                case 'Past 1 Day':
                    if (nowTime > rowValue && rowValue > oneDayPast) match = true;
                    break;
                case 'Past 2 Days':
                    if (nowTime > rowValue && rowValue > twoDaysPast) match = true;
                    break;
                case 'Past 7 Days':
                    if (nowTime > rowValue && rowValue > sevenDaysPast) match = true;
                    break;
                case 'Past 14 Days':
                    if (nowTime > rowValue && rowValue > fourteenDaysPast) match = true;
                    break;
                case 'Past 30 Days':
                    if (nowTime > rowValue && rowValue > thirtyDaysPast) match = true;
                    break;
            }
        }
        function numberFilter(rangeExp, rowValue){
            var compareChar = rangeExp.charAt(0);
            var compareValue = parseInt(rangeExp.substr(1, rangeExp.length - 1));
            rowValue = (jQuery(rowValue).text()) ? jQuery(rowValue).text() : rowValue;
            match = false;
            switch (compareChar) {
                case '<':
                    if(compareValue > rowValue) match = true;
                    break;
                case '>':
                    if(compareValue < rowValue) match = true;
                    break;
                case '=':
                    if(compareValue == rowValue) match = true;
                    break;
                default:
                    match = false;
            }
        }
        return match;
    }
)
);




jQuery.extend(jQuery.fn.dataTableExt.oApi, {
    "fnFilterClear":function ( oSettings )
    {
        /* Remove global filter */
        oSettings.oPreviousSearch.sSearch = "";

        /* Remove the text of the global filter in the input boxes */
        if ( typeof oSettings.aanFeatures.f != 'undefined' )
        {
            var n = oSettings.aanFeatures.f;
            for ( var i=0, iLen=n.length ; i<iLen ; i++ )
            {
                $('input', n[i]).val( '' );
            }
        }

        /* Remove the search text for the column filters - NOTE - if you have input boxes for these
         * filters, these will need to be reset
         */
        for ( var i=0, iLen=oSettings.aoPreSearchCols.length ; i<iLen ; i++ )
        {
            oSettings.aoPreSearchCols[i].sSearch = "";
        }

        /* Redraw */
        oSettings.oApi._fnReDraw( oSettings );
    }
});
jQuery.extend($.fn.dataTableExt.afnFiltering.push(
    function( oSettings, aData, iDataIndex ) {
        var inputFilters = [
            {iColumn:'4', elementId: 'jobs_filter', type:'number'},
            {iColumn:'5', elementId: 'input_filter', type:'number' },
            {iColumn:'6', elementId: 'output_filter', type:'number' },
            {iColumn:'7', elementId: 'duration_filter', type:'number' },
            {iColumn:'8', elementId: 'rundate_filter', type:'date' }
        ];
        var match = true;
        for(i = 0; i < inputFilters.length; i++){
            if(inputFilters[i].type === 'date'){
                if(jQuery('#'+inputFilters[i].elementId).val() !== 'Any' && match) {
                    dateFilter(jQuery('#'+inputFilters[i].elementId).val(), aData[inputFilters[i].iColumn]);
                }
            }
            if(inputFilters[i].type === 'number'){
                if(jQuery('#'+inputFilters[i].elementId).val() && match){
                    numberFilter(jQuery('#'+inputFilters[i].elementId).val(), aData[inputFilters[i].iColumn]);
                }
            }
        }

        function dateFilter(condition, rowValue) {
            var nowTime = new Date().getTime();
            var oneDayPast = nowTime - 86400000;
            var twoDaysPast = nowTime - 172800000;
            var sevenDaysPast = nowTime - 604800000;
            var fourteenDaysPast = nowTime - 1209600000;
            var thirtyDaysPast = nowTime - 2592000000;
            rowValue = (jQuery(rowValue).text()) ? jQuery(rowValue).text() : rowValue;
            rowValue = new Date(rowValue).getTime();
            match = false;
            switch (condition) {
                case 'Past 1 Day':
                    if (nowTime > rowValue && rowValue > oneDayPast) match = true;
                    break;
                case 'Past 2 Days':
                    if (nowTime > rowValue && rowValue > twoDaysPast) match = true;
                    break;
                case 'Past 7 Days':
                    if (nowTime > rowValue && rowValue > sevenDaysPast) match = true;
                    break;
                case 'Past 14 Days':
                    if (nowTime > rowValue && rowValue > fourteenDaysPast) match = true;
                    break;
                case 'Past 30 Days':
                    if (nowTime > rowValue && rowValue > thirtyDaysPast) match = true;
                    break;
            }
        }
        function numberFilter(rangeExp, rowValue){
            var compareChar = rangeExp.charAt(0);
            var compareValue = parseInt(rangeExp.substr(1, rangeExp.length - 1));
            rowValue = (jQuery(rowValue).text()) ? jQuery(rowValue).text() : rowValue;
            match = false;
            switch (compareChar) {
                case '<':
                    if(compareValue > rowValue) match = true;
                    break;
                case '>':
                    if(compareValue < rowValue) match = true;
                    break;
                case '=':
                    if(compareValue == rowValue) match = true;
                    break;
                default:
                    match = false;
            }
        }
        return match;
    }
)
);




