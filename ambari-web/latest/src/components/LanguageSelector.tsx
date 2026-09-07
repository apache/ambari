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

import { Form } from "react-bootstrap";
import { useTranslation } from "react-i18next";

export default function LanguageSelector() {
  const { t, i18n } = useTranslation();

  return (
    <Form.Select
      size="sm"
      className="w-auto flex-shrink-0"
      aria-label={t("app.language")}
      title={t("app.language")}
      value={i18n.resolvedLanguage || "en"}
      onChange={(event) => void i18n.changeLanguage(event.target.value)}
    >
      <option value="en" lang="en">English</option>
      <option value="zh" lang="zh-CN">{t("app.language.chinese")}</option>
    </Form.Select>
  );
}
