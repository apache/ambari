/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { useEffect, useRef, useState } from "react";
import { Alert, Badge, Button, Form, Modal, Table, Spinner } from "react-bootstrap";
import { ArrowClockwise, BoxSeam, Check2, CloudUpload, Eye, Link45deg, Search, Trash, XCircle } from "react-bootstrap-icons";
import { Link, useSearchParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import MpackApi, {
  isMpackWaiting, isMpackDefinitiveRejection, mpackErrorMessage, type MpackBinding, type MpackCapabilities,
  type MpackMemberResult, type MpackMutation, type MpackOperation, type MpackPlan,
  type MpackRelease, type MpackUpload, type MpackUploadMember,
  type MpackServiceCatalog, type MpackDeploymentHandoff,
} from "../../api/mpackApi";
import { useAuth } from "../../hooks/useAuth";
import { clusterDraftPath } from "../../Utils/scopedWorkflow";
import { clusterPath } from "../../Utils/clusterRoute";
import { createSecureUuid } from "../../Utils/uuid";
import { chooseServiceVersion, groupServiceCatalog } from "./serviceCatalog";
import "./mpacks.scss";

type Submission = { schema_version: 1; plan_id: string; plan_digest: string; key: string };
export function compareVersion(first: string, second: string): number {
  const pattern = /^(0|[1-9][0-9]*)(\.(0|[1-9][0-9]*))*$/;
  if (!pattern.test(first) || !pattern.test(second)) throw new Error("Invalid stack version contract");
  const left = first.split(".").map(BigInt), right = second.split(".").map(BigInt);
  for (let index = 0; index < Math.max(left.length, right.length); index++) {
    const a = left[index] ?? 0n, b = right[index] ?? 0n;
    if (a !== b) return a < b ? -1 : 1;
  }
  return 0;
}
const emptyMutation = (action: MpackMutation["action"]): MpackMutation => ({
  schema_version: 1, action, archive_digests: [], release_ids: [], bindings: [], activate: true, maintenance: false,
});
const providedExtensions = (release: MpackRelease) => [...new Set(release.contributions
  .map((item) => item.scope).filter((scope) => scope.startsWith("extensions/")))].map((scope) => {
    const [, name, version] = scope.split("/");
    return { name, version };
  });

export default function Mpacks() {
  const { user } = useAuth();
  return user?.user_name ? <MpackManagement key={user.user_name} username={user.user_name} /> : null;
}

function MpackManagement({ username }: { username: string }) {
  const { t } = useTranslation();
  const [parameters, setParameters] = useSearchParams();
  const operationId = parameters.get("operation");
  const storageKey = "ambari.mpack.submission.user." + encodeURIComponent(username);
  const [releases, setReleases] = useState<MpackRelease[]>([]);
  const [bindings, setBindings] = useState<MpackBinding[]>([]);
  const [operations, setOperations] = useState<MpackOperation[]>([]);
  const [capabilities, setCapabilities] = useState<MpackCapabilities | null>(null);
  const [catalog, setCatalog] = useState<MpackServiceCatalog | null>(null);
  const [serviceSelection, setServiceSelection] = useState<string[]>([]);
  const [destination, setDestination] = useState("");
  const [handoff, setHandoff] = useState<MpackDeploymentHandoff | null>(null);
  const [operationPlan, setOperationPlan] = useState<MpackPlan | null>(null);
  const [upload, setUpload] = useState<MpackUpload | null>(null);
  const [selected, setSelected] = useState<string[]>([]);
  const [updateRelease, setUpdateRelease] = useState<string | null>(null);
  const [target, setTarget] = useState("");
  const [storeOnly, setStoreOnly] = useState(false);
  const [maintenance, setMaintenance] = useState(false);
  const [plan, setPlan] = useState<MpackPlan | null>(null);
  const [operation, setOperation] = useState<MpackOperation | null>(null);
  const [members, setMembers] = useState<MpackMemberResult[]>([]);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const [query, setQuery] = useState("");
  const [activeTab, setActiveTab] = useState("catalog");
  const [showImport, setShowImport] = useState(false);
  const [versionChoices, setVersionChoices] = useState<Record<string, string>>({});
  const [environment, setEnvironment] = useState("");
  const [loading, setLoading] = useState(true);
  const [historyLimit, setHistoryLimit] = useState(10);
  useEffect(() => { if (operationId) setActiveTab("history"); }, [operationId]);
  const [reload, setReload] = useState(0);
  const generation = useRef(0);
  const [submission, setSubmission] = useState<Submission | null>(() => {
    try {
      const item = JSON.parse(localStorage.getItem(storageKey) || "null");
      return item?.schema_version === 1 && typeof item.plan_id === "string" &&
        typeof item.plan_digest === "string" && typeof item.key === "string" ? item : null;
    } catch { return null; }
  });
  const refresh = () => setReload((value) => value + 1);
  useEffect(() => {
    let stopped = false;
    setLoading(true);
    Promise.allSettled([MpackApi.releases(), MpackApi.bindings(), MpackApi.capabilities(), MpackApi.operations(), MpackApi.services()])
      .then(([inventory, links, supported, history, available]) => {
        if (stopped) return;
        if (inventory.status === "fulfilled") setReleases(inventory.value);
        if (links.status === "fulfilled") setBindings(links.value);
        if (supported.status === "fulfilled") setCapabilities(supported.value);
        if (history.status === "fulfilled") setOperations(history.value);
        if (available.status === "fulfilled") setCatalog(available.value);
        const failed = [inventory, links, supported, history, available].find(item => item.status === "rejected");
        if (failed?.status === "rejected") setError(mpackErrorMessage(failed.reason));
      }).catch((failure) => { if (!stopped) setError(mpackErrorMessage(failure)); })
      .finally(() => { if (!stopped) setLoading(false); });
    return () => { stopped = true; };
  }, [reload]);
  useEffect(() => {
    let stopped = false;
    let timer: ReturnType<typeof setTimeout> | undefined;
    if (!operationId) { setOperation(null); setMembers([]); setHandoff(null); setOperationPlan(null); return; }
    const poll = async () => {
      try {
        const [current, result] = await Promise.all([MpackApi.operation(operationId), MpackApi.members(operationId)]);
        if (stopped) return;
        setOperation(current); setMembers(result);
        const acceptedPlan = await MpackApi.getPlan(current.plan_id);
        if (stopped) return;
        if (acceptedPlan.digest !== current.plan_digest) throw new Error("Management pack plan identity changed");
        setOperationPlan(acceptedPlan);
        if (isMpackWaiting(current)) timer = setTimeout(poll, 2000);
        else {
          if (current.phase === "SUCCEEDED" && acceptedPlan.deployment) {
            const verified = await MpackApi.deployment(current.id);
            if (verified.plan_id !== current.plan_id || verified.effective_snapshot !== current.effective_snapshot) {
              throw new Error("Management pack deployment identity changed");
            }
            if (!stopped) setHandoff(verified);
          } else if (!stopped) setHandoff(null);
          const [inventory, history, links, available] = await Promise.all([
            MpackApi.releases(), MpackApi.operations(), MpackApi.bindings(), MpackApi.services(),
          ]);
          if (!stopped) { setReleases(inventory); setOperations(history); setBindings(links); setCatalog(available); }
        }
      } catch (failure) {
        if (!stopped) setError(mpackErrorMessage(failure));
      }
    };
    void poll();
    return () => { stopped = true; if (timer) clearTimeout(timer); };
  }, [operationId, reload]);

  const run = async (work: () => Promise<void>) => {
    const current = ++generation.current;
    setBusy(true); setError("");
    try { await work(); }
    catch (failure) { if (generation.current === current) setError(mpackErrorMessage(failure)); }
    finally { if (generation.current === current) setBusy(false); }
  };
  const targets = new Map<string, { stack_name: string; stack_version: string }>();
  capabilities?.target_stack_versions.forEach((item) => targets.set(item.stack_name + "/" + item.stack_version, item));
  const uploadedMembers: MpackUploadMember[] = upload ? upload.members || [upload] : [];
  uploadedMembers.forEach((member) => member.stacks.forEach((item) =>
    targets.set(item.stack_name + "/" + item.stack_version, item)));

  const uploadFile = (file: File) => run(async () => {
    setPlan(null); setUpload(null);
    const result = await MpackApi.upload(file);
    setUpload(result);
    setSelected((result.members || [result]).map((member) => member.archive_digest));
    const available = [...(capabilities?.target_stack_versions || []),
      ...(result.members || [result]).flatMap((member) => member.stacks)];
    const extensions = (result.members || [result]).flatMap(member => member.extensions);
    const compatible = available.filter(item => extensions.every(extension => extension.minimum_stacks.some(minimum =>
      minimum.stack_name === item.stack_name && compareVersion(item.stack_version, minimum.stack_version) >= 0)));
    const unique = [...new Set(compatible.map((item) => item.stack_name + "/" + item.stack_version))];
    setTarget(unique.length === 1 ? unique[0] : "");
  });
  const previewUpload = () => run(async () => {
    const mutation = emptyMutation(updateRelease ? "UPDATE" : "IMPORT");
    mutation.archive_digests = selected;
    mutation.release_ids = updateRelease ? [updateRelease] : [];
    mutation.activate = !!updateRelease && !storeOnly;
    mutation.maintenance = maintenance;
    if (mutation.activate) {
      const chosen = targets.get(target);
      const extensions = uploadedMembers.filter((member) => selected.includes(member.archive_digest))
        .flatMap((member) => member.extensions);
      if (extensions.length && !chosen) throw new Error(t("mpack.selectTarget"));
      if (chosen) mutation.bindings = extensions.map((extension) => ({
        stack_name: chosen.stack_name, stack_version: chosen.stack_version,
        extension_name: extension.name, extension_version: extension.version,
      }));
    }
    const preview = await MpackApi.plan(mutation);
    if (mutation.action === "IMPORT") { await beginSubmission(preview); setShowImport(false); }
    else { setPlan(preview); setShowImport(false); }
  });
  const previewRelease = (release: MpackRelease, action: "BIND" | "UNBIND" | "UNINSTALL") => run(async () => {
    const mutation = emptyMutation(action);
    mutation.release_ids = [release.id]; mutation.maintenance = maintenance;
    if (action === "BIND") {
      const chosen = targets.get(target);
      if (!chosen) throw new Error(t("mpack.selectTarget"));
      mutation.bindings = providedExtensions(release).map((extension) => ({
        ...chosen, extension_name: extension.name, extension_version: extension.version,
      }));
    } else if (action === "UNBIND") {
      const owned = providedExtensions(release);
      mutation.bindings = bindings.filter((binding) => owned.some((extension) =>
        extension.name === binding.extension_name && extension.version === binding.extension_version));
    }
    setPlan(await MpackApi.plan(mutation));
  });
  const submitRequest = async (accepted: Submission) => {
    try {
      const result = await MpackApi.submit(accepted.plan_id, accepted.key, accepted.plan_digest);
      localStorage.removeItem(storageKey); setSubmission(null); setPlan(null); setUpload(null);
      setOperation(result); setParameters({ operation: result.id }); refresh();
    } catch (failure) {
      setPlan(null);
      if (isMpackDefinitiveRejection(failure)) {
        localStorage.removeItem(storageKey); setSubmission(null);
      }
      throw failure;
    }
  };
  const submit = (accepted: Submission) => run(() => submitRequest(accepted));
  const beginSubmission = async (preview: MpackPlan) => {
    const accepted: Submission = { schema_version: 1, plan_id: preview.id, plan_digest: preview.digest, key: createSecureUuid() };
    localStorage.setItem(storageKey, JSON.stringify(accepted)); setSubmission(accepted);
    await submitRequest(accepted);
  };
  const confirm = () => {
    if (!plan) return;
    void run(() => beginSubmission(plan));
  };
  const recover = (action: "recover" | "retry" | "cancel") => run(async () => {
    if (!operation) return;
    setOperation(await MpackApi.recover(operation.id, action)); refresh();
  });

  const retryAllowed = operation && Object.values(operation.hooks).some((receipt) =>
    receipt.state === "FAILED" && receipt.effect_state === "NOT_APPLIED");
  const cancelAllowed = operation && !["SUCCEEDED", "FAILED", "CANCELLED"].includes(operation.phase) &&
    Object.values(operation.hooks).every((receipt) => receipt.effect_state === "NOT_APPLIED");
  const installed = releases.filter((release) => release.installed);
  const selectedServices = catalog?.items.filter(item => serviceSelection.includes(item.id)) || [];
  const serviceTarget = selectedServices[0];
  const destinationInfo = catalog?.destinations.find(item => String(item.cluster_id) === destination);
  const serviceGroups = groupServiceCatalog(catalog?.items || []);
  const environments = [...new Set((catalog?.items || []).map(item => item.stack_name + "/" + item.stack_version))].sort();
  const filteredGroups = serviceGroups.filter(group => group.entries.some(item =>
    (!environment || item.stack_name + "/" + item.stack_version === environment)
    && [item.display_name, item.service_name, item.release_id, item.description || ""].some(value => value.toLowerCase().includes(query.toLowerCase()))));
  const deploymentPath = handoff ? handoff.cluster_name
    ? clusterPath(handoff.cluster_name, "/main/service/add/step1?mpack_operation=" + handoff.operation_id)
    : clusterDraftPath(handoff.operation_id) + "&mpack_operation=" + handoff.operation_id : null;
  const previewServices = () => run(async () => {
    if (selectedServices.length !== serviceSelection.length || (destination && !destinationInfo)) throw new Error(t("mpackUi.selectionChanged"));
    const prepared = await MpackApi.planServices(serviceSelection, destination ? Number(destination) : null, maintenance);
    if (prepared.maintenance_required || prepared.restart_required) setPlan(prepared);
    else await beginSubmission(prepared);
  });

  return (
    <main className="mpack-workspace">
      <div className="mpack-page-header">
        <div><div className="mpack-heading"><span className="mpack-brand-icon"><BoxSeam /></span><h1>{t("mpack.title")}</h1></div><p>{t("mpackUi.subtitle")}</p></div>
        <div className="d-flex gap-2">
        <Button variant="outline-secondary" size="sm" title={t("mpack.refresh")} aria-label={t("mpack.refresh")}
          disabled={busy || loading} onClick={refresh}><ArrowClockwise /></Button>
        <Button disabled={busy || !!submission} onClick={() => { setUpdateRelease(null); setUpload(null); setShowImport(true); }}><CloudUpload className="me-2" />{t("mpackUi.importBundle")}</Button></div>
      </div>
      {error && <Alert variant="danger" role="alert">{error}</Alert>}
      {submission && <Alert variant="warning">
        {t("mpack.unresolvedSubmission")}{" "}
        <Button size="sm" disabled={busy} onClick={() => void submit(submission)}>{t("mpack.reconcile")}</Button>
      </Alert>}
      <div className="mpack-navigation" role="tablist" aria-label={t("mpack.title")} onKeyDown={event => {
        const tabs = ["catalog", "packages", "history"];
        const current = tabs.indexOf(activeTab);
        const index = event.key === "ArrowRight" ? (current + 1) % 3 : event.key === "ArrowLeft" ? (current + 2) % 3 : event.key === "Home" ? 0 : event.key === "End" ? 2 : -1;
        if (index < 0) return;
        event.preventDefault(); setActiveTab(tabs[index]); document.getElementById(`mpack-tab-${tabs[index]}`)?.focus();
      }}>{[["catalog", "catalog", serviceGroups.length], ["packages", "packages", installed.length], ["history", "history", operations.length]].map(([id, label, count]) => <button type="button" role="tab" key={id} id={`mpack-tab-${id}`} tabIndex={activeTab === id ? 0 : -1} aria-controls={`mpack-panel-${id}`} aria-selected={activeTab === id} className={activeTab === id ? "active" : ""} onClick={() => setActiveTab(String(id))}>{t("mpackUi." + label)}<span>{loading ? "…" : count}</span></button>)}</div>
      <Modal show={showImport} onHide={() => { if (!busy) setShowImport(false); }} size="lg" centered>
      <Modal.Header closeButton={!busy}><Modal.Title>{t(updateRelease ? "mpack.update" : "mpackUi.importBundle")}</Modal.Title></Modal.Header><Modal.Body>
      <section className="mpack-import-area">
        <p>{t("mpack.importHelp")}</p>
        <Form.Group controlId="mpack-upload">
          <Form.Label>{updateRelease ? t("mpack.updateArchive", { release: updateRelease }) : t("mpack.archive")}</Form.Label>
          <Form.Control type="file" accept=".tar.gz,.tgz" disabled={busy || !!submission}
            onChange={(event: React.ChangeEvent<HTMLInputElement>) => {
              const file = event.target.files?.[0]; if (file) void uploadFile(file);
            }} />
        </Form.Group>
        {uploadedMembers.length > 0 && <div className="my-2">
          {uploadedMembers.map((member) => <Form.Check key={member.archive_digest} type="checkbox"
            id={"member-" + member.archive_digest} disabled={busy || !updateRelease}
            label={member.name + "/" + member.version} checked={selected.includes(member.archive_digest)}
            onChange={(event) => setSelected((items) => event.target.checked
              ? [...items, member.archive_digest] : items.filter((item) => item !== member.archive_digest))} />)}
        </div>}
        <div className="d-flex flex-wrap align-items-end gap-3 mt-3">
          {updateRelease && <Form.Group controlId="mpack-target" style={{ minWidth: "12rem", maxWidth: "100%" }}>
            <Form.Label>{t("mpack.target")}</Form.Label>
            <Form.Select value={target} disabled={busy || storeOnly} onChange={(event) => setTarget(event.target.value)}>
              <option value="">{t("mpack.selectTarget")}</option>
              {[...targets.entries()].filter(([, item]) => uploadedMembers.flatMap(member => member.extensions).every(extension =>
                extension.minimum_stacks.some(minimum => minimum.stack_name === item.stack_name &&
                  compareVersion(item.stack_version, minimum.stack_version) >= 0))).map(([identity]) =>
                  <option key={identity} value={identity}>{identity}</option>)}
            </Form.Select>
          </Form.Group>}
          <Button disabled={busy || !upload || !selected.length || !!submission} onClick={() => void previewUpload()}>
            <CloudUpload className="me-1" />{t(updateRelease ? "mpack.preview" : "mpack.importStore")}
          </Button>
          {updateRelease && <Button variant="link" disabled={busy} onClick={() => {
            setUpdateRelease(null); setUpload(null);
          }}>{t("mpack.cancelUpdate")}</Button>}
        </div>
      </section>
      </Modal.Body></Modal>
      <section id="mpack-panel-catalog" role="tabpanel" aria-labelledby="mpack-tab-catalog" hidden={activeTab !== "catalog"}>
        <div className="mpack-catalog-toolbar"><div className="mpack-search"><Search aria-hidden="true" /><Form.Control aria-label={t("mpack.search")} placeholder={t("mpack.search")} value={query}
          onChange={event => setQuery(event.target.value)} /></div><Form.Select className="mpack-environment-filter" aria-label={t("mpack.environment")} value={environment} onChange={event => setEnvironment(event.target.value)}><option value="">{t("mpackUi.allEnvironments")}</option>{environments.map(value => <option key={value} value={value}>{value}</option>)}</Form.Select></div>
        {catalog?.unavailable.map(item => <Alert variant="warning" key={item.release_id}>
          {item.release_id}: {item.message}
        </Alert>)}
        <div className="mpack-catalog-layout"><div>
        <div className="mpack-catalog-caption"><h2>{t("mpack.availableServices")}</h2><span>{t("mpackUi.resultCount", { count: filteredGroups.length })}</span></div>
        {loading && !catalog ? <div className="mpack-empty" role="status"><Spinner size="sm" /> {t("mpackUi.loading")}</div> : <div className="mpack-service-grid">
          {filteredGroups.map(group => {
              const item = group.entries.find(entry => serviceSelection.includes(entry.id)) || group.entries.find(entry => entry.id === versionChoices[group.key]) || group.entries[0];
              const compatible = (!serviceTarget || item.stack_name === serviceTarget.stack_name && item.stack_version === serviceTarget.stack_version)
                && (!destinationInfo || item.stack_name === destinationInfo.stack_name && item.stack_version === destinationInfo.stack_version);
              const checked = serviceSelection.includes(item.id);
              return <article key={group.key} className={`mpack-service-card ${checked ? "is-selected" : ""} ${!compatible ? "is-incompatible" : ""}`}>
                <div className="mpack-service-card-header"><span className="mpack-service-symbol" aria-hidden="true">{item.display_name.replace(/^Apache /, "").slice(0, 2)}</span><Badge bg={item.enabled ? "success" : "secondary"}>{t(item.enabled ? "mpack.enabled" : "mpack.imported")}</Badge></div>
                <div className="mpack-service-title">
                <Form.Check id={"service-" + item.id} label={item.display_name + " " + item.service_version}
                  checked={checked} disabled={busy || !!submission || !compatible}
                  onChange={event => { setServiceSelection(values => chooseServiceVersion(values, group, event.target.checked ? item.id : null)); }} />
                </div>
                <p className="mpack-service-description">{item.description}</p>
                <div className="mpack-service-meta"><span>{item.stack_name}/{item.stack_version}</span><span>{t("mpackUi.versionCount", { count: group.entries.length })}</span></div>
                <Form.Group controlId={`version-${item.id}`} className="mpack-version"><Form.Label>{t("mpackUi.definitionVersion")}</Form.Label><Form.Select size="sm" disabled={busy || !!submission || !compatible} value={item.id} onChange={event => { const id = event.target.value; setVersionChoices(values => ({ ...values, [group.key]: id })); if (checked) setServiceSelection(values => chooseServiceVersion(values, group, id)); }}>{group.entries.map(version => <option key={version.id} value={version.id}>{version.release_id}{version.enabled ? " · " + t("mpack.enabled") : ""}</option>)}</Form.Select></Form.Group>
                {!compatible && <p className="mpack-compatibility-note">{t("mpackUi.incompatible")}</p>}
                {!!item.required_services.length && <details className="mpack-dependencies"><summary>{t("mpackUi.dependencies")}</summary>{item.required_services.join(", ")}</details>}
              </article>;
            })}
        </div>}
        {!loading && filteredGroups.length === 0 && <div className="mpack-empty"><BoxSeam size={30} /><h3>{t(catalog?.items.length ? "mpackUi.noMatches" : "mpackUi.emptyTitle")}</h3><p>{t(catalog?.items.length ? "mpackUi.noMatchesHelp" : "mpack.noServices")}</p>{!!catalog?.items.length && <Button variant="outline-secondary" onClick={() => { setQuery(""); setEnvironment(""); }}>{t("mpackUi.clearFilters")}</Button>}</div>}
        </div><aside className="mpack-selection">
          <div className="mpack-selection-heading"><h2>{t("mpackUi.selection")}</h2><span>{selectedServices.length}</span></div>
          {selectedServices.length ? <ul>{selectedServices.map(item => <li key={item.id}><Check2 aria-hidden="true" /><div><strong>{item.display_name}</strong><small>{item.release_id}</small></div><Button variant="link" aria-label={t("mpackUi.removeService", { name: item.display_name })} disabled={busy || !!submission} onClick={() => setServiceSelection(values => values.filter(id => id !== item.id))}><XCircle /></Button></li>)}</ul> : <div className="mpack-selection-empty"><BoxSeam size={28} /><p>{t("mpackUi.selectPrompt")}</p></div>}
          <p className="mpack-selection-help">{t("mpack.selectServicesHelp")}</p>
          <Form.Group controlId="mpack-destination"><Form.Label>{t("mpack.destination")}</Form.Label>
            <Form.Select value={destination} disabled={busy || !!submission} onChange={event => setDestination(event.target.value)}>
              <option value="">{t("mpack.newEnvironment")}</option>
              {catalog?.destinations.filter(item => !serviceTarget || item.stack_name === serviceTarget.stack_name &&
                item.stack_version === serviceTarget.stack_version).map(item =>
                  <option key={item.cluster_id} value={item.cluster_id}>{item.cluster_name}</option>)}
            </Form.Select>
          </Form.Group>
          <Button className="w-100" disabled={busy || !!submission || !selectedServices.length} onClick={() => void previewServices()}>
            {busy && <Spinner size="sm" className="me-2" />}
            {t("mpack.prepareServices")}
          </Button>
        </aside></div>
      </section>
      <section className="mpack-inventory" id="mpack-panel-packages" role="tabpanel" aria-labelledby="mpack-tab-packages" hidden={activeTab !== "packages"}>
      <div className="mpack-section-heading"><h2>{t("mpack.installed")}</h2><p>{t("mpackUi.inventoryHelp")}</p></div>
      <details className="mpack-advanced"><summary>{t("mpack.advanced")}</summary>
        <Form.Group controlId="mpack-binding-target" className="my-2">
          <Form.Label>{t("mpack.target")}</Form.Label>
          <Form.Select value={target} disabled={busy} onChange={event => setTarget(event.target.value)}>
            <option value="">{t("mpack.selectTarget")}</option>
            {[...targets.keys()].sort().map(identity => <option key={identity} value={identity}>{identity}</option>)}
          </Form.Select>
        </Form.Group>
        <Form.Check id="mpack-maintenance" label={t("mpack.maintenance")} checked={maintenance} disabled={busy}
          onChange={(event) => setMaintenance(event.target.checked)} />
      </details>
      <section className="mt-3">
        <h2 className="h5">{t("mpack.installed")}</h2>
        <Table responsive size="sm">
          <thead><tr><th>{t("mpack.release")}</th><th>{t("mpack.bindings")}</th><th>{t("mpack.actions")}</th></tr></thead>
          <tbody>{installed.map((release) => {
            const provided = providedExtensions(release);
            const links = bindings.filter((binding) => provided.some((extension) =>
              extension.name === binding.extension_name && extension.version === binding.extension_version));
            return <tr key={release.id}><td>{release.id}</td>
              <td>{links.map((binding) => <div key={JSON.stringify(binding)}>
                {binding.stack_name}/{binding.stack_version}: {binding.extension_name}/{binding.extension_version}
              </div>)}</td>
              <td><div className="d-flex gap-1">
                <Button size="sm" variant="outline-secondary" disabled={busy || !!submission}
                  title={t("mpack.update")} aria-label={t("mpack.update") + " " + release.id}
                  onClick={() => { setUpdateRelease(release.id); setUpload(null); setStoreOnly(false); setShowImport(true); }}><CloudUpload /></Button>
                {!!provided.length && <Button size="sm" variant="outline-secondary" disabled={busy || !target || !!submission}
                  title={t("mpack.bind")} aria-label={t("mpack.bind") + " " + release.id}
                  onClick={() => void previewRelease(release, "BIND")}><Link45deg /></Button>}
                {!!links.length && <Button size="sm" variant="outline-secondary" disabled={busy || !!submission}
                  title={t("mpack.unbind")} aria-label={t("mpack.unbind") + " " + release.id}
                  onClick={() => void previewRelease(release, "UNBIND")}><XCircle /></Button>}
                <Button size="sm" variant="outline-danger" disabled={busy || !!submission}
                  title={t("mpack.uninstall")} aria-label={t("mpack.uninstall") + " " + release.id}
                  onClick={() => void previewRelease(release, "UNINSTALL")}><Trash /></Button>
              </div></td></tr>;
          })}</tbody>
        </Table>
        {!installed.length && <p>{t("mpack.noReleases")}</p>}
      </section>
      </section>
      <section className="mpack-history" id="mpack-panel-history" role="tabpanel" aria-labelledby="mpack-tab-history" hidden={activeTab !== "history"}>
        <div className="mpack-section-heading"><h2>{t("mpack.operations")}</h2><p>{t("mpackUi.historyHelp")}</p></div>
        <Table responsive size="sm"><thead><tr>
          <th>{t("mpack.operation")}</th><th>{t("mpack.status")}</th><th>{t("mpack.actions")}</th>
        </tr></thead><tbody>{operations.slice().sort((a, b) => b.updated_at - a.updated_at).slice(0, historyLimit).map((item) =>
          <tr key={item.id}><td>
            {item.action ? t("mpack.operationTypes." + item.action) : t("mpack.operation")}
            {" "}{(item.service_names?.length ? item.service_names : item.release_ids)?.join(", ")}
            <div className="small text-muted">{new Date(item.updated_at).toLocaleString()}</div>
          </td><td>{t("mpack.phases." + item.phase)}</td>
            <td><Button size="sm" variant="outline-secondary" title={t("mpack.details")} aria-label={t("mpack.details") + " " + item.id}
              onClick={() => setParameters({ operation: item.id })}><Eye /></Button></td></tr>)}</tbody></Table>
        {operations.length > historyLimit && <Button variant="outline-secondary" onClick={() => setHistoryLimit(value => value + 10)}>{t("mpackUi.showMore")}</Button>}
        {!operations.length && !loading && <div className="mpack-empty"><p>{t("mpackUi.noOperations")}</p></div>}
        {operation && <div className="border-top pt-3">
          <h3 className="h6">{operationPlan?.deployment?.service_names.join(", ") || t("mpack.operation")}</h3>
          <small className="text-muted">{operation.id}</small>
          <Badge bg={operation.phase === "SUCCEEDED" ? "success" : "secondary"}>{t("mpack.phases." + operation.phase)}</Badge>
          {operation.error_code && <Alert variant="warning" className="mt-2">{operation.error_code}</Alert>}
          {operation.phase === "RECOVERY_REQUIRED" && <p className="mt-2">{t("mpack.recoveryHelp")}</p>}
          {Array.isArray(operation.error_details.blockers) && <ul>
            {operation.error_details.blockers.map((blocker: any) => <li key={blocker.task_id}>
              {t("mpack.taskBlocker", { cluster: blocker.cluster_name ?? blocker.cluster_id, service: blocker.service,
                task: blocker.task_id, status: blocker.status })}
            </li>)}
          </ul>}
          <Table responsive size="sm" className="mt-2"><tbody>{members.map((member) =>
            <tr key={member.id}><td>{member.release_id}</td><td>{t("mpack.phases." + member.phase)}</td></tr>)}</tbody></Table>
          <div className="d-flex flex-wrap gap-2">
            {operation.phase === "RECOVERY_REQUIRED" && <>
              <Button size="sm" disabled={busy} onClick={() => void recover("recover")}>{t("mpack.reconcile")}</Button>
              {retryAllowed && <Button size="sm" disabled={busy} onClick={() => void recover("retry")}>{t("mpack.retry")}</Button>}
            </>}
            {cancelAllowed && <Button size="sm" variant="outline-danger" disabled={busy}
              onClick={() => void recover("cancel")}>{t("mpack.cancelOperation")}</Button>}
            {deploymentPath && <Link className="btn btn-sm btn-primary" to={deploymentPath}>
              {t(handoff?.cluster_name ? "mpack.addToCluster" : "mpack.deploy")}
            </Link>}
          </div>
        </div>}
      </section>
      <Modal show={!!plan} onHide={() => { if (!busy && !submission) setPlan(null); }}>
        <Modal.Header closeButton={!busy && !submission}><Modal.Title>{t("mpack.preview")}</Modal.Title></Modal.Header>
        <Modal.Body>{plan && <>
          <p>{t("mpack.scope")}: {plan.mutation.bindings.map((binding) =>
            binding.stack_name + "/" + binding.stack_version).join(", ") || t("mpack.server")}</p>
          <p>{t("mpack.affected")}: {plan.affected_clusters.join(", ") || t("mpack.none")}</p>
          <p>{t("mpack.release")}: {plan.mutation.release_ids.join(", ") ||
            uploadedMembers.filter((member) => selected.includes(member.archive_digest))
              .map((member) => member.name + "/" + member.version).join(", ")}</p>
          {plan.deployment && <p>{t("mpack.service")}: {plan.deployment.service_names.join(", ")}</p>}
          {plan.restart_required && <Alert variant="warning">{t("mpack.restartRequired")}</Alert>}
          {plan.maintenance_required && <Alert variant="warning">{t("mpack.maintenanceRequired")}</Alert>}
        </>}</Modal.Body>
        <Modal.Footer><Button variant="secondary" disabled={busy || !!submission} onClick={() => setPlan(null)}>{t("mpack.close")}</Button>
          <Button disabled={busy || !!submission} onClick={confirm}>{t("mpack.confirm")}</Button></Modal.Footer>
      </Modal>
    </main>
  );
}
