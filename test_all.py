
import glob
import json
import logging
from optparse import OptionParser
import os
import os.path as path
import re
import shutil
import subprocess
import sys
import time

from collections import defaultdict

jar_path = "eclipse_build"
libs = ""
def main():
    global libs
    print "running relogger tests"
    if os.path.exists('relogger'):
        shutil.rmtree("relogger")
    shutil.os.mkdir("relogger")
    libs = get_libs()
    print "running first test: persistance"
    test_persist()
    print "doing performance test"
    test_performance()
    

def test_persist():
    run_and_capture_relogged("edu.berkeley.NondeterministicLoad", ["a"])
    print "ran once"
    output = run_and_capture_relogged("edu.berkeley.NondeterministicLoad", ["b"])
    
    if "(4) I am class B" in output:
        print "persistance works"
    else:
        print "persistance is broken"
        
        
def test_performance():
    unrelogged =  run_and_capture("edu.berkeley.LogPerfTest")
    print "performance without relogger:"+ unrelogged
    relogged = run_and_capture_relogged("edu.berkeley.LogPerfTest")    
    print "performance with relogger:"+ relogged

AGENT_INJECT="-javaagent:numberedlogs.jar"
def run_and_capture_relogged(java_main, args=[]):
    return run_and_capture(java_main, args, relogged=True)

def run_and_capture(java_main, args=[], relogged=False):
    cmd_array = ["java"]
    if relogged:
        cmd_array.append(AGENT_INJECT)
    cmd_array.extend( ["-ea", "-cp", jar_path+":conf:" + libs, java_main])
    cmd_array.extend(args)
    p = subprocess.Popen(cmd_array, stdout=subprocess.PIPE, stderr=None)
    (output, err) = p.communicate()
    return output

def get_libs():

    jar_list = glob.glob("lib/*.jar")
    joined_path = ":".join(jar_list)
    return joined_path


if __name__ == '__main__':
    logger = logging.getLogger()
    h = logging.StreamHandler()
    logger.setLevel(logging.INFO)
    logger.addHandler(h)
    main()