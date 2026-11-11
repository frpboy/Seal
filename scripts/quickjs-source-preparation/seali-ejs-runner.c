/* Seali EJS-only QuickJS driver. Review candidate, not a sandbox certification.
 * Uses QuickJS under its MIT license. No quickjs-libc, modules, std/os or loadScript.
 */
#define _GNU_SOURCE
#include "quickjs.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <time.h>
#include <unistd.h>
#include <signal.h>
#include <sys/prctl.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <errno.h>

#define SCRIPT_LIMIT (16u * 1024u * 1024u)
#define MEMORY_LIMIT (256u * 1024u * 1024u)
#define STACK_LIMIT (1024u * 1024u)
#ifndef DEADLINE_SECONDS
#define DEADLINE_SECONDS 120
#endif

static int64_t deadline_ms;
static size_t output_bytes;
static int64_t monotonic_ms(void) {
    struct timespec ts;
    if (clock_gettime(CLOCK_MONOTONIC, &ts) != 0) return -1;
    return (int64_t)ts.tv_sec * 1000 + ts.tv_nsec / 1000000;
}
static int interrupted(JSRuntime *rt, void *opaque) {
    (void)rt; (void)opaque;
    int64_t now = monotonic_ms();
    return now < 0 || now >= deadline_ms;
}
static JSValue print_line(JSContext *ctx, JSValueConst self,
                          int argc, JSValueConst *argv) {
    (void)self;
    for (int i = 0; i < argc; i++) {
        const char *text = JS_ToCString(ctx, argv[i]);
        if (!text) return JS_EXCEPTION;
        size_t len = strlen(text);
        if (len >= SCRIPT_LIMIT || output_bytes > SCRIPT_LIMIT - len - 1) {
            JS_FreeCString(ctx, text);
            return JS_ThrowInternalError(ctx, "EJS output limit exceeded");
        }
        output_bytes += len + 1;
        if (i) fputc(' ', stdout);
        fputs(text, stdout);
        JS_FreeCString(ctx, text);
    }
    fputc('\n', stdout);
    fflush(stdout);
    return JS_UNDEFINED;
}
static int fail_exception(JSContext *ctx) {
    JSValue error = JS_GetException(ctx);
    /* Deliberately do not print player source or engine error contents. */
    JS_FreeValue(ctx, error);
    fputs("EJS execution failed\n", stderr);
    return 1;
}
int main(int argc, char **argv) {
    if (argc == 2 && strcmp(argv[1], "--help") == 0) {
        puts("QuickJS version 2026-06-04\nSeali EJS-only runtime\nusage: libqjs.so --script FILE");
        return 1; /* actual yt-dlp detector explicitly permits help exit1 */
    }
    if (argc != 3 || strcmp(argv[1], "--script") != 0) return 2;
    pid_t parent = getppid();
    if (parent <= 1 || prctl(PR_SET_PDEATHSIG, SIGKILL) != 0 || getppid() != parent)
        return 2;
    /* One open, no symlink following. Final temp cleanup still belongs to yt-dlp. */
    int fd = open(argv[2], O_RDONLY | O_CLOEXEC | O_NOFOLLOW);
    if (fd < 0) return 2;
    struct stat st;
    if (fstat(fd, &st) != 0 || !S_ISREG(st.st_mode) || st.st_uid != getuid() ||
        (st.st_mode & 0077) != 0 || st.st_size < 0 || st.st_size > SCRIPT_LIMIT) {
        close(fd); return 2;
    }
    size_t size = (size_t)st.st_size;
    char *source = malloc(size + 1);
    if (!source) { close(fd); return 2; }
    size_t pos = 0;
    while (pos < size) {
        ssize_t n = read(fd, source + pos, size - pos);
        if (n < 0 && errno == EINTR) continue;
        if (n <= 0) { free(source); close(fd); return 2; }
        pos += (size_t)n;
    }
    close(fd); source[size] = 0;
    int64_t now = monotonic_ms();
    if (now < 0) { free(source); return 2; }
    deadline_ms = now + DEADLINE_SECONDS * 1000;
    /* Hard wall-clock fallback also covers a native-code loop without JS ticks. */
    signal(SIGALRM, SIG_DFL);
    alarm(DEADLINE_SECONDS + 2);
    JSRuntime *rt = JS_NewRuntime();
    if (!rt) { free(source); return 2; }
    JS_SetMemoryLimit(rt, MEMORY_LIMIT);
    JS_SetMaxStackSize(rt, STACK_LIMIT);
    JS_SetInterruptHandler(rt, interrupted, NULL);
    JSContext *ctx = JS_NewContext(rt);
    if (!ctx) { free(source); JS_FreeRuntime(rt); return 2; }
    JSValue global = JS_GetGlobalObject(ctx);
    JSValue console = JS_NewObject(ctx);
    int setup = JS_SetPropertyStr(ctx, console, "log", JS_NewCFunction(ctx, print_line, "log", 1));
    setup |= JS_SetPropertyStr(ctx, global, "console", console);
    setup |= JS_SetPropertyStr(ctx, global, "print", JS_NewCFunction(ctx, print_line, "print", 1));
    JS_FreeValue(ctx, global);
    int result = 1;
    if (setup < 0) result = fail_exception(ctx);
    else {
        JSValue value = JS_Eval(ctx, source, size, "<private-ejs-script>", JS_EVAL_TYPE_GLOBAL);
        if (JS_IsException(value)) result = fail_exception(ctx);
        else result = 0;
        JS_FreeValue(ctx, value);
        JSContext *job_ctx;
        int jobs = 0;
        while (result == 0 && (jobs = JS_ExecutePendingJob(rt, &job_ctx)) > 0) {
            if (interrupted(rt, NULL)) result = 1;
        }
        if (result == 0 && jobs < 0) result = fail_exception(job_ctx);
    }
    free(source);
    JS_FreeContext(ctx);
    JS_FreeRuntime(rt);
    alarm(0);
    return result;
}
