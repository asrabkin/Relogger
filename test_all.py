
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
        shutil.rmtree("relogger") #Deletes default cached-mappings.
    shutil.os.mkdir("relogger")
    libs = get_libs()
    print "Running base test"
    test_base()
    if os.path.exists('relogger'):
        shutil.rmtree("relogger") #Deletes default cached-mappings.
    shutil.os.mkdir("relogger")
    print "......ok"
    print "Running second test: persistance"
    test_persist()
    print "Doing performance test"
    test_performance()

levels = ['fatal', 'error', 'warn', 'info']

def    test_base():
    out = run_and_capture_relogged("edu.berkeley.BaseTest").splitlines()
    out = out[2:] #drop prolog
    
    adj = 0
    for base_out in (out[0:16], out[16:32]):
        for i,lev in zip(range(1,4), levels):
            expected = "%s main BaseTest - (%d) I am %s" % (lev.upper(), i + adj, lev)
            assert base_out[i-1].endswith(expected), "saw %s" % base_out[i-1]
    
        for s,i,lev in zip(base_out[4::3], range(7,10), levels):
            expected = "%s main BaseTest - (%d) I am %s" % (lev.upper(), i + adj, lev)
            assert s.endswith(expected), "saw " + s
        for s in base_out[5::3]:
            assert s == "java.io.IOException: An exception", "saw " + s
        for s in base_out[6::3]:
            assert s == '\tat edu.berkeley.BaseTest.main(BaseTest.java:18)', "saw " + s 
        adj += 12

def test_persist():
    out1 = run_and_capture_relogged("edu.berkeley.NondeterministicLoad", ["a"])
    print "...ran once"
    output = run_and_capture_relogged("edu.berkeley.NondeterministicLoad", ["b"])
    
    if "(4) I am class B" in output:
        print "......ok"
    else:
        print "persistance is broken, output follows:"
        print "first run:\n--------"
        print out1
        print "\n\nsecond run:\n--------"
        print output

        
        
def test_performance():
    unrelogged =  run_and_capture("edu.berkeley.LogPerfTest")
    print "performance without relogger:\t"+ unrelogged
    relogged = run_and_capture_relogged("edu.berkeley.LogPerfTest")    
    print "performance with relogger:\t"+ "\n".join(relogged.splitlines()[2:5])

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