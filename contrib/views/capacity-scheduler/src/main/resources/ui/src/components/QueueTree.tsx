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
import { ChevronDown, ChevronRight, CirclePause, CirclePlay, Plus, Trash2 } from "lucide-react";
import { useState } from "react";
import type { QueueConfig } from "../types";

type Props = {
  queues: QueueConfig[];
  selectedPath: string;
  operator: boolean;
  rmOffline: boolean;
  rmStates: Record<string, string>;
  issues: Set<string>;
  onSelect: (path: string) => void;
  onAdd: (path: string) => void;
  onDelete: (path: string) => void;
};

export default function QueueTree({
  queues,
  selectedPath,
  operator,
  rmOffline,
  rmStates,
  issues,
  onSelect,
  onAdd,
  onDelete,
}: Props) {
  const [collapsed, setCollapsed] = useState<Set<string>>(new Set());
  const childrenOf = (path: string) => queues.filter((queue) => queue.parentPath === path);
  const queueByPath = (path: string) => queues.find((queue) => queue.path === path);

  const renderQueue = (queue: QueueConfig) => {
    const children = childrenOf(queue.path);
    const isCollapsed = collapsed.has(queue.path);
    const runtimeState = rmStates[queue.path.toLowerCase()] || "UNKNOWN";
    const isNew = !queue.sourcePath;
    const canDelete = queue.path !== "root" && operator && (rmOffline || isNew || runtimeState === "STOPPED");
    const canAdd = operator && (rmOffline || isNew || children.length > 0 || runtimeState === "STOPPED");
    return (
      <li key={queue.path}>
        <div className={`queue-row ${selectedPath === queue.path ? "selected" : ""} ${issues.has(queue.path) ? "invalid" : ""}`}>
          <button
            className="tree-toggle"
            type="button"
            title={children.length ? (isCollapsed ? "Expand queue" : "Collapse queue") : "Leaf queue"}
            disabled={!children.length}
            onClick={() => setCollapsed((current) => {
              const next = new Set(current);
              if (next.has(queue.path)) next.delete(queue.path); else next.add(queue.path);
              return next;
            })}
          >
            {children.length && !isCollapsed ? <ChevronDown size={15} /> : <ChevronRight size={15} />}
          </button>
          <button className="queue-select" type="button" onClick={() => onSelect(queue.path)}>
            <span className="queue-name">{queue.name}</span>
            <span className="queue-capacity">{queue.capacity}%</span>
          </button>
          <span className={`runtime-state ${runtimeState.toLowerCase()}`} title={`ResourceManager state: ${runtimeState}`}>
            {runtimeState === "STOPPED" ? <CirclePause size={14} /> : <CirclePlay size={14} />}
          </span>
          {operator && (
            <span className="queue-actions">
              <button type="button" title={canAdd ? "Add child queue" : "Stop this leaf queue before adding a child"} disabled={!canAdd} onClick={() => onAdd(queue.path)}><Plus size={14} /></button>
              {queue.path !== "root" && <button type="button" title={canDelete ? "Delete queue" : "Stop this queue before deleting it"} disabled={!canDelete} onClick={() => onDelete(queue.path)}><Trash2 size={14} /></button>}
            </span>
          )}
        </div>
        {children.length > 0 && !isCollapsed && <ul>{children.map(renderQueue)}</ul>}
      </li>
    );
  };

  const root = queueByPath("root") ?? queues[0];
  return <ul className="queue-tree">{root && renderQueue(root)}</ul>;
}
