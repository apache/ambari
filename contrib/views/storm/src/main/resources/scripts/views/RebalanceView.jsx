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
	'utils/Utils', 
	'models/VCluster', 
	'utils/Globals', 
	'bootstrap', 
	'bootstrap-slider'], function(React, ReactDOM, Utils, VCluster, Globals) {
	'use strict';
	return React.createClass({
		displayName: 'Rebalance',
		propTypes: {
			modalId: React.PropTypes.string.isRequired,
			topologyId: React.PropTypes.string.isRequired,
			topologyExecutors: React.PropTypes.string.isRequired,
			spouts: React.PropTypes.array.isRequired,
			bolts: React.PropTypes.array.isRequired
		},
		getInitialState: function(){
			var spoutArr = [];
			var boltArr = [];
			this.getClusterDetails();
			return {
				spout: spoutArr,
				bolt: boltArr,
				workers: parseInt(this.props.topologyExecutors,10),
				waitTime: 30,
				freeSlots: 0
			};
		},
		componentWillMount: function(){
			this.syncData();
		},
		componentDidMount: function(){
			$('.error-msg').hide();
		},
		componentDidUpdate: function(){
			$('#ex1').slider({
				value: this.state.workers,
				min: 0,
				step: 1,
				max: this.state.workers + this.state.freeSlots,
				tooltip_position: 'bottom',
				formatter: function(value) {
					return 'Current value: ' + value;
				}
			});
		},
		syncData: function(){
			var spoutArr, boltArr;
			if(this.props.spouts){
				spoutArr = this.props.spouts.map(function(spout){
					var obj = {
						key: spout.spoutId,
						value: spout.executors
					};
					return obj;
				});
				this.setState({'spout': spoutArr});
			}
			if(this.props.bolts){
				boltArr = this.props.bolts.map(function(bolt){
					var obj = {
						key: bolt.boltId,
						value: bolt.executors
					};
					return obj;
				});
				this.setState({'bolt': boltArr});
			}
		},
		getClusterDetails: function(){
			var model = new VCluster();
			model.fetch({
				success: function(model){
					this.setState({"freeSlots": model.get('slotsFree')});
				}.bind(this)
			});
		},
		rebalanceTopologyAction: function(e){
			var arr = $('form').serializeArray();
			var errorFlag = false;
			var finalData = {
				"rebalanceOptions": {
					"executors": {}
				},
			};
			var waitTime;
			var result = arr.map(function(obj){
				if(!errorFlag){
					if(obj.value === ''){
						errorFlag = true;
					} else {
						if(obj.name === 'workers'){
							finalData.rebalanceOptions.numWorkers = obj.value;
						} else if(obj.name === 'waitTime'){
							waitTime = obj.value;
						} else {
							finalData.rebalanceOptions.executors[obj.name] = obj.value;
						}
					}
				}
			});
			if(errorFlag){
				$('.error-msg').show();
			} else {
				$('.error-msg').hide();
				$.ajax({
			        url: Globals.baseURL + '/api/v1/topology/' + this.props.topologyId + '/rebalance/' + waitTime,
			        data: (_.keys(finalData.rebalanceOptions).length) ? JSON.stringify(finalData) : null,
			        cache: false,
			        contentType: 'application/json',
			        type: 'POST',
			        success: function(model, response, options){
			          if(!_.isUndefined(model.error)){
			            if(model.errorMessage.search("msg:") != -1){
			              var startIndex = model.errorMessage.search("msg:") + 4;
			              var endIndex = model.errorMessage.split("\n")[0].search("\\)");
			              Utils.notifyError(model.error+":<br/>"+model.errorMessage.substring(startIndex, endIndex));
			            } else {
			              Utils.notifyError(model.error);
			            }
			          } else {
			            Utils.notifySuccess("Topology rebalanced successfully.");
			          }
			          this.closeModal();
			        }.bind(this),
			        error: function(model, response, options){
			        	Utils.notifyError("Error occured in rebalancing topology.");
			        }
			      });
			}
		},
		renderSpoutInput: function(){
			if(this.state.spout){
				return this.state.spout.map(function(spout, i){
					return (
						<div key={i} className="form-group">
					      <label className="control-label col-sm-3">{spout.key}*:</label>
					      <div className="col-sm-9">
					        <input type="number" min="0" name={spout.key} className="form-control" defaultValue={spout.value} required="required"/>
					      </div>
					    </div>
					);
				});
			}
		},
		renderBoltInput: function(){
			if(this.state.bolt){
				return this.state.bolt.map(function(bolt, i){
					return (
						<div key={i} className="form-group">
					      <label className="control-label col-sm-3">{bolt.key}*:</label>
					      <div className="col-sm-9">
					        <input type="number" min="0" name={bolt.key} className="form-control" defaultValue={bolt.value} />
					      </div>
					    </div>
					);
				});
			}
		},
		closeModal: function(){
			$('#'+this.props.modalId).modal("hide");
		},
		render: function() {
			var totalExecutor = this.state.workers + this.state.freeSlots;
			return (
				<div className="modal fade" id={this.props.modalId} role="dialog" data-backdrop="static">
				    <div className="modal-dialog">
				      	<div className="modal-content">
				        	<div className="modal-header">
				          		<button type="button" className="close" data-dismiss="modal">&times;</button>
				          		<h4 className="modal-title">Rebalance Topology</h4>
				        	</div>
			        		<div className="modal-body">
			        			<div className="alert alert-danger alert-dismissible error-msg" role="alert">
								  <strong>Warning!</strong> Please fill out all the required (*) fields.
  								</div>
				          		<form className="form-horizontal" role="form">
								    <div className="form-group">
								      <label className="control-label col-sm-3">Workers*:</label>
								      <div className="col-sm-9">
								        <b>0</b><input id="ex1" name="workers" data-slider-id='ex1Slider' type="text" /><b>{totalExecutor}</b>
								      </div>
								    </div>
								    {this.renderSpoutInput()}
								    {this.renderBoltInput()}
								    <div className="form-group">
								      <label className="control-label col-sm-3">Wait Time*:</label>
								      <div className="col-sm-9">
								        <input type="number" min="0" name="waitTime" className="form-control" defaultValue={this.state.waitTime}/>
								      </div>
								    </div>
								  </form>
				        	</div>
				        	<div className="modal-footer">
				          		<button type="button" className="btn btn-default" onClick={this.closeModal}>Close</button>
								<button type="button" className="btn btn-success" onClick={this.rebalanceTopologyAction}>Save</button>
				        	</div>
				      	</div>
				    </div>
				</div>
			);
		},
	});
});