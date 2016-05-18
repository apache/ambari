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

define([
	'jsx!components/Table',
	'react',
	'react-dom',
	'jsx!containers/SupervisorSummary',
	'jsx!components/Breadcrumbs'
	],function(Table, React, ReactDOM, SupervisorSummary, Breadcrumbs){
	'use strict';

	return React.createClass({
		displayName: 'SupervisorSummaryView',
		getInitialState: function(){
			return null;
		},
		componentWillMount: function(){
			$('.loader').show();
		},
		componentDidMount: function(){
			$('.loader').hide();
		},
		componentWillUpdate: function(){
			$('.loader').show();
		},
		componentDidUpdate: function(){
			$('.loader').hide();
		},
		render: function() {			
			return (
				<div>					
					<Breadcrumbs links={this.getLinks()} />
					<div className="row">
						<div className="col-sm-12">
							<SupervisorSummary/>
						</div>
					</div>
				</div>
			);
	    },
	    getLinks: function() {
	    	var links = [
				{link: '#!/dashboard', title: 'Dashboard'},
				{link: 'javascript:void(0);', title: 'Supervisor Summary'}
				];
			return links;
	    }
	});
});