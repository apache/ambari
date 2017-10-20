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

define(['react', 'react-dom', 'bootstrap'], function(React, ReactDOM) {
	'use strict';
	return React.createClass({
		displayName: 'Modal',
		propTypes: {
			modalId: React.PropTypes.string.isRequired,
			header: React.PropTypes.node,
			content: React.PropTypes.node,
			footer: React.PropTypes.node
		},
		getInitialState: function(){
			
			return null;
		},
		componentDidUpdate: function(){
			
		},
		componentDidMount: function(){

		},
		render: function() {
			return (
				<div className="modal fade" id={this.props.modalId} role="dialog">
				    <div className="modal-dialog">
				      	<div className="modal-content">
				        	<div className="modal-header">
				          		<button type="button" className="close" data-dismiss="modal">&times;</button>
				          		<h4 className="modal-title">{this.props.header ? <this.props.header /> : null}</h4>
				        	</div>
			        		<div className="modal-body">
				          		{this.props.content ? <this.props.content /> : null}
				        	</div>
				        	<div className="modal-footer">
				          		{this.props.footer ? <this.props.footer /> : <button type="button" className="btn btn-default" data-dismiss="modal">Close</button>}
				        	</div>
				      	</div>
				    </div>
				</div>
			);
		},
	});
});