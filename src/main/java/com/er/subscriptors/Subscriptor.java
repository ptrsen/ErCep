package com.er.subscriptors;

import com.er.erCep.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

public class Subscriptor {
    private static final Logger log = LoggerFactory.getLogger(Main.class);      // Logger
    private int a = 0;
/*
    public void update(Object[] row) {
        a=a+1;
        log.info( Integer.toString(  row.length)+" , "+Integer.toString(a)   );
        log.info(row[0].toString());
    }
*/
    public void update(Map row) {
        //log.info(  row.toString()  );
        log.info( row.get("Type").toString()  );
    }

/*
    public void updateEnd() {
    //    log.info("end");
    }
*/

}

