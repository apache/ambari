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
  "ambari-date-pre": function (date_string) {
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

  "ambari-date-asc": function (a, b) {
    return a - b;
  },

  "ambari-date-desc": function (a, b) {
    return b - a;
  },
  /**
   * Custom methods for correct bandwidth sorting
   */
  "ambari-bandwidth-pre": function (bandwidth_string) {
    bandwidth_string = (jQuery(bandwidth_string).text()) ? jQuery(bandwidth_string).text() : bandwidth_string;
    var convertedRowValue;
    if (bandwidth_string === '<1KB') {
      convertedRowValue = 1;
    } else {
      var rowValueScale = bandwidth_string.substr(bandwidth_string.length - 2, 2);
      switch (rowValueScale) {
        case 'KB':
          convertedRowValue = parseFloat(bandwidth_string)*1024;
          break;
        case 'MB':
          convertedRowValue = parseFloat(bandwidth_string)*1048576;
          break;
      }
    }
    return convertedRowValue;
  },
  "ambari-bandwidth-asc": function (a, b) {
    return a - b;
  },
  "ambari-bandwidth-desc": function (a, b) {
    return b - a;
  }
});

jQuery.extend(jQuery.fn.dataTableExt.oApi, {
  "fnFilterClear": function (oSettings) {
    /* Remove global filter */
    oSettings.oPreviousSearch.sSearch = "";

    /* Remove the text of the global filter in the input boxes */
    if (typeof oSettings.aanFeatures.f != 'undefined') {
      var n = oSettings.aanFeatures.f;
      for (var i = 0, iLen = n.length; i < iLen; i++) {
        $('input', n[i]).val('');
      }
    }

    /* Remove the search text for the column filters - NOTE - if you have input boxes for these
     * filters, these will need to be reset
     */
    for (var i = 0, iLen = oSettings.aoPreSearchCols.length; i < iLen; i++) {
      oSettings.aoPreSearchCols[i].sSearch = "";
    }

    /* Redraw */
    oSettings.oApi._fnReDraw(oSettings);
  }
});

