import logging


levels = ['fatal', 'error', 'warn', 'info', 'debug', 'trace']


if_skel = """
    else if(methname.equals("%s")) {
      if(args.length == 1) 
        log.%s("(" + id + ") " +args[0]);
      else
        log.%s("(" + id + ") " +args[0], (Throwable) args[1]);
    } """ 
    
long_case_skel = """case %s:
    if(log.is%sEnabled())
        log4J_%s(log, id, msg, ex);
    else
        cached_disable(id);
    break;"""
short_case_skel = """case %s:
        log4J_%s(log, id, msg, ex);
        break;"""
    
has_isEnabled = ['info', 'debug','trace']    
def for_level(l):
    if l in has_isEnabled:
        return long_case_skel % (l.upper(), l.capitalize(), l)
    else:
        return short_case_skel % (l.upper(), l)


stub_skel = """  public static void log4J_%s(%s log, int id, Object msg, Throwable ex) {
    if(ex == null) 
      log.%s("(" + id + ") " +msg);
    else
      log.%s("(" + id + ") " +msg, ex);
  }"""
def lev_stub(lev ):
    return stub_skel % (lev, "Logger", lev, lev)
    
    
def main():
    
    print "switch(methname) {"
    for lev in levels:
        print for_level(lev)
    print "}"

    for lev in levels:
        print lev_stub(lev)


if __name__ == '__main__':
    logger = logging.getLogger()
    h = logging.StreamHandler()
    logger.setLevel(logging.INFO)
    logger.addHandler(h)
    main()
