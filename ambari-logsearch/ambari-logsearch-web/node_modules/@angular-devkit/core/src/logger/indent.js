"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
require("rxjs/add/operator/map");
const logger_1 = require("./logger");
/**
 * Keep an map of indentation => array of indentations based on the level.
 * This is to optimize calculating the prefix based on the indentation itself. Since most logs
 * come from similar levels, and with similar indentation strings, this will be shared by all
 * loggers. Also, string concatenation is expensive so performing concats for every log entries
 * is expensive; this alleviates it.
 */
const indentationMap = {};
class IndentLogger extends logger_1.Logger {
    constructor(name, parent = null, indentation = '  ') {
        super(name, parent);
        indentationMap[indentation] = indentationMap[indentation] || [''];
        const map = indentationMap[indentation];
        this._observable = this._observable.map(entry => {
            const l = entry.path.length;
            if (l >= map.length) {
                let current = map[map.length - 1];
                while (l >= map.length) {
                    current += indentation;
                    map.push(current);
                }
            }
            entry.message = map[l] + entry.message;
            return entry;
        });
    }
}
exports.IndentLogger = IndentLogger;
//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJmaWxlIjoiaW5kZW50LmpzIiwic291cmNlUm9vdCI6Ii9Vc2Vycy9oYW5zbC9Tb3VyY2VzL2RldmtpdC8iLCJzb3VyY2VzIjpbInBhY2thZ2VzL2FuZ3VsYXJfZGV2a2l0L2NvcmUvc3JjL2xvZ2dlci9pbmRlbnQudHMiXSwibmFtZXMiOltdLCJtYXBwaW5ncyI6Ijs7QUFBQTs7Ozs7O0dBTUc7QUFDSCxpQ0FBK0I7QUFDL0IscUNBQWtDO0FBR2xDOzs7Ozs7R0FNRztBQUNILE1BQU0sY0FBYyxHQUEwQyxFQUFFLENBQUM7QUFHakUsa0JBQTBCLFNBQVEsZUFBTTtJQUN0QyxZQUFZLElBQVksRUFBRSxTQUF3QixJQUFJLEVBQUUsV0FBVyxHQUFHLElBQUk7UUFDeEUsS0FBSyxDQUFDLElBQUksRUFBRSxNQUFNLENBQUMsQ0FBQztRQUVwQixjQUFjLENBQUMsV0FBVyxDQUFDLEdBQUcsY0FBYyxDQUFDLFdBQVcsQ0FBQyxJQUFJLENBQUMsRUFBRSxDQUFDLENBQUM7UUFDbEUsTUFBTSxHQUFHLEdBQUcsY0FBYyxDQUFDLFdBQVcsQ0FBQyxDQUFDO1FBRXhDLElBQUksQ0FBQyxXQUFXLEdBQUcsSUFBSSxDQUFDLFdBQVcsQ0FBQyxHQUFHLENBQUMsS0FBSztZQUMzQyxNQUFNLENBQUMsR0FBRyxLQUFLLENBQUMsSUFBSSxDQUFDLE1BQU0sQ0FBQztZQUM1QixFQUFFLENBQUMsQ0FBQyxDQUFDLElBQUksR0FBRyxDQUFDLE1BQU0sQ0FBQyxDQUFDLENBQUM7Z0JBQ3BCLElBQUksT0FBTyxHQUFHLEdBQUcsQ0FBQyxHQUFHLENBQUMsTUFBTSxHQUFHLENBQUMsQ0FBQyxDQUFDO2dCQUNsQyxPQUFPLENBQUMsSUFBSSxHQUFHLENBQUMsTUFBTSxFQUFFLENBQUM7b0JBQ3ZCLE9BQU8sSUFBSSxXQUFXLENBQUM7b0JBQ3ZCLEdBQUcsQ0FBQyxJQUFJLENBQUMsT0FBTyxDQUFDLENBQUM7Z0JBQ3BCLENBQUM7WUFDSCxDQUFDO1lBRUQsS0FBSyxDQUFDLE9BQU8sR0FBRyxHQUFHLENBQUMsQ0FBQyxDQUFDLEdBQUcsS0FBSyxDQUFDLE9BQU8sQ0FBQztZQUV2QyxNQUFNLENBQUMsS0FBSyxDQUFDO1FBQ2YsQ0FBQyxDQUFDLENBQUM7SUFDTCxDQUFDO0NBQ0Y7QUF0QkQsb0NBc0JDIiwic291cmNlc0NvbnRlbnQiOlsiLyoqXG4gKiBAbGljZW5zZVxuICogQ29weXJpZ2h0IEdvb2dsZSBJbmMuIEFsbCBSaWdodHMgUmVzZXJ2ZWQuXG4gKlxuICogVXNlIG9mIHRoaXMgc291cmNlIGNvZGUgaXMgZ292ZXJuZWQgYnkgYW4gTUlULXN0eWxlIGxpY2Vuc2UgdGhhdCBjYW4gYmVcbiAqIGZvdW5kIGluIHRoZSBMSUNFTlNFIGZpbGUgYXQgaHR0cHM6Ly9hbmd1bGFyLmlvL2xpY2Vuc2VcbiAqL1xuaW1wb3J0ICdyeGpzL2FkZC9vcGVyYXRvci9tYXAnO1xuaW1wb3J0IHsgTG9nZ2VyIH0gZnJvbSAnLi9sb2dnZXInO1xuXG5cbi8qKlxuICogS2VlcCBhbiBtYXAgb2YgaW5kZW50YXRpb24gPT4gYXJyYXkgb2YgaW5kZW50YXRpb25zIGJhc2VkIG9uIHRoZSBsZXZlbC5cbiAqIFRoaXMgaXMgdG8gb3B0aW1pemUgY2FsY3VsYXRpbmcgdGhlIHByZWZpeCBiYXNlZCBvbiB0aGUgaW5kZW50YXRpb24gaXRzZWxmLiBTaW5jZSBtb3N0IGxvZ3NcbiAqIGNvbWUgZnJvbSBzaW1pbGFyIGxldmVscywgYW5kIHdpdGggc2ltaWxhciBpbmRlbnRhdGlvbiBzdHJpbmdzLCB0aGlzIHdpbGwgYmUgc2hhcmVkIGJ5IGFsbFxuICogbG9nZ2Vycy4gQWxzbywgc3RyaW5nIGNvbmNhdGVuYXRpb24gaXMgZXhwZW5zaXZlIHNvIHBlcmZvcm1pbmcgY29uY2F0cyBmb3IgZXZlcnkgbG9nIGVudHJpZXNcbiAqIGlzIGV4cGVuc2l2ZTsgdGhpcyBhbGxldmlhdGVzIGl0LlxuICovXG5jb25zdCBpbmRlbnRhdGlvbk1hcDoge1tpbmRlbnRhdGlvblR5cGU6IHN0cmluZ106IHN0cmluZ1tdfSA9IHt9O1xuXG5cbmV4cG9ydCBjbGFzcyBJbmRlbnRMb2dnZXIgZXh0ZW5kcyBMb2dnZXIge1xuICBjb25zdHJ1Y3RvcihuYW1lOiBzdHJpbmcsIHBhcmVudDogTG9nZ2VyIHwgbnVsbCA9IG51bGwsIGluZGVudGF0aW9uID0gJyAgJykge1xuICAgIHN1cGVyKG5hbWUsIHBhcmVudCk7XG5cbiAgICBpbmRlbnRhdGlvbk1hcFtpbmRlbnRhdGlvbl0gPSBpbmRlbnRhdGlvbk1hcFtpbmRlbnRhdGlvbl0gfHwgWycnXTtcbiAgICBjb25zdCBtYXAgPSBpbmRlbnRhdGlvbk1hcFtpbmRlbnRhdGlvbl07XG5cbiAgICB0aGlzLl9vYnNlcnZhYmxlID0gdGhpcy5fb2JzZXJ2YWJsZS5tYXAoZW50cnkgPT4ge1xuICAgICAgY29uc3QgbCA9IGVudHJ5LnBhdGgubGVuZ3RoO1xuICAgICAgaWYgKGwgPj0gbWFwLmxlbmd0aCkge1xuICAgICAgICBsZXQgY3VycmVudCA9IG1hcFttYXAubGVuZ3RoIC0gMV07XG4gICAgICAgIHdoaWxlIChsID49IG1hcC5sZW5ndGgpIHtcbiAgICAgICAgICBjdXJyZW50ICs9IGluZGVudGF0aW9uO1xuICAgICAgICAgIG1hcC5wdXNoKGN1cnJlbnQpO1xuICAgICAgICB9XG4gICAgICB9XG5cbiAgICAgIGVudHJ5Lm1lc3NhZ2UgPSBtYXBbbF0gKyBlbnRyeS5tZXNzYWdlO1xuXG4gICAgICByZXR1cm4gZW50cnk7XG4gICAgfSk7XG4gIH1cbn1cbiJdfQ==