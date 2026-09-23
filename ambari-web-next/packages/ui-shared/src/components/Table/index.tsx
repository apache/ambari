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
import "./style.css";
import { get } from "lodash";
interface TableProps {
  columns: ColumnDef<unknown, unknown>[];
  data: unknown[];
  onSortingChange?: OnChangeFn<SortingState>;
  sorting?: SortingState;
  className?: string;
  restProps?: any;
  striped?: boolean;
  bordered?: boolean;
  hover?: boolean;
  entityName?: string;
}

const Table: React.FC<TableProps> = ({
  columns,
  data,
  sorting,
  onSortingChange,
  entityName,
  ...restProps
}) => {
  const table = useReactTable({
    columns,
    data,
    debugTable: true,
    getCoreRowModel: getCoreRowModel(),
    getSortedRowModel: getSortedRowModel(), //client-side sorting
    onSortingChange,
    state: {
      sorting,
    },
  });

  if (!data.length && entityName) {
    return (
      <div className="d-flex justify-content-center">
        <h4>NO {entityName.toUpperCase()} TO DISPLAY.</h4>
      </div>
    );
  }

  return (
    <div>
      <BootstrapTable responsive {...restProps}>
        <thead>
          {table.getHeaderGroups().map((headerGroup) => (
            <tr key={headerGroup.id}>
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
                                sorting?.[0].id === header.id
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
                        {{
                          asc: "a",
                          desc: "d",
                        }[header.column.getIsSorted() as string] ?? null}
                      </div>
                    )}
                  </th>
                );
              })}
            </tr>
          ))}
        </thead>
        <tbody>
          {table.getRowModel().rows.map((row) => {
            return (
              <tr key={row.id} className="text-break" role="listitem">
                {row.getVisibleCells().map((cell) => {
                  return (
                    <td
                      key={cell.id}
                      style={{
                        width: get(cell, "column.columnDef.width", "auto")
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
