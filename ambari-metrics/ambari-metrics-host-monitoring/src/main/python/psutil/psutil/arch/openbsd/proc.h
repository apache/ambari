/*
 * Copyright (c) 2009, Giampaolo Rodola', Landry Breuil.
 * All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

#include <Python.h>

typedef struct kinfo_proc kinfo_proc;

int psutil_kinfo_proc(pid_t pid, struct kinfo_proc *proc);
struct kinfo_file * kinfo_getfile(pid_t pid, int* cnt);
int psutil_get_proc_list(struct kinfo_proc **procList, size_t *procCount);
char **_psutil_get_argv(pid_t pid);

PyObject *psutil_proc_cmdline(PyObject *self, PyObject *args);
PyObject *psutil_proc_threads(PyObject *self, PyObject *args);
PyObject *psutil_proc_num_fds(PyObject *self, PyObject *args);
PyObject *psutil_proc_cwd(PyObject *self, PyObject *args);