jQuery.extend(jQuery.fn.dataTableExt.oApi, {
  "fnGetColumnData": function (oSettings, iColumn, bUnique, bFiltered, bIgnoreEmpty) {
    // check that we have a column id
    if (typeof iColumn == "undefined") return [];

    // by default we only wany unique data
    if (typeof bUnique == "undefined") bUnique = true;

    // by default we do want to only look at filtered data
    if (typeof bFiltered == "undefined") bFiltered = true;

    // by default we do not wany to include empty values
    if (typeof bIgnoreEmpty == "undefined") bIgnoreEmpty = true;

    // list of rows which we're going to loop through
    var aiRows;

    // use only filtered rows
    if (bFiltered == true) aiRows = oSettings.aiDisplay;
    // use all rows
    else aiRows = oSettings.aiDisplayMaster; // all row numbers

    // set up data array
    var asResultData = new Array();

    for (var i = 0, c = aiRows.length; i < c; i++) {
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
    function (oSettings, aData, iDataIndex) {
      var inputFilters = [
        {iColumn: '0', elementId: 'star_filter', type: 'star'},
        {iColumn: '4', elementId: 'user_filter', type: 'multiple'},
        {iColumn: '5', elementId: 'jobs_filter', type: 'number' },
        {iColumn: '6', elementId: 'input_filter', type: 'bandwidth' },
        {iColumn: '7', elementId: 'output_filter', type: 'bandwidth' },
        {iColumn: '8', elementId: 'duration_filter', type: 'time' },
        {iColumn: '9', elementId: 'rundate_filter', type: 'date' }
      ];
      var match = true;
      for (i = 0; i < inputFilters.length; i++) {
        switch (inputFilters[i].type) {
          case 'date':
            if (jQuery('#' + inputFilters[i].elementId).val() !== 'Any' && match) {
              dateFilter(jQuery('#' + inputFilters[i].elementId).val(), aData[inputFilters[i].iColumn]);
            }
            break;
          case 'number':
            if (jQuery('#' + inputFilters[i].elementId).val() && match) {
              numberFilter(jQuery('#' + inputFilters[i].elementId).val(), aData[inputFilters[i].iColumn]);
            }
            break;
          case 'multiple':
            if (jQuery('#' + inputFilters[i].elementId).val() && match) {
              multipleFilter(jQuery('#' + inputFilters[i].elementId).val(), aData[inputFilters[i].iColumn]);
            }
            break;
          case 'time':
            if (jQuery('#' + inputFilters[i].elementId).val() && match) {
              timeFilter(jQuery('#' + inputFilters[i].elementId).val(), aData[inputFilters[i].iColumn]);
            }
            break;
          case 'bandwidth':
            if (jQuery('#' + inputFilters[i].elementId).val() && match) {
              bandwidthFilter(jQuery('#' + inputFilters[i].elementId).val(), aData[inputFilters[i].iColumn]);
            }
            break;
          case 'star':
            if (jQuery('#' + inputFilters[i].elementId).val() && match) {
              starFilter(jQuery('#' + inputFilters[i].elementId).val(), aData[inputFilters[i].iColumn]);
            }
            break;
        }
      }

      function starFilter(d, rowValue) {
        match = false;
        if (rowValue == null) return;
        if (rowValue.indexOf(d) != -1) match = true;
      }

      function multipleFilter(condition, rowValue) {
        var options = condition.split(',');
        match = false;
        rowValue = (jQuery(rowValue).text()) ? jQuery(rowValue).text() : rowValue;
        for (var i = 0; i < options.length; i++) {
          if (options[i] === rowValue) match = true;
        }
      }

      function timeFilter(rangeExp, rowValue) {
        var compareChar = rangeExp.charAt(0);
        var compareScale = rangeExp.charAt(rangeExp.length - 1);
        var compareValue = isNaN(parseInt(compareScale)) ? parseInt(rangeExp.substr(1, rangeExp.length - 2)) : parseInt(rangeExp.substr(1, rangeExp.length - 1));
        rowValue = (jQuery(rowValue).text()) ? jQuery(rowValue).text() : rowValue;
        var convertedRowValue = parseInt(rowValue.substr(0, 2)) * 3600 + parseInt(rowValue.substr(3, 2)) * 60 + parseInt(rowValue.substr(6, 2));
        switch (compareScale) {
          case 'm':
            convertedRowValue /= 60;
            break;
          case 'h':
            convertedRowValue /= 3600;
            break;
        }
        match = false;
        switch (compareChar) {
          case '<':
            if (compareValue > convertedRowValue) match = true;
            break;
          case '>':
            if (compareValue < convertedRowValue) match = true;
            break;
          case '=':
            if (compareValue == convertedRowValue) match = true;
            break;
          default:
            match = false;
        }
      }

      function bandwidthFilter(rangeExp, rowValue) {
        var compareChar = rangeExp.charAt(0);
        var compareScale = rangeExp.charAt(rangeExp.length - 1);
        var compareValue = isNaN(parseFloat(compareScale)) ? parseFloat(rangeExp.substr(1, rangeExp.length - 2)) : parseFloat(rangeExp.substr(1, rangeExp.length - 1));
        switch (compareScale) {
          case 'm':
            compareValue *= 1048576;
            break;
          default:
            compareValue *= 1024;
        }
        rowValue = (jQuery(rowValue).text()) ? jQuery(rowValue).text() : rowValue;
        var convertedRowValue;
        if (rowValue === '<1KB') {
          convertedRowValue = 1;
        } else {
          var rowValueScale = rowValue.substr(rowValue.length - 2, 2);
          switch (rowValueScale) {
            case 'KB':
              convertedRowValue = parseFloat(rowValue)*1024;
              break;
            case 'MB':
              convertedRowValue = parseFloat(rowValue)*1048576;
              break;
          }
        }
        match = false;
        switch (compareChar) {
          case '<':
            if (compareValue > convertedRowValue) match = true;
            break;
          case '>':
            if (compareValue < convertedRowValue) match = true;
            break;
          case '=':
            if (compareValue == convertedRowValue) match = true;
            break;
          default:
            match = false;
        }
      }

      function dateFilter(condition, rowValue) {
        refinedRowValue = $.trim(rowValue.replace(/<script[^>]*?>.*<\/script>/g, ''));
        var nowTime = new Date().getTime();
        var oneDayPast = nowTime - 86400000;
        var twoDaysPast = nowTime - 172800000;
        var sevenDaysPast = nowTime - 604800000;
        var fourteenDaysPast = nowTime - 1209600000;
        var thirtyDaysPast = nowTime - 2592000000;
        rowValue = (jQuery(rowValue).text()) ? jQuery(rowValue).text() : rowValue;
        var lastChar = rowValue.charAt(rowValue.length - 1);
        rowValue = lastChar === '*' ? rowValue.substr(0, rowValue.length - 1) : rowValue;
        rowValue = new Date(refinedRowValue).getTime();
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
          case 'Running Now':
            if (lastChar === '*') match = true;
            break;
        }
      }

      function numberFilter(rangeExp, rowValue) {
        var compareChar = rangeExp.charAt(0);
        var compareValue = parseInt(rangeExp.substr(1, rangeExp.length - 1));
        rowValue = (jQuery(rowValue).text()) ? jQuery(rowValue).text() : rowValue;
        match = false;
        switch (compareChar) {
          case '<':
            if (compareValue > rowValue) match = true;
            break;
          case '>':
            if (compareValue < rowValue) match = true;
            break;
          case '=':
            if (compareValue == rowValue) match = true;
          default:
            if (rangeExp == rowValue) match = true;
        }
      }

      return match;
    }
)
);

jQuery.extend(jQuery.fn.dataTableExt.oApi, {
  "fnFilterClear": function (oSettings) {
    /* Remove global filter */
    oSettings.oPreviousSearch.sSearch = "";

    /* Remove the text of the global filter in the input boxes */
    if (typeof oSettings.aanFeatures.f != 'undefined') {
      var n = oSettings.aanFeatures.f;
      for (var i = 0, iLen = n.length; i < iLen; i++) {
        $('input', n[i]).val('');
      }
    }

    /* Remove the search text for the column filters - NOTE - if you have input boxes for these
     * filters, these will need to be reset
     */
    for (var i = 0, iLen = oSettings.aoPreSearchCols.length; i < iLen; i++) {
      oSettings.aoPreSearchCols[i].sSearch = "";
    }

    /* Redraw */
    oSettings.oApi._fnReDraw(oSettings);
  }
});


jQuery.fn.dataTableExt.oApi.fnGetColumnData = function ( oSettings, iColumn, bUnique, bFiltered, bIgnoreEmpty ) {
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
};



