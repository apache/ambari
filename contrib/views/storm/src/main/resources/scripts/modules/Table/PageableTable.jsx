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

var React = require('react');
var Table = require('jsx!components/Table');
var Pagination = require('jsx!modules/Table/Pagination');


module.exports = React.createClass({
  displayName: 'PageableTable',
  propTypes: {
    nextPageCallback: React.PropTypes.func.isRequired,
    previousPageCallback: React.PropTypes.func.isRequired,
    pageCallback: React.PropTypes.func.isRequired,
    totalPages: React.PropTypes.number.isRequired,
    currentPage: React.PropTypes.number.isRequired,
    maximumPages: React.PropTypes.number,
    collection: React.PropTypes.array.isRequired,
    columns: React.PropTypes.object.isRequired,
    sortingCallback: React.PropTypes.func.isRequired,
    sortKey: React.PropTypes.string,
    sortOrder: React.PropTypes.number
  },
  render: function () {
    var maximumPages = this.props.maximumPages  ? this.props.maximumPages : 10;
    return(
      <div>
        <Table {...this.props} />
        <Pagination {...this.props} maximumPages={maximumPages}/>
     </div>);
  }
});