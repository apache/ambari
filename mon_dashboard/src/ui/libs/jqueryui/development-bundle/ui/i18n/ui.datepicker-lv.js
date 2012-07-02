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

/* Latvian (UTF-8) initialisation for the jQuery UI date picker plugin. */
/* @author Arturas Paleicikas <arturas.paleicikas@metasite.net> */
jQuery(function($){
	$.datepicker.regional['lv'] = {
		closeText: 'Aizvērt',
		prevText: 'Iepr',
		nextText: 'Nāka',
		currentText: 'Šodien',
		monthNames: ['Janvāris','Februāris','Marts','Aprīlis','Maijs','Jūnijs',
		'Jūlijs','Augusts','Septembris','Oktobris','Novembris','Decembris'],
		monthNamesShort: ['Jan','Feb','Mar','Apr','Mai','Jūn',
		'Jūl','Aug','Sep','Okt','Nov','Dec'],
		dayNames: ['svētdiena','pirmdiena','otrdiena','trešdiena','ceturtdiena','piektdiena','sestdiena'],
		dayNamesShort: ['svt','prm','otr','tre','ctr','pkt','sst'],
		dayNamesMin: ['Sv','Pr','Ot','Tr','Ct','Pk','Ss'],
		dateFormat: 'dd-mm-yy', firstDay: 1,
		isRTL: false};
	$.datepicker.setDefaults($.datepicker.regional['lv']);
});
