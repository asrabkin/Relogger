#!/bin/bash

export JAVA_HOME=/System/Library/Frameworks/JavaVM.framework/Versions/1.6.0/Home
HADOOP=/Users/asrabkin/workspace/hadoop-0.20.2

export HADOOP_OPTS="-javaagent:numberedlogs.jar"
export HADOOP_CLASSPATH=lib/javassist-3.15.0.jar

#HADOOP=/Users/asrabkin/Documents/cloudera/hadoop-0.20.2-cdh3u0
CONFDIR=hadoop_conf

echo "JAVA_HOME is $JAVA_HOME"
echo "Hadoop dir is $HADOOP confdir is $CONFDIR"

rm -rf /tmp/hadoop-asrabkin

$HADOOP/bin/hadoop --config $CONFDIR namenode -format


#exit 0
