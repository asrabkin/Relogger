

from collections import defaultdict
import logging
from optparse import OptionParser
import os
import os.path as path
import re
import shutil
import subprocess
import sys
import time


TABLE = True
ONLY_LEVELED_LOGS = True

def main():
    if(len(sys.argv) < 2):
        print "Usage:  lexical_ambiguity <stmt file> [<log>]"
        sys.exit(1)

    stmts = read_statement_map(sys.argv[1])
    multiline = 0
    
    txt_to_stmts = defaultdict(list)
    distinct_short = 0
    for (shortID, source_loc, lev, text) in stmts.values():
        if "\n" in text:
            multiline += 1
        if len(text) < 3:
            distinct_short += 1
            continue
        if 'Exception' in text:
            continue
        short_loc = source_loc.split(":")[0]
        long_txt = short_loc + " " + lev + " " + text
        txt_to_stmts[long_txt].append( (shortID, source_loc, text) ) 


    clones = 0
    ambigIDs = []
    ambigTexts = []
    unambigTexts = []
    for long_txt,stmts_for_txt in txt_to_stmts.items():
        if len(stmts_for_txt) > 1:
            clones += 1
            print "%d\t%s" % ( len(stmts_for_txt), long_txt)
            
            for (shortID, source_loc, text) in stmts_for_txt:
                ambigIDs.append(shortID)
                print "\t%d %s" % (shortID, source_loc)
            if len(text) > 6:
                ambigTexts.append( (text,long_txt) )
        else:
            (_, _, text) = stmts_for_txt[0]
            unambigTexts.append(  (text,long_txt)  )  
    print "\n\n"
    
    print "Ambiguous IDs: ", sorted(ambigIDs)
    ambig_percent = 100 * clones / len(txt_to_stmts)
    print "read %d statements, %d lexically_distinct, %d clones. This is %d%% of statements" % (len(stmts), len(txt_to_stmts)+ distinct_short, clones, ambig_percent)
    print "%d were multiline" % multiline
    
    table_entries = []
    table_entries.append( ("Statements", len(stmts)) ) 
    table_entries.append( ("Distinct Statements", len(txt_to_stmts)+ distinct_short) ) 

    table_entries.append( ("Indistinct Statements ", clones) ) 
        
    if len(sys.argv) > 2:
        check_against_concrete(ambigTexts, sys.argv[2], table_entries)
    
    if TABLE:
        for (label,v) in table_entries:
            print "%s & %d \\\\" % (label, v)
            print "\hline"
    

def read_statement_map(fname):
    f = open(fname, 'r')
    stmts = {}
    prevID = ""
    lineno = 0
    for ln in f.readlines():
        lineno += 1
        if ln[0].isdigit():
            parts = ln.split("\t", 4)
            if len(parts) < 5:
                print "Trouble with line %d: %s " % (lineno, ln)
            (shortID, canonID, sourceLoc, lev, text) = parts
            shortID = int(shortID)
            if lev == 'println':
                prevID = ""
                continue
            if len(text) > 1:
                text = text.strip()
            stmts[canonID] = (shortID, sourceLoc, lev, text)
            prevID = canonID
        elif prevID != '':
            (shortID, sourceLoc, lev, text) = stmts[prevID]
            ln = ln.strip()
            stmts[prevID] = (shortID, sourceLoc, lev, text + ln)
    f.close()
    return stmts


def check_against_concrete(ambigTexts, concrete_log, table_entries):
    print "matching against concrete log..."
    
    
    if 'pastry' in concrete_log:
        ONLY_LEVELED_LOGS = False
    
    regexes = [ (re.compile(t), long_t) for t,long_t in ambigTexts]
    pat_to_count_seen = defaultdict(int)
    messages = 0
    f = open(concrete_log, 'r')
    for ln in f.readlines():
        if not ONLY_LEVELED_LOGS or ('INFO' in ln or 'DEBUG' in ln or 'WARN' in ln):
            messages += 1
        for r, long_t in regexes:
            if r.search(ln):
                pat_to_count_seen[long_t] += 1

    ambig_messages = 0
    for text,count in pat_to_count_seen.items():
        print "%d\t%s" % (count, text)
        ambig_messages += count
        
    table_entries.append( ("Messages", messages)) 
    table_entries.append( ("Ambiguous Messages", ambig_messages)) 
    

    
if __name__ == '__main__':
    logger = logging.getLogger()
    h = logging.StreamHandler()
    logger.setLevel(logging.INFO)
    logger.addHandler(h)
    main()