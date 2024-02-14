"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
const path_1 = require("path");
class ExportStringRef {
    constructor(ref, parentPath = process.cwd(), inner = true) {
        const [path, name] = ref.split('#', 2);
        this._module = path[0] == '.' ? path_1.resolve(parentPath, path) : path;
        this._module = require.resolve(this._module);
        this._path = path_1.dirname(this._module);
        if (inner) {
            this._ref = require(this._module)[name || 'default'];
        }
        else {
            this._ref = require(this._module);
        }
    }
    get ref() { return this._ref; }
    get module() { return this._module; }
    get path() { return this._path; }
}
exports.ExportStringRef = ExportStringRef;
//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJmaWxlIjoiZXhwb3J0LXJlZi5qcyIsInNvdXJjZVJvb3QiOiIvVXNlcnMvaGFuc2wvU291cmNlcy9kZXZraXQvIiwic291cmNlcyI6WyJwYWNrYWdlcy9hbmd1bGFyX2RldmtpdC9zY2hlbWF0aWNzL3Rvb2xzL2V4cG9ydC1yZWYudHMiXSwibmFtZXMiOltdLCJtYXBwaW5ncyI6Ijs7QUFBQTs7Ozs7O0dBTUc7QUFDSCwrQkFBd0M7QUFHeEM7SUFLRSxZQUFZLEdBQVcsRUFBRSxhQUFxQixPQUFPLENBQUMsR0FBRyxFQUFFLEVBQUUsS0FBSyxHQUFHLElBQUk7UUFDdkUsTUFBTSxDQUFDLElBQUksRUFBRSxJQUFJLENBQUMsR0FBRyxHQUFHLENBQUMsS0FBSyxDQUFDLEdBQUcsRUFBRSxDQUFDLENBQUMsQ0FBQztRQUN2QyxJQUFJLENBQUMsT0FBTyxHQUFHLElBQUksQ0FBQyxDQUFDLENBQUMsSUFBSSxHQUFHLEdBQUcsY0FBTyxDQUFDLFVBQVUsRUFBRSxJQUFJLENBQUMsR0FBRyxJQUFJLENBQUM7UUFDakUsSUFBSSxDQUFDLE9BQU8sR0FBRyxPQUFPLENBQUMsT0FBTyxDQUFDLElBQUksQ0FBQyxPQUFPLENBQUMsQ0FBQztRQUM3QyxJQUFJLENBQUMsS0FBSyxHQUFHLGNBQU8sQ0FBQyxJQUFJLENBQUMsT0FBTyxDQUFDLENBQUM7UUFFbkMsRUFBRSxDQUFDLENBQUMsS0FBSyxDQUFDLENBQUMsQ0FBQztZQUNWLElBQUksQ0FBQyxJQUFJLEdBQUcsT0FBTyxDQUFDLElBQUksQ0FBQyxPQUFPLENBQUMsQ0FBQyxJQUFJLElBQUksU0FBUyxDQUFDLENBQUM7UUFDdkQsQ0FBQztRQUFDLElBQUksQ0FBQyxDQUFDO1lBQ04sSUFBSSxDQUFDLElBQUksR0FBRyxPQUFPLENBQUMsSUFBSSxDQUFDLE9BQU8sQ0FBQyxDQUFDO1FBQ3BDLENBQUM7SUFDSCxDQUFDO0lBRUQsSUFBSSxHQUFHLEtBQUssTUFBTSxDQUFDLElBQUksQ0FBQyxJQUFJLENBQUMsQ0FBQyxDQUFDO0lBQy9CLElBQUksTUFBTSxLQUFLLE1BQU0sQ0FBQyxJQUFJLENBQUMsT0FBTyxDQUFDLENBQUMsQ0FBQztJQUNyQyxJQUFJLElBQUksS0FBSyxNQUFNLENBQUMsSUFBSSxDQUFDLEtBQUssQ0FBQyxDQUFDLENBQUM7Q0FDbEM7QUFyQkQsMENBcUJDIiwic291cmNlc0NvbnRlbnQiOlsiLyoqXG4gKiBAbGljZW5zZVxuICogQ29weXJpZ2h0IEdvb2dsZSBJbmMuIEFsbCBSaWdodHMgUmVzZXJ2ZWQuXG4gKlxuICogVXNlIG9mIHRoaXMgc291cmNlIGNvZGUgaXMgZ292ZXJuZWQgYnkgYW4gTUlULXN0eWxlIGxpY2Vuc2UgdGhhdCBjYW4gYmVcbiAqIGZvdW5kIGluIHRoZSBMSUNFTlNFIGZpbGUgYXQgaHR0cHM6Ly9hbmd1bGFyLmlvL2xpY2Vuc2VcbiAqL1xuaW1wb3J0IHsgZGlybmFtZSwgcmVzb2x2ZSB9IGZyb20gJ3BhdGgnO1xuXG5cbmV4cG9ydCBjbGFzcyBFeHBvcnRTdHJpbmdSZWY8VD4ge1xuICBwcml2YXRlIF9yZWY/OiBUO1xuICBwcml2YXRlIF9tb2R1bGU6IHN0cmluZztcbiAgcHJpdmF0ZSBfcGF0aDogc3RyaW5nO1xuXG4gIGNvbnN0cnVjdG9yKHJlZjogc3RyaW5nLCBwYXJlbnRQYXRoOiBzdHJpbmcgPSBwcm9jZXNzLmN3ZCgpLCBpbm5lciA9IHRydWUpIHtcbiAgICBjb25zdCBbcGF0aCwgbmFtZV0gPSByZWYuc3BsaXQoJyMnLCAyKTtcbiAgICB0aGlzLl9tb2R1bGUgPSBwYXRoWzBdID09ICcuJyA/IHJlc29sdmUocGFyZW50UGF0aCwgcGF0aCkgOiBwYXRoO1xuICAgIHRoaXMuX21vZHVsZSA9IHJlcXVpcmUucmVzb2x2ZSh0aGlzLl9tb2R1bGUpO1xuICAgIHRoaXMuX3BhdGggPSBkaXJuYW1lKHRoaXMuX21vZHVsZSk7XG5cbiAgICBpZiAoaW5uZXIpIHtcbiAgICAgIHRoaXMuX3JlZiA9IHJlcXVpcmUodGhpcy5fbW9kdWxlKVtuYW1lIHx8ICdkZWZhdWx0J107XG4gICAgfSBlbHNlIHtcbiAgICAgIHRoaXMuX3JlZiA9IHJlcXVpcmUodGhpcy5fbW9kdWxlKTtcbiAgICB9XG4gIH1cblxuICBnZXQgcmVmKCkgeyByZXR1cm4gdGhpcy5fcmVmOyB9XG4gIGdldCBtb2R1bGUoKSB7IHJldHVybiB0aGlzLl9tb2R1bGU7IH1cbiAgZ2V0IHBhdGgoKSB7IHJldHVybiB0aGlzLl9wYXRoOyB9XG59XG4iXX0=