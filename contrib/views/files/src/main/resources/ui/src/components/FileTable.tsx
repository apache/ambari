/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { ChevronDown, ChevronUp, File, Folder, LockKeyhole, ShieldCheck } from "lucide-react";
import { basename, formatDate, humanSize } from "../format";
import type { HdfsFile } from "../types";

export type SortKey = "name" | "len" | "modificationTime" | "owner" | "group";
export type SortState = { key: SortKey; direction: "asc" | "desc" } | null;

type FileTableProps = {
  files: HdfsFile[];
  selected: Set<string>;
  sort: SortState;
  onSort: (key: SortKey) => void;
  onSelect: (file: HdfsFile, event: React.MouseEvent) => void;
  onOpen: (file: HdfsFile) => void;
};

const Header = ({ label, field, sort, onSort }: { label: string; field?: SortKey; sort: SortState; onSort: (key: SortKey) => void }) => (
  <th>
    {field ? (
      <button type="button" className="sort-button" onClick={() => onSort(field)}>
        {label}
        {sort?.key === field && (sort.direction === "asc" ? <ChevronUp size={14} /> : <ChevronDown size={14} />)}
      </button>
    ) : label}
  </th>
);

export default function FileTable({ files, selected, sort, onSort, onSelect, onOpen }: FileTableProps) {
  return (
    <div className="file-table-wrap">
      <table className="file-table">
        <thead>
          <tr>
            <Header label="Name" field="name" sort={sort} onSort={onSort} />
            <Header label="Size" field="len" sort={sort} onSort={onSort} />
            <Header label="Last modified" field="modificationTime" sort={sort} onSort={onSort} />
            <Header label="Owner" field="owner" sort={sort} onSort={onSort} />
            <Header label="Group" field="group" sort={sort} onSort={onSort} />
            <Header label="Permission" sort={sort} onSort={onSort} />
            <Header label="Erasure coding" sort={sort} onSort={onSort} />
            <Header label="Encrypted" sort={sort} onSort={onSort} />
          </tr>
        </thead>
        <tbody>
          {files.map((file) => (
            <tr
              key={file.path}
              className={selected.has(file.path) ? "selected" : ""}
              aria-selected={selected.has(file.path)}
              onClick={(event) => onSelect(file, event)}
              onDoubleClick={() => onOpen(file)}
            >
              <td className="file-name">
                {file.isDirectory ? <Folder size={18} /> : <File size={18} />}
                <button type="button" onClick={(event) => { event.stopPropagation(); onOpen(file); }}>{basename(file.path)}</button>
              </td>
              <td>{file.isDirectory ? "-" : humanSize(file.len)}</td>
              <td>{formatDate(file.modificationTime)}</td>
              <td>{file.owner || "-"}</td>
              <td>{file.group || "-"}</td>
              <td className="mono">{file.permission || "-"}</td>
              <td>{file.erasureCodingPolicyName || (file.isErasureCoded ? "Enabled" : "-")}</td>
              <td>{file.isEncrypted ? <span className="status-icon" title="Encrypted"><LockKeyhole size={16} /></span> : <span className="status-icon muted" title="Not encrypted"><ShieldCheck size={16} /></span>}</td>
            </tr>
          ))}
        </tbody>
      </table>
      {files.length === 0 && <div className="empty-state">This directory is empty.</div>}
    </div>
  );
}
