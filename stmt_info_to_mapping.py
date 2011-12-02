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
    if len(sys.argv) < 2:
        print "Usage: stmt_info_to_mapping stmtinfo [out] ..."
        sys.exit(0)
    stmt_info_file = sys.argv[1]
    if len(sys.argv) > 2:
        out_file = sys.argv[2]
    else:
        out_file = "relogger/mapping.out"
        
    stmt_info_to_mapping(stmt_info_file, out_file)

LN_PAT = re.compile("([0-9]+)\t([a-fA-F0-9]+_[0-9]+)\t([^:]+)\t")
def stmt_info_to_mapping(stmt_info_file, out_file):
    """Takes input and output paths as parameters. 
     Inputs a statement-info file, outputs a mapping file"""
    in_stream = open(stmt_info_file, 'r')
    out = open(out_file, 'w')
     
    for ln in in_stream.readlines():
        m = LN_PAT.match(ln)
        if m:
            print >>out, m.group(2) + " " + m.group(1) + " " + m.group(3)
    
    in_stream.close()
    out.close()


if __name__ == '__main__':
    logger = logging.getLogger()
    h = logging.StreamHandler()
    logger.setLevel(logging.INFO)
    logger.addHandler(h)
    main()