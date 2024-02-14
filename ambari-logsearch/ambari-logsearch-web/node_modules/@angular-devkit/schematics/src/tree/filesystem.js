"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
const path_1 = require("../utility/path");
const entry_1 = require("./entry");
const virtual_1 = require("./virtual");
class FileSystemTree extends virtual_1.VirtualTree {
    constructor(_host, asCreate = false) {
        super();
        this._host = _host;
        this._recursiveFileList().forEach(([system, schematic]) => {
            if (asCreate) {
                this.create(schematic, _host.readFile(system));
            }
            else {
                this._root.set(schematic, new entry_1.LazyFileEntry(schematic, () => _host.readFile(system)));
            }
        });
    }
    _recursiveFileList() {
        const host = this._host;
        const list = [];
        function recurse(systemPath, schematicPath) {
            for (const name of host.listDirectory(systemPath)) {
                const systemName = host.join(systemPath, name);
                const normalizedPath = path_1.normalizePath(schematicPath + '/' + name);
                if (host.isDirectory(normalizedPath)) {
                    recurse(systemName, normalizedPath);
                }
                else {
                    list.push([systemName, normalizedPath]);
                }
            }
        }
        recurse('', '/');
        return list;
    }
}
exports.FileSystemTree = FileSystemTree;
//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJmaWxlIjoiZmlsZXN5c3RlbS5qcyIsInNvdXJjZVJvb3QiOiIvVXNlcnMvaGFuc2wvU291cmNlcy9kZXZraXQvIiwic291cmNlcyI6WyJwYWNrYWdlcy9hbmd1bGFyX2RldmtpdC9zY2hlbWF0aWNzL3NyYy90cmVlL2ZpbGVzeXN0ZW0udHMiXSwibmFtZXMiOltdLCJtYXBwaW5ncyI6Ijs7QUFBQTs7Ozs7O0dBTUc7QUFDSCwwQ0FBK0Q7QUFDL0QsbUNBQXdDO0FBQ3hDLHVDQUF3QztBQVl4QyxvQkFBNEIsU0FBUSxxQkFBVztJQUM3QyxZQUFvQixLQUF5QixFQUFFLFFBQVEsR0FBRyxLQUFLO1FBQzdELEtBQUssRUFBRSxDQUFDO1FBRFUsVUFBSyxHQUFMLEtBQUssQ0FBb0I7UUFHM0MsSUFBSSxDQUFDLGtCQUFrQixFQUFFLENBQUMsT0FBTyxDQUFDLENBQUMsQ0FBQyxNQUFNLEVBQUUsU0FBUyxDQUFDO1lBQ3BELEVBQUUsQ0FBQyxDQUFDLFFBQVEsQ0FBQyxDQUFDLENBQUM7Z0JBQ2IsSUFBSSxDQUFDLE1BQU0sQ0FBQyxTQUFTLEVBQUUsS0FBSyxDQUFDLFFBQVEsQ0FBQyxNQUFNLENBQUMsQ0FBQyxDQUFDO1lBQ2pELENBQUM7WUFBQyxJQUFJLENBQUMsQ0FBQztnQkFDTixJQUFJLENBQUMsS0FBSyxDQUFDLEdBQUcsQ0FBQyxTQUFTLEVBQUUsSUFBSSxxQkFBYSxDQUFDLFNBQVMsRUFBRSxNQUFNLEtBQUssQ0FBQyxRQUFRLENBQUMsTUFBTSxDQUFDLENBQUMsQ0FBQyxDQUFDO1lBQ3hGLENBQUM7UUFDSCxDQUFDLENBQUMsQ0FBQztJQUNMLENBQUM7SUFFUyxrQkFBa0I7UUFDMUIsTUFBTSxJQUFJLEdBQUcsSUFBSSxDQUFDLEtBQUssQ0FBQztRQUN4QixNQUFNLElBQUksR0FBOEIsRUFBRSxDQUFDO1FBRTNDLGlCQUFpQixVQUFrQixFQUFFLGFBQXFCO1lBQ3hELEdBQUcsQ0FBQyxDQUFDLE1BQU0sSUFBSSxJQUFJLElBQUksQ0FBQyxhQUFhLENBQUMsVUFBVSxDQUFDLENBQUMsQ0FBQyxDQUFDO2dCQUNsRCxNQUFNLFVBQVUsR0FBRyxJQUFJLENBQUMsSUFBSSxDQUFDLFVBQVUsRUFBRSxJQUFJLENBQUMsQ0FBQztnQkFDL0MsTUFBTSxjQUFjLEdBQUcsb0JBQWEsQ0FBQyxhQUFhLEdBQUcsR0FBRyxHQUFHLElBQUksQ0FBQyxDQUFDO2dCQUNqRSxFQUFFLENBQUMsQ0FBQyxJQUFJLENBQUMsV0FBVyxDQUFDLGNBQWMsQ0FBQyxDQUFDLENBQUMsQ0FBQztvQkFDckMsT0FBTyxDQUFDLFVBQVUsRUFBRSxjQUFjLENBQUMsQ0FBQztnQkFDdEMsQ0FBQztnQkFBQyxJQUFJLENBQUMsQ0FBQztvQkFDTixJQUFJLENBQUMsSUFBSSxDQUFDLENBQUMsVUFBVSxFQUFFLGNBQWMsQ0FBQyxDQUFDLENBQUM7Z0JBQzFDLENBQUM7WUFDSCxDQUFDO1FBQ0gsQ0FBQztRQUVELE9BQU8sQ0FBQyxFQUFFLEVBQUUsR0FBRyxDQUFDLENBQUM7UUFFakIsTUFBTSxDQUFDLElBQUksQ0FBQztJQUNkLENBQUM7Q0FDRjtBQWpDRCx3Q0FpQ0MiLCJzb3VyY2VzQ29udGVudCI6WyIvKipcbiAqIEBsaWNlbnNlXG4gKiBDb3B5cmlnaHQgR29vZ2xlIEluYy4gQWxsIFJpZ2h0cyBSZXNlcnZlZC5cbiAqXG4gKiBVc2Ugb2YgdGhpcyBzb3VyY2UgY29kZSBpcyBnb3Zlcm5lZCBieSBhbiBNSVQtc3R5bGUgbGljZW5zZSB0aGF0IGNhbiBiZVxuICogZm91bmQgaW4gdGhlIExJQ0VOU0UgZmlsZSBhdCBodHRwczovL2FuZ3VsYXIuaW8vbGljZW5zZVxuICovXG5pbXBvcnQgeyBTY2hlbWF0aWNQYXRoLCBub3JtYWxpemVQYXRoIH0gZnJvbSAnLi4vdXRpbGl0eS9wYXRoJztcbmltcG9ydCB7IExhenlGaWxlRW50cnkgfSBmcm9tICcuL2VudHJ5JztcbmltcG9ydCB7IFZpcnR1YWxUcmVlIH0gZnJvbSAnLi92aXJ0dWFsJztcblxuXG5leHBvcnQgaW50ZXJmYWNlIEZpbGVTeXN0ZW1UcmVlSG9zdCB7XG4gIGxpc3REaXJlY3Rvcnk6IChwYXRoOiBzdHJpbmcpID0+IHN0cmluZ1tdO1xuICBpc0RpcmVjdG9yeTogKHBhdGg6IHN0cmluZykgPT4gYm9vbGVhbjtcbiAgcmVhZEZpbGU6IChwYXRoOiBzdHJpbmcpID0+IEJ1ZmZlcjtcblxuICBqb2luOiAocGF0aDE6IHN0cmluZywgb3RoZXI6IHN0cmluZykgPT4gc3RyaW5nO1xufVxuXG5cbmV4cG9ydCBjbGFzcyBGaWxlU3lzdGVtVHJlZSBleHRlbmRzIFZpcnR1YWxUcmVlIHtcbiAgY29uc3RydWN0b3IocHJpdmF0ZSBfaG9zdDogRmlsZVN5c3RlbVRyZWVIb3N0LCBhc0NyZWF0ZSA9IGZhbHNlKSB7XG4gICAgc3VwZXIoKTtcblxuICAgIHRoaXMuX3JlY3Vyc2l2ZUZpbGVMaXN0KCkuZm9yRWFjaCgoW3N5c3RlbSwgc2NoZW1hdGljXSkgPT4ge1xuICAgICAgaWYgKGFzQ3JlYXRlKSB7XG4gICAgICAgIHRoaXMuY3JlYXRlKHNjaGVtYXRpYywgX2hvc3QucmVhZEZpbGUoc3lzdGVtKSk7XG4gICAgICB9IGVsc2Uge1xuICAgICAgICB0aGlzLl9yb290LnNldChzY2hlbWF0aWMsIG5ldyBMYXp5RmlsZUVudHJ5KHNjaGVtYXRpYywgKCkgPT4gX2hvc3QucmVhZEZpbGUoc3lzdGVtKSkpO1xuICAgICAgfVxuICAgIH0pO1xuICB9XG5cbiAgcHJvdGVjdGVkIF9yZWN1cnNpdmVGaWxlTGlzdCgpOiBbIHN0cmluZywgU2NoZW1hdGljUGF0aCBdW10ge1xuICAgIGNvbnN0IGhvc3QgPSB0aGlzLl9ob3N0O1xuICAgIGNvbnN0IGxpc3Q6IFtzdHJpbmcsIFNjaGVtYXRpY1BhdGhdW10gPSBbXTtcblxuICAgIGZ1bmN0aW9uIHJlY3Vyc2Uoc3lzdGVtUGF0aDogc3RyaW5nLCBzY2hlbWF0aWNQYXRoOiBzdHJpbmcpIHtcbiAgICAgIGZvciAoY29uc3QgbmFtZSBvZiBob3N0Lmxpc3REaXJlY3Rvcnkoc3lzdGVtUGF0aCkpIHtcbiAgICAgICAgY29uc3Qgc3lzdGVtTmFtZSA9IGhvc3Quam9pbihzeXN0ZW1QYXRoLCBuYW1lKTtcbiAgICAgICAgY29uc3Qgbm9ybWFsaXplZFBhdGggPSBub3JtYWxpemVQYXRoKHNjaGVtYXRpY1BhdGggKyAnLycgKyBuYW1lKTtcbiAgICAgICAgaWYgKGhvc3QuaXNEaXJlY3Rvcnkobm9ybWFsaXplZFBhdGgpKSB7XG4gICAgICAgICAgcmVjdXJzZShzeXN0ZW1OYW1lLCBub3JtYWxpemVkUGF0aCk7XG4gICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgbGlzdC5wdXNoKFtzeXN0ZW1OYW1lLCBub3JtYWxpemVkUGF0aF0pO1xuICAgICAgICB9XG4gICAgICB9XG4gICAgfVxuXG4gICAgcmVjdXJzZSgnJywgJy8nKTtcblxuICAgIHJldHVybiBsaXN0O1xuICB9XG59XG4iXX0=