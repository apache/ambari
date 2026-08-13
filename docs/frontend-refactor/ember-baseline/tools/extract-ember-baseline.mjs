#!/usr/bin/env node

/*
 * Generates traceable inventories for the classic Ambari Web application.
 * This intentionally uses only Node built-ins so it can run before npm install.
 */

import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { fileURLToPath } from 'node:url';
import dynamicAjaxResolutions from './contracts/dynamic-ajax-resolutions.mjs';
import realtimeChannels from './contracts/realtime-channels.mjs';

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(scriptDir, '../../../..');
const classicRoot = path.join(repoRoot, 'ambari-web/classic');
const appRoot = path.join(classicRoot, 'app');
const baselineRoot = path.resolve(scriptDir, '..');
const outputRoot = path.resolve(scriptDir, '..', 'generated');
const ajaxFile = path.join(appRoot, 'utils/ajax/ajax.js');

const METRICS_MARKERS = [
  /metrics?/i,
  /heatmaps?/i,
  /horizon[_-]?chart/i,
  /\/charts?(?:\/|$)/i,
  /timeline/i,
  /ambari[_-]?metrics/i,
  /graph[_-]?(?:data|widget)/i,
  /chart[_-]?(?:data|widget)/i,
  /widgets?\.(?:wizard|layout|hostComponent|hosts)/i,
];

const METRICS_REQUEST_NAMES = [
  /(?:^|\.)metrics?(?:\.|$)/i,
  /(?:^|\.)heatmaps?(?:\.|$)/i,
  /^dashboard\.cluster_metrics\./i,
  /^namenode\.cpu_wio$/i,
  /^widgets?\./i,
  /^widget\.(?!activelayouts)/i,
];

const OPERATIONAL_REQUEST_NAMES = new Set([
  'hosts.ips',
  'hosts.metrics.lazy_load',
  'hiveServerInteractive.getStatus',
]);

const FEATURE_MODULE_FILE_PATTERN = /^(?:0[1-9]|1[0-3])-.*\.md$/;

const OPEN_DYNAMIC_AJAX_DISPATCH_KINDS = new Set([
  'PARAMETER_WRAPPER',
  'MODEL_METADATA_LOOKUP',
  'MIXIN_REQUEST_PROPERTY',
  'FIFO_OPTIONS_QUEUE',
]);

function toPosix(value) {
  return value.split(path.sep).join('/');
}

function relativeToRepo(file) {
  return toPosix(path.relative(repoRoot, file));
}

function isMetricsRelated(...values) {
  return METRICS_MARKERS.some((marker) =>
    values.some((value) => marker.test(String(value ?? ''))),
  );
}

function isMetricsUiFile(file) {
  const relative = toPosix(path.relative(appRoot, file));
  return [
    /^routes\/(?:create_widget|edit_widget)\.js$/,
    /^templates\/main\/service\/widgets(?:\/|$)/,
    /^templates\/main\/dashboard\/(?:widgets(?:\.hbs|\/)|edit_widget_popup(?:_single_threshold)?\.hbs$|plus_button_filter\.hbs$)/,
    /^templates\/common\/widget(?:\/|$)/,
    /^templates\/common\/modal_popups\/widget_browser_(?:footer|popup)\.hbs$/,
  ].some((pattern) => pattern.test(relative));
}

function isMetricsCaller(caller) {
  return isMetricsRelated(caller) || /\/mixins\/common\/widgets\//i.test(caller);
}

function isMetricsDefinition(definition) {
  if (OPERATIONAL_REQUEST_NAMES.has(definition.name)) return false;
  if (METRICS_REQUEST_NAMES.some((pattern) => pattern.test(definition.name))) return true;
  return definition.callers.length > 0 && definition.callers.every(isMetricsCaller);
}

function maskJavaScriptComments(source) {
  const masked = source.split('');
  let quote = null;
  let escaped = false;

  for (let index = 0; index < source.length; index += 1) {
    const char = source[index];
    const next = source[index + 1];
    if (quote) {
      if (escaped) escaped = false;
      else if (char === '\\') escaped = true;
      else if (char === quote) quote = null;
      continue;
    }
    if (char === "'" || char === '"' || char === '`') {
      quote = char;
      continue;
    }
    if (char === '/' && next === '/') {
      masked[index] = ' ';
      masked[index + 1] = ' ';
      index += 2;
      while (index < source.length && source[index] !== '\n') {
        masked[index] = source[index] === '\r' ? '\r' : ' ';
        index += 1;
      }
      index -= 1;
      continue;
    }
    if (char === '/' && next === '*') {
      masked[index] = ' ';
      masked[index + 1] = ' ';
      index += 2;
      while (index < source.length) {
        if (source[index] === '*' && source[index + 1] === '/') {
          masked[index] = ' ';
          masked[index + 1] = ' ';
          index += 1;
          break;
        }
        if (source[index] !== '\n' && source[index] !== '\r') masked[index] = ' ';
        index += 1;
      }
    }
  }
  return masked.join('');
}

function maskDelimitedComments(source, open, close) {
  const masked = source.split('');
  let cursor = 0;
  while (cursor < source.length) {
    const start = source.indexOf(open, cursor);
    if (start === -1) break;
    const closeIndex = source.indexOf(close, start + open.length);
    const end = closeIndex === -1 ? source.length : closeIndex + close.length;
    for (let index = start; index < end; index += 1) {
      if (source[index] !== '\n' && source[index] !== '\r') masked[index] = ' ';
    }
    cursor = end;
  }
  return masked.join('');
}

function maskHandlebarsComments(source) {
  let masked = maskDelimitedComments(source, '{{!--', '--}}');
  masked = maskDelimitedComments(masked, '{{!', '}}');
  return maskDelimitedComments(masked, '<!--', '-->');
}

function listFiles(root, predicate) {
  const files = [];
  for (const entry of fs.readdirSync(root, { withFileTypes: true })) {
    const fullPath = path.join(root, entry.name);
    if (entry.isDirectory()) {
      files.push(...listFiles(fullPath, predicate));
    } else if (predicate(fullPath)) {
      files.push(fullPath);
    }
  }
  return files.sort();
}

function lineNumberAt(source, index) {
  let line = 1;
  for (let cursor = 0; cursor < index; cursor += 1) {
    if (source.charCodeAt(cursor) === 10) line += 1;
  }
  return line;
}

function scanBalanced(source, openIndex, openChar = '{', closeChar = '}') {
  let depth = 0;
  let quote = null;
  let escaped = false;
  let lineComment = false;
  let blockComment = false;

  for (let index = openIndex; index < source.length; index += 1) {
    const char = source[index];
    const next = source[index + 1];

    if (lineComment) {
      if (char === '\n') lineComment = false;
      continue;
    }
    if (blockComment) {
      if (char === '*' && next === '/') {
        blockComment = false;
        index += 1;
      }
      continue;
    }
    if (quote) {
      if (escaped) {
        escaped = false;
      } else if (char === '\\') {
        escaped = true;
      } else if (char === quote) {
        quote = null;
      }
      continue;
    }
    if (char === '/' && next === '/') {
      lineComment = true;
      index += 1;
      continue;
    }
    if (char === '/' && next === '*') {
      blockComment = true;
      index += 1;
      continue;
    }
    if (char === "'" || char === '"' || char === '`') {
      quote = char;
      continue;
    }
    if (char === openChar) depth += 1;
    if (char === closeChar) {
      depth -= 1;
      if (depth === 0) return index;
    }
  }
  throw new Error(`Unbalanced ${openChar}${closeChar} block at offset ${openIndex}`);
}

function extractTopLevelProperty(objectSource, propertyName) {
  const propertyPattern = new RegExp(`(?:^|\\n)\\s{4}(?:['\"]${propertyName}['\"]|${propertyName})\\s*:\\s*`, 'g');
  const match = propertyPattern.exec(objectSource);
  if (!match) return null;

  const start = match.index + match[0].length;
  let quote = null;
  let escaped = false;
  let lineComment = false;
  let blockComment = false;
  let roundDepth = 0;
  let squareDepth = 0;
  let curlyDepth = 0;

  for (let index = start; index < objectSource.length; index += 1) {
    const char = objectSource[index];
    const next = objectSource[index + 1];
    if (lineComment) {
      if (char === '\n') lineComment = false;
      continue;
    }
    if (blockComment) {
      if (char === '*' && next === '/') {
        blockComment = false;
        index += 1;
      }
      continue;
    }
    if (quote) {
      if (escaped) escaped = false;
      else if (char === '\\') escaped = true;
      else if (char === quote) quote = null;
      continue;
    }
    if (char === '/' && next === '/') {
      lineComment = true;
      index += 1;
      continue;
    }
    if (char === '/' && next === '*') {
      blockComment = true;
      index += 1;
      continue;
    }
    if (char === "'" || char === '"' || char === '`') {
      quote = char;
      continue;
    }
    if (char === '(') roundDepth += 1;
    if (char === ')') roundDepth -= 1;
    if (char === '[') squareDepth += 1;
    if (char === ']') squareDepth -= 1;
    if (char === '{') curlyDepth += 1;
    if (char === '}') {
      if (curlyDepth === 0 && roundDepth === 0 && squareDepth === 0) {
        return objectSource.slice(start, index).trim();
      }
      curlyDepth -= 1;
    }
    if (char === ',' && roundDepth === 0 && squareDepth === 0 && curlyDepth === 0) {
      return objectSource.slice(start, index).trim();
    }
  }
  return objectSource.slice(start).trim();
}

function skipWhitespaceAndComments(source, start) {
  let index = start;
  while (index < source.length) {
    if (/\s/.test(source[index])) {
      index += 1;
      continue;
    }
    if (source[index] === '/' && source[index + 1] === '/') {
      index += 2;
      while (index < source.length && source[index] !== '\n') index += 1;
      continue;
    }
    if (source[index] === '/' && source[index + 1] === '*') {
      const end = source.indexOf('*/', index + 2);
      return end === -1 ? source.length : skipWhitespaceAndComments(source, end + 2);
    }
    break;
  }
  return index;
}

/**
 * Splits an object literal into its top-level properties. This deliberately
 * handles only ordinary identifier/string keys; spread and computed keys are
 * retained by callers as part of the raw expression instead of being guessed.
 */
function extractObjectProperties(objectSource) {
  const open = objectSource.indexOf('{');
  if (open === -1) return new Map();
  let close;
  try {
    close = scanBalanced(objectSource, open);
  } catch {
    return new Map();
  }

  const properties = new Map();
  let index = open + 1;
  while (index < close) {
    index = skipWhitespaceAndComments(objectSource, index);
    while (objectSource[index] === ',') {
      index = skipWhitespaceAndComments(objectSource, index + 1);
    }
    if (index >= close) break;

    let key = null;
    if (objectSource[index] === "'" || objectSource[index] === '"') {
      const quote = objectSource[index];
      let cursor = index + 1;
      let escaped = false;
      for (; cursor < close; cursor += 1) {
        if (escaped) escaped = false;
        else if (objectSource[cursor] === '\\') escaped = true;
        else if (objectSource[cursor] === quote) break;
      }
      key = objectSource.slice(index + 1, cursor);
      index = cursor + 1;
    } else {
      const match = objectSource.slice(index).match(/^[$A-Z_a-z][$\w]*/);
      if (match) {
        key = match[0];
        index += match[0].length;
      }
    }

    index = skipWhitespaceAndComments(objectSource, index);
    if (!key || objectSource[index] !== ':') {
      // Skip methods, spreads, and computed keys without pretending to parse them.
      let cursor = index;
      let roundDepth = 0;
      let squareDepth = 0;
      let curlyDepth = 0;
      let quote = null;
      let escaped = false;
      for (; cursor < close; cursor += 1) {
        const char = objectSource[cursor];
        if (quote) {
          if (escaped) escaped = false;
          else if (char === '\\') escaped = true;
          else if (char === quote) quote = null;
          continue;
        }
        if (char === "'" || char === '"' || char === '`') quote = char;
        else if (char === '(') roundDepth += 1;
        else if (char === ')') roundDepth -= 1;
        else if (char === '[') squareDepth += 1;
        else if (char === ']') squareDepth -= 1;
        else if (char === '{') curlyDepth += 1;
        else if (char === '}') curlyDepth -= 1;
        else if (char === ',' && roundDepth === 0 && squareDepth === 0 && curlyDepth === 0) break;
      }
      index = cursor + 1;
      continue;
    }

    const valueStart = skipWhitespaceAndComments(objectSource, index + 1);
    let cursor = valueStart;
    let roundDepth = 0;
    let squareDepth = 0;
    let curlyDepth = 0;
    let quote = null;
    let escaped = false;
    let lineComment = false;
    let blockComment = false;
    for (; cursor < close; cursor += 1) {
      const char = objectSource[cursor];
      const next = objectSource[cursor + 1];
      if (lineComment) {
        if (char === '\n') lineComment = false;
        continue;
      }
      if (blockComment) {
        if (char === '*' && next === '/') {
          blockComment = false;
          cursor += 1;
        }
        continue;
      }
      if (quote) {
        if (escaped) escaped = false;
        else if (char === '\\') escaped = true;
        else if (char === quote) quote = null;
        continue;
      }
      if (char === '/' && next === '/') {
        lineComment = true;
        cursor += 1;
      } else if (char === '/' && next === '*') {
        blockComment = true;
        cursor += 1;
      } else if (char === "'" || char === '"' || char === '`') quote = char;
      else if (char === '(') roundDepth += 1;
      else if (char === ')') roundDepth -= 1;
      else if (char === '[') squareDepth += 1;
      else if (char === ']') squareDepth -= 1;
      else if (char === '{') curlyDepth += 1;
      else if (char === '}') curlyDepth -= 1;
      else if (char === ',' && roundDepth === 0 && squareDepth === 0 && curlyDepth === 0) break;
    }
    properties.set(key, objectSource.slice(valueStart, cursor).trim());
    index = cursor + 1;
  }
  return properties;
}

