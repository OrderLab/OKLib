.PHONY: all clean

HADOOP_HOME=/home/yuzhuo/t2c/hadoop/hadoop-dist/target/hadoop-2.10.0-SNAPSHOT

CFLAGS = -Wall -Wextra -O2 -I$(HADOOP_HOME)/include
LDFLAGS = -L$(HADOOP_HOME)/lib/native
LDLIBS = -lhdfs

all: append

clean:
	rm -f append

append: append.c
