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
    check_for_dup_vals(f1_as_tuples, sys.argv[1])
    check_for_dup_vals(f2_as_tuples, sys.argv[2])

    matchup(f1_as_tuples, f2_as_tuples, sys.argv[1], sys.argv[2])
    

def read_statement_map(fname):
    f = open(fname, 'r')
    stmts = {}
    prevID = ""
    for ln in f.readlines():
        if ln[0].isdigit():
            (_, canonID, sourceLoc, lev, text) = ln.split("\t", 4)
            stmts[canonID] = (sourceLoc, lev, text)
            prevID = canonID
        else:
            (sourceLoc, lev, text) = stmts[prevID]
            stmts[prevID] = (sourceLoc, lev, text + ln)
    f.close()
    return stmts

def check_for_dup_vals(messages, filename):
    invert_m = {}
    print filename,"\n-------\n"
    for (k,v) in messages.items():
        if v in invert_m:
            print "WARN: saw",v,"with keys",k,"and",invert_m[v]
        else:
            invert_m[v] = k

def matchup(f1_messages, f2_messages, f1_name, f2_name):
    
        #First, prune the common parts
    prune_common_parts(f1_messages, f2_messages)
    print "left with %d messages in %s and %d in %s" % \
       (len(f1_messages), f1_name, len(f2_messages), f2_name)
       
       #Next, remove all the exact content matches
    exact_content_matches = match_content_exactly(f1_messages, f2_messages)
    print len(exact_content_matches),"exact matches by content"
    for (k1,k2) in exact_content_matches:
        del f1_messages[k1]
        del f2_messages[k2]

    #Now we're finally down to the fuzzy matches
    
    f2_invert_as_strs = dict( [(" ".join(val), k) for (k,val) in f2_messages.items() ]  )
    f2_strs = set(f2_invert_as_strs.keys())
    matches = 0
    for (k, v1) in f1_messages.items():
        v1_str = " ".join(v1)
        
        best_match_list = difflib.get_close_matches(v1_str, f2_strs, 1, 0.8)
        if len(best_match_list) > 0:
            best_match = best_match_list[0]
            f2_strs.remove(best_match)
            print matches,"-- Tentatively matching",v1_str,"with",best_match
            matches += 1
    print "total of",matches,"approximate matches" 
#    if len(f1_messages) + len(f2_messages) > 0:
#        print f1_messages
#        print "\n versus \n"
#        print f2_messages

def prune_common_parts(f1_messages, f2_messages):
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


def match_content_exactly(f1_messages, f2_messages):
    """Takes two sets of messages, in key:value map format. Returns a list
    of exact matches by content.
    """
    invert_f2 = dict([(v,k) for (k,v) in f2_messages.items()])
    used_keys = []
    for (k, v1) in f1_messages.items():
        if v1 in invert_f2:
            print "Matching on",v1
            used_keys.append(  (k,invert_f2[v1]) )
    return used_keys

if __name__ == '__main__':
    logger = logging.getLogger()
    h = logging.StreamHandler()
    logger.setLevel(logging.INFO)
    logger.addHandler(h)
    main()