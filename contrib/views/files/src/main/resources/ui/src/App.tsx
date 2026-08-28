/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0.
 */
import {
  ArrowLeft,
  Bell,
  Clipboard,
  Copy,
  Download,
  Eye,
  FilePenLine,
  Files,
  FolderInput,
  FolderOpen,
  FolderPlus,
  Home,
  KeyRound,
  LoaderCircle,
  RefreshCw,
  Search,
  Trash2,
  Upload,
} from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { createFilesApi, ViewApiError, type FilesApi } from "./api";
import DirectoryPicker from "./components/DirectoryPicker";
import FileTable, { type SortKey, type SortState } from "./components/FileTable";
import Modal from "./components/Modal";
import { basename, joinPath, parentPath, permissionBits, permissionFromBits } from "./format";
import type { HdfsFile, Notification } from "./types";

type ModalName = "create" | "upload" | "rename" | "permission" | "copy" | "move" | "delete" | "preview" | "emptyTrash";
type OperationRecovery = { failed: string[]; unprocessed: string[] };

const defaultApi = createFilesApi();
const pathFromHash = () => {
  const query = window.location.hash.includes("?") ? window.location.hash.split("?")[1] : "";
  return new URLSearchParams(query).get("path") || "";
};

const detailForError = (reason: unknown) => {
  if (reason instanceof ViewApiError) {
    const operation = reason.operation;
    const paths = operation
      ? [...(operation.failed ?? []), ...(operation.unprocessed ?? [])]
      : [];
    return [reason.message, paths.length ? `Remaining paths: ${paths.join(", ")}` : "", reason.detail ?? ""]
      .filter(Boolean)
      .join("\n");
  }
  return reason instanceof Error ? reason.message : String(reason);
};

