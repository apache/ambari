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
	'jsx!components/RadialChart',
	'react',
	'react-dom',
	'jsx!containers/ClusterSummary',
	'jsx!containers/NimbusSummary',
	'jsx!containers/SupervisorSummary',
	'jsx!containers/TopologyListing',
	'jsx!containers/NimbusConfigSummary'
	],function(Table,RadialChart, React, ReactDOM, ClusterSummary, NimbusSummary, SupervisorSummary, TopologyListing, NimbusConfigSummary){
	'use strict';

	return React.createClass({
		displayName: 'Dashboard',
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
					<div className="row" style={{marginTop: '20px'}}>
						<ClusterSummary />
						<TopologyListing fromDashboard={true} />
					</div>
					<div className="row">
					    <div className="col-sm-12">
					        <NimbusConfigSummary />
					    </div>
					</div>
				</div>
			);
	    }
	});
});