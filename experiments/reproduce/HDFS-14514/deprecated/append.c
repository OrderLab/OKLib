#include "hdfs.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

int main(int argc, char **argv) {
    (void)argc;
    (void)argv;

    hdfsFS fs = hdfsConnect("default", 0);

    hdfsFile writeFile = hdfsOpenFile(fs, "/dataenc/a.txt", O_WRONLY | O_CREAT, 0, 0, 0);
    if(!writeFile) {
        fprintf(stderr, "Failed to open %s for writing!\n", writePath);
        exit(-1);
    }

#define strarg(s) (void*)s, strlen(s)

    tSize total_size = 0, num_written_bytes;

    num_written_bytes = hdfsWrite(fs, writeFile, strargs("hello"));
    if (hdfsFlush(fs, writeFile)) {
        fprintf(stderr, "Failed to flush\n");
        exit(-1);
    }

    // TODO

    hdfsCloseFile(fs, writeFile);
}
