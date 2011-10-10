
import difflib
import filecmp
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
    test_mapfile()

    print "Doing performance test"
    
    if len(sys.argv) > 1 and "perftable" in sys.argv[1]:
        detailed_performance()
    else:
        test_performance()

levels = ['fatal', 'error', 'warn', 'info']

def    test_base():
    out = run_and_capture_relogged("edu.berkeley.numberlogs.test.BaseTest").splitlines()
    out = out[2:] #drop prolog
    
    adj = 12
    for base_out in (out[0:16], out[16:32]):
        for i,lev in zip(range(1,4), levels):
            expected = "%s main BaseTest - (%d) I am %s" % (lev.upper(), i + adj, lev)
            assert base_out[i-1].endswith(expected), "saw %s" % base_out[i-1] + " but expected "+ expected
    
        for s,i,lev in zip(base_out[4::3], range(7,10), levels):
            expected = "%s main BaseTest - (%d) I am %s" % (lev.upper(), i + adj, lev)
            assert s.endswith(expected), "saw " + s + " but expected "+ expected
        for s in base_out[5::3]:
            assert s == "java.io.IOException: An exception", "saw " + s
        for s in base_out[6::3]:
            assert s == '\tat edu.berkeley.numberlogs.test.BaseTest.main(BaseTest.java:18)', "saw " + s 
        adj -= 12
    
    filecmp_with_diff('relogger/statement_map', 'relogger_reference_output/basic_statement_map',
        "ERR: message map doesn't match template.")
        
        
def filecmp_with_diff(fname1, fname2, err):
    if not filecmp.cmp(fname1, fname2):
        print err
        f1 = open(fname1, 'r')
        f2 = open(fname2, 'r')
        diffs = difflib.unified_diff(f1.readlines(), f2.readlines())
        for d in diffs:
            sys.stdout.write(d)
        f1.close()
        f2.close()

def test_persist():
    out1 = run_and_capture_relogged("edu.berkeley.numberlogs.test.NondeterministicLoad", ["a"])
    print "...ran once"
    
    output = run_and_capture_relogged("edu.berkeley.numberlogs.test.NondeterministicLoad", ["b"])
    
    if "(4) I am class B" in output:
        print "......ok"
    else:
        print "persistance is broken, output follows:"
        print "first run:\n--------"
        print out1
        print "\n\nsecond run:\n--------"
        print output

def    test_mapfile():
    mappingfile = open('relogger/mapping.out', 'r')
    mapflines = []
    for ln in mappingfile:
        assert(ln.endswith(" info\n"))  #Tags present
        mapflines.append(ln.strip())
    mappingfile.close()
    print "...tags present"
    mappingfile = open('relogger/mapping.out', 'w')
    print >>mappingfile,mapflines[0]
    print >>mappingfile,mapflines[1]
    mappingfile.close()
    

#Format is new to old; we're going to rerun B.
    canonIDs = [ l.split()[0] for l in mapflines]
    relocation = open('relogger/relocation', 'w')
    for Aent,Bent in zip(canonIDs[2:4], canonIDs[0:2]):
        print >>relocation, Aent,Bent
    relocation.close()
    #Write a remap file to alias the A entries to B

    output = run_and_capture_relogged("edu.berkeley.numberlogs.test.NondeterministicLoad", ["b"]).splitlines()
    if not "(1) loading class B" in output[2]:
        print "ERROR: relocation failed on first try. Output follows\n\n---\n"
        print output
        print "Relevant line was",output[2]
    output = run_and_capture_relogged("edu.berkeley.numberlogs.test.NondeterministicLoad", ["b"]).splitlines()
    if not "(1) loading class B" in output[2]:
        print "ERROR: relocation failed on second try. Output follows\n\n---\n"
        print output
        print "Relevant line was",output[2]


def test_performance():
    unrelogged =  run_and_capture("edu.berkeley.numberlogs.test.LogPerfTest")
    print "performance without relogger:\t"+ unrelogged
    relogged = run_and_capture_relogged("edu.berkeley.numberlogs.test.LogPerfTest")    
    relogged = "\n".join(filter(lambda x: "-- " in x, relogged.splitlines()))
    print "performance with relogger:\t"+ relogged



unrelogged_txt="""-- unused Log4J avg = 3.98 ns	stddev = 0.11 ns
-- unused Apache Commons avg = 5.54 ns	stddev = 0.09 ns
-- unused Java.util.log avg = 2.05 ns	stddev = 0.05 ns
-- formatted Log4J avg = 620.70 ns	stddev = 7.86 ns
"""
relogged_txt="""UDP listener alive on port 2345
-- unused Log4J avg = 2.74 ns	stddev = 0.08 ns
-- unused Apache Commons avg = 2.70 ns	stddev = 0.02 ns
-- unused Java.util.log avg = 1.91 ns	stddev = 0.01 ns
-- formatted Log4J avg = 992.80 ns	stddev = 15.68 ns
RELOGGER triggering write on exit"""

PERF_LINE_RE = re.compile("-- ([^ ]+) (.*) avg = ([0-9\.]+) ns	stddev = ([0-9\.]+) ns")
RUNS = "runs=20"
def detailed_performance():
    unrelogged_txt =  run_and_capture("edu.berkeley.numberlogs.test.LogPerfTest", args=[RUNS])
    perf_table_without = get_perftable(unrelogged_txt)
    print "Ran without relogger. Now running with..."
    relogged_txt = run_and_capture_relogged("edu.berkeley.numberlogs.test.LogPerfTest", args=[RUNS])
    perf_table_with = get_perftable(relogged_txt)
    print "...OK\n\n"
    
    for (k,(avg,dev)) in sorted(perf_table_without.items()):
        (fmt,logger) = k
        avg_with,dev_with = perf_table_with[k]
        print "%s & %s & %0.2f & %0.2f & %0.2f & %0.2f \\\\" % \
            (logger, fmt, avg,dev, avg_with,dev_with)
        print "\hline"
    print "\n\nEND"


def get_perftable(cmd_out):
    table = {}
    for ln in cmd_out.splitlines():
        m = PERF_LINE_RE.search(ln)
        if m:
            fmt = m.group(1)
            fmt = "Yes" if 'formatted' in fmt else 'No'
            logger = m.group(2)
            avg = float(m.group(3))
            stddev = float(m.group(4))
            table[ (fmt,logger) ] = (avg,stddev)
        elif "-- " in ln:
            print "UNMATCHED",ln
    return table


# java -javaagent:numberedlogs.jar -cp numberedlogs.jar:lib/log4j-1.2.15.jar:lib/commons-logging-api-1.0.4.jar:lib/commons-logging-1.0.4.jar:lib/javassist-3.15.0.jar:conf edu.berkeley.BaseTest



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
#    print output
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