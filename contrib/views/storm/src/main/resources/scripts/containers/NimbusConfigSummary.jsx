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
	'utils/Utils',
	'jsx!modules/Table/Pagination',
	'collections/VNimbusConfigList',
	'models/VNimbusConfig',
	'bootstrap'
	], function(React, ReactDOM, Table, Utils, Pagination, VNimbusConfigList, VNimbusConfig){
	'use strict';

	return React.createClass({
		displayName: 'NimbusConfigSummary',
		getInitialState: function(){
			this.initializeCollection();
			return null;
		},
		initializeCollection: function(){
			this.collection = new VNimbusConfigList();
			this.collection.comparator = "key";
			this.collection.fetch({
				success: function(model, response){
					if(response.error || model.error){
						Utils.notifyError(response.error || model.error+'('+model.errorMessage.split('(')[0]+')');
					} else {
						var result = [];
						var keys = _.keys(response);
						for(var k in keys){
							result.push(new VNimbusConfig({
								key: keys[k],
								value: String(response[keys[k]])
							}));
						}
						this.collection.getFirstPage().fullCollection.reset(result);
					}
				}.bind(this),
				error: function(model, response, options){
					Utils.notifyError("Error occured in fetching nimbus configuration data.");
				}
			});
		},
		componentDidMount: function() {
			$('#collapseBody').on('hidden.bs.collapse', function () {
				$("#collapseTable").toggleClass("fa-compress fa-expand");
			}).on('shown.bs.collapse', function() {
				$("#collapseTable").toggleClass("fa-compress fa-expand");
			});
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
				<div className="box node-accordian">
		            <div className="box-header" data-toggle="collapse" data-target="#collapseBody" aria-expanded="false" aria-controls="collapseBody">
		                <h4>Nimbus Configuration</h4>
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
						<Table className="table no-margin" collection={this.collection} emptyText="No nimbus configuration found !" columns={this.getColumns()}/>
						<Pagination collection={this.collection} />
		            </div>
		        </div>
			);
		}
	});
});