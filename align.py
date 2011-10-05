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


def main():
    if(len(sys.argv) < 3):
        print "Usage: align f1 f2"
        sys.exit(0)
        
    f1_as_tuples = read_statement_map(sys.argv[1])
    f2_as_tuples = read_statement_map(sys.argv[2])
    matchup(f1_as_tuples, f2_as_tuples, sys.argv[1], sys.argv[2])
    

def read_statement_map(fname):
    f = open(fname, 'r')
    stmts = {}
    for ln in f.readlines():
        if ln[0].isdigit():
            (_, canonID, sourceLoc, lev, text) = ln.split("\t")
            stmts[canonID] = (sourceLoc, lev, text)
    f.close()
    return stmts

def matchup(f1_messages, f2_messages, f1_name, f2_name):
    set1 = set(f1_messages.keys())
    set2 = set(f2_messages.keys())
    common_keys = set1 & set2
    print len(common_keys),"canonical IDs common to both"
    for k in common_keys:
        loc1 = f1_messages[k][0:1]
        loc2 = f1_messages[k][0:1]
        if loc1 != loc2:
            print "WARN: same canonical ID mapped to both",f1_messages[k],"and",f2_messages[k],"."
        del f1_messages[k]
        del f2_messages[k]
        
    print "left with %d messages in %s and %d in %s" % \
       (len(f1_messages), f1_name, len(f2_messages), f2_name)
    if len(f1_messages) + len(f2_messages) > 0:
        print f1_messages
        print "\n versus \n"
        print f2_messages



if __name__ == '__main__':
    logger = logging.getLogger()
    h = logging.StreamHandler()
    logger.setLevel(logging.INFO)
    logger.addHandler(h)
    main()