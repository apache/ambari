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

import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import LanguageDetector from 'i18next-browser-languagedetector';
import englishTranslations from './locales/en/translation.json';
import chineseTranslations from './locales/zh/translation.json';

const languageDetector = new LanguageDetector();
languageDetector.addDetector({
  name: 'ambariLocalStorage',
  lookup() {
    try {
      return localStorage.getItem('i18nextLng') || undefined;
    } catch {
      return undefined;
    }
  },
  cacheUserLanguage(language: string) {
    try {
      localStorage.setItem('i18nextLng', language);
    } catch {
      // Language switching must still work when browser storage is blocked.
    }
  },
});

// Keep the document language aligned with the bundled Simplified Chinese locale.
i18n.on('languageChanged', (language: string) => {
  if (typeof document !== 'undefined') {
    document.documentElement.lang = language.startsWith('zh') ? 'zh-CN' : 'en';
  }
});

i18n
  // detect user language
  .use(languageDetector)
  // pass the i18n instance to react-i18next.
  .use(initReactI18next)
  // init i18next
  .init({
    initAsync: false,
    supportedLngs: ['en', 'zh'],
    load: 'languageOnly',
    fallbackLng: 'en',
    detection: {
      order: ['ambariLocalStorage', 'navigator', 'htmlTag'],
      caches: ['ambariLocalStorage'],
    },
    interpolation: {
      escapeValue: false, // not needed for react as it escapes by default
    },
    resources: {
      en: {
        translation: englishTranslations
      },
      zh: {
        translation: chineseTranslations
      }
    }
  });

export default i18n;
