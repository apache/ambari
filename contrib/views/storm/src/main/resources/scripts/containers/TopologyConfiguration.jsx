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
	'jsx!components/Table',
	'jsx!modules/Table/Pagination',
	'collections/VTopologyConfigList',
	'models/VTopologyConfig',
	'bootstrap'
	], function(React, ReactDOM, Table, Pagination, VTopologyConfigList, VTopologyConfig){
	'use strict';

	return React.createClass({
		displayName: 'TopologyConfiguration',
		propTypes: {
			configArr: React.PropTypes.object.isRequired
		},
		getInitialState: function(){
			this.collection = new VTopologyConfigList();
			this.collection.comparator = "key";
			return null;
		},
		componentDidMount: function() {
			$('#collapseBody').on('hidden.bs.collapse', function () {
				$("#collapseTable").toggleClass("fa-compress fa-expand");
			}).on('shown.bs.collapse', function() {
				$("#collapseTable").toggleClass("fa-compress fa-expand");
			});
		},
		componentDidUpdate: function(){
			var keys = _.keys(this.props.configArr);
			var results = [];
			for(var k in keys){
				results.push(new VTopologyConfig({
					key: keys[k],
					value: String(this.props.configArr[keys[k]])
				}));
			}
			this.collection.getFirstPage().fullCollection.reset(results);
		},
		getColumns: function(){
			return [
				{name: 'key', title: 'Key'},
				{name: 'value', title: 'Value'}
			];
		},
		handleFilter: function(e){
			var value = e.currentTarget.value;
			this.collection.search(value);
		},
		handleCollapseClick: function(e){
			$("#collapseBody").collapse('toggle');
  		},
		render: function(){
			return (
				<div className="box">
		            <div className="box-header" data-toggle="collapse" data-target="#collapseBody" aria-expanded="false" aria-controls="collapseBody">
		                <h4>Topology Configuration</h4>
		                <div className="box-control">
		                	<a href="javascript:void(0);" className="primary"><i className="fa fa-expand" id="collapseTable" onClick={this.handleCollapseClick}></i></a>
		                </div>
		            </div>
		            <div className="box-body collapse" id="collapseBody">
		                	<div className="input-group col-sm-4">
								<input type="text"  onKeyUp={this.handleFilter} className="form-control" placeholder="Search By Key" />
								<span className="input-group-btn">
								<button className="btn btn-primary" type="button"><i className="fa fa-search"></i></button>
								</span>
							</div>
		                <Table className="table no-margin" collection={this.collection} emptyText="No topology configuration found !" columns={this.getColumns()}/>
		                <Pagination collection={this.collection} /> 
		            </div>
		        </div>
			);
		}
	});
});