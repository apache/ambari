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

define(['react', 'react-dom'], function(React, ReactDOM) {
	'use strict';
	var Row = React.createClass({
		displayName: 'Row',
		propTypes: {
			model: React.PropTypes.object.isRequired
		},
		componentDidMount: function(){
			if(!this.props.model._highlighted){
				$(ReactDOM.findDOMNode(this)).addClass('');
				this.props.model._highlighted = true;
			}
		},
		render: function(){
			return (<tr>{this.props.children}</tr>);
		}
	});
	return React.createClass({
		displayName: 'Table',
		propTypes: {
			collection: React.PropTypes.object.isRequired,
			emptyText: React.PropTypes.string,
			columns: React.PropTypes.array.isRequired,
			limitRows: React.PropTypes.string
		},
		getInitialState: function(){
			this.highlight = false;
			return null;
		},
		componentDidMount: function() {
			this.props.collection.on('add remove change reset', this.forceUpdate.bind(this, null));
			this.highlight = true;
			$('[data-rel="tooltip"]').tooltip({
				container: '#container'
			});
		},
		render: function() {
			return ( <div className="table-responsive"><table className={this.props.className}>
				<thead>
					<tr>
						{this.getHeaderTHs()}
					</tr>
				</thead>
				<tbody>
					{this.getRows()}
				</tbody>
			</table> </div>);
		},
		getRows: function(){
			var self = this;
			var limitRows = this.props.collection.models.length;
			if(this.props.limitRows){
				limitRows = parseInt(this.props.limitRows, 10);
			}
			var rows = this.props.collection.map(function(model, i) {
				if(i < limitRows){
					if(!self.highlight){
						model._highlighted = true;
					}
					var tds = self.getRowsTDs(model);
					return (<Row key={i} model={model}>{tds}</Row>);
				}
			});
			if(!rows.length)
				rows.push(<tr key="0"><td>{this.props.emptyText}</td></tr>);
			return rows;			
		},
		getRowsTDs: function(model){
			var tds = this.props.columns.map(function(column, i){
				var content = column.component ? <column.component model={model} column={column}/> : model.get(column.name);
				return (<td key={i}>{content}</td>);
			});
			return tds;
		},
		getHeaderTHs: function(){
			var ths = this.props.columns.map(function(column, i){
				var stringTitle = typeof column.title === 'string' ? true : false;
				return (<th key={i}><span data-rel="tooltip" data-placement="bottom" title={column.tooltip ? column.tooltip : ""}>{stringTitle ? column.title : <column.title/>}</span></th>);
			});
			return ths;			
		}
	});
});