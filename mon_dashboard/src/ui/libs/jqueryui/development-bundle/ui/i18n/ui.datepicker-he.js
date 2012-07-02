/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
*/

/* Hebrew initialisation for the UI Datepicker extension. */
/* Written by Amir Hardon (ahardon at gmail dot com). */
jQuery(function($){
	$.datepicker.regional['he'] = {
		closeText: 'סגור',
		prevText: '&#x3c;הקודם',
		nextText: 'הבא&#x3e;',
		currentText: 'היום',
		monthNames: ['ינואר','פברואר','מרץ','אפריל','מאי','יוני',
		'יולי','אוגוסט','ספטמבר','אוקטובר','נובמבר','דצמבר'],
		monthNamesShort: ['1','2','3','4','5','6',
		'7','8','9','10','11','12'],
		dayNames: ['ראשון','שני','שלישי','רביעי','חמישי','שישי','שבת'],
		dayNamesShort: ['א\'','ב\'','ג\'','ד\'','ה\'','ו\'','שבת'],
		dayNamesMin: ['א\'','ב\'','ג\'','ד\'','ה\'','ו\'','שבת'],
		dateFormat: 'dd/mm/yy', firstDay: 0,
		isRTL: true};
	$.datepicker.setDefaults($.datepicker.regional['he']);
});
