"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
require("rxjs/add/operator/filter");
const terminal_1 = require("../terminal");
const indent_1 = require("./indent");
/**
 * A Logger that sends information to STDOUT and STDERR.
 */
function createLogger(verbose = false) {
    const logger = new indent_1.IndentLogger('cling');
    logger
        .filter((entry) => (entry.level != 'debug' || verbose))
        .subscribe((entry) => {
        let color = x => terminal_1.dim(terminal_1.white(x));
        let output = process.stdout;
        switch (entry.level) {
            case 'info':
                color = terminal_1.white;
                break;
            case 'warn':
                color = terminal_1.yellow;
                break;
            case 'error':
                color = terminal_1.red;
                output = process.stderr;
                break;
            case 'fatal':
                color = (x) => terminal_1.bold(terminal_1.red(x));
                output = process.stderr;
                break;
        }
        output.write(color(entry.message) + '\n');
    });
    return logger;
}
exports.createLogger = createLogger;
//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJmaWxlIjoiY2xpLWxvZ2dlci5qcyIsInNvdXJjZVJvb3QiOiIvVXNlcnMvaGFuc2wvU291cmNlcy9kZXZraXQvIiwic291cmNlcyI6WyJwYWNrYWdlcy9hbmd1bGFyX2RldmtpdC9jb3JlL3NyYy9sb2dnZXIvY2xpLWxvZ2dlci50cyJdLCJuYW1lcyI6W10sIm1hcHBpbmdzIjoiOztBQUFBOzs7Ozs7R0FNRztBQUNILG9DQUFrQztBQUNsQywwQ0FBNEQ7QUFDNUQscUNBQXdDO0FBSXhDOztHQUVHO0FBQ0gsc0JBQTZCLE9BQU8sR0FBRyxLQUFLO0lBQzFDLE1BQU0sTUFBTSxHQUFHLElBQUkscUJBQVksQ0FBQyxPQUFPLENBQUMsQ0FBQztJQUV6QyxNQUFNO1NBQ0gsTUFBTSxDQUFDLENBQUMsS0FBZSxLQUFLLENBQUMsS0FBSyxDQUFDLEtBQUssSUFBSSxPQUFPLElBQUksT0FBTyxDQUFDLENBQUM7U0FDaEUsU0FBUyxDQUFDLENBQUMsS0FBZTtRQUN6QixJQUFJLEtBQUssR0FBMEIsQ0FBQyxJQUFJLGNBQUcsQ0FBQyxnQkFBSyxDQUFDLENBQUMsQ0FBQyxDQUFDLENBQUM7UUFDdEQsSUFBSSxNQUFNLEdBQUcsT0FBTyxDQUFDLE1BQU0sQ0FBQztRQUM1QixNQUFNLENBQUMsQ0FBQyxLQUFLLENBQUMsS0FBSyxDQUFDLENBQUMsQ0FBQztZQUNwQixLQUFLLE1BQU07Z0JBQ1QsS0FBSyxHQUFHLGdCQUFLLENBQUM7Z0JBQ2QsS0FBSyxDQUFDO1lBQ1IsS0FBSyxNQUFNO2dCQUNULEtBQUssR0FBRyxpQkFBTSxDQUFDO2dCQUNmLEtBQUssQ0FBQztZQUNSLEtBQUssT0FBTztnQkFDVixLQUFLLEdBQUcsY0FBRyxDQUFDO2dCQUNaLE1BQU0sR0FBRyxPQUFPLENBQUMsTUFBTSxDQUFDO2dCQUN4QixLQUFLLENBQUM7WUFDUixLQUFLLE9BQU87Z0JBQ1YsS0FBSyxHQUFHLENBQUMsQ0FBUyxLQUFLLGVBQUksQ0FBQyxjQUFHLENBQUMsQ0FBQyxDQUFDLENBQUMsQ0FBQztnQkFDcEMsTUFBTSxHQUFHLE9BQU8sQ0FBQyxNQUFNLENBQUM7Z0JBQ3hCLEtBQUssQ0FBQztRQUNWLENBQUM7UUFFRCxNQUFNLENBQUMsS0FBSyxDQUFDLEtBQUssQ0FBQyxLQUFLLENBQUMsT0FBTyxDQUFDLEdBQUcsSUFBSSxDQUFDLENBQUM7SUFDNUMsQ0FBQyxDQUFDLENBQUM7SUFFTCxNQUFNLENBQUMsTUFBTSxDQUFDO0FBQ2hCLENBQUM7QUE3QkQsb0NBNkJDIiwic291cmNlc0NvbnRlbnQiOlsiLyoqXG4gKiBAbGljZW5zZVxuICogQ29weXJpZ2h0IEdvb2dsZSBJbmMuIEFsbCBSaWdodHMgUmVzZXJ2ZWQuXG4gKlxuICogVXNlIG9mIHRoaXMgc291cmNlIGNvZGUgaXMgZ292ZXJuZWQgYnkgYW4gTUlULXN0eWxlIGxpY2Vuc2UgdGhhdCBjYW4gYmVcbiAqIGZvdW5kIGluIHRoZSBMSUNFTlNFIGZpbGUgYXQgaHR0cHM6Ly9hbmd1bGFyLmlvL2xpY2Vuc2VcbiAqL1xuaW1wb3J0ICdyeGpzL2FkZC9vcGVyYXRvci9maWx0ZXInO1xuaW1wb3J0IHsgYm9sZCwgZGltLCByZWQsIHdoaXRlLCB5ZWxsb3cgfSBmcm9tICcuLi90ZXJtaW5hbCc7XG5pbXBvcnQgeyBJbmRlbnRMb2dnZXIgfSBmcm9tICcuL2luZGVudCc7XG5pbXBvcnQgeyBMb2dFbnRyeSwgTG9nZ2VyIH0gZnJvbSAnLi9sb2dnZXInO1xuXG5cbi8qKlxuICogQSBMb2dnZXIgdGhhdCBzZW5kcyBpbmZvcm1hdGlvbiB0byBTVERPVVQgYW5kIFNUREVSUi5cbiAqL1xuZXhwb3J0IGZ1bmN0aW9uIGNyZWF0ZUxvZ2dlcih2ZXJib3NlID0gZmFsc2UpOiBMb2dnZXIge1xuICBjb25zdCBsb2dnZXIgPSBuZXcgSW5kZW50TG9nZ2VyKCdjbGluZycpO1xuXG4gIGxvZ2dlclxuICAgIC5maWx0ZXIoKGVudHJ5OiBMb2dFbnRyeSkgPT4gKGVudHJ5LmxldmVsICE9ICdkZWJ1ZycgfHwgdmVyYm9zZSkpXG4gICAgLnN1YnNjcmliZSgoZW50cnk6IExvZ0VudHJ5KSA9PiB7XG4gICAgICBsZXQgY29sb3I6IChzOiBzdHJpbmcpID0+IHN0cmluZyA9IHggPT4gZGltKHdoaXRlKHgpKTtcbiAgICAgIGxldCBvdXRwdXQgPSBwcm9jZXNzLnN0ZG91dDtcbiAgICAgIHN3aXRjaCAoZW50cnkubGV2ZWwpIHtcbiAgICAgICAgY2FzZSAnaW5mbyc6XG4gICAgICAgICAgY29sb3IgPSB3aGl0ZTtcbiAgICAgICAgICBicmVhaztcbiAgICAgICAgY2FzZSAnd2Fybic6XG4gICAgICAgICAgY29sb3IgPSB5ZWxsb3c7XG4gICAgICAgICAgYnJlYWs7XG4gICAgICAgIGNhc2UgJ2Vycm9yJzpcbiAgICAgICAgICBjb2xvciA9IHJlZDtcbiAgICAgICAgICBvdXRwdXQgPSBwcm9jZXNzLnN0ZGVycjtcbiAgICAgICAgICBicmVhaztcbiAgICAgICAgY2FzZSAnZmF0YWwnOlxuICAgICAgICAgIGNvbG9yID0gKHg6IHN0cmluZykgPT4gYm9sZChyZWQoeCkpO1xuICAgICAgICAgIG91dHB1dCA9IHByb2Nlc3Muc3RkZXJyO1xuICAgICAgICAgIGJyZWFrO1xuICAgICAgfVxuXG4gICAgICBvdXRwdXQud3JpdGUoY29sb3IoZW50cnkubWVzc2FnZSkgKyAnXFxuJyk7XG4gICAgfSk7XG5cbiAgcmV0dXJuIGxvZ2dlcjtcbn1cbiJdfQ==