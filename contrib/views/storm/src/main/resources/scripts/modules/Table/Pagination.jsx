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

define(['react', 'utils/Globals'], function(React, Globals){
  'use strict';
  return React.createClass({
    displayName: 'Pagination',
    propTypes: {
      collection: React.PropTypes.object.isRequired,
      maximumPages: React.PropTypes.number
    },
    getInitialState: function(){
      this.props.collection.on('reset', function(data){
        this.setState({'collection': data});
      }.bind(this));
      return {
        collection: this.props.collection
      };
    },
    /**
     * Next page button being clicked
     */
    nextPage: function(e) {
      e.preventDefault();
      this.props.collection.getNextPage(this.props.collection.state.currentPage);
    },
    /**
     * Previous page button being clicked
     */
    previousPage: function(e) {
      e.preventDefault();
      this.props.collection.getPreviousPage(this.props.collection.state.currentPage);
    },
    /**
     * Change page being clicked
     * @param {Event} e Event of the page number being clicked
     */
    changePage: function(e) {
      e.preventDefault();
      var pageNumber = +e.currentTarget.getAttribute('data-page');
      this.props.collection.getParticularPage(pageNumber);
    },
    /**
     * Render function for the next page button.
     * If the current page is the last then it shouldn't render a clickable next page
     */
    renderNext: function() {
      if(this.props.collection.state.currentPage < this.props.collection.state.totalPages){
        return (<li><a href="javascript: void(0);" ref="nextPage" onClick={this.nextPage}>&raquo;</a></li>);
      } else {
        return (<li className="disabled"><a href="javascript: void 0;">&raquo;</a></li>);
      }
    },
    /**
     * Render functon for the pages
     * If the number of maximumPages is exceeded by the number of pages then that must be handled with an ellipsis
     * If the page is active then it should have the active class
     *
     */
    renderPages: function(){
      var pages = [];
      var starterPage = 1;
      if(this.props.collection.state.currentPage >= 4) {
        starterPage = this.props.collection.state.currentPage - 1;
      }
      var page = 1;
      if(!this.props.maximumPages || this.props.maximumPages > this.props.collection.state.totalPages) {
        for(page = 1; page <= this.props.collection.state.totalPages; page++){
          if(page !== this.props.collection.state.currentPage) {
            pages.push(<li key={page}><a href="javascript: void 0;" onClick={this.changePage} data-page={page}>{page}</a></li>);
          } else {
            pages.push(<li key={page} className="active"><a href="javascript: void 0;" >{page}</a></li>);

          }
        }
      } else {
        if(this.props.collection.state.currentPage >= 4) {
          pages.push(<li key={1}><a href="javascript: void 0;" onClick={this.changePage} data-page={1} >{1}</a></li>);
          pages.push(<li  key="leftellips" className="disabled"><a href="javascript: void 0;">&hellip;</a></li>);

        }
        for(page = starterPage; page <= this.props.collection.state.totalPages; ++page) {
          if((starterPage + this.props.maximumPages) < page && (page + this.props.maximumPages) < this.props.collection.state.totalPages) {
            pages.push(<li key={'ellips'} className="disabled"><a href="javascript: void 0;">&hellip;</a></li>);
            pages.push(<li key={'collection.state.totalPages'}><a href="javascript: void 0;" onClick={this.changePage} data-page={this.props.collection.state.totalPages} className="">{this.props.collection.state.totalPages}</a></li>);
            break;
          } else if (page !== this.props.collection.state.currentPage){
            pages.push(<li key={page}><a href="javascript: void 0;" onClick={this.changePage} data-page={page} className="">{page}</a></li>);
          } else {
            pages.push(<li key={page} className="active"><a href="javascript: void 0;" >{page}</a></li>);

          }
        }
      }
      return pages;

    },
    /**
     * Render function for the previous page button.
     * If the current page is the first then it shouldn't render a clickable previous page
     */
    renderPrevious : function() {
      if(this.props.collection.state.currentPage > 1){
        return (<li className=""><a href="javascript: void 0;"  ref="prevPage" onClick={this.previousPage}>&laquo;</a></li>);
      }else {
        return (<li className="disabled"><a href="javascript: void 0;" >&laquo;</a></li>);
      }
    },
    renderNumber: function(){
      var startNumber, endNumber;
      if(this.props.collection.state.currentPage > 1){
        startNumber = ((this.props.collection.state.currentPage - 1) * Globals.settings.PAGE_SIZE) + 1;
        endNumber = startNumber + Globals.settings.PAGE_SIZE - 1;
        if(endNumber > this.props.collection.state.totalRecords){
          endNumber = this.props.collection.state.totalRecords;
        }
      } else {
        startNumber = 1;
        if(this.props.collection.state.totalRecords){
          endNumber = (this.props.collection.state.totalRecords > Globals.settings.PAGE_SIZE ? Globals.settings.PAGE_SIZE : this.props.collection.state.totalRecords);
        } else {
          startNumber = 0;
          endNumber = 0;
        }
      }
      return (
        <span className="pull-left">Showing {startNumber} to {endNumber} of {this.props.collection.state.totalRecords || 0} entries.</span>
      );
    },

    render: function () {
      var next = this.renderNext();
      var pages = this.renderPages();
      var previous = this.renderPrevious();
      var number = this.renderNumber();
      return(<div className="clearfix">
        {number}
        <ul className="pagination pagination-sm pull-right no-margin">
          {previous}
          {pages}
          {next}
        </ul>
      </div>);
    }
  });
});