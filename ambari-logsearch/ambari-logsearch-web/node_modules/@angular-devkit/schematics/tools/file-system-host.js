"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const fs_1 = require("fs");
const path_1 = require("path");
class FileSystemHost {
    constructor(_root) {
        this._root = _root;
    }
    listDirectory(path) {
        let files = fs_1.readdirSync(path_1.join(this._root, path));
        if (path == '/' || path == '') {
            files = files
                .filter(path => path !== '.git')
                .filter(path => path !== 'node_modules');
        }
        // Add the path as root is part of the file list.
        return files;
    }
    isDirectory(path) {
        return fs_1.statSync(path_1.join(this._root, path)).isDirectory();
    }
    readFile(path) {
        return fs_1.readFileSync(path_1.join(this._root, path));
    }
    join(path1, path2) {
        return path_1.join(path1, path2);
    }
}
exports.FileSystemHost = FileSystemHost;
//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJmaWxlIjoiZmlsZS1zeXN0ZW0taG9zdC5qcyIsInNvdXJjZVJvb3QiOiIvVXNlcnMvaGFuc2wvU291cmNlcy9kZXZraXQvIiwic291cmNlcyI6WyJwYWNrYWdlcy9hbmd1bGFyX2RldmtpdC9zY2hlbWF0aWNzL3Rvb2xzL2ZpbGUtc3lzdGVtLWhvc3QudHMiXSwibmFtZXMiOltdLCJtYXBwaW5ncyI6Ijs7QUFRQSwyQkFBeUQ7QUFDekQsK0JBQTRCO0FBRTVCO0lBQ0UsWUFBb0IsS0FBYTtRQUFiLFVBQUssR0FBTCxLQUFLLENBQVE7SUFBRyxDQUFDO0lBRXJDLGFBQWEsQ0FBQyxJQUFZO1FBQ3hCLElBQUksS0FBSyxHQUFHLGdCQUFXLENBQUMsV0FBSSxDQUFDLElBQUksQ0FBQyxLQUFLLEVBQUUsSUFBSSxDQUFDLENBQUMsQ0FBQztRQUNoRCxFQUFFLENBQUMsQ0FBQyxJQUFJLElBQUksR0FBRyxJQUFJLElBQUksSUFBSSxFQUFFLENBQUMsQ0FBQyxDQUFDO1lBQzlCLEtBQUssR0FBRyxLQUFLO2lCQUVWLE1BQU0sQ0FBQyxJQUFJLElBQUksSUFBSSxLQUFLLE1BQU0sQ0FBQztpQkFFL0IsTUFBTSxDQUFDLElBQUksSUFBSSxJQUFJLEtBQUssY0FBYyxDQUFDLENBQUM7UUFDN0MsQ0FBQztRQUVELGlEQUFpRDtRQUNqRCxNQUFNLENBQUMsS0FBSyxDQUFDO0lBQ2YsQ0FBQztJQUNELFdBQVcsQ0FBQyxJQUFZO1FBQ3RCLE1BQU0sQ0FBQyxhQUFRLENBQUMsV0FBSSxDQUFDLElBQUksQ0FBQyxLQUFLLEVBQUUsSUFBSSxDQUFDLENBQUMsQ0FBQyxXQUFXLEVBQUUsQ0FBQztJQUN4RCxDQUFDO0lBQ0QsUUFBUSxDQUFDLElBQVk7UUFDbkIsTUFBTSxDQUFDLGlCQUFZLENBQUMsV0FBSSxDQUFDLElBQUksQ0FBQyxLQUFLLEVBQUUsSUFBSSxDQUFDLENBQUMsQ0FBQztJQUM5QyxDQUFDO0lBRUQsSUFBSSxDQUFDLEtBQWEsRUFBRSxLQUFhO1FBQy9CLE1BQU0sQ0FBQyxXQUFJLENBQUMsS0FBSyxFQUFFLEtBQUssQ0FBQyxDQUFDO0lBQzVCLENBQUM7Q0FDRjtBQTFCRCx3Q0EwQkMiLCJzb3VyY2VzQ29udGVudCI6WyIvKipcbiAqIEBsaWNlbnNlXG4gKiBDb3B5cmlnaHQgR29vZ2xlIEluYy4gQWxsIFJpZ2h0cyBSZXNlcnZlZC5cbiAqXG4gKiBVc2Ugb2YgdGhpcyBzb3VyY2UgY29kZSBpcyBnb3Zlcm5lZCBieSBhbiBNSVQtc3R5bGUgbGljZW5zZSB0aGF0IGNhbiBiZVxuICogZm91bmQgaW4gdGhlIExJQ0VOU0UgZmlsZSBhdCBodHRwczovL2FuZ3VsYXIuaW8vbGljZW5zZVxuICovXG5pbXBvcnQgeyBGaWxlU3lzdGVtVHJlZUhvc3QgfSBmcm9tICdAYW5ndWxhci1kZXZraXQvc2NoZW1hdGljcyc7XG5pbXBvcnQgeyByZWFkRmlsZVN5bmMsIHJlYWRkaXJTeW5jLCBzdGF0U3luYyB9IGZyb20gJ2ZzJztcbmltcG9ydCB7IGpvaW4gfSBmcm9tICdwYXRoJztcblxuZXhwb3J0IGNsYXNzIEZpbGVTeXN0ZW1Ib3N0IGltcGxlbWVudHMgRmlsZVN5c3RlbVRyZWVIb3N0IHtcbiAgY29uc3RydWN0b3IocHJpdmF0ZSBfcm9vdDogc3RyaW5nKSB7fVxuXG4gIGxpc3REaXJlY3RvcnkocGF0aDogc3RyaW5nKSB7XG4gICAgbGV0IGZpbGVzID0gcmVhZGRpclN5bmMoam9pbih0aGlzLl9yb290LCBwYXRoKSk7XG4gICAgaWYgKHBhdGggPT0gJy8nIHx8IHBhdGggPT0gJycpIHtcbiAgICAgIGZpbGVzID0gZmlsZXNcbiAgICAgIC8vIFJlbW92ZSAuZ2l0LlxuICAgICAgICAuZmlsdGVyKHBhdGggPT4gcGF0aCAhPT0gJy5naXQnKVxuICAgICAgICAvLyBSZW1vdmUgbm9kZV9tb2R1bGVzLlxuICAgICAgICAuZmlsdGVyKHBhdGggPT4gcGF0aCAhPT0gJ25vZGVfbW9kdWxlcycpO1xuICAgIH1cblxuICAgIC8vIEFkZCB0aGUgcGF0aCBhcyByb290IGlzIHBhcnQgb2YgdGhlIGZpbGUgbGlzdC5cbiAgICByZXR1cm4gZmlsZXM7XG4gIH1cbiAgaXNEaXJlY3RvcnkocGF0aDogc3RyaW5nKSB7XG4gICAgcmV0dXJuIHN0YXRTeW5jKGpvaW4odGhpcy5fcm9vdCwgcGF0aCkpLmlzRGlyZWN0b3J5KCk7XG4gIH1cbiAgcmVhZEZpbGUocGF0aDogc3RyaW5nKSB7XG4gICAgcmV0dXJuIHJlYWRGaWxlU3luYyhqb2luKHRoaXMuX3Jvb3QsIHBhdGgpKTtcbiAgfVxuXG4gIGpvaW4ocGF0aDE6IHN0cmluZywgcGF0aDI6IHN0cmluZykge1xuICAgIHJldHVybiBqb2luKHBhdGgxLCBwYXRoMik7XG4gIH1cbn1cbiJdfQ==