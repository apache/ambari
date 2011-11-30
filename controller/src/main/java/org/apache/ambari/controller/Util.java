package org.apache.ambari.controller;

import java.util.Date;
import java.util.GregorianCalendar;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

public class Util {
    
    public static XMLGregorianCalendar getXMLGregorianCalendar (Date date) throws Exception {
        if (date == null) {
            return null;
        }
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(date);
        return DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);   
    }
    
    public static String getInstallAndConfigureCommand() {
      return "puppet --apply"; //TODO: this needs to be 'pluggable'/configurable
    }

}
