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

export const generateQueryParam = (param: any) => {
    const expressions = param.key;
    const pHash = createComboParamHash(param);
    return createComboParamURL(pHash, expressions);
}

const createComboParamHash = (param: any) => {
    const pHash: any = {};
    if (Array.isArray(param.value)) {
      param.value.forEach((item: any) => {
        const [k, v] = item.split(':');
        if (!pHash[k]) {
          pHash[k] = v;
        } else {
          if (Array.isArray(pHash[k])) {
            if (!pHash[k].includes(v)) {
              pHash[k].push(v);
            }
          } else {
            pHash[k] = [pHash[k], v];
          }
        }
      });
    } else {
      const [k, v] = param.value.split(':');
      pHash[k] = v;
    }
    return pHash;
  }

  const createComboParamURL = (pHash: any, expressions: any) => {
    let result = '';
    Object.keys(pHash).forEach(key => {
      const v = pHash[key];
      if (Array.isArray(v)) {
        let ex = '(';
        v.forEach(item => {
          let expression = getComboParamURL(item, expressions);
          let toAdd = expression.replace('{0}', key).replace('{1}', item);
          ex += toAdd + '|';
        });
        ex = ex.slice(0, -1);
        result += ex + ')';
      } else {
        let expression = getComboParamURL(v, expressions);
        let ex = expression.replace('{0}', key).replace('{1}', v);
        result += ex;
      }
      result += '|';
    });
  
    return result.slice(0, -1);
  }

  const getComboParamURL = (value: any, expressions: any) => {
    let expression = expressions[1];
    switch (value) {
      case 'ALL':
        expression = expressions[0];
        break;
      case 'STARTED':
      case 'STARTING':
      case 'INSTALLED':
      case 'STOPPING':
      case 'INSTALL_FAILED':
      case 'INSTALLING':
      case 'UPGRADE_FAILED':
      case 'UNKNOWN':
      case 'DISABLED':
      case 'INIT':
        break;
      case 'INSERVICE':
      case 'DECOMMISSIONING':
      case 'DECOMMISSIONED':
      case 'RS_DECOMMISSIONED':
        expression = expressions[2];
        break;
      case 'ON':
      case 'OFF':
        expression = expressions[3];
        break;
    }
    return expression;
  }