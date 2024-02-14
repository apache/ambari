/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
import 'rxjs/add/operator/filter';
import { Logger } from './logger';
/**
 * A Logger that sends information to STDOUT and STDERR.
 */
export declare function createLogger(verbose?: boolean): Logger;
