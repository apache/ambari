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

/* eslint-disable @typescript-eslint/no-explicit-any */
//@ts-nocheck
import React from "react";
import {
  ColumnDef,
  flexRender,
  getCoreRowModel,
  getSortedRowModel,
  OnChangeFn,
  SortingState,
  useReactTable,
} from "@tanstack/react-table";
import { Table as BootstrapTable } from "react-bootstrap";
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faSort, faCaretUp, faCaretDown } from '@fortawesome/free-solid-svg-icons';
import "./style.css";
import { get } from "lodash";
interface TableProps {
  columns: any[];
  data: unknown[];
  onSortingChange?: OnChangeFn<SortingState>;
  sorting?: SortingState;
  className?: string;
  restProps?: any;
  striped?: boolean;
  bordered?: boolean;
  hover?: boolean;
  entityName?: string;
  scrollable?: boolean;
  showHeader?: boolean;
  noBorder?: boolean;
  restProps:{[key:string]:any};
}

const Table: React.FC<TableProps> = ({
  columns,
  data,
  sorting,
  onSortingChange,
  entityName,
  scrollable = true,
  showHeader = true,
  noBorder = false,
  maxHeight,
  ...restProps
}) => {
  const table = useReactTable({
    columns,
    data,
    debugTable: false,
    getCoreRowModel: getCoreRowModel(),
    getSortedRowModel: getSortedRowModel(), //client-side sorting
    onSortingChange,
    state: {
      sorting,
    },
  });

  if (!data.length && entityName) {
    return (
      <div className="d-flex justify-content-center mt-4">
        <h4>NO {entityName.toUpperCase()} TO DISPLAY.</h4>
      </div>
    );
  }

  return (
    <div className={scrollable ? "scrollable" : ""} style={{maxHeight: maxHeight || "auto", overflowY: scrollable ? "auto" : "visible"}}>
      <BootstrapTable responsive {...restProps}>
        {showHeader ? (
          <thead>
            {table.getHeaderGroups().map((headerGroup) => (
              <tr key={headerGroup.id} >
                {headerGroup.headers.map((header) => {
                  return (
                    <th key={header.id} colSpan={header.colSpan}>
                      {header.isPlaceholder ? null : (
                        <div
                          className={
                            header.column.getCanSort()
                              ? "cursor-pointer select-none"
                              : ""
                          }
                          onClick={() => {
                            if (!header.column.getCanSort()) return;
                            onSortingChange?.([
                              {
                                id: header.id,
                                desc:
                                  sorting?.[0]?.id === header.id
                                    ? !sorting[0].desc
                                    : false,
                              },
                            ]);
                            header.column.getToggleSortingHandler();
                          }}
                          title={
                            header.column.getCanSort()
                              ? header.column.getNextSortingOrder() === "asc"
                                ? "Sort ascending"
                                : header.column.getNextSortingOrder() === "desc"
                                ? "Sort descending"
                                : "Clear sort"
                              : undefined
                          }
                        >
                          {flexRender(
                            header.column.columnDef.header,
                            header.getContext()
                          )}
                          {header.column.getCanSort() && header.column.columnDef.sortingFn!=="auto" && (
                            <span className="ms-1">
                              {header.column.getIsSorted() === "asc" ? (
                                <FontAwesomeIcon icon={faCaretUp} />
                              ) : header.column.getIsSorted() === "desc" ? (
                                <FontAwesomeIcon icon={faCaretDown} />
                              ) : (
                                <FontAwesomeIcon icon={faSort} />
                              )}
                            </span>
                          )}
                        </div>
                      )}
                    </th>
                  );
                })}
              </tr>
            ))}
          </thead>
        ) : null}
        <tbody>
          {table.getRowModel().rows.map((row) => {
            return (
              <tr key={row.id} className="text-break bg-danger-subtle" role="listitem">
                {row.getVisibleCells().map((cell) => {
                  return (
                    <td
                      key={cell.id}
                      className={get(cell, "row.original.className", "")}
                      style={{
                        width: get(cell, "column.columnDef.width", "auto"),
                        border:
                          row.index === table.getRowModel().rows.length - 1
                            ? "none"
                            : noBorder
                            ? "none"
                            : "",
                      }}
                    >
                      <div>
                        {flexRender(
                          cell.column.columnDef.cell,
                          cell.getContext()
                        )}
                      </div>
                    </td>
                  );
                })}
              </tr>
            );
          })}
        </tbody>
      </BootstrapTable>
    </div>
  );
};

export default Table;
