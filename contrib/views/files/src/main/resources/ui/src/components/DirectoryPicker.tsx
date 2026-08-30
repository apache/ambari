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
import { ArrowUp, Folder } from "lucide-react";
import { useEffect, useState } from "react";
import type { FilesApi } from "../api";
import { basename, parentPath } from "../format";
import type { HdfsFile } from "../types";

export default function DirectoryPicker({ api, value, onChange }: { api: FilesApi; value: string; onChange: (value: string) => void }) {
  const [browsePath, setBrowsePath] = useState(value || "/");
  const [directories, setDirectories] = useState<HdfsFile[]>([]);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    setError("");
    api.list(browsePath)
      .then((listing) => active && setDirectories(listing.files.filter((entry) => entry.isDirectory)))
      .catch((reason: unknown) => active && setError(reason instanceof Error ? reason.message : String(reason)));
    return () => { active = false; };
  }, [api, browsePath]);

  const navigate = (path: string) => {
    setBrowsePath(path);
    onChange(path);
  };

  return (
    <div className="directory-picker">
      <label htmlFor="destination-path">Destination path</label>
      <input id="destination-path" value={value} onChange={(event) => onChange(event.target.value)} />
      <div className="directory-list" aria-label="Directory browser">
        {browsePath !== "/" && (
          <button type="button" onClick={() => navigate(parentPath(browsePath) || "/")}>
            <ArrowUp size={16} /> Parent directory
          </button>
        )}
        {directories.map((directory) => (
          <button type="button" key={directory.path} onClick={() => navigate(directory.path)}>
            <Folder size={16} /> {basename(directory.path)}
          </button>
        ))}
        {!error && directories.length === 0 && <span className="empty-directory">No child directories</span>}
        {error && <span className="field-error">{error}</span>}
      </div>
    </div>
  );
}