export default function App({ api = defaultApi }: { api?: FilesApi }) {
  const [health, setHealth] = useState<"checking" | "ready" | "failed">("checking");
  const [healthError, setHealthError] = useState("");
  const [files, setFiles] = useState<HdfsFile[]>([]);
  const [meta, setMeta] = useState<{ originalSize?: number; truncated?: boolean }>({});
  const [path, setPath] = useState(pathFromHash() || "/");
  const [homePath, setHomePath] = useState("");
  const [trashPath, setTrashPath] = useState("");
  const [searchText, setSearchText] = useState("");
  const [filter, setFilter] = useState("");
  const [sort, setSort] = useState<SortState>(null);
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [lastSelected, setLastSelected] = useState("");
  const [loading, setLoading] = useState(false);
  const [loadError, setLoadError] = useState("");
  const [modal, setModal] = useState<ModalName | null>(null);
  const [busy, setBusy] = useState(false);
  const [modalError, setModalError] = useState("");
  const [operationRecovery, setOperationRecovery] = useState<OperationRecovery | null>(null);
  const [name, setName] = useState("");
  const [destination, setDestination] = useState("/");
  const [permanent, setPermanent] = useState(false);
  const [bits, setBits] = useState<boolean[]>(Array(9).fill(false));
  const [uploadFile, setUploadFile] = useState<File | null>(null);
  const [previewContent, setPreviewContent] = useState("");
  const [previewOffset, setPreviewOffset] = useState(0);
  const [previewDone, setPreviewDone] = useState(false);
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [showNotifications, setShowNotifications] = useState(false);
  const notificationId = useRef(0);

  const selectedFiles = files.filter((file) => selected.has(file.path));
  const selectedFolders = selectedFiles.filter((file) => file.isDirectory).length;
  const selectedRegularFiles = selectedFiles.length - selectedFolders;
  const isTrash = Boolean(trashPath && path === trashPath);

  const notify = (level: Notification["level"], title: string, detail?: string) => {
    notificationId.current += 1;
    setNotifications((current) => [{ id: notificationId.current, level, title, detail }, ...current]);
  };

  const checkHealth = () => {
    setHealth("checking");
    setHealthError("");
    api.health()
      .then((result) => {
        if (Number(result.status) !== 200) throw new Error(result.message || "HDFS service check failed");
        setHealth("ready");
      })
      .catch((reason: unknown) => {
        setHealth("failed");
        setHealthError(detailForError(reason));
      });
  };

  useEffect(checkHealth, [api]);

  useEffect(() => {
    let active = true;
    Promise.allSettled([api.home(), api.trash()]).then(([home, trash]) => {
      if (!active) return;
      if (home.status === "fulfilled") {
        setHomePath(home.value.path);
        if (!pathFromHash()) setPath(home.value.path || "/");
      }
      if (trash.status === "fulfilled") setTrashPath(trash.value.path);
    });
    return () => { active = false; };
  }, [api]);

  const refresh = () => {
    if (health !== "ready") return;
    setLoading(true);
    setLoadError("");
    api.list(path, filter)
      .then((listing) => {
        setFiles(listing.files);
        setMeta(listing.meta ?? {});
        setSelected(new Set());
        setLastSelected("");
      })
      .catch((reason: unknown) => {
        const detail = detailForError(reason);
        setLoadError(detail);
        notify("error", `Failed to load ${path}`, detail);
      })
      .finally(() => setLoading(false));
  };

  useEffect(refresh, [api, filter, health, path]);

  const navigate = (nextPath: string) => {
    const normalized = nextPath || "/";
    window.history.replaceState(null, "", `${window.location.pathname}${window.location.search}#/files?path=${encodeURIComponent(normalized)}`);
    setPath(normalized);
    setFilter("");
    setSearchText("");
  };

  const sortedFiles = [...files].sort((left, right) => {
    if (!sort) return 0;
    const leftValue = sort.key === "name" ? basename(left.path).toLocaleLowerCase() : left[sort.key] ?? "";
    const rightValue = sort.key === "name" ? basename(right.path).toLocaleLowerCase() : right[sort.key] ?? "";
    const result = typeof leftValue === "number" && typeof rightValue === "number"
      ? leftValue - rightValue
      : String(leftValue).localeCompare(String(rightValue));
    return sort.direction === "asc" ? result : -result;
  });

  const toggleSort = (key: SortKey) => setSort((current) => {
    if (!current || current.key !== key) return { key, direction: "asc" };
    if (current.direction === "asc") return { key, direction: "desc" };
    return null;
  });

  const selectFile = (file: HdfsFile, event: React.MouseEvent) => {
    if (event.shiftKey && lastSelected) {
      const start = sortedFiles.findIndex((entry) => entry.path === lastSelected);
      const end = sortedFiles.findIndex((entry) => entry.path === file.path);
      if (start >= 0 && end >= 0) {
        const [low, high] = start < end ? [start, end] : [end, start];
        setSelected(new Set(sortedFiles.slice(low, high + 1).map((entry) => entry.path)));
        return;
      }
    }
    if (event.ctrlKey || event.metaKey) {
      setSelected((current) => {
        const next = new Set(current);
        if (next.has(file.path)) next.delete(file.path); else next.add(file.path);
        return next;
      });
    } else {
      setSelected(new Set([file.path]));
    }
    setLastSelected(file.path);
  };

  const openFile = (file: HdfsFile) => {
    if (file.isDirectory) navigate(file.path);
    else {
      setSelected(new Set([file.path]));
      setLastSelected(file.path);
      openModal("preview", file);
    }
  };

  const openModal = (nextModal: ModalName, file?: HdfsFile) => {
    const target = file ?? selectedFiles[0];
    setModalError("");
    setOperationRecovery(null);
    setBusy(false);
    setName(nextModal === "rename" && target ? basename(target.path) : "");
    setDestination("/");
    setPermanent(isTrash);
    setUploadFile(null);
    if (nextModal === "permission" && target) setBits(permissionBits(target.permission));
    if (nextModal === "preview") {
      setPreviewContent("");
      setPreviewOffset(0);
      setPreviewDone(false);
    }
    setModal(nextModal);
  };

  const closeModal = () => {
    if (!busy) setModal(null);
  };

  const runOperation = async (operation: () => Promise<unknown>, success: string) => {
    setBusy(true);
    setModalError("");
    try {
      await operation();
      notify("success", success);
      setModal(null);
      refresh();
    } catch (reason) {
      const detail = detailForError(reason);
      if (reason instanceof ViewApiError && reason.operation) {
        setOperationRecovery({
          failed: reason.operation.failed ?? [],
          unprocessed: reason.operation.unprocessed ?? [],
        });
      }
      setModalError(detail);
      notify("error", success.replace(/^Successfully /, "Failed to "), detail);
    } finally {
      setBusy(false);
    }
  };

  const validateName = (candidate: string, original?: string) => {
    if (!candidate.trim()) return "Name cannot be blank.";
    if (candidate.length > 255) return `Name cannot exceed 255 characters (received ${candidate.length}).`;
    if (candidate.includes("/")) return "Name cannot contain a slash.";
    if (candidate === original) return "The new name must be different.";
    if (files.some((entry) => basename(entry.path) === candidate && candidate !== original)) return "An entry with this name already exists.";
    return "";
  };

  const performCreate = () => {
    const error = validateName(name);
    if (error) return setModalError(error);
    runOperation(() => api.mkdir(joinPath(path, name)), `Successfully created ${joinPath(path, name)}`);
  };

  const performRename = () => {
    const target = selectedFiles[0];
    if (!target) return;
    const error = validateName(name, basename(target.path));
    if (error) return setModalError(error);
    runOperation(() => api.rename(target.path, joinPath(parentPath(target.path) || "/", name)), `Successfully renamed ${target.path}`);
  };

  const performPermission = () => {
    const target = selectedFiles[0];
    if (!target) return;
    const mode = permissionFromBits(target.permission, bits);
    runOperation(() => api.chmod(target.path, mode), `Successfully changed permissions for ${target.path}`);
  };

  const performTransfer = (mode: "copy" | "move", paths?: string[]) => {
    const targetPaths = paths ?? selectedFiles.map((file) => file.path);
    const operation = mode === "copy" ? api.copy : api.move;
    runOperation(() => operation(targetPaths, destination || "/"), `Successfully ${mode === "copy" ? "copied" : "moved"} ${targetPaths.length} item(s)`);
  };

  const performDelete = (paths?: string[]) => {
    const targetPaths = paths ?? selectedFiles.map((file) => file.path);
    runOperation(() => api.remove(targetPaths, isTrash || permanent), `Successfully deleted ${targetPaths.length} item(s)`);
  };

  const performUpload = () => {
    if (!uploadFile) return setModalError("Select one file to upload.");
    runOperation(() => api.upload(path, uploadFile), `Successfully uploaded ${uploadFile.name}`);
  };

  const loadPreview = async () => {
    const target = selectedFiles[0];
    if (!target || previewDone || busy) return;
    setBusy(true);
    setModalError("");
    try {
      const response = await api.preview(target.path, previewOffset, previewOffset + 5000);
      setPreviewContent((current) => current + response.data.slice(0, Math.max(0, response.readbytes)));
      setPreviewOffset((current) => current + 5000);
      setPreviewDone(response.isFileEnd);
    } catch (reason) {
      setModalError(detailForError(reason));
    } finally {
      setBusy(false);
    }
  };

  useEffect(() => {
    if (modal === "preview" && previewOffset === 0 && !previewContent) loadPreview();
  }, [modal]);

  const download = async (concatenate = false) => {
    if (!selectedFiles.length) return;
    try {
      if (selectedFiles.length === 1 && !selectedFiles[0].isDirectory && !concatenate) {
        const permission = await api.checkDownload(selectedFiles[0].path);
        if (!permission.allowed) throw new Error(`Read access was denied for ${selectedFiles[0].path}`);
        window.location.assign(api.downloadUrl(selectedFiles[0].path));
      } else {
        const generated = await api.generateArchive(selectedFiles.map((file) => file.path), concatenate);
        window.location.assign(api.generatedDownloadUrl(generated.requestId, concatenate));
      }
    } catch (reason) {
      notify("error", concatenate ? "Failed to concatenate files" : "Failed to prepare download", detailForError(reason));
    }
  };

  const remainingPaths = operationRecovery
    ? [...new Set([...operationRecovery.failed, ...operationRecovery.unprocessed])]
    : [];

  const finishPartialOperation = () => {
    setModal(null);
    setOperationRecovery(null);
    refresh();
  };

  const breadcrumbs = path === "/" ? [] : path.split("/").filter(Boolean);

  if (health !== "ready") {
    return (
      <main className="service-check">
        <div className={`service-check-icon ${health}`}>
          {health === "checking" ? <LoaderCircle className="spin" size={30} /> : <FolderOpen size={30} />}
        </div>
        <h1>{health === "checking" ? "Checking HDFS access" : "Unable to access HDFS"}</h1>
        <p>{health === "checking" ? "Validating the Files View connection and permissions." : healthError}</p>
        {health === "failed" && <button className="primary-button" type="button" onClick={checkHealth}><RefreshCw size={16} /> Retry</button>}
      </main>
    );
  }

  return (
    <main className="files-app">
      <header className="app-header">
        <div>
          <span className="eyebrow">Ambari View</span>
          <h1>HDFS Files</h1>
        </div>
        <button className="icon-button notification-button" type="button" title="Notifications" aria-label="Notifications" onClick={() => setShowNotifications((value) => !value)}>
          <Bell size={19} />
          {notifications.length > 0 && <span>{notifications.length}</span>}
        </button>
      </header>

      {showNotifications && (
        <aside className="notifications" aria-label="Notifications">
          <div className="notifications-header"><strong>Activity</strong><button type="button" onClick={() => setNotifications([])}>Clear</button></div>
          {notifications.length === 0 && <p>No activity recorded.</p>}
          {notifications.map((notification) => (
            <article key={notification.id} className={notification.level}>
              <strong>{notification.title}</strong>
              {notification.detail && <pre>{notification.detail}</pre>}
            </article>
          ))}
        </aside>
      )}

      <section className="path-bar">
        <div className="path-actions">
          <button className="icon-button" type="button" title="Parent directory" aria-label="Parent directory" disabled={!parentPath(path)} onClick={() => navigate(parentPath(path) || "/")}><ArrowLeft size={18} /></button>
          {homePath && <button className="icon-button" type="button" title="Home directory" aria-label="Home directory" onClick={() => navigate(homePath)}><Home size={18} /></button>}
          {trashPath && <button className={`icon-button ${isTrash ? "active" : ""}`} type="button" title="Trash" aria-label="Trash" onClick={() => navigate(trashPath)}><Trash2 size={18} /></button>}
          <button className="icon-button" type="button" title="Copy current path" aria-label="Copy current path" onClick={() => navigator.clipboard.writeText(selectedFiles.length ? selectedFiles.map((file) => file.path).join(", ") : path)}><Clipboard size={18} /></button>
          <button className="icon-button" type="button" title="Refresh" aria-label="Refresh" onClick={refresh}><RefreshCw className={loading ? "spin" : ""} size={18} /></button>
        </div>
        <nav className="breadcrumbs" aria-label="Current path">
          <button type="button" onClick={() => navigate("/")}>/</button>
          {breadcrumbs.map((segment, index) => {
            const target = `/${breadcrumbs.slice(0, index + 1).join("/")}`;
            return <button type="button" key={target} onClick={() => navigate(target)}>{segment}</button>;
          })}
        </nav>
        <form className="search" onSubmit={(event) => { event.preventDefault(); setFilter(searchText); }}>
          <Search size={17} />
          <input aria-label="Search files" placeholder="Filter this directory" value={searchText} onChange={(event) => setSearchText(event.target.value)} />
          {filter && <button type="button" onClick={() => { setSearchText(""); setFilter(""); }}>Clear</button>}
        </form>
      </section>

      <section className="command-bar">
        <div className="selection-summary">
          {selectedFiles.length
            ? <><strong>{selectedRegularFiles}</strong> files, <strong>{selectedFolders}</strong> folders selected</>
            : meta.truncated
              ? <>Showing <strong>{files.length}</strong> of <strong>{meta.originalSize}</strong> entries</>
              : <>Total: <strong>{files.length}</strong> entries</>}
        </div>
        <div className="commands">
          {selectedFiles.length > 0 ? (
            <>
              <button type="button" disabled={selectedFiles.length !== 1} onClick={() => openFile(selectedFiles[0])}><Eye size={16} /> Open</button>
              <button type="button" disabled={selectedFiles.length !== 1} onClick={() => openModal("rename")}><FilePenLine size={16} /> Rename</button>
              <button type="button" disabled={selectedFiles.length !== 1} onClick={() => openModal("permission")}><KeyRound size={16} /> Permissions</button>
              <button type="button" onClick={() => openModal("copy")}><Copy size={16} /> Copy</button>
              <button type="button" onClick={() => openModal("move")}><FolderInput size={16} /> Move</button>
              <button type="button" onClick={() => download(false)}><Download size={16} /> Download</button>
              <button type="button" disabled={selectedRegularFiles < 2 || selectedFolders > 0} onClick={() => download(true)}><Files size={16} /> Concatenate</button>
              <button className="danger-command" type="button" onClick={() => openModal("delete")}><Trash2 size={16} /> Delete</button>
            </>
          ) : (
            <>
              <button type="button" disabled={isTrash} onClick={() => openModal("create")}><FolderPlus size={16} /> New folder</button>
              <button type="button" disabled={isTrash} onClick={() => openModal("upload")}><Upload size={16} /> Upload</button>
              {isTrash && <button className="danger-command" type="button" onClick={() => openModal("emptyTrash")}><Trash2 size={16} /> Empty trash</button>}
              <button type="button" disabled={files.length === 0} onClick={() => setSelected(new Set(sortedFiles.map((file) => file.path)))}>Select all</button>
            </>
          )}
          {selectedFiles.length > 0 && <button type="button" onClick={() => setSelected(new Set())}>Deselect</button>}
        </div>
      </section>

      {loadError && <div className="inline-error"><strong>Directory could not be loaded.</strong><span>{loadError}</span><button type="button" onClick={refresh}>Retry</button></div>}
      <FileTable files={sortedFiles} selected={selected} sort={sort} onSort={toggleSort} onSelect={selectFile} onOpen={openFile} />

      {modal === "create" && <Modal title="Create directory" subtitle={path} busy={busy} onClose={closeModal} footer={<><button type="button" onClick={closeModal}>Cancel</button><button className="primary-button" type="button" disabled={busy} onClick={performCreate}>Create</button></>}><label htmlFor="directory-name">Directory name</label><input id="directory-name" autoFocus value={name} onChange={(event) => setName(event.target.value)} />{modalError && <pre className="modal-error">{modalError}</pre>}</Modal>}
      {modal === "upload" && <Modal title="Upload file" subtitle={`Destination: ${path}`} busy={busy} onClose={closeModal} footer={<><button type="button" onClick={closeModal}>Cancel</button><button className="primary-button" type="button" disabled={busy || !uploadFile} onClick={performUpload}>Upload</button></>}><label className="file-drop"><Upload size={28} /><strong>{uploadFile ? uploadFile.name : "Choose one file"}</strong><input type="file" onChange={(event) => setUploadFile(event.target.files?.[0] ?? null)} /></label>{modalError && <pre className="modal-error">{modalError}</pre>}</Modal>}
      {modal === "rename" && selectedFiles[0] && <Modal title={`Rename ${selectedFiles[0].isDirectory ? "directory" : "file"}`} subtitle={selectedFiles[0].path} busy={busy} onClose={closeModal} footer={<><button type="button" onClick={closeModal}>Cancel</button><button className="primary-button" type="button" disabled={busy} onClick={performRename}>Rename</button></>}><label htmlFor="new-name">New name</label><input id="new-name" autoFocus value={name} onChange={(event) => setName(event.target.value)} />{modalError && <pre className="modal-error">{modalError}</pre>}</Modal>}
      {modal === "permission" && selectedFiles[0] && <Modal title="Edit permissions" subtitle={selectedFiles[0].path} busy={busy} onClose={closeModal} footer={<><button type="button" onClick={closeModal}>Cancel</button><button className="primary-button" type="button" disabled={busy} onClick={performPermission}>Save</button></>}><div className="permissions-grid">{["User", "Group", "Other"].map((owner, ownerIndex) => <div key={owner}><strong>{owner}</strong>{["Read", "Write", "Execute"].map((permission, permissionIndex) => { const index = ownerIndex * 3 + permissionIndex; return <label key={permission}><input type="checkbox" checked={bits[index]} onChange={() => setBits((current) => current.map((value, currentIndex) => currentIndex === index ? !value : value))} />{permission}</label>; })}</div>)}</div>{modalError && <pre className="modal-error">{modalError}</pre>}</Modal>}
      {(modal === "copy" || modal === "move") && <Modal title={`${modal === "copy" ? "Copy" : "Move"} ${selectedFiles.length} item(s)`} busy={busy} onClose={closeModal} footer={operationRecovery ? <><button type="button" onClick={finishPartialOperation}>Done</button><button type="button" disabled={busy || operationRecovery.unprocessed.length === 0} onClick={() => performTransfer(modal, operationRecovery.unprocessed)}>Skip failed and continue</button><button className="primary-button" type="button" disabled={busy || remainingPaths.length === 0} onClick={() => performTransfer(modal, remainingPaths)}>Retry remaining</button></> : <><button type="button" onClick={closeModal}>Cancel</button><button className="primary-button" type="button" disabled={busy} onClick={() => performTransfer(modal)}> {modal === "copy" ? "Copy" : "Move"}</button></>}><DirectoryPicker api={api} value={destination} onChange={setDestination} />{modalError && <pre className="modal-error">{modalError}</pre>}</Modal>}
      {modal === "delete" && <Modal title="Delete selected items" busy={busy} onClose={closeModal} footer={operationRecovery ? <><button type="button" onClick={finishPartialOperation}>Done</button><button type="button" disabled={busy || operationRecovery.unprocessed.length === 0} onClick={() => performDelete(operationRecovery.unprocessed)}>Skip failed and continue</button><button className="danger-button" type="button" disabled={busy || remainingPaths.length === 0} onClick={() => performDelete(remainingPaths)}>Retry remaining</button></> : <><button type="button" onClick={closeModal}>Cancel</button><button className="danger-button" type="button" disabled={busy} onClick={() => performDelete()}>Delete</button></>}><p>Delete {selectedRegularFiles} file(s) and {selectedFolders} folder(s)?</p>{!isTrash && <label className="checkbox-line"><input type="checkbox" checked={permanent} onChange={(event) => setPermanent(event.target.checked)} />Delete permanently instead of moving to trash</label>}{modalError && <pre className="modal-error">{modalError}</pre>}</Modal>}
      {modal === "emptyTrash" && <Modal title="Empty trash" busy={busy} onClose={closeModal} footer={<><button type="button" onClick={closeModal}>Cancel</button><button className="danger-button" type="button" disabled={busy} onClick={() => runOperation(api.emptyTrash, "Successfully emptied trash")}>Empty trash</button></>}><p>Permanently delete every entry in the HDFS trash directory?</p>{modalError && <pre className="modal-error">{modalError}</pre>}</Modal>}
      {modal === "preview" && selectedFiles[0] && <Modal title="File preview" subtitle={selectedFiles[0].path} busy={busy} onClose={closeModal} footer={<><button type="button" onClick={closeModal}>Close</button>{!previewDone && <button type="button" disabled={busy} onClick={loadPreview}>Load more</button>}<button className="primary-button" type="button" onClick={() => download(false)}><Download size={16} /> Download</button></>}><pre className="preview-content">{previewContent || (busy ? "Loading..." : "No content")}</pre>{modalError && <pre className="modal-error">{modalError}</pre>}</Modal>}
    </main>
  );
}
