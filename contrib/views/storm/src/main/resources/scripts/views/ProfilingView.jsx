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
	'collections/BaseCollection',
	'models/VTopology',
	'utils/Utils', 
	'utils/Globals', 
	'jsx!components/Table',
	'bootstrap'
	], function(React, ReactDOM, BaseCollection, VTopology, Utils, Globals, Table) {
	'use strict';
	return React.createClass({
		displayName: 'Profiling',
		propTypes: {
			modalId: React.PropTypes.string.isRequired,
			topologyId: React.PropTypes.string.isRequired,
			executorStats: React.PropTypes.array.isRequired
		},
		getInitialState: function(){
			this.model = new VTopology();
			this.selectedWorker = [];
			return null;
		},
		componentWillMount: function(){
			this.syncData();
		},
		componentDidMount: function(){
			$('.error-msg').hide();
			$('.warning-msg').hide();
			$('.success-msg').hide();
		},
		componentDidUpdate: function(){

		},
		syncData: function(){
			this.collection = new BaseCollection();
			if(this.props.executorStats.length){
				var data = {};
				this.props.executorStats.map(function(obj){
					var hostPort = obj.host + ":" + obj.port;
					if(!data[hostPort]){
						data[hostPort] = {};
					}
					if(!data[hostPort].idArr){
						data[hostPort].idArr = [];
					}
					data[hostPort].idArr.push(obj.id);
				});
				var keys = this.hostPortArr = _.keys(data);
				keys.map(function(key){
					this.collection.add(new Backbone.Model({
						hostPort: key,
						executorId: data[key].idArr
					}));
				}.bind(this));
			}
		},
		handleJStackOp: function(){
			this.performOp('JStack');
		},
		handleRestartWorker: function(){
			this.performOp('RestartWorker');
		},
		handleHeapOp: function(){
			this.performOp('Heap');
		},
		performOp: function(opType){
			if(!this.selectedWorker.length){
				$('.warning-msg').show();
				$('.success-msg').hide();
				$('.error-msg').hide();
			} else {
				$('.warning-msg').hide();
				$('.success-msg').hide();
				$('.error-msg').hide();
				var promiseArr = [];
				this.selectedWorker.map(function(worker){
					var obj = {
						id: this.props.topologyId,
						hostPort: worker
					};
					if(opType === 'JStack'){
						promiseArr.push(this.model.profileJStack(obj));
					} else if(opType === 'RestartWorker'){
						promiseArr.push(this.model.profileRestartWorker(obj));
					} else if(opType === 'Heap'){
						promiseArr.push(this.model.profileHeap(obj));
					}
				}.bind(this));
				Promise.all(promiseArr)
				.then(function(resultsArr){
					$('.success-msg').show();
				})
				.catch(function(){
					$('.error-msg').show();
				});
			}
		},
		getColumns: function(){
			var self = this;
			return [
				{
					name: 'hostPort',
					title: React.createClass({
						handleChange: function(e){
							if($(e.currentTarget).prop('checked')){
								self.selectedWorker = self.hostPortArr;
								$('[name="single"]').prop("checked", true)
							} else {
								self.selectedWorker = [];
								$('[name="single"]').prop("checked", false)
							}
						},
						render: function(){
							return (
								<input type="checkbox" name="selectAll" onChange={this.handleChange}/>
							);
						}
					}), 
					component: React.createClass({
						propTypes: {
							model: React.PropTypes.object.isRequired
						},
						handleChange: function(e){
							var hostPort = this.props.model.get('hostPort')
							if($(e.currentTarget).prop('checked')){
								self.selectedWorker.push(hostPort);
							} else {
								var index = _.indexOf(self.selectedWorker, hostPort);
								if(index > -1){
									self.selectedWorker.splice(index, 1);
								}
							}
						},
						render: function(){
							return (
								<input type="checkbox" name="single" onChange={this.handleChange}/>
							);
						}
					})
				},
				{name: 'hostPort', title:'Host:Port'},
				{name: 'executorId', title:'Executor Id', component: React.createClass({
					propTypes: {
						model: React.PropTypes.object.isRequired
					},
					render: function(){
						var executors = this.props.model.get('executorId').join(', ');
						return (
							<span>{executors}</span>
						);
					}
				})}
			];
		},
		closeModal: function(){
			$('#'+this.props.modalId).modal("hide");
		},
		render: function() {
			return (
				<div className="modal fade" id={this.props.modalId} role="dialog">
				    <div className="modal-dialog">
				      	<div className="modal-content">
				        	<div className="modal-header">
				          		<button type="button" className="close" data-dismiss="modal">&times;</button>
				          		<h4 className="modal-title">Profiling & Debugging</h4>
				        	</div>
			        		<div className="modal-body">
			        			<div className="alert alert-warning alert-dismissible warning-msg" role="alert">
								  <strong>Warning!</strong> Please select atleast one worker to perform operation.
  								</div>
  								<div className="alert alert-success alert-dismissible success-msg" role="alert">
								  <strong>Success!</strong> Action performed successfully.
  								</div>
  								<div className="alert alert-danger alert-dismissible error-msg" role="alert">
								  <strong>Error!</strong> Error occured while performing the action.
  								</div>
			        			<div className="clearfix">
									<div className="btn-group btn-group-sm pull-right">
										<button type="button" className="btn btn-primary" onClick={this.handleJStackOp}>JStack</button>
										<button type="button" className="btn btn-primary" onClick={this.handleRestartWorker}>Restart Worker</button>
										<button type="button" className="btn btn-primary" onClick={this.handleHeapOp}>Heap</button>
									</div>
								</div>
								<hr />
			        			<Table className="table table-bordered" collection={this.collection} columns={this.getColumns()} emptyText="No workers found !" />
				        	</div>
				        	<div className="modal-footer">
				          		<button type="button" className="btn btn-default" onClick={this.closeModal}>Close</button>
				        	</div>
				      	</div>
				    </div>
				</div>
			);
		},
	});
});