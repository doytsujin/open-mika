/**************************************************************************
* Parts copyright (c) 2001 by Punch Telematix. All rights reserved.       *
* Parts copyright (c) 2004, 2010, 2013 by Chris Gray, /k/ Embedded Java   *
* Solutions.  All rights reserved.                                        *
*                                                                         *
* Redistribution and use in source and binary forms, with or without      *
* modification, are permitted provided that the following conditions      *
* are met:                                                                *
* 1. Redistributions of source code must retain the above copyright       *
*    notice, this list of conditions and the following disclaimer.        *
* 2. Redistributions in binary form must reproduce the above copyright    *
*    notice, this list of conditions and the following disclaimer in the  *
*    documentation and/or other materials provided with the distribution. *
* 3. Neither the name of Punch Telematix or of /k/ Embedded Java Solutions*
*    nor the names of other contributors may be used to endorse or promote*
*    products derived from this software without specific prior written   *
*    permission.                                                          *
*                                                                         *
* THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESS OR IMPLIED          *
* WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF    *
* MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.    *
* IN NO EVENT SHALL PUNCH TELEMATIX, /K/ EMBEDDED JAVA SOLUTIONS OR OTHER *
* CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,   *
* EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,     *
* PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR      *
* PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF  *
* LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING    *
* NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS      *
* SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.            *
**************************************************************************/

#include "misc.h"
#include "oswald.h"
#include "wonka.h"
#include "ts-mem.h"

#include <stdio.h>  /* for sprintf */
#include <string.h> /* for strlen */
#include <unistd.h>

#include "vfs.h"
#include <dlfcn.h>

static x_Mutex handles_Mutex;
static x_mutex handles_mutex = &handles_Mutex;
static void **handles = NULL;
static void **current = NULL;
extern char *fsroot;

static char *buildLibPath(char *path, int pathlen, char *filename, int filenamelen) {
  int fsrooted = (pathlen >= 3 && path[0] == '{' && path[1] == '}' && path[2] == '/');
  int fsrootlen = strlen(fsroot);
  char *result = x_mem_alloc(pathlen + filenamelen + (fsrooted ? fsrootlen : 2));
  int l = 0;

  if (result) {
    if (fsrooted) {
      strcpy(result, fsroot);
      l = fsrootlen;
      if (fsroot[fsrootlen - 1] != '/') {
        result[l++] = '/';
      }
      strcpy(result + l, path + 3);
      l += pathlen - 3;
    }
    else {
      strcpy(result + l, path);
      l += pathlen;
    }
    if (path[pathlen - 1] != '/') {
      result[l++] = '/';
    }
    strcpy(result + l, filename);
  }

  return result;
}

/*
 * Call the JNI_OnLoad function if it exists.
 */
void callOnLoad(void *handle) {
  void *sym;
  jint  (*function_OnLoad)(JavaVM*,void*);
  jint version; // TODO use this for something?
  JNIEnv *env;
  JavaVM *vm;

  sym = dlsym(handle, "JNI_OnLoad");
  function_OnLoad = sym;
  if (!function_OnLoad) {

    return;

  }
  env = w_thread2JNIEnv(currentWonkaThread);
  if ((*env)->GetJavaVM(env, &vm) != 0) {

    return;

  }
  version = function_OnLoad(vm, NULL);

  if ((version & 0xffff0000) != 0x00010000 || (version & 0x0000ffff) == 0 || (version & 0x0000ffff) > 4) {
    // TODO: refuse to load library
  }
}

/*
 * Call the JNI_OnUnload function if it exists.
 */
void callOnUnload(void *handle) {
  void *sym;
  void  (*function_OnUnload)(JavaVM*,void*);
  JNIEnv *env;
  JavaVM *vm;

  sym = dlsym(handle, "JNI_OnUnload");
  function_OnUnload = sym;
  if (!function_OnUnload) {

    return;

  }
  env = w_thread2JNIEnv(currentWonkaThread);
  if ((*env)->GetJavaVM(env, &vm) != 0) {

    return;

  }
  function_OnUnload(vm, NULL);
}

void initModules() {
  x_mutex_create(handles_mutex);
}

