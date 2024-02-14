/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */

import * as path from 'path';

// Postprocess generated JS.
export function pathToModuleName(context: string, fileName: string): string {
  fileName = fileName.replace(/\.js$/, '');

  if (fileName[0] === '.') {
    // './foo' or '../foo'.
    // Resolve the path against the dirname of the current module.
    fileName = path.join(path.dirname(context), fileName);
  }
  // Replace characters not supported by goog.module.
  let moduleName =
      fileName.replace(/\//g, '.').replace(/^[^a-zA-Z_$]/, '_').replace(/[^a-zA-Z0-9._$]/g, '_');

  return moduleName;
}
