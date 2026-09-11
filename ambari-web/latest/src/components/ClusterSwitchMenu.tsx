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

import { useContext, useEffect, useState } from "react";
import { Dropdown } from "react-bootstrap";
import { useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { AppContext } from "../store/context";
import { useAuth } from "../hooks/useAuth";
import ClusterApi from "../api/clusterApi";
import { clusterPath } from "../Utils/clusterRoute";
import { rememberCluster } from "../Utils/authNavigation";
import { redirectToAdminView } from "../Utils/adminViewRedirect";

type ClusterItem = { Clusters: { cluster_id: number; cluster_name: string } };

export default function ClusterSwitchMenu() {
  const { clusterName } = useContext(AppContext);
  const { user, canAccessCluster, hasGlobalAuthorization } = useAuth();
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const [clusters, setClusters] = useState<ClusterItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [failed, setFailed] = useState(false);
  const [retry, setRetry] = useState(0);
  const canManage = hasGlobalAuthorization("AMBARI.RENAME_CLUSTER");

  useEffect(() => {
    if (!open) return;
    let active = true;
    setLoading(true);
    setFailed(false);
    ClusterApi.getClusterData().then((data) => {
      if (!Array.isArray(data?.items)) throw new Error("Invalid cluster list");
      if (active) setClusters(data.items);
    }).catch(() => {
      if (active) setFailed(true);
    }).finally(() => {
      if (active) setLoading(false);
    });
    return () => { active = false; };
  }, [open, retry, user?.user_name]);

  if (!clusterName) return null;
  return (
    <Dropdown show={open} onToggle={setOpen} className="me-3">
      <Dropdown.Toggle variant="outline-secondary" id="cluster-switch-menu"
        title={t("clusterNavigation.hint")}>
        {t("clusterNavigation.current", { name: clusterName })}
      </Dropdown.Toggle>
      <Dropdown.Menu align="end" style={{ maxHeight: "70vh", overflowY: "auto" }}>
        <Dropdown.Header>{t("clusterNavigation.switch")}</Dropdown.Header>
        {loading ? <Dropdown.ItemText>{t("clusterNavigation.loading")}</Dropdown.ItemText> : failed ? (
          <Dropdown.Item onClick={() => { setRetry((value) => value + 1); setOpen(true); }}>
            {t("clusterNavigation.retry")}
          </Dropdown.Item>
        ) : clusters.filter((item) => canAccessCluster(item.Clusters.cluster_name)).map(({ Clusters: cluster }) => (
          <Dropdown.Item key={cluster.cluster_id} active={cluster.cluster_name === clusterName}
            aria-current={cluster.cluster_name === clusterName ? "true" : undefined}
            onClick={() => {
              rememberCluster(user?.user_name, cluster.cluster_id);
              navigate(clusterPath(cluster.cluster_name));
            }}>
            {cluster.cluster_name === clusterName ? "✓ " : ""}{cluster.cluster_name}
          </Dropdown.Item>
        ))}
        {canManage && <>
          <Dropdown.Divider />
          <Dropdown.Item onClick={() => void redirectToAdminView("clusters", clusterName)}>
            ⚙ {t("clusterNavigation.manage")}
          </Dropdown.Item>
        </>}
      </Dropdown.Menu>
    </Dropdown>
  );
}
