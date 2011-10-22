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
    shouldPrint = shouldPrint(id, log.is%sEnabled());    
    if( (shouldPrint & LOG_OUT) !=0)
        commonsLog_%s(log, id, msg, ex, printID);
    break;"""
short_case_skel = """case %s:
        commonsLog_%s(log, id, msg, ex, printID);
        break;"""
    
has_isEnabled = ['info', 'debug','trace']    
def log4j_for_level(l):
    if l in has_isEnabled:
        return long_case_skel % (l.upper(), l.capitalize(), l)
    else:
        return short_case_skel % (l.upper(), l)


def commons_for_level(l):
    return long_case_skel % (l.upper(), l.capitalize(), l)


stub_skel = """  public static void commonsLog_%s(%s log, int id, Object msg, Throwable ex, boolean printID) {
    String msg_str;
    if(printID)
        msg_str = taggedID(id) + msg;
    else
        msg_str = msg.toString();

    if(ex == null) 
      log.%s(msg_str);
    else
      log.%s(msg_str, ex);
  }"""
def lev_stub(lev ):
    return stub_skel % (lev, "Log", lev, lev)
    
    
def main():
    
    print "switch(methname) {"
    for lev in levels:
        print "  ",commons_for_level(lev)
    print "}"

    for lev in levels:
        print lev_stub(lev)


if __name__ == '__main__':
    logger = logging.getLogger()
    h = logging.StreamHandler()
    logger.setLevel(logging.INFO)
    logger.addHandler(h)
    main()