void *loadModule(char *name, char *path) {
  char *filename = NULL;
  void *handle = NULL;
  char *orig_ld = NULL;
  char *ld_start = NULL;
  char *ld_end;
  char *ld_segment;
  char *chptr;
  char *libPath = NULL;

  if(name) {
  // 'name' is non-null, must search path
    filename = name;

    woempa(7, "Module name is %s, path is %s\n", filename, path);

    if (path) {
      // search path is given
      orig_ld = getenv("LD_LIBRARY_PATH");
      if(orig_ld) {
        woempa(7, "Appending path to existing LD_LIBRARY_PATH = %s\n", orig_ld);
        ld_start = x_mem_calloc(strlen(orig_ld) + strlen(path) + 2);
	ld_end = ld_start + strlen(orig_ld) + strlen(path) + 1;
        sprintf(ld_start, "%s:%s", orig_ld, path);
      }
      else {
        woempa(7, "No existing LD_LIBRARY_PATH\n");
        ld_start = x_mem_calloc(strlen(path) + 1);
	ld_end = ld_start + strlen(path);
	strcpy(ld_start, path);
      }
      woempa(9, "new LD_LIBRARY_PATH = %s\n", ld_start);
      // try each element in the ld_start path
      ld_segment = ld_start;
      while (!handle && ld_segment < ld_end) {
        chptr = strchr(ld_start, ':');
        if (chptr == NULL) {
          libPath = buildLibPath(ld_segment, ld_end - ld_segment, filename, strlen(filename));
          woempa(9, "Calling dlopen on %s\n", libPath);
          handle = dlopen(libPath, RTLD_LAZY | RTLD_GLOBAL);
          x_mem_free(libPath);
          break;
	}
	else {
          *chptr = 0;
          libPath = buildLibPath(ld_segment, chptr - ld_segment, filename, strlen(filename));
          woempa(9, "Calling dlopen on %s\n", libPath);
          handle = dlopen(libPath, RTLD_LAZY | RTLD_GLOBAL);
          x_mem_free(libPath);
          ld_segment = ++chptr;
	}
      }
      x_mem_free(ld_start);
    }
    else {
    // no search path given, LD_LIBRARY_PATH will be used
      woempa(7, "LD_LIBRARY_PATH = %s\n", getenv("LD_LIBRARY_PATH"));
      handle = dlopen(filename, RTLD_LAZY | RTLD_GLOBAL);
      woempa(7, "handle = %p\n", handle);
    }
  }
  else if(path) {
    woempa(7, "path = %s\n", path);
    filename = path;
    handle = dlopen(filename, RTLD_LAZY | RTLD_GLOBAL);
    woempa(7, "handle = %p\n", handle);
  }
  
  if(handle) {
  int  offset;
    x_mutex_lock(handles_mutex, x_eternal);
  if (!handles) {
    woempa(7, "No handles array allocated yet, allocating array of 10\n");
    handles = x_mem_alloc(10 * sizeof(void *));
    current = handles;
    offset = 0;
  }
  else {
    offset = current - handles;
    if((offset % 10) == 0) {
      woempa(7, "Size of handles array is now %d, expanding to %d\n", offset, offset + 10);
      handles = x_mem_realloc(handles, (offset + 10) * sizeof(void *));
      if (!handles) {
        wabort(ABORT_WONKA, "Unable to allocate memory for native library handles!");
      }
      current = handles + offset;
    }
  }
  *current++ = handle;
    x_mutex_unlock(handles_mutex);
    woempa(7, "Added handle %p to list, now have %d entries\n", handle, current - handles);
    callOnLoad(handle);
  }

  return handle;
}

void unloadModule(void *handle) {
  int offset;

  callOnUnload(handle);
  dlclose(handle);

  // Remove from list
  x_mutex_lock(handles_mutex, x_eternal);
  for (offset = 0; offset < (current - handles); ++offset) {
    if (handles[offset] == handle) {
      handles[offset] == *current--;
      break;
    }
  }
  x_mutex_unlock(handles_mutex);
}

void *lookupModuleSymbol(char *name) {
  void *symbol = NULL;
  void **check;

  woempa(9, "%s\n", name);

  x_mutex_lock(handles_mutex, x_eternal);
  check = handles;
  while(symbol == NULL && check != current) {
    symbol = dlsym(*check++, name);
  }
  x_mutex_unlock(handles_mutex);
  
  return symbol;
}