function extractReturnedObjectProperties(functionExpression) {
  if (!functionExpression) return [];
  const masked = maskJavaScriptComments(functionExpression);
  const returnedObjects = [];
  for (const match of masked.matchAll(/\breturn\b/g)) {
    const objectStart = skipWhitespaceAndComments(masked, match.index + match[0].length);
    if (masked[objectStart] !== '{') continue;
    let objectEnd;
    try {
      objectEnd = scanBalanced(functionExpression, objectStart);
    } catch {
      continue;
    }
    returnedObjects.push(extractObjectProperties(functionExpression.slice(objectStart, objectEnd + 1)));
  }
  return returnedObjects;
}

function staticHttpMethod(expression) {
  return expression?.match(/^(['"])([A-Z]+)\1$/)?.[2] ?? null;
}

function isStaticEmptyString(expression) {
  return /^(['"])\1$/.test(expression?.trim() ?? '');
}

function isStaticStringExpression(expression) {
  if (!expression) return false;
  const stringPattern = /(['"])((?:\\.|(?!\1).)*)\1/g;
  return expression.replace(stringPattern, '').replace(/[+\s()]/g, '') === '';
}

function sha256(value) {
  return crypto.createHash('sha256').update(value).digest('hex');
}

function sha256Json(value) {
  return sha256(JSON.stringify(value));
}

function normalizeExpression(expression, maxLength = 180) {
  const value = String(expression ?? '').replace(/\s+/g, ' ').trim();
  return value.length <= maxLength ? value : `${value.slice(0, maxLength - 3)}...`;
}

function extractReferencedDataKeys(source) {
  const keys = new Set();
  for (const match of source.matchAll(/\bdata(?:\?\.)?\.([$A-Z_a-z][$\w]*)/g)) keys.add(match[1]);
  for (const match of source.matchAll(/\bdata\s*\[\s*(['"])([^'"\n]+)\1\s*\]/g)) keys.add(match[2]);
  return [...keys].sort();
}

function collapseStringExpression(expression) {
  if (!expression) return '';
  const stringParts = [];
  const stringPattern = /(['"])((?:\\.|(?!\1).)*)\1/g;
  let match;
  while ((match = stringPattern.exec(expression))) {
    stringParts.push(match[2].replace(/\\(['"\\])/g, '$1'));
  }
  const collapsed = stringParts.join('');
  if (!collapsed) return expression.replace(/\s+/g, ' ').trim();
  const isOnlyStringsAndConcatenation = expression
    .replace(stringPattern, '')
    .replace(/[+\s()]/g, '') === '';
  return isOnlyStringsAndConcatenation
    ? collapsed
    : `${collapsed} [dynamic: ${expression.replace(/\s+/g, ' ').trim()}]`;
}

function escapeMarkdown(value) {
  return String(value ?? '')
    .replace(/\|/g, '\\|')
    .replace(/\r?\n/g, ' ')
    .replace(/`/g, '\\`');
}

function extractAjaxDefinitions() {
  const source = fs.readFileSync(ajaxFile, 'utf8');
  const declaration = source.indexOf('var urls = {');
  if (declaration === -1) throw new Error('Could not find AJAX URL registry');
  const registryOpen = source.indexOf('{', declaration);
  const registryClose = scanBalanced(source, registryOpen);
  const registry = source.slice(registryOpen + 1, registryClose);
  const entryPattern = /^\s{2}(['"])([^'"\n]+)\1\s*:\s*\{/gm;
  const definitions = [];
  let match;

  while ((match = entryPattern.exec(registry))) {
    const entryOpen = registry.indexOf('{', match.index);
    const entryClose = scanBalanced(registry, entryOpen);
    const objectSource = registry.slice(entryOpen, entryClose + 1);
    const realExpression = extractTopLevelProperty(objectSource, 'real');
    const apiPrefixExpression = extractTopLevelProperty(objectSource, 'apiPrefix');
    const topLevelType = extractTopLevelProperty(objectSource, 'type');
    const formatExpression = extractTopLevelProperty(objectSource, 'format');
    const formatProperties = extractReturnedObjectProperties(formatExpression);
    const formatTypeExpressions = formatProperties
      .map((properties) => properties.get('type'))
      .filter((expression) => expression !== undefined);
    const formatUrlExpressions = formatProperties
      .map((properties) => properties.get('url'))
      .filter((expression) => expression !== undefined);
    const methods = new Set();
    const methodExpressions = [topLevelType, ...formatTypeExpressions]
      .filter((expression) => expression !== null && expression !== undefined);
    for (const expression of methodExpressions) {
      const method = staticHttpMethod(expression);
      if (method) methods.add(method);
    }
    if (methods.size === 0) methods.add('GET');
    const hasDynamicMethod = methodExpressions.some((expression) => !staticHttpMethod(expression));
    if (hasDynamicMethod) methods.add('DYNAMIC');

    const realIsEmpty = realExpression === null || isStaticEmptyString(realExpression);
    const usesFormatUrl = realIsEmpty && formatUrlExpressions.length > 0;
    const endpointExpressions = usesFormatUrl ? formatUrlExpressions : [realExpression].filter(Boolean);
    let endpoint = endpointExpressions.map(collapseStringExpression).join(' OR ');
    let endpointSource = 'real';
    if (usesFormatUrl) {
      endpointSource = 'format';
    }
    const endpointExpression = endpointExpressions.map((expression) => normalizeExpression(expression)).join(' OR ') || null;
    const hasDynamicUrl = endpointExpressions.some((expression) => !isStaticStringExpression(expression));
    const registryOffset = registryOpen + 1 + match.index;
    definitions.push({
      name: match[2],
      methods: [...methods].sort(),
      endpoint: endpoint || '(empty)',
      endpointSource,
      endpointExpression,
      hasDynamicUrl,
      apiPrefix: apiPrefixExpression === null ? '/api/v1 (default)' : collapseStringExpression(apiPrefixExpression) || '(empty)',
      hasFormat: formatExpression !== null,
      formatExpression,
      topLevelTypeExpression: topLevelType,
      formatTypeExpressions,
      formatUrlExpressions,
      inputKeys: extractReferencedDataKeys(objectSource),
      line: lineNumberAt(source, registryOffset),
      source: relativeToRepo(ajaxFile),
      sourceContractSha256: sha256(objectSource),
      objectSource,
    });
    entryPattern.lastIndex = entryClose + 1;
  }
  return definitions;
}

function extractAjaxCalls(jsFiles, includedRequestNames, allRequestNames) {
  const calls = [];
  const callPattern = /App\.ajax\.send\s*\(/g;
  for (const file of jsFiles) {
    if (file === ajaxFile || isMetricsUiFile(file)) continue;
    const isMetricsFile = isMetricsRelated(file);
    const originalSource = fs.readFileSync(file, 'utf8');
    const source = maskJavaScriptComments(originalSource);
    let match;
    while ((match = callPattern.exec(source))) {
      const argumentStart = skipWhitespaceAndComments(source, match.index + match[0].length);
      let argumentExpression;
      let properties = new Map();
      if (source[argumentStart] === '{') {
        let argumentEnd;
        try {
          argumentEnd = scanBalanced(source, argumentStart);
        } catch {
          argumentEnd = source.indexOf('\n', argumentStart);
          if (argumentEnd === -1) argumentEnd = source.length;
        }
        argumentExpression = originalSource.slice(argumentStart, argumentEnd + 1);
        properties = extractObjectProperties(argumentExpression);
        callPattern.lastIndex = Math.max(callPattern.lastIndex, argumentEnd + 1);
      } else {
        let cursor = argumentStart;
        let roundDepth = 0;
        let squareDepth = 0;
        let curlyDepth = 0;
        let quote = null;
        let escaped = false;
        for (; cursor < source.length; cursor += 1) {
          const char = source[cursor];
          if (quote) {
            if (escaped) escaped = false;
            else if (char === '\\') escaped = true;
            else if (char === quote) quote = null;
            continue;
          }
          if (char === "'" || char === '"' || char === '`') quote = char;
          else if (char === '(') roundDepth += 1;
          else if (char === ')') {
            if (roundDepth === 0 && squareDepth === 0 && curlyDepth === 0) break;
            roundDepth -= 1;
          } else if (char === '[') squareDepth += 1;
          else if (char === ']') squareDepth -= 1;
          else if (char === '{') curlyDepth += 1;
          else if (char === '}') curlyDepth -= 1;
          else if (char === ',' && roundDepth === 0 && squareDepth === 0 && curlyDepth === 0) break;
        }
        argumentExpression = originalSource.slice(argumentStart, cursor);
      }

      const nameExpression = properties.get('name');
      const staticNameMatch = nameExpression?.match(/^(['"])([^'"\n]+)\1$/);
      if (isMetricsFile && !OPERATIONAL_REQUEST_NAMES.has(staticNameMatch?.[2])) continue;
      if (staticNameMatch && allRequestNames.has(staticNameMatch[2])
          && !includedRequestNames.has(staticNameMatch[2])) continue;
      const dataExpression = properties.get('data');
      const dataProperties = dataExpression?.trim().startsWith('{')
        ? [...extractObjectProperties(dataExpression).keys()]
        : [];
      const callbacks = ['success', 'error', 'callback', 'complete', 'statusCode']
        .filter((key) => properties.has(key));
      calls.push({
        requestName: staticNameMatch?.[2] ?? null,
        requestExpression: normalizeExpression(nameExpression || argumentExpression),
        registrationStatus: staticNameMatch
          ? (includedRequestNames.has(staticNameMatch[2]) ? 'REGISTERED' : 'UNREGISTERED')
          : 'DYNAMIC',
        dataKeys: dataProperties.sort(),
        dataExpression: dataExpression && dataProperties.length === 0 ? normalizeExpression(dataExpression) : null,
        callbacks,
        source: relativeToRepo(file),
        line: lineNumberAt(source, match.index),
      });
    }
  }
  return calls;
}

function dynamicAjaxResolutionKey({ source, line, requestExpression }) {
  return `${source}\u0000${line}\u0000${requestExpression}`;
}

function attachDynamicAjaxResolutions(calls) {
  const resolutionsByKey = new Map();
  for (const resolution of dynamicAjaxResolutions) {
    const key = dynamicAjaxResolutionKey(resolution);
    if (resolutionsByKey.has(key)) {
      throw new Error(`Duplicate dynamic AJAX resolution contract for ${resolution.source}:${resolution.line}`);
    }
    resolutionsByKey.set(key, resolution);
  }

  const consumedKeys = new Set();
  const resolvedCalls = calls.map((call) => {
    if (call.registrationStatus !== 'DYNAMIC') return call;
    const key = dynamicAjaxResolutionKey(call);
    const resolution = resolutionsByKey.get(key);
    if (!resolution) {
      throw new Error(`Dynamic AJAX call ${call.source}:${call.line} (${call.requestExpression}) has no exact resolution contract`);
    }
    if (consumedKeys.has(key)) {
      throw new Error(`Dynamic AJAX resolution contract was consumed more than once at ${call.source}:${call.line}`);
    }
    consumedKeys.add(key);
    return {
      ...call,
      resolutionStatus: OPEN_DYNAMIC_AJAX_DISPATCH_KINDS.has(resolution.dispatchKind)
        ? 'RESOLVED_OPEN_BOUNDARY'
        : 'RESOLVED_CLOSED',
      dispatchKind: resolution.dispatchKind,
      candidateRequestNames: resolution.candidateRequestNames,
      dispatchCondition: resolution.dispatchCondition,
      boundaryNotes: resolution.boundaryNotes,
      evidence: resolution.evidence,
    };
  });

  const unused = dynamicAjaxResolutions.filter((resolution) =>
    !consumedKeys.has(dynamicAjaxResolutionKey(resolution)));
  if (unused.length) {
    throw new Error(`Unused dynamic AJAX resolution contracts: ${unused.map((item) => `${item.source}:${item.line}`).join(', ')}`);
  }
  return resolvedCalls;
}

function extractPermissionUses(jsFiles, templateFiles) {
  const uses = new Map();
  const addUse = (permissions, gate, file, source, index) => {
    for (const permission of permissions.split(',').map((item) => item.trim()).filter(Boolean)) {
      if (isMetricsRelated(permission)) continue;
      if (!uses.has(permission)) uses.set(permission, []);
      uses.get(permission).push({
        kind: gate,
        source: relativeToRepo(file),
        line: lineNumberAt(source, index),
      });
    }
  };
  const permissionPattern = /\b(?:App\.)?(isAuthorized|havePermissions)\s*\(\s*(['"])([^'"\n]+)\2\s*\)/g;
  for (const file of jsFiles) {
    if (isMetricsUiFile(file) || isMetricsRelated(file)) continue;
    const source = maskJavaScriptComments(fs.readFileSync(file, 'utf8'));
    let match;
    while ((match = permissionPattern.exec(source))) {
      addUse(match[3], match[1], file, source, match.index);
    }
  }
  const templatePattern = /{{#?(isAuthorized|havePermissions)\s+(['"])([^'"\n]+)\2/g;
  for (const file of templateFiles) {
    if (isMetricsUiFile(file) || isMetricsRelated(file)) continue;
    const source = maskHandlebarsComments(fs.readFileSync(file, 'utf8'));
    let match;
    while ((match = templatePattern.exec(source))) {
      addUse(match[3], match[1], file, source, match.index);
    }
  }
  return [...uses.entries()]
    .map(([name, rawUses]) => {
      const uniqueUses = [...new Map(rawUses.map((use) => [
        `${use.kind}:${use.source}:${use.line}`,
        use,
      ])).values()];
      return {
        name,
        kinds: [...new Set(uniqueUses.map((use) => use.kind))].sort(),
        callers: uniqueUses.map((use) => `${use.source}:${use.line}`),
        uses: uniqueUses,
      };
    })
    .sort((left, right) => left.name.localeCompare(right.name));
}

function extractFeatureFlagUses(jsFiles, templateFiles) {
  const uses = new Map();
  const patterns = [
    /App\.supports\.([$A-Z_a-z][$\w]*)/g,
    /App\.get\(\s*(['"])supports\.([$A-Z_a-z][$\w]*)\1\s*\)/g,
    /(?:this\.)?get\(\s*(['"])App\.supports\.([$A-Z_a-z][$\w]*)\1\s*\)/g,
  ];
  for (const file of jsFiles) {
    if (isMetricsUiFile(file) || isMetricsRelated(file)) continue;
    const source = maskJavaScriptComments(fs.readFileSync(file, 'utf8'));
    for (const pattern of patterns) {
      let match;
      while ((match = pattern.exec(source))) {
        const name = match[2] ?? match[1];
        if (!name || isMetricsRelated(name)) continue;
        if (!uses.has(name)) uses.set(name, []);
        uses.get(name).push({
          kind: 'JavaScript',
          source: relativeToRepo(file),
          line: lineNumberAt(source, match.index),
        });
      }
    }
  }
  const templatePattern = /\bApp\.supports\.([$A-Z_a-z][$\w]*)/g;
  for (const file of templateFiles) {
    if (isMetricsUiFile(file) || isMetricsRelated(file)) continue;
    const source = maskHandlebarsComments(fs.readFileSync(file, 'utf8'));
    let match;
    while ((match = templatePattern.exec(source))) {
      if (isMetricsRelated(match[1])) continue;
      if (!uses.has(match[1])) uses.set(match[1], []);
      uses.get(match[1]).push({
        kind: 'Handlebars',
        source: relativeToRepo(file),
        line: lineNumberAt(source, match.index),
      });
    }
  }
  return [...uses.entries()]
    .map(([name, rawUses]) => {
      const uniqueUses = [...new Map(rawUses.map((use) => [
        `${use.kind}:${use.source}:${use.line}`,
        use,
      ])).values()];
      return {
        name,
        kinds: [...new Set(uniqueUses.map((use) => use.kind))].sort(),
        callers: uniqueUses.map((use) => `${use.source}:${use.line}`),
        uses: uniqueUses,
      };
    })
    .sort((left, right) => left.name.localeCompare(right.name));
}

function findDefinitionCallers(definitions, jsFiles) {
  const searchableFiles = jsFiles.filter((file) => file !== ajaxFile);
  const fileContents = searchableFiles.map((file) => ({
    file,
    source: maskJavaScriptComments(fs.readFileSync(file, 'utf8')),
  }));

  for (const definition of definitions) {
    definition.callers = [];
    const quotedName = new RegExp(`(['"])${definition.name.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\1`, 'g');
    for (const { file, source } of fileContents) {
      let match;
      while ((match = quotedName.exec(source))) {
        definition.callers.push(`${relativeToRepo(file)}:${lineNumberAt(source, match.index)}`);
      }
    }
  }
}

function classifyModules(definition) {
  const evidence = `${definition.name} ${definition.callers.join(' ')}`;
  const modules = [];
  const rules = [
    ['认证与应用外壳', /router|login|application|user_settings|inactiv|keepalive/i],
    ['安装向导', /installer|wizard\/step|controllers\/wizard|bootstrap|recommendation|blueprint/i],
    ['主机', /main\/host|hosts?|host_component/i],
    ['服务与配置', /main\/service|service_config|config_group|configs?\//i],
    ['告警', /alerts?|alert_/i],
    ['Stack 与升级', /stack_and_upgrade|stack_upgrade|repo|version_definition|upgrades?/i],
    ['安全、高可用与联邦', /kerberos|security|highAvailability|federation|journal|namenode|resourceManager|rangerAdmin|hawq/i],
    ['Views', /main\/views|routes\/view|views?\./i],
    ['后台操作与通用能力', /background|request|cluster_controller|update_controller|utils\//i],
  ];
  for (const [name, pattern] of rules) {
    if (pattern.test(evidence)) modules.push(name);
  }
  return modules.length ? modules : ['跨模块与待人工归类'];
}

function classifyModule(definition) {
  return classifyModules(definition).join('、');
}

const DIRECT_HTTP_CONTRACTS = {
  'ambari-web/classic/app/controllers/global/cluster_controller.js:194': {
    semanticKind: 'CLUSTER_MODEL_LOAD',
    urlContract: 'GET /api/v1/clusters/{clusterName}?fields=Clusters',
    operationalFields: ['Clusters/*'],
    notes: '初始加载 cluster identity/security/credential-store 等基础模型；complete 在 mapper 后刷新 isCredentialStorePersistent。',
  },
  'ambari-web/classic/app/controllers/global/cluster_controller.js:384': {
    semanticKind: 'DYNAMIC_HOST_MODEL_LOAD_HELPER',
    urlContract: 'GET {callerSuppliedRealUrl}; test mode uses /data/hosts/HDP2/hosts.json',
    operationalFields: ['caller supplied'],
    notes: '`requestHosts()` 在经典 app 树中无生产调用者，只有 controller unit test；保留为 STATIC_ONLY 遗留 helper。',
  },
  'ambari-web/classic/app/controllers/global/cluster_controller.js:449': {
    semanticKind: 'CLUSTER_MODEL_REFRESH',
    urlContract: 'GET /api/v1/clusters/{clusterName}?fields=Clusters',
    operationalFields: ['Clusters/*'],
    notes: '运行期刷新 cluster mapper；complete callback 为空。',
  },
  'ambari-web/classic/app/controllers/global/cluster_controller.js:485': {
    semanticKind: 'ORIGINAL_REQUEST_REPLAY',
    urlContract: '{ajaxOpt.type} {ajaxOpt.url}; payload/headers/callbacks are the original failed jQuery request',
    operationalFields: ['original request dependent'],
    notes: '先保存 KDC credential，再原样重放 `ajaxOpt`；不是新 endpoint，也不得丢失原 success/error/statusCode 处理。',
  },
  'ambari-web/classic/app/controllers/global/update_controller.js:323': {
    semanticKind: 'HOST_MODEL_LOAD',
    urlContract: 'GET-over-POST /api/v1/clusters/{clusterName}/hosts?fields={runtimeFields}&minimal_response=true&{pagination}&{sort}; RequestInfo.query={hostPredicate}',
    operationalFields: [
      'Hosts rack/name/maintenance/public_host/cpu_count/ph_cpu_count/status/state/heartbeat/ip/os/agent-env',
      'host component state/maintenance/stale-config/service/display/desired-admin-state',
      'stack/repository version identity',
      'logging metadata when supports.logSearch',
      'NameNode ClusterId and HAState when HDFS is loaded',
    ],
    notes: '响应也可条件性携带 disk/load/cpu/memory 指标字段，这些数值排除；过滤、排序、分页和超长 GET override 行为必须保留。',
  },
  'ambari-web/classic/app/controllers/global/update_controller.js:568': {
    semanticKind: 'COMPONENT_TOPOLOGY_LOAD',
    urlContract: 'GET /api/v1/clusters/{clusterName}/components/?{runtimeComponentPredicates}&fields={runtimeFields}&minimal_response=true',
    operationalFields: [
      'service/component identity and master/client topology',
      'host/display/public-host/state/maintenance/stale-config/ha-state/desired-admin-state',
      'HDFS ClusterId and HBase IsActiveMaster operational selectors',
    ],
    notes: '共用 serviceMetricsMapper 且后续请求会携带大量指标；本基线只保留 topology/state/HA/Active-Standby 等运维语义。',
  },
  'ambari-web/classic/app/controllers/global/update_controller.js:615': {
    semanticKind: 'SERVICE_STATE_LOAD',
    urlContract: 'GET /api/v1/clusters/{clusterName}/services?fields=ServiceInfo/state,ServiceInfo/maintenance_state,ServiceInfo/desired_repository_version_id,components/ServiceComponentInfo/component_name&minimal_response=true',
    operationalFields: ['service state/maintenance/desired repository version', 'component names'],
    notes: '集群初始化的 service 模型加载。',
  },
  'ambari-web/classic/app/controllers/global/update_controller.js:626': {
    semanticKind: 'COMPONENT_STATE_LOAD',
    urlContract: 'GET /api/v1/clusters/{clusterName}/components/?fields=ServiceComponentInfo/{service_name,category,installed_count,started_count,init_count,install_failed_count,unknown_count,total_count,display_name},host_components/HostRoles/host_name&minimal_response=true',
    operationalFields: ['component category and aggregate state counts', 'host component host names'],
    notes: '用于服务组件状态和拓扑映射。',
  },
  'ambari-web/classic/app/controllers/global/update_controller.js:639': {
    semanticKind: 'ALERT_DEFINITION_LOAD',
    urlContract: 'GET /api/v1/clusters/{clusterName}/alert_definitions?fields=AlertDefinition/{component_name,description,enabled,repeat_tolerance,repeat_tolerance_enabled,id,ignore_host,interval,label,name,scope,service_name,source,help_url}',
    operationalFields: ['alert definition configuration and source'],
    notes: '初始加载全部告警定义。',
  },
  'ambari-web/classic/app/controllers/global/update_controller.js:654': {
    semanticKind: 'UNHEALTHY_ALERT_LOAD',
    urlContract: 'GET /api/v1/clusters/{clusterName}/alerts?fields={Alert operational fields}&Alert/state.in(CRITICAL,WARNING)&Alert/maintenance_state.in(OFF)&from={from}&page_size={pageSize}',
    operationalFields: ['critical/warning non-maintenance alert instances', 'repeat-tolerance remaining', 'timestamps/text/host/service/component'],
    notes: '只载入非健康且 maintenance OFF 的分页实例。',
  },
  'ambari-web/classic/app/controllers/global/update_controller.js:665': {
    semanticKind: 'ALERT_GROUPED_SUMMARY_LOAD',
    urlContract: 'GET /api/v1/clusters/{clusterName}/alerts?format=groupedSummary',
    operationalFields: ['server grouped alert summary'],
    notes: '为告警定义摘要 mapper 加载服务端分组统计。',
  },
  'ambari-web/classic/app/controllers/global/update_controller.js:676': {
    semanticKind: 'ALERT_GROUP_LOAD',
    urlContract: 'GET /api/v1/clusters/{clusterName}/alert_groups?fields=AlertGroup/{default,definitions,id,name,targets}',
    operationalFields: ['alert group membership and targets'],
    notes: '初始加载告警组。',
  },
  'ambari-web/classic/app/controllers/global/update_controller.js:682': {
    semanticKind: 'ALERT_TARGET_LOAD',
    urlContract: 'GET /api/v1/alert_targets?fields=*',
    operationalFields: ['all alert target/notification fields'],
    notes: '根级 alert target 资源，不带 cluster path。',
  },
  'ambari-web/classic/app/controllers/main/admin/stack_and_upgrade_controller.js:2025': {
    semanticKind: 'STACK_VERSION_LOAD',
    urlContract: 'GET /api/v1/clusters/{clusterName}/stack_versions?fields={full: *,repository_versions/*,... | update: ClusterStackVersions/*}',
    operationalFields: ['cluster stack versions', 'repository/OS/repository details on full load'],
    notes: '`fullLoad` 选择初始全量 URL 或运行期轻量 URL。',
  },
  'ambari-web/classic/app/controllers/main/admin/stack_and_upgrade_controller.js:2041': {
    semanticKind: 'REPOSITORY_VERSION_LOAD',
    urlContract: 'GET /api/v1/stacks?fields=versions/repository_versions/RepositoryVersions,versions/repository_versions/operating_systems/*,versions/repository_versions/operating_systems/repositories/*',
    operationalFields: ['stack repository versions', 'OS and repository definitions'],
    notes: '全量 repository-version model 加载。',
  },
  'ambari-web/classic/app/controllers/main/admin/stack_upgrade_history_controller.js:69': {
    semanticKind: 'UPGRADE_HISTORY_LOAD',
    urlContract: 'GET /api/v1/clusters/{clusterName}/upgrades?fields=Upgrade',
    operationalFields: ['Upgrade/*'],
    notes: '历史列表 complete 时即 resolve；HttpClient error 不调 complete，可留下未解决 promise。',
  },
  'ambari-web/classic/app/controllers/main/dashboard/config_history_controller.js:131': {
    semanticKind: 'CONFIG_HISTORY_LOAD',
    urlContract: 'GET /api/v1/clusters/{clusterName}/configurations/service_config_versions?{filterAndSortPredicates}fields=service_config_version,user,group_id,group_name,is_current,createtime,service_name,hosts,service_config_version_note,is_cluster_compatible,stack_id&minimal_response=true',
    operationalFields: ['service config version history and compatibility metadata'],
    notes: '过滤、排序条件由 table mixin 动态生成。',
  },
  'ambari-web/classic/app/utils/polling.js:67': {
    semanticKind: 'GENERIC_MUTATION_POLL_HELPER',
    urlContract: 'PUT {App.Poll.url}; body={App.Poll.data}; response is text or JSON with Requests.id',
    operationalFields: ['Requests.id when asynchronous', 'caller-defined task/request polling data'],
    notes: '经典 app 树未发现 `App.Poll.create()` 生产调用，只有 unit test；空/非 JSON 成功会直接标记 success，有 request ID 才转轮询。',
  },
  'ambari-web/classic/app/views/main/admin/stack_upgrade/custom_cluster_checks/custom_cluster_ckecks_host_hearbeat_view.js:52': {
    semanticKind: 'HOST_DELETE_PREFLIGHT_LOAD',
    urlContract: 'GET /api/v1/clusters/{clusterName}/hosts/?Hosts/host_name.in({hostName})&fields={host,host-component,stack-version,logging and metric fields}&minimal_response=true&page_size=10&from=0&sortBy=Hosts/host_name.asc',
    operationalFields: ['host and host-component state/maintenance/stale-config/desired-admin-state', 'stack versions and logging', 'NameNode ClusterId/HAState safety selectors'],
    notes: '升级前 heartbeat check 中的安全删除流程；响应携带 disk/load/cpu/memory 指标但该指标展示不纳入。',
  },
};

const pageReloadContract = (urlContract, notes) => ({
  semanticKind: 'PAGE_RELOAD',
  networkEffect: 'NAVIGATION_REQUEST',
  urlContract,
  notes,
});

const BROWSER_NETWORK_CONTRACTS = {
  'ambari-web/classic/app/controllers/global/user_settings_controller.js:327': pageReloadContract(
    'reload current Ambari document after show_bg/timezone preference requests settle',
    '两个 preference mutation 都用 `.always()` 串联；成功或失败均可 reload 以应用 timezone。',
  ),
  'ambari-web/classic/app/controllers/main.js:122': pageReloadContract(
    'delayed reload of current Ambari document after route/status change',
    'cluster 已安装时，route 或 install status observer 延迟 `App.pageReloadTime` 后 reload。',
  ),
  'ambari-web/classic/app/controllers/main/host/bulk_operations_controller.js:453': pageReloadContract(
    'reload current Ambari document after bulk host-delete result closes',
    '成功/部分失败结果都整页刷新，先清除已删除 host selection。',
  ),
  'ambari-web/classic/app/controllers/main/host/bulk_operations_controller.js:792': pageReloadContract(
    'reload current Ambari document after bulk host-component-delete result primary action',
    '结果 popup 的 Primary 与 Close 各有独立 reload 调用。',
  ),
  'ambari-web/classic/app/controllers/main/host/bulk_operations_controller.js:797': pageReloadContract(
    'reload current Ambari document after bulk host-component-delete result closes',
    '结果 popup 的 Close 分支整页重建 host/component 模型。',
  ),
  'ambari-web/classic/app/controllers/main/alerts/alert_definitions_actions_controller.js:251': {
    semanticKind: 'PAGE_RELOAD',
    networkEffect: 'NAVIGATION_REQUEST',
    urlContract: 'reload current Ambari document after starting repeat-tolerance config save',
    notes: '配置 PUT 不被 await；保存请求仍在途时立即整页 reload，可能中断或隐藏失败结果。',
  },
  'ambari-web/classic/app/controllers/main/admin/stack_and_upgrade_controller.js:2329': {
    semanticKind: 'LOCAL_DOCUMENT',
    networkEffect: 'LOCAL_ONLY',
    urlContract: 'about:blank; writes generated HTML configuration diff',
    notes: '新窗口只接收内存中生成的 HTML，不请求后端。',
  },
  'ambari-web/classic/app/controllers/main/views_controller.js:112': {
    semanticKind: 'INTERNAL_AMBARI_ROUTE',
    networkEffect: 'NAVIGATION_REQUEST',
    urlContract: '#/main/view/{viewName}/{shortUrl} or #/main/views/{viewName}/{version}/{instanceName}',
    notes: '新浏览上下文打开 Ember hash route，随后由 View details iframe 请求 context。',
  },
  'ambari-web/classic/app/controllers/main/service/item.js:2042': {
    semanticKind: 'PAGE_RELOAD',
    networkEffect: 'NAVIGATION_REQUEST',
    urlContract: 'reload current Ambari document after service deletion confirmation',
    notes: '用户关闭删除成功确认后整页 reload，以 REST 启动链重新构建 cluster/service 模型。',
  },
  'ambari-web/classic/app/mixins/main/host/details/support_client_configs_download.js:39': {
    semanticKind: 'AMBARI_REST_DOWNLOAD',
    networkEffect: 'REMOTE_REQUEST',
    urlContract: 'GET one of the five generated client-config contracts (`?format=client_config_tar`)',
    notes: '精确 resource-scope URL 见 client-config-downloads 清单。',
  },
  'ambari-web/classic/app/utils/configs/database.js:223': {
    semanticKind: 'URL_PARSER_NO_NETWORK',
    networkEffect: 'NO_NETWORK',
    urlContract: 'none; assigns caller URL to a detached anchor to read `.hostname`',
    notes: '只借浏览器 URL parser 解析 hostname。',
  },
  'ambari-web/classic/app/utils/file_utils.js:76': {
    semanticKind: 'LOCAL_DOCUMENT',
    networkEffect: 'LOCAL_ONLY',
    urlContract: 'about:blank; writes caller-provided text/HTML',
    notes: '本地内容预览。',
  },
  'ambari-web/classic/app/utils/file_utils.js:91': {
    semanticKind: 'LOCAL_DOWNLOAD',
    networkEffect: 'LOCAL_ONLY',
    urlContract: 'data:attachment/{fileType};charset=utf-8,{encodedData}',
    notes: 'Safari fallback 的本地 data URL 下载。',
  },
  'ambari-web/classic/app/utils/helper.js:1115': {
    semanticKind: 'ADMIN_VIEW_REDIRECT_HELPER',
    networkEffect: 'NAVIGATION_REQUEST',
    urlContract: 'window.location.replace({callerSuppliedLocation})',
    notes: '可测试的 helper 实现；真实业务调用点另列。',
  },
  'ambari-web/classic/app/views/common/host_progress_popup_body_view.js:1044': {
    semanticKind: 'LOCAL_DOCUMENT',
    networkEffect: 'LOCAL_ONLY',
    urlContract: 'about:blank; writes selected task/component log text',
    notes: '不发新后端请求。',
  },
  'ambari-web/classic/app/views/common/modal_popups/log_tail_popup.js:57': {
    semanticKind: 'LOCAL_DOCUMENT',
    networkEffect: 'LOCAL_ONLY',
    urlContract: 'about:blank; writes already-loaded log tail',
    notes: '不发新后端请求。',
  },
  'ambari-web/classic/app/views/common/modal_popups/logs_popup.js:43': {
    semanticKind: 'LOCAL_DOCUMENT',
    networkEffect: 'LOCAL_ONLY',
    urlContract: 'about:blank; writes task log HTML already in the DOM',
    notes: '不发新后端请求。',
  },
  'ambari-web/classic/app/views/main/admin/stack_upgrade/failed_hosts_modal_view.js:77': {
    semanticKind: 'LOCAL_DOCUMENT',
    networkEffect: 'LOCAL_ONLY',
    urlContract: 'about:blank; writes failed-host JSON',
    notes: '不发新后端请求。',
  },
  'ambari-web/classic/app/views/main/admin/stack_upgrade/upgrade_task_view.js:174': {
    semanticKind: 'LOCAL_DOCUMENT',
    networkEffect: 'LOCAL_ONLY',
    urlContract: 'about:blank; writes already-loaded upgrade task log',
    notes: '不发新后端请求。',
  },
  'ambari-web/classic/app/views/wizard/step3/hostWarningPopupBody_view.js:480': {
    semanticKind: 'LOCAL_DOCUMENT',
    networkEffect: 'LOCAL_ONLY',
    urlContract: 'about:blank; writes bootstrap warning details',
    notes: '不发新后端请求。',
  },
  'ambari-web/classic/app/views/wizard/step9/hostLogPopupBody_view.js:197': {
    semanticKind: 'LOCAL_DOCUMENT',
    networkEffect: 'LOCAL_ONLY',
    urlContract: 'about:blank; writes already-loaded install task logs',
    notes: '不发新后端请求。',
  },
  'ambari-web/classic/app/router.js:669': {
    semanticKind: 'ADMIN_VIEW_REDIRECT',
    networkEffect: 'NAVIGATION_REQUEST',
    urlContract: '{appURLRoot}views/ADMIN_VIEW/{lexicallyLatestServerComponentVersion}/INSTANCE/#/',
    notes: '无 cluster 的 Admin View 分流；location.replace 整页离开 Ember。',
  },
  'ambari-web/classic/app/router.js:349': {
    semanticKind: 'JWT_PROVIDER_REDIRECT',
    networkEffect: 'NAVIGATION_REQUEST',
    urlContract: '{401/403 response jwtProviderUrl}{encoded current window URL}{redirected query flag}',
    notes: '认证失败响应驱动的外部 IdP 整页跳转；redirect counter 仅限制循环，不约束 provider origin。',
  },
  'ambari-web/classic/app/router.js:773': {
    semanticKind: 'PAGE_RELOAD',
    networkEffect: 'NAVIGATION_REQUEST',
    urlContract: 'reload current Ambari document after logout when cluster model was loaded',
    notes: '先 transition 到 login，下一 run-loop 再整页 reload。',
  },
  'ambari-web/classic/app/router.js:806': {
    semanticKind: 'PREFERRED_PATH_REDIRECT',
    networkEffect: 'NAVIGATION_REQUEST',
    urlContract: '{saved preferredPath beginning with / or #, except a path containing #/login}',
    notes: '`startsWith("/")` 也接受 `//host/path`，因此旧实现可能发生 protocol-relative 跨源导航；React 应按已知安全缺陷处理。',
  },
  'ambari-web/classic/app/router.js:889': {
    semanticKind: 'WINDOW_LOCATION_SETTER',
    networkEffect: 'NAVIGATION_REQUEST',
    urlContract: 'window.location={callerSuppliedUrl}',
    notes: 'JWT redirect helper 的最终可测试 setter；实际 provider 数据入口另列。',
  },
  'ambari-web/classic/app/routes/activate_hawq_standby_routes.js:56': pageReloadContract(
    'reload current Ambari document after closing an unfinished Activate HAWQ Standby wizard',
    'cluster status persist 以 `alwaysCallback` 收尾，失败也 transition 后 reload。',
  ),
  'ambari-web/classic/app/routes/activate_hawq_standby_routes.js:178': pageReloadContract(
    'reload current Ambari document after completing Activate HAWQ Standby',
    '完成清理 persist 以 `alwaysCallback` 收尾。',
  ),
  'ambari-web/classic/app/routes/add_alert_definition_routes.js:141': pageReloadContract(
    'reload current Ambari document after Add Alert Definition completes',
    '清理向导状态后 transition 到 Alerts；status persist 失败也 reload。',
  ),
  'ambari-web/classic/app/routes/add_hawq_standby_routes.js:57': pageReloadContract(
    'reload current Ambari document after closing an unfinished Add HAWQ Standby wizard',
    'cluster status persist 以 `alwaysCallback` 收尾，失败也 reload。',
  ),
  'ambari-web/classic/app/routes/add_hawq_standby_routes.js:202': pageReloadContract(
    'reload current Ambari document after completing Add HAWQ Standby',
    '完成清理 persist 以 `alwaysCallback` 收尾。',
  ),
  'ambari-web/classic/app/routes/main.js:549': pageReloadContract(
    'reload current Ambari document after Disable Kerberos closes',
    'Disable 清理和 cluster status persist 以 `alwaysCallback` 收尾，随后返回 Kerberos 页并 reload。',
  ),
  'ambari-web/classic/app/routes/ra_high_availability_routes.js:179': pageReloadContract(
    'reload current Ambari document after Ranger Admin HA completes',
    '完成清理 persist 以 `alwaysCallback` 收尾。',
  ),
  'ambari-web/classic/app/routes/reassign_master_routes.js:314': pageReloadContract(
    'reload current Ambari document after Reassign Master completes',
    'cluster status persist 以 `alwaysCallback` 收尾，失败也 transition 后 reload。',
  ),
  'ambari-web/classic/app/routes/remove_hawq_standby_routes.js:63': pageReloadContract(
    'reload current Ambari document after confirmed close of an active Remove HAWQ Standby wizard',
    '确认关闭后清理状态；persist 成功或失败都 reload。',
  ),
  'ambari-web/classic/app/routes/remove_hawq_standby_routes.js:81': pageReloadContract(
    'reload current Ambari document after closing an inactive Remove HAWQ Standby wizard',
    '非活动阶段无需确认，但同样在 persist settle 后 reload。',
  ),
  'ambari-web/classic/app/routes/remove_hawq_standby_routes.js:189': pageReloadContract(
    'reload current Ambari document after completing Remove HAWQ Standby',
    '完成清理 persist 以 `alwaysCallback` 收尾。',
  ),
  'ambari-web/classic/app/routes/rollbackHA_routes.js:154': pageReloadContract(
    'reload current Ambari document after NameNode HA rollback completes',
    '本分支不等待 cluster status persist，transition 后下一 run-loop 直接 reload。',
  ),
  'ambari-web/classic/app/routes/stack_upgrade_routes.js:83': pageReloadContract(
    'conditionally reload current Ambari document when closing an upgrade wizard',
    '只在全局 upgrade state 为 NOT_REQUIRED 或 COMPLETED 时 reload 并 reset wizard owner。',
  ),
  'ambari-web/classic/app/controllers/wizard.js:1528': pageReloadContract(
    'reload current Ambari document after generic long-running wizard exit cleanup',
    'HA/Federation 等共享 `exitWizard()` 在 status persist settle 后 transition 并 reload。',
  ),
  'ambari-web/classic/app/mixins/common/reload_popup.js:63': pageReloadContract(
    'reload current Ambari document from the stale-page popup link',
    'reload 调用位于运行时编译的 inline onclick；用户点击时执行。',
  ),
  'ambari-web/classic/app/views/main/admin/stack_upgrade/versions_view.js:233': {
    semanticKind: 'ADMIN_VIEW_REDIRECT',
    networkEffect: 'NAVIGATION_REQUEST',
    urlContract: '{appURLRoot}views/ADMIN_VIEW/{lexicallyLatestServerComponentVersion}/INSTANCE/#/',
    notes: 'Manage Versions 确认后整页跳转 Admin View。',
  },
  'ambari-web/classic/app/views/main/views/details.js:85': {
    semanticKind: 'VIEW_WEB_CONTEXT',
    networkEffect: 'REMOTE_REQUEST',
    urlContract: '{window.location.protocol}//{window.location.host}{ViewInstanceInfo.context_path}/{viewPath}',
    notes: '同源 View application iframe；不是 `/api/v1` REST。',
  },
  'ambari-web/classic/app/templates/common/about.hbs:30': {
    semanticKind: 'STATIC_EXTERNAL_LINK',
    networkEffect: 'NAVIGATION_REQUEST',
    urlContract: 'http://ambari.apache.org/',
    notes: 'About 弹窗静态项目链接。',
  },
  'ambari-web/classic/app/templates/common/about.hbs:32': {
    semanticKind: 'STATIC_EXTERNAL_LINK',
    networkEffect: 'NAVIGATION_REQUEST',
    urlContract: 'http://www.apache.org/licenses/LICENSE-2.0',
    notes: 'About 弹窗静态 license 链接。',
  },
  'ambari-web/classic/app/assets/index.html:48': {
    semanticKind: 'STATIC_EXTERNAL_LINK',
    networkEffect: 'NAVIGATION_REQUEST',
    urlContract: 'http://www.apache.org/licenses/LICENSE-2.0',
    notes: '经典应用 footer 的 Apache License 外链。',
  },
  'ambari-web/classic/app/assets/index.html:49': {
    semanticKind: 'STATIC_NOTICE_DOCUMENT',
    networkEffect: 'REMOTE_REQUEST',
    urlContract: 'GET /licenses/NOTICE.txt',
    notes: '经典应用 footer 打开同源第三方 NOTICE 静态文档。',
  },
  'ambari-web/classic/app/templates/application.hbs:70': {
    semanticKind: 'NEW_UI_NAVIGATION',
    networkEffect: 'NAVIGATION_REQUEST',
    urlContract: 'GET /latest/#',
    notes: '用户菜单的 Switch Experience 整页导航到新版前端入口。',
  },
  'ambari-web/classic/app/templates/common/host_progress_popup.hbs:345': {
    semanticKind: 'EXTERNAL_LOG_SEARCH',
    networkEffect: 'NAVIGATION_REQUEST',
    urlContract: '{Log Search UI quick-link URL}{hostLog.linkTail}',
    notes: '依赖 Log Search service quick-link descriptor 和已加载日志上下文。',
  },
  'ambari-web/classic/app/templates/common/modal_popups/log_tail_popup.hbs:32': {
    semanticKind: 'EXTERNAL_LOG_SEARCH',
    networkEffect: 'NAVIGATION_REQUEST',
    urlContract: '{Log Search quick-link template}/#/logs/serviceLogs;{component/host/file query}',
    notes: '在 Log Search service 和 quick-link 可用时显示。',
  },
  'ambari-web/classic/app/templates/main/alerts/definition_details.hbs:196': {
    semanticKind: 'EXTERNAL_ALERT_HELP',
    networkEffect: 'NAVIGATION_REQUEST',
    urlContract: '{AlertDefinition.help_url}',
    notes: '只在定义有 help URL 时显示，URL 由后端数据提供。',
  },
  'ambari-web/classic/app/templates/main/host/logs.hbs:46': {
    semanticKind: 'EXTERNAL_LOG_SEARCH',
    networkEffect: 'NAVIGATION_REQUEST',
    urlContract: '{Log Search UI quick-link URL}{file.linkTail}',
    notes: '主机日志页按文件构造 Log Search 深链。',
  },
  'ambari-web/classic/app/templates/main/service/info/summary.hbs:103': {
    semanticKind: 'EXTERNAL_QUICK_LINK',
    networkEffect: 'NAVIGATION_REQUEST',
    urlContract: '{stack quicklinks descriptor + current configs + selected public host/protocol/port}',
    notes: '多 master/group 的服务 Quick Links。',
  },
  'ambari-web/classic/app/templates/main/service/info/summary.hbs:111': {
    semanticKind: 'EXTERNAL_QUICK_LINK',
    networkEffect: 'NAVIGATION_REQUEST',
    urlContract: '{stack quicklinks descriptor + current configs + selected public host/protocol/port}',
    notes: '单 host/平铺的服务 Quick Links。',
  },
  'ambari-web/classic/app/templates/main/service/services/hive.hbs:54': {
    semanticKind: 'INTERNAL_AMBARI_ROUTE',
    networkEffect: 'CONDITIONAL',
    urlContract: '{ViewInstance.internalAmbariUrl}',
    notes: '遗留 Hive-to-View hook；当前 `viewsToShow` 为空，默认无可见链接，有 runtime extension 时才路由。',
  },
  'ambari-web/classic/app/templates/main/views.hbs:28': {
    semanticKind: 'VIEW_ICON_RESOURCE',
    networkEffect: 'REMOTE_REQUEST',
    urlContract: 'GET {ViewInstanceInfo.icon_path}',
    notes: '由后端 View instance metadata 驱动的动态图片请求；不是固定构建图片。',
  },
};

function attachRequiredContract(record, contracts, catalogName) {
  const key = `${record.source}:${record.line}`;
  const contract = contracts[key];
  if (!contract) throw new Error(`${catalogName} record ${key} has no audited semantic contract`);
  return { ...record, ...contract };
}

function extractDirectCalls(jsFiles) {
  const calls = [];
  const patterns = [
    { kind: 'HttpClient', regex: /App\.HttpClient\.(get|post|request)\s*\(([^\n]*)/g },
    { kind: 'jQuery AJAX', regex: /(?:\$|jQuery)\.ajax\s*\(([^\n]*)/g },
    { kind: 'XMLHttpRequest', regex: /new\s+XMLHttpRequest\s*\(([^\n]*)/g },
  ];
  for (const file of jsFiles) {
    if (file === ajaxFile || file.endsWith('/utils/http_client.js')) continue;
    const source = maskJavaScriptComments(fs.readFileSync(file, 'utf8'));
    for (const { kind, regex } of patterns) {
      let match;
      while ((match = regex.exec(source))) {
        const excerpt = match[0].replace(/\s+/g, ' ').trim();
        const isMixedOperationalCall = file.endsWith('/controllers/global/update_controller.js')
          && excerpt.includes('App.serviceMetricsMapper');
        if (!isMixedOperationalCall && isMetricsRelated(file, excerpt)) continue;
        const record = {
          kind,
          method: kind === 'HttpClient' ? match[1].toUpperCase() : 'DYNAMIC',
          expression: excerpt,
          source: relativeToRepo(file),
          line: lineNumberAt(source, match.index),
          scope: isMixedOperationalCall ? 'MIXED' : 'NON_METRICS',
        };
        if (record.source.endsWith('/controllers/global/update_controller.js') && record.line === 323) {
          record.scope = 'MIXED';
        }
        if (record.source.endsWith('/custom_cluster_ckecks_host_hearbeat_view.js')) {
          record.scope = 'MIXED';
        }
        calls.push(attachRequiredContract(record, DIRECT_HTTP_CONTRACTS, 'direct HTTP'));
      }
    }
  }
  return calls;
}

function extractBrowserNetworkEntrypoints(jsFiles, markupFiles) {
  const entries = [];
  const patterns = [
    { kind: 'window.open', regex: /\bwindow\.open\s*\(([^\n]*)/g },
    { kind: 'location.replace', regex: /\bwindow\.location\.replace\s*\(([^\n]*)/g },
    { kind: 'replaceWindowLocation call', regex: /\bApp\.replaceWindowLocation\s*\(([^\n]*)/g },
    { kind: 'redirectByURL call', regex: /\bthis\.redirectByURL\s*\(([^\n]*)/g },
    { kind: 'window.location assignment', regex: /\bwindow\.location\s*=\s*([^;\n]+)/g },
    { kind: 'window.location.reload', regex: /\bwindow\.location\.reload\s*\(([^\n]*)/g },
    { kind: 'location.reload', regex: /(?<!window\.)\blocation\.reload\s*\(([^\n]*)/g },
    { kind: 'anchor href', regex: /\.href\s*=\s*([^;\n]+)/g },
    { kind: 'iframe src', regex: /\.attr\s*\(\s*['"]src['"]\s*,\s*([^\n]*)/g },
    { kind: 'iframe computed src', regex: /\bsrc\s*:\s*function\s*\([^)]*\)\s*\{/g },
  ];
  for (const file of jsFiles) {
    if (isMetricsUiFile(file) || isMetricsRelated(file)) continue;
    const source = maskJavaScriptComments(fs.readFileSync(file, 'utf8'));
    for (const { kind, regex } of patterns) {
      let match;
      while ((match = regex.exec(source))) {
        const record = {
          kind,
          expression: normalizeExpression(match[0]),
          source: relativeToRepo(file),
          line: lineNumberAt(source, match.index),
        };
        entries.push(attachRequiredContract(record, BROWSER_NETWORK_CONTRACTS, 'browser network'));
      }
    }
  }
  const markupPatterns = [
    { kind: 'target=_blank anchor', regex: /<a\b[^>]*\btarget\s*=\s*['"]_blank['"][^>]*>/g },
    { kind: 'new UI anchor', regex: /<a\b[^>]*\bhref\s*=\s*['"]\/latest\/#['"][^>]*>/g },
    { kind: 'dynamic image src', regex: /<img\b[^>]*\{\{bindAttr\s+src=[^}]+\}\}[^>]*>/g },
  ];
  for (const file of markupFiles) {
    if (isMetricsUiFile(file) || isMetricsRelated(file)) continue;
    const source = maskHandlebarsComments(fs.readFileSync(file, 'utf8'));
    for (const { kind, regex } of markupPatterns) {
      let match;
      while ((match = regex.exec(source))) {
        const record = {
          kind,
          expression: normalizeExpression(match[0]),
          source: relativeToRepo(file),
          line: lineNumberAt(source, match.index),
        };
        entries.push(attachRequiredContract(record, BROWSER_NETWORK_CONTRACTS, 'browser network'));
      }
    }
  }
  entries.sort((left, right) => left.source.localeCompare(right.source) || left.line - right.line || left.kind.localeCompare(right.kind));
  const consumedContracts = new Set(entries.map((entry) => `${entry.source}:${entry.line}`));
  const unusedContracts = Object.keys(BROWSER_NETWORK_CONTRACTS).filter((key) => !consumedContracts.has(key));
  if (unusedContracts.length) {
    throw new Error(`Unused browser network contracts: ${unusedContracts.join(', ')}`);
  }
  return entries;
}

function extractClientConfigDownloads() {
  const file = path.join(appRoot, 'mixins/main/host/details/support_client_configs_download.js');
  const source = maskJavaScriptComments(fs.readFileSync(file, 'utf8'));
  const suffix = '?format=client_config_tar';
  const contracts = [
    {
      resourceType: 'CLUSTER',
      endpoint: '/clusters/{clusterName}/components' + suffix,
      sourceMarker: 'case this.resourceTypeEnum.CLUSTER:',
    },
    {
      resourceType: 'HOST',
      endpoint: '/clusters/{clusterName}/hosts/{hostName}/host_components' + suffix,
      sourceMarker: 'case this.resourceTypeEnum.HOST:',
    },
    {
      resourceType: 'SERVICE',
      endpoint: '/clusters/{clusterName}/services/{serviceName}/components' + suffix,
      sourceMarker: 'case this.resourceTypeEnum.SERVICE:',
    },
    {
      resourceType: 'SERVICE_COMPONENT',
      endpoint: '/clusters/{clusterName}/services/{serviceName}/components/{componentName}' + suffix,
      sourceMarker: 'case this.resourceTypeEnum.SERVICE_COMPONENT:',
    },
    {
      resourceType: 'HOST_COMPONENT',
      endpoint: '/clusters/{clusterName}/hosts/{hostName}/host_components/{componentName}' + suffix,
      sourceMarker: 'case this.resourceTypeEnum.HOST_COMPONENT:',
    },
  ];
  if (!source.includes("result += '?format=client_config_tar'")) {
    throw new Error('Could not find client config download suffix');
  }
  return contracts.map(({ sourceMarker, ...contract }) => {
    const markerIndex = source.indexOf(sourceMarker);
    if (markerIndex === -1) {
      throw new Error(`Could not find client config download branch ${contract.resourceType}`);
    }
    return {
      ...contract,
      method: 'GET (browser download)',
      source: relativeToRepo(file),
      line: lineNumberAt(source, markerIndex),
    };
  });
}

function extractRoutes(jsFiles) {
  const routeFiles = jsFiles.filter((file) =>
    file === path.join(appRoot, 'router.js') || file.includes(`${path.sep}routes${path.sep}`),
  );
  const routes = [];
  for (const file of routeFiles) {
    if (isMetricsUiFile(file)) continue;
    const source = maskJavaScriptComments(fs.readFileSync(file, 'utf8'));
    const routePattern = /\broute\s*:\s*(['"])([^'"\n]+)\1/g;
    let match;
    while ((match = routePattern.exec(source))) {
      if (isMetricsRelated(file, match[2])) continue;
      routes.push({
        fragment: match[2],
        source: relativeToRepo(file),
        line: lineNumberAt(source, match.index),
      });
    }
  }
  return routes;
}

function extractTemplateActions(templateFiles) {
  const actions = new Map();
  for (const file of templateFiles) {
    if (isMetricsUiFile(file) || isMetricsRelated(file)) continue;
    const source = maskHandlebarsComments(fs.readFileSync(file, 'utf8'));
    const actionPattern = /{{action\s+['"]?([^\s'"}]+)/g;
    let match;
    while ((match = actionPattern.exec(source))) {
      if (isMetricsRelated(match[1])) continue;
      const key = match[1];
      if (!actions.has(key)) actions.set(key, []);
      actions.get(key).push(`${relativeToRepo(file)}:${lineNumberAt(source, match.index)}`);
    }
  }
  return [...actions.entries()]
    .map(([name, callers]) => ({ name, callers }))
    .sort((left, right) => left.name.localeCompare(right.name));
}

function extractFeatureIndex() {
  const moduleFiles = fs.readdirSync(baselineRoot, { withFileTypes: true })
    .filter((entry) => entry.isFile() && FEATURE_MODULE_FILE_PATTERN.test(entry.name))
    .map((entry) => path.join(baselineRoot, entry.name))
    .sort();
  const features = [];
  const idPattern = /^\|\s*([A-Z][A-Z0-9]*(?:-[A-Z0-9]+)+)\s*\|/;
  const extractFirstCell = (row, start) => {
    let inCode = false;
    let escaped = false;
    for (let index = start; index < row.length; index += 1) {
      const char = row[index];
      if (escaped) {
        escaped = false;
        continue;
      }
      if (char === '\\') {
        escaped = true;
        continue;
      }
      if (char === '`') {
        inCode = !inCode;
        continue;
      }
      if (char === '|' && !inCode) return row.slice(start, index).trim();
    }
    return row.slice(start).trim();
  };
  for (const file of moduleFiles) {
    const lines = fs.readFileSync(file, 'utf8').split(/\r?\n/);
    let section = '';
    for (let index = 0; index < lines.length; index += 1) {
      const heading = lines[index].match(/^#{2,3}\s+(.+?)\s*$/);
      if (heading) section = heading[1];
      const feature = lines[index].match(idPattern);
      if (!feature) continue;
      features.push({
        id: feature[1],
        summary: extractFirstCell(lines[index], feature[0].length),
        section,
        moduleFile: path.basename(file),
        line: index + 1,
      });
    }
  }
  return features;
}

function renderAjaxCatalog(definitions, excludedDefinitions) {
  const lines = [
    '# Ember 非 Metrics 命名 AJAX 接口目录',
    '',
    '> 由 `tools/extract-ember-baseline.mjs` 生成，请勿手工编辑。URL 默认追加 `/api/v1`；Method 中的 `DYNAMIC` 只表示 HTTP method 由运行时表达式决定，动态 URL 单独标为 `DYNAMIC_URL`。调用者计数为经典前端 `app/` 目录中的字符串引用数。',
    '',
    `- 纳入定义：${definitions.length}`,
    `- 排除 Metrics 定义：${excludedDefinitions.length}`,
    `- 有经典前端调用证据：${definitions.filter((item) => item.callers.length > 0).length}`,
    `- 未发现经典前端调用证据：${definitions.filter((item) => item.callers.length === 0).length}`,
    '',
    '| 请求名 | Method | URL（不含默认 prefix） | format 输入键 | Prefix | 模块 | 调用者 | 定义 |',
    '| --- | --- | --- | --- | --- | --- | ---: | --- |',
  ];
  for (const definition of definitions) {
    const inputKeys = definition.hasFormat
      ? (definition.inputKeys.length ? definition.inputKeys.map((key) => `\`${escapeMarkdown(key)}\``).join(', ') : '无静态 `data.*` 引用')
      : '无 `format()`';
    const endpoint = `${definition.endpoint}${definition.hasDynamicUrl ? ' [DYNAMIC_URL]' : ''}`;
    lines.push(`| \`${escapeMarkdown(definition.name)}\` | \`${definition.methods.join('/')}\` | \`${escapeMarkdown(endpoint)}\` | ${inputKeys} | \`${escapeMarkdown(definition.apiPrefix)}\` | ${classifyModule(definition)} | ${definition.callers.length} | [source](../../../../${definition.source}#L${definition.line}) |`);
    if (definition.callers.length) {
      lines.push(`|  |  | 调用位置：${definition.callers.map((caller) => `\`${escapeMarkdown(caller)}\``).join('<br>')} |  |  |  |  |  |`);
    }
  }
  lines.push('', '## 已排除的 Metrics 请求', '', excludedDefinitions.map((item) => `- \`${item.name}\` ([source](../../../../${item.source}#L${item.line}))`).join('\n'), '');
  return lines.join('\n');
}

function renderAjaxCalls(calls, includedRequestNames) {
  const dynamicCalls = calls.filter((call) => call.registrationStatus === 'DYNAMIC');
  const dynamicCandidateNames = new Set(dynamicCalls.flatMap((call) => call.candidateRequestNames));
  const dynamicMissingCandidates = new Set(dynamicCalls.flatMap((call) =>
    call.candidateRequestNames.filter((name) => !includedRequestNames.has(name))));
  const lines = [
    '# Ember 非 Metrics AJAX 调用点',
    '',
    '> 由 `tools/extract-ember-baseline.mjs` 生成。每行是一个 `App.ajax.send(...)` 调用点；动态请求按 `tools/contracts/dynamic-ajax-resolutions.mjs` 的人工审计契约解析。参数键只列调用对象中 `data: {...}` 的顶层键。',
    '',
    `共 ${calls.length} 个调用点，其中 ${calls.filter((call) => call.registrationStatus === 'REGISTERED').length} 个命中纳入范围的注册请求，${calls.filter((call) => call.registrationStatus === 'UNREGISTERED').length} 个静态请求名未在注册表定义，${calls.filter((call) => call.registrationStatus === 'DYNAMIC').length} 个使用动态表达式。`,
    '',
    '| 请求名/表达式 | 注册状态 | 调用参数 | 回调 | 位置 |',
    '| --- | --- | --- | --- | --- |',
  ];
  for (const call of calls) {
    const request = call.requestName
      ? `\`${escapeMarkdown(call.requestName)}\``
      : `DYNAMIC: \`${escapeMarkdown(call.requestExpression)}\``;
    const data = call.dataKeys.length
      ? call.dataKeys.map((key) => `\`${escapeMarkdown(key)}\``).join(', ')
      : call.dataExpression
        ? `表达式：\`${escapeMarkdown(call.dataExpression)}\``
        : '未发现内联 `data`';
    const callbacks = call.callbacks.length
      ? call.callbacks.map((key) => `\`${key}\``).join(', ')
      : '默认处理';
    lines.push(`| ${request} | \`${call.registrationStatus}\` | ${data} | ${callbacks} | [source](../../../../${call.source}#L${call.line}) |`);
  }
  lines.push(
    '',
    '## 动态调用解析',
    '',
    `动态解析：${dynamicCalls.length}/${dynamicCalls.length} 个调用点已绑定契约，${dynamicCandidateNames.size} 个唯一候选请求名，${dynamicMissingCandidates.size} 个候选未命中纳入注册表。`,
    '',
    '| 位置/表达式 | 解析状态 | 分派种类 | 候选请求名 | 分派条件 | 运行时边界 |',
    '| --- | --- | --- | --- | --- | --- |',
  );
  for (const call of dynamicCalls) {
    const candidates = call.candidateRequestNames
      .map((name) => `\`${escapeMarkdown(name)}\``)
      .join('<br>');
    lines.push(`| [source](../../../../${call.source}#L${call.line})<br>\`${escapeMarkdown(call.requestExpression)}\` | \`${call.resolutionStatus}\` | \`${call.dispatchKind}\` | ${candidates} | ${escapeMarkdown(call.dispatchCondition)} | ${escapeMarkdown(call.boundaryNotes)}<br>证据：${call.evidence.map((item) => `\`${escapeMarkdown(item)}\``).join('<br>')} |`);
  }
  lines.push(
    '',
    '`RESOLVED_CLOSED` 表示当前生产分支可穷举；`RESOLVED_OPEN_BOUNDARY` 表示当前候选已穷举，但通用 wrapper、model metadata、mixin property 或 FIFO queue 仍可被未来代码扩展。',
    '',
    '`UNREGISTERED` 是经典代码实际调用但 `app/utils/ajax/ajax.js` 没有同名定义的遗留行为。它不是有效后端契约；迁移时应先确认预期 endpoint，再决定修复或保留兼容行为。',
    '',
  );
  return lines.join('\n');
}

function renderDirectCalls(calls) {
  const lines = [
    '# Ember 非 Metrics 直接 HTTP 调用',
    '',
    '> 由 `tools/extract-ember-baseline.mjs` 生成。这些调用绕过 `App.ajax` 命名注册表；每条已人工还原业务语义、URL 契约和本基线保留的运维字段。生成器遇到新调用而没有语义契约时会直接失败。',
    '',
    `共 ${calls.length} 个静态调用点。`,
    '',
    '| 语义 | Method / 范围 | URL 契约 | 保留字段 | 行为/边界 | 位置 |',
    '| --- | --- | --- | --- | --- | --- |',
  ];
  for (const call of calls) {
    const fields = call.operationalFields.map((field) => `\`${escapeMarkdown(field)}\``).join('<br>');
    lines.push(`| \`${call.semanticKind}\`<br>${call.kind} | \`${call.method}\` / \`${call.scope}\` | \`${escapeMarkdown(call.urlContract)}\` | ${fields} | ${escapeMarkdown(call.notes)}<br>调用：\`${escapeMarkdown(call.expression)}\` | [source](../../../../${call.source}#L${call.line}) |`);
  }
  lines.push('', '`MIXED` 调用同时返回指标字段和非 Metrics 运维字段。本文只纳入表中列明的 topology/state/maintenance/stale config/HA/Active-Standby/安全判断字段；指标数值仍排除。`ORIGINAL_REQUEST_REPLAY` 是保存 KDC credential 后重放原请求，不能当成一个固定 endpoint。', '');
  return lines.join('\n');
}

function renderBrowserNetworkEntrypoints(entries) {
  const lines = [
    '# Ember 非 Metrics 浏览器网络与下载入口',
    '',
    '> 由 `tools/extract-ember-baseline.mjs` 生成。每个命令式入口、整页 reload/redirect、View iframe/icon 和非 Metrics 声明式导航链接均已人工分类。生成器遇到新入口而没有语义契约时会直接失败。普通启动脚本、样式表、favicon 和固定 UI 图片属于构建/静态资源，不在本目录范围。',
    '',
    `共 ${entries.length} 个候选调用点。`,
    '',
    '| 语义 | 网络效果 | URL / 内容契约 | 表达式 | 边界 | 位置 |',
    '| --- | --- | --- | --- | --- | --- |',
  ];
  for (const entry of entries) {
    lines.push(`| \`${entry.semanticKind}\`<br>${entry.kind} | \`${entry.networkEffect}\` | \`${escapeMarkdown(entry.urlContract)}\` | \`${escapeMarkdown(entry.expression)}\` | ${escapeMarkdown(entry.notes)} | [source](../../../../${entry.source}#L${entry.line}) |`);
  }
  const effects = [...new Set(entries.map((entry) => entry.networkEffect))].sort();
  lines.push('', `网络效果分类：${effects.map((effect) => `\`${effect}\``).join('、')}。\`LOCAL_ONLY\` 和 \`NO_NETWORK\` 不是后端接口，但仍是 React 需保留或明确变更的用户行为。`, '');
  return lines.join('\n');
}

function renderClientConfigDownloads(contracts) {
  const lines = [
    '# Ember Client Config 浏览器下载契约',
    '',
    '> 由 `tools/extract-ember-baseline.mjs` 生成。五种 resource scope 共用一个 mixin，并由 `window.open()` 直接请求 archive；不会进入 `App.ajax` 或直接 HTTP 调用计数。',
    '',
    `共 ${contracts.length} 种 resource scope。`,
    '',
    '| Resource type | Method | URL（含 `/api/v1` 后的路径） | 分支位置 |',
    '| --- | --- | --- | --- |',
  ];
  for (const contract of contracts) {
    lines.push(`| \`${contract.resourceType}\` | ${contract.method} | \`${escapeMarkdown(contract.endpoint)}\` | [source](../../../../${contract.source}#L${contract.line}) |`);
  }
  lines.push('');
  return lines.join('\n');
}

function renderRealtimeLocations(locations) {
  if (!locations.length) return '无';
  return locations
    .map(({ source, line }) => `[source](../../../../${source}#L${line})`)
    .join('<br>');
}

function renderRealtimeChannels({ transports, subscriptions, lifecycle }) {
  const countSites = (property) => subscriptions.reduce(
    (count, subscription) => count + subscription[property].length,
    0,
  );
  const staticDestinations = subscriptions.filter(
    (subscription) => !subscription.destinationTemplate.includes('{'),
  ).length;
  const dynamicDestinations = subscriptions.length - staticDestinations;
  const lines = [
    '# Ember 非 Metrics 实时通道契约',
    '',
    '> 由 `tools/extract-ember-baseline.mjs` 从已审计的静态 contract 生成。本文冻结 classic UI 的 STOMP/WebSocket/SockJS 行为，不包含 Metrics 时序数据通道。',
    '',
    '## 可校验摘要',
    '',
    `- transports：${transports.length}`,
    `- destinations：${subscriptions.length}（${staticDestinations} static + ${dynamicDestinations} dynamic）`,
    `- subscribe sites：${countSites('subscribeSites')}`,
    `- addHandler sites：${countSites('addHandlerSites')}`,
    `- removeHandler sites：${countSites('removeHandlerSites')}`,
    `- business unsubscribe sites：${countSites('unsubscribeSites')}`,
    `- lifecycle contracts：${lifecycle.length}`,
    '',
    '## Transport',
    '',
    '| ID / 类型 | URL | 协议与心跳 | Fallback / 重连 | 风险边界 | Source / Test |',
    '| --- | --- | --- | --- | --- | --- |',
  ];
  for (const transport of transports) {
    const protocolDetail = [
      `STOMP ${transport.protocols.stompVersions.join('/')}`,
      `heartbeat ${transport.heartbeat.clientOutgoingMs}/${transport.heartbeat.clientIncomingMs} ms`,
      transport.protocols.sockJsTransports?.length
        ? `SockJS ${transport.protocols.sockJsTransports.join(', ')}`
        : `schemes ${transport.protocols.socketSchemes.join('/')}`,
    ].join('<br>');
    const recovery = [
      `fallback: ${transport.fallback.trigger}`,
      `reconnect: ${transport.reconnect.policy}`,
    ].map(escapeMarkdown).join('<br>');
    const locations = [
      `实现：${renderRealtimeLocations(transport.sourceLocations)}`,
      `测试：${renderRealtimeLocations(transport.testLocations)}`,
    ].join('<br>');
    lines.push(`| \`${transport.id}\`<br>\`${transport.kind}\` | \`${escapeMarkdown(transport.urlTemplate)}\` | ${protocolDetail} | ${recovery} | ${transport.failureBoundaries.map(escapeMarkdown).join('<br>')} | ${locations} |`);
  }

  lines.push(
    '',
    '## Destination 契约',
    '',
    '| ID / Destination | Event | Ember 消费字段 | Handler chain | Lifecycle | REST reconcile | 风险边界 | Source |',
    '| --- | --- | --- | --- | --- | --- | --- | --- |',
  );
  for (const subscription of subscriptions) {
    const consumedFields = subscription.consumedFields
      .map((field) => `\`${escapeMarkdown(field)}\``)
      .join('<br>');
    const handlers = subscription.handlerChain
      .map((handler) => `\`${escapeMarkdown(handler)}\``)
      .join('<br>');
    const sources = [
      ['subscribe', subscription.subscribeSites],
      ['addHandler', subscription.addHandlerSites],
      ['removeHandler', subscription.removeHandlerSites],
      ['business unsubscribe', subscription.unsubscribeSites],
      ['event', subscription.eventSourceSites],
      ['test', subscription.testLocations],
    ]
      .filter(([, locations]) => locations.length)
      .map(([name, locations]) => `${name}：${renderRealtimeLocations(locations)}`)
      .join('<br>');
    lines.push(`| \`${subscription.id}\`<br>\`${escapeMarkdown(subscription.destinationTemplate)}\` | \`${escapeMarkdown(subscription.eventClass)}\` | ${consumedFields} | ${handlers} | ${escapeMarkdown(subscription.lifecycle)} | ${escapeMarkdown(subscription.restReconciliation)} | ${subscription.failureBoundaries.map(escapeMarkdown).join('<br>')} | ${sources} |`);
  }

  lines.push('', '## Payload Schema', '');
  for (const subscription of subscriptions) {
    lines.push(
      '<details>',
      `<summary><code>${subscription.id}</code> <code>${escapeMarkdown(subscription.destinationTemplate)}</code></summary>`,
      '',
      '```json',
      JSON.stringify(subscription.payloadSchema, null, 2),
      '```',
      '',
      '</details>',
      '',
    );
  }

  lines.push(
    '## Lifecycle',
    '',
    '| ID / 名称 | 行为 | 风险边界 | Source / Test |',
    '| --- | --- | --- | --- |',
  );
  for (const entry of lifecycle) {
    const locations = [
      `实现：${renderRealtimeLocations(entry.sourceLocations)}`,
      `测试：${renderRealtimeLocations(entry.testLocations)}`,
    ].join('<br>');
    lines.push(`| \`${entry.id}\`<br>\`${entry.name}\` | ${escapeMarkdown(entry.behavior)} | ${entry.failureBoundaries.map(escapeMarkdown).join('<br>')} | ${locations} |`);
  }
  lines.push('');
  return lines.join('\n');
}

function renderNamedUsageCatalog(title, description, items) {
  const lines = [
    `# ${title}`,
    '',
    `> ${description}`,
    '',
    `共 ${items.length} 个不同名称。`,
    '',
    '| 名称 | 使用形式 | 调用点数 | 位置 |',
    '| --- | --- | ---: | --- |',
  ];
  for (const item of items) {
    const kinds = item.kinds?.length ? item.kinds.map((kind) => `\`${kind}\``).join(', ') : '静态引用';
    lines.push(`| \`${escapeMarkdown(item.name)}\` | ${kinds} | ${item.callers.length} | ${item.callers.map((caller) => `\`${escapeMarkdown(caller)}\``).join('<br>')} |`);
  }
  lines.push('');
  return lines.join('\n');
}

function renderFeatureIndex(features) {
  const lines = [
    '# Ember 稳定功能索引',
    '',
    '> 由 `tools/extract-ember-baseline.mjs` 从 `01` 至 `13` 模块文档生成，请勿手工编辑。`00` 方法论、`14` React 矩阵和 `15` 审计报告不会被识别为功能来源。',
    '',
    `共 ${features.length} 个稳定功能 ID。`,
    '',
    '| 功能 ID | 模块 | 小节 | 摘要 | 定义 |',
    '| --- | --- | --- | --- | --- |',
  ];
  for (const feature of features) {
    lines.push(`| \`${feature.id}\` | \`${feature.moduleFile}\` | ${escapeMarkdown(feature.section)} | ${escapeMarkdown(feature.summary)} | [source](../${feature.moduleFile}#L${feature.line}) |`);
  }
  lines.push('');
  return lines.join('\n');
}

function renderRoutes(routes) {
  const lines = [
    '# Ember 非 Metrics 路由片段',
    '',
    '> 由 `tools/extract-ember-baseline.mjs` 生成。旧版使用嵌套 `Em.Route`，表中是源码中的 route fragment，不是已拼接的最终 URL。',
    '',
    `共 ${routes.length} 个非 Metrics route fragment。`,
    '',
    '| Route fragment | 位置 |',
    '| --- | --- |',
  ];
  for (const route of routes) {
    lines.push(`| \`${escapeMarkdown(route.fragment)}\` | [source](../../../../${route.source}#L${route.line}) |`);
  }
  lines.push('');
  return lines.join('\n');
}

function renderActions(actions) {
  const lines = [
    '# Ember 非 Metrics 模板动作',
    '',
    '> 由 `tools/extract-ember-baseline.mjs` 生成。该表用于发现用户可触发行为；动态 action 和仅由 JavaScript 触发的行为仍需查阅模块文档与源码。',
    '',
    `共 ${actions.length} 个不同 action 名。`,
    '',
    '| Action | 出现次数 | 模板位置 |',
    '| --- | ---: | --- |',
  ];
  for (const action of actions) {
    lines.push(`| \`${escapeMarkdown(action.name)}\` | ${action.callers.length} | ${action.callers.map((caller) => `\`${escapeMarkdown(caller)}\``).join('<br>')} |`);
  }
  lines.push('');
  return lines.join('\n');
}

function renderModuleCatalog(moduleName, definitions) {
  const contentContract = definitions.map((definition) => ({
    name: definition.name,
    methods: definition.methods,
    endpoint: definition.endpoint,
    inputKeys: definition.inputKeys,
    callers: definition.callers,
  }));
  const lines = [
    `# ${moduleName}：Ember 非 Metrics 命名 AJAX 候选索引`,
    '',
    '> 由 `tools/extract-ember-baseline.mjs` 生成。该页只按请求名和调用者路径的宽正则启发式归类：共享请求可能跨模块混入或重复，模块请求也可能漏列或归到其他页。它不是模块接口全集；权威核对必须联合 `../ajax-endpoints.json`、`../ajax-calls.json`、`../direct-http-calls.json`、`../browser-network-entrypoints.json` 和 `../realtime-channels.json`。',
    '',
    `共 ${definitions.length} 个命名请求候选。`,
    `候选内容 SHA-256：\`${sha256Json(contentContract)}\`。`,
    '',
    '| 请求名 | Method | URL（不含默认 prefix） | format 输入键 | 调用位置 |',
    '| --- | --- | --- | --- | --- |',
  ];
  for (const definition of definitions) {
    const callers = definition.callers.length
      ? definition.callers.map((caller) => `\`${escapeMarkdown(caller)}\``).join('<br>')
      : '未发现经典前端字符串调用证据';
    const inputKeys = definition.hasFormat
      ? (definition.inputKeys.length ? definition.inputKeys.map((key) => `\`${escapeMarkdown(key)}\``).join(', ') : '无静态 `data.*` 引用')
      : '无 `format()`';
    const endpoint = `${definition.endpoint}${definition.hasDynamicUrl ? ' [DYNAMIC_URL]' : ''}`;
    lines.push(`| \`${escapeMarkdown(definition.name)}\` | \`${definition.methods.join('/')}\` | \`${escapeMarkdown(endpoint)}\` | ${inputKeys} | ${callers} |`);
  }
  lines.push('');
  return lines.join('\n');
}

function writeGenerated(fileName, content) {
  fs.mkdirSync(outputRoot, { recursive: true });
  fs.writeFileSync(path.join(outputRoot, fileName), content);
}

function writeJson(fileName, value) {
  fs.mkdirSync(outputRoot, { recursive: true });
  fs.writeFileSync(path.join(outputRoot, fileName), `${JSON.stringify(value, null, 2)}\n`);
}

const jsFiles = listFiles(appRoot, (file) => file.endsWith('.js'));
const templateFiles = listFiles(path.join(appRoot, 'templates'), (file) => file.endsWith('.hbs'));
const browserMarkupFiles = [...templateFiles, path.join(appRoot, 'assets/index.html')];
const allDefinitions = extractAjaxDefinitions();
findDefinitionCallers(allDefinitions, jsFiles);
const excludedDefinitions = allDefinitions.filter(isMetricsDefinition);
const definitions = allDefinitions.filter((definition) => !excludedDefinitions.includes(definition));
const includedRequestNames = new Set(definitions.map((definition) => definition.name));
const ajaxCalls = attachDynamicAjaxResolutions(extractAjaxCalls(
  jsFiles,
  includedRequestNames,
  new Set(allDefinitions.map((definition) => definition.name)),
));
const directCalls = extractDirectCalls(jsFiles);
const browserNetworkEntrypoints = extractBrowserNetworkEntrypoints(jsFiles, browserMarkupFiles);
const clientConfigDownloads = extractClientConfigDownloads();
const permissionUses = extractPermissionUses(jsFiles, templateFiles);
const featureFlagUses = extractFeatureFlagUses(jsFiles, templateFiles);
const routes = extractRoutes(jsFiles);
const actions = extractTemplateActions(templateFiles);
const features = extractFeatureIndex();

writeGenerated('ajax-endpoints.md', renderAjaxCatalog(definitions, excludedDefinitions));
writeGenerated('ajax-calls.md', renderAjaxCalls(ajaxCalls, includedRequestNames));
writeGenerated('direct-http-calls.md', renderDirectCalls(directCalls));
writeGenerated('browser-network-entrypoints.md', renderBrowserNetworkEntrypoints(browserNetworkEntrypoints));
writeGenerated('client-config-downloads.md', renderClientConfigDownloads(clientConfigDownloads));
writeGenerated('permissions.md', renderNamedUsageCatalog(
  'Ember 非 Metrics Permission 使用点',
  '由 `tools/extract-ember-baseline.mjs` 生成。识别 JavaScript 与 Handlebars 中静态字符串形式的 `isAuthorized`/`havePermissions`；helper 动态参数和服务端 privilege 仍需人工审计。',
  permissionUses,
));
writeGenerated('feature-flags.md', renderNamedUsageCatalog(
  'Ember 非 Metrics Feature Flag 使用点',
  '由 `tools/extract-ember-baseline.mjs` 生成。识别 JavaScript 和 Handlebars 中的 `App.supports.flag` 及 `App.get(\'supports.flag\')`；默认值和服务端覆盖语义见手写权限文档。',
  featureFlagUses,
));
writeGenerated('routes.md', renderRoutes(routes));
writeGenerated('template-actions.md', renderActions(actions));
writeGenerated('feature-index.md', renderFeatureIndex(features));
writeGenerated('realtime-channels.md', renderRealtimeChannels(realtimeChannels));
writeJson('ajax-endpoints.json', definitions.map(({ objectSource, ...definition }) => ({
  ...definition,
  modules: classifyModules(definition),
})));
writeJson('excluded-metrics-ajax-endpoints.json', excludedDefinitions.map(({ objectSource, ...definition }) => definition));
writeJson('ajax-calls.json', ajaxCalls);
writeJson('direct-http-calls.json', directCalls);
writeJson('browser-network-entrypoints.json', browserNetworkEntrypoints);
writeJson('client-config-downloads.json', clientConfigDownloads);
writeJson('permissions.json', permissionUses);
writeJson('feature-flags.json', featureFlagUses);
writeJson('routes.json', routes);
writeJson('template-actions.json', actions);
writeJson('feature-index.json', features);
writeJson('realtime-channels.json', realtimeChannels);

const definitionsByModule = new Map();
for (const definition of definitions) {
  for (const moduleName of classifyModules(definition)) {
    if (!definitionsByModule.has(moduleName)) definitionsByModule.set(moduleName, []);
    definitionsByModule.get(moduleName).push(definition);
  }
}
const moduleFileNames = new Map([
  ['认证与应用外壳', 'auth-shell.md'],
  ['安装向导', 'installation-wizards.md'],
  ['主机', 'hosts.md'],
  ['服务与配置', 'services-configs.md'],
  ['告警', 'alerts.md'],
  ['Stack 与升级', 'stack-upgrades.md'],
  ['安全、高可用与联邦', 'security-ha-federation.md'],
  ['Views', 'views.md'],
  ['后台操作与通用能力', 'background-common.md'],
  ['跨模块与待人工归类', 'cross-cutting.md'],
]);
const moduleDir = path.join(outputRoot, 'api-by-module');
fs.mkdirSync(moduleDir, { recursive: true });
for (const [moduleName, moduleDefinitions] of definitionsByModule) {
  fs.writeFileSync(
    path.join(moduleDir, moduleFileNames.get(moduleName)),
    renderModuleCatalog(moduleName, moduleDefinitions),
  );
}

console.log(JSON.stringify({
  ajaxDefinitions: definitions.length,
  excludedMetricsDefinitions: excludedDefinitions.length,
  ajaxCalls: ajaxCalls.length,
  dynamicAjaxCalls: ajaxCalls.filter((call) => !call.requestName).length,
  unregisteredAjaxCalls: ajaxCalls.filter((call) => call.registrationStatus === 'UNREGISTERED').length,
  directHttpCalls: directCalls.length,
  browserNetworkEntrypoints: browserNetworkEntrypoints.length,
  clientConfigDownloadScopes: clientConfigDownloads.length,
  permissions: permissionUses.length,
  featureFlags: featureFlagUses.length,
  routeFragments: routes.length,
  templateActions: actions.length,
  featureIds: features.length,
  realtimeTransports: realtimeChannels.transports.length,
  realtimeDestinations: realtimeChannels.subscriptions.length,
  realtimeLifecycleContracts: realtimeChannels.lifecycle.length,
}, null, 2));
