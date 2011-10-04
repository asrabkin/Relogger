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

def read_statement_map(fname):
    f = open(fname, 'r')
    stmts = {}
    for ln in f.readlines():
        if ln[0].isdigit():
            (_, canonID, sourceLoc, lev, text) = ln.split("\t")
            stmts[canonID] = (sourceLoc, lev, text)
    f.close()
    return stmts


if __name__ == '__main__':
    logger = logging.getLogger()
    h = logging.StreamHandler()
    logger.setLevel(logging.INFO)
    logger.addHandler(h)
    main()