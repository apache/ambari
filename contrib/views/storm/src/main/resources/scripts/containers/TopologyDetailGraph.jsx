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

define(['react',
	'react-dom',
	'jsx!components/TopologyGraph'
	], function(React, ReactDOM, TopologyGraph){
	'use strict';
	return React.createClass({
		displayName: 'TopologyDetailGraph',
		propTypes: {
			model: React.PropTypes.object.isRequired,
			graphData: React.PropTypes.object.isRequired
		},
		getInitialState: function(){
			return null;
		},
		componentWillUpdate: function(){
			$('#collapse-graph').off('hidden.bs.collapse').off('shown.bs.collapse');
		},
		componentDidUpdate: function(){
			$('#collapse-graph').on('hidden.bs.collapse', function () {
				$("#graph-icon").toggleClass("fa-compress fa-expand");
			}).on('shown.bs.collapse', function() {
				$("#graph-icon").toggleClass("fa-compress fa-expand");
			});
		},
		toggleAccordionIcon: function(){
			$("#collapse-graph").collapse('toggle');
		},
		render: function(){
			return (
				<div className="box">
					<div className="box-header" data-toggle="collapse" data-target="#collapse-graph" aria-expanded="false" aria-controls="collapse-graph">
						<h4>{this.props.model.get('name')}</h4>
						<h4 className="box-control">
							<a href="javascript:void(0);" className="primary">
								<i className="fa fa-compress" id="graph-icon" onClick={this.toggleAccordionIcon}></i>
							</a>
						</h4>
					</div>
					<div className="box-body graph-bg collapse in" id="collapse-graph">
						<div className="col-sm-12 text-center">
							<TopologyGraph data={this.props.graphData}/>
						</div>
					</div>
				</div>
			);
		},
	});
});