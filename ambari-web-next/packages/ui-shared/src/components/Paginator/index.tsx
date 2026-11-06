/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { Dropdown, DropdownButton, Pagination } from "react-bootstrap";

type PaginatorProps = {
  currentPage: number;
  maxPage: number;
  changePage: (pageNumber: number) => void;
  itemsPerPage: number;
  setItemsPerPage: (recordsCount: number) => void;
  totalItems: number;
};

const Paginator = ({
  currentPage,
  maxPage,
  changePage,
  itemsPerPage,
  setItemsPerPage,
  totalItems,
}: PaginatorProps) => {
  const items = [];
  const perPageOptions = [10, 25, 50, 100];
  for (let number = 1; number <= maxPage; number++) {
    if (
      number === currentPage - 1 ||
      number === currentPage ||
      number === currentPage + 1 ||
      number === 1 ||
      number === maxPage
    ) {
      items.push(
        <Pagination.Item
          key={number}
          active={number === currentPage}
          onClick={() => changePage(number)}
          className="pagination-btn"
        >
          {number}
        </Pagination.Item>
      );
    } else if (number === currentPage - 2 || number === currentPage + 2) {
      items.push(<Pagination.Ellipsis />);
    }
  }
  const firstItemIndex = (currentPage - 1) * itemsPerPage + 1;
  const lastItemIndex = Math.min(currentPage * itemsPerPage, totalItems);
  return (
    <>
      {totalItems > 10 ? (
        <div className="mt-4 border p-3 py-0" data-testid="pagination">
          <div className="d-flex justify-content-between align-items-center py-0">
            <div>
              Showing {firstItemIndex}-{lastItemIndex} of {totalItems} items
            </div>
            <div className="mt-3 d-flex">
              <DropdownButton title={itemsPerPage} size="sm" variant="light">
                {perPageOptions.map((perPageOption) => {
                  return (
                    <Dropdown.Item
                      key={perPageOption}
                      onClick={() => {
                        setItemsPerPage(perPageOption);
                      }}
                    >
                      {perPageOption}
                    </Dropdown.Item>
                  );
                })}
              </DropdownButton>
              <Pagination>
                <Pagination.Prev
                  title="Previous"
                  className="ms-1"
                  onClick={() => changePage(currentPage - 1)}
                />
                {items}
                <Pagination.Next
                  className="ms-1"
                  onClick={() => changePage(currentPage + 1)}
                />
              </Pagination>
            </div>
          </div>
        </div>
      ) : null}
    </>
  );
};

export default Paginator;
