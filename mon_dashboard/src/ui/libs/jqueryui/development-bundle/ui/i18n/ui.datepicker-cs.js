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

/* Czech initialisation for the jQuery UI date picker plugin. */
/* Written by Tomas Muller (tomas@tomas-muller.net). */
jQuery(function($){
	$.datepicker.regional['cs'] = {
		closeText: 'Zavřít',
		prevText: '&#x3c;Dříve',
		nextText: 'Později&#x3e;',
		currentText: 'Nyní',
		monthNames: ['leden','únor','březen','duben','květen','červen',
        'červenec','srpen','září','říjen','listopad','prosinec'],
		monthNamesShort: ['led','úno','bře','dub','kvě','čer',
		'čvc','srp','zář','říj','lis','pro'],
		dayNames: ['neděle', 'pondělí', 'úterý', 'středa', 'čtvrtek', 'pátek', 'sobota'],
		dayNamesShort: ['ne', 'po', 'út', 'st', 'čt', 'pá', 'so'],
		dayNamesMin: ['ne','po','út','st','čt','pá','so'],
		dateFormat: 'dd.mm.yy', firstDay: 1,
		isRTL: false};
	$.datepicker.setDefaults($.datepicker.regional['cs']);
});
