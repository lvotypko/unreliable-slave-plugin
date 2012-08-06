/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jenkinsci.plugins.detection.unreliable.slave;

import hudson.tasks.Mailer;

/**
 *
 * @author lucinka
 */
public class DetectionUtil {
    
    public static boolean isAdminMailSet(){
        boolean ok = Mailer.descriptor().getAdminAddress()!=null;
        if(ok)
            ok= (!Mailer.descriptor().getAdminAddress().equals(""));
        return ok;
    }
    
    public static boolean isStmpServerSet(){
        return Mailer.descriptor().getSmtpServer()!=null;
    }
}
