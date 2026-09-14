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

import { Alert, Button } from "react-bootstrap";
import { useTranslation } from "react-i18next";
import type { ManagedDependencySelections } from "./managedDependencySelection";
import { reviewedManagedDependencies } from "./managedDependencyConfig";

type ManagedDependencySettingsProps = {
  onReturn: () => void;
  selections: ManagedDependencySelections;
  view: "configuration" | "review";
};

export default function ManagedDependencySettings({
  onReturn,
  selections,
  view,
}: ManagedDependencySettingsProps) {
  const { t } = useTranslation();
  const managed = reviewedManagedDependencies(selections);
  if (!managed.length) return null;

  return (
    <section aria-labelledby={`managed-dependency-${view}-heading`} className="mb-4">
      <div className="d-flex flex-wrap justify-content-between align-items-start gap-2 mb-2">
        <div>
          <h3 className="h5 mb-1" id={`managed-dependency-${view}-heading`}>
            {t("managedDependencySettings.heading")}
          </h3>
          <p className="text-body-secondary mb-0">
            {t(`managedDependencySettings.${view}Description`)}
          </p>
        </div>
        <Button onClick={onReturn} size="sm" variant="outline-primary">
          {t("managedDependencySettings.returnToProviders")}
        </Button>
      </div>
      <div className="row g-3">
        {managed.map(({ choice, dependencyType }) => {
          const preview = choice.preview;
          const settings = Object.entries(preview?.client_config || {}).flatMap(
            ([type, properties]) => Object.entries(properties).map(([property, value]) => ({
              label: `${type}/${property}`,
              value,
            })),
          );
          const paths = [
            [t("managedDependencySettings.rootPath"), preview?.namespace?.root_uri],
            [t("managedDependencySettings.walPath"), preview?.namespace?.wal_uri],
            [t("managedDependencySettings.znode"), preview?.namespace?.znode],
            [t("managedDependencySettings.serviceUser"), preview?.consumer?.planned_hbase_user],
          ].filter(([, value]) => Boolean(value));
          return (
            <div className="col-12 col-xl-6" key={dependencyType}>
              <div className="border rounded p-3 h-100">
                <h4 className="h6 mb-1">
                  {t(`managedDependencies.${dependencyType.toLocaleLowerCase()}.label`)}
                </h4>
                <div className="small text-body-secondary mb-2">
                  {choice.provider?.cluster_name} / {choice.provider?.service_name}
                </div>
                {preview?.compatible ? (
                  <>
                    <Alert className="py-2" variant="success">
                      {t("managedDependencySettings.compatible")}
                    </Alert>
                    <dl className="row small mb-0">
                      {paths.map(([label, value]) => (
                          <div className="col-12 mb-2" key={`${label}:${value}`}>
                            <dt>{label}</dt>
                            <dd className="text-break mb-0">{value}</dd>
                          </div>
                        ))}
                    </dl>
                    {settings.length ? (
                      <details className="small mt-2">
                        <summary className="fw-semibold">
                          {t("managedDependencySettings.advancedSettings")}
                        </summary>
                        <dl className="row mb-0 mt-2">
                          {settings.map(({ label, value }) => (
                            <div className="col-12 mb-2" key={`${label}:${value}`}>
                              <dt>{label}</dt>
                              <dd className="text-break mb-0">{value}</dd>
                            </div>
                          ))}
                        </dl>
                      </details>
                    ) : null}
                  </>
                ) : (
                  <Alert className="mb-0 py-2" variant="warning">
                    {choice.planningIssue?.message
                      || preview?.errors?.[0]?.message
                      || t("managedDependencySettings.pending")}
                  </Alert>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </section>
  );
}
