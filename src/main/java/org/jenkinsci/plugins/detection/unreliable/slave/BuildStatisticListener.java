/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jenkinsci.plugins.detection.unreliable.slave;

import hudson.Extension;
import hudson.Util;
import hudson.matrix.MatrixBuild;
import hudson.model.Computer;
import hudson.model.Hudson;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import hudson.slaves.OfflineCause;
import hudson.slaves.SlaveComputer;
import hudson.tasks.Mailer;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;


/**
 *
 * @author lucinka
 */
 @Extension
public class BuildStatisticListener extends RunListener<Run>{

   private Set<SlaveBuildFailureStatistic> statistics = new HashSet<SlaveBuildFailureStatistic>();
    
    @Override
    public void onCompleted(Run run, TaskListener listener) {
        if(run instanceof MatrixBuild)
            return;
        UnreliableSlaveDetection.DescriptorImpl descriptor = (UnreliableSlaveDetection.DescriptorImpl) Hudson.getInstance().getDescriptorOrDie(UnreliableSlaveDetection.class);
        if(descriptor.getSettings().getNumberOfFailureInRow()==0){
            Logger.getLogger(BuildStatisticListener.class.getName()).log(Level.INFO, "Unreliable slave plugin is nost set");
            return;
        }
        listener.getLogger().println("Loading slave statistic");
        Result r = run.getResult();
        Computer computer = run.getExecutor().getOwner();
        SlaveBuildFailureStatistic slaveStatistic = getSlaveStatistic(computer.getDisplayName());
        if(r.equals(Result.FAILURE) || r.equals(Result.ABORTED)){
            slaveStatistic.failure(run.getParent().getDisplayName(),run.getUrl());
            if(slaveStatistic.getNumberOfFailuresInRow()>=descriptor.getSettings().getNumberOfFailureInRow()){
                try {
                    handleUnreliableSlave(computer, descriptor.getSettings(), slaveStatistic);
                } catch (IOException ex) {
                    Logger.getLogger(BuildStatisticListener.class.getName()).log(Level.SEVERE, null, ex);
                } catch (InterruptedException ex) {
                    Logger.getLogger(BuildStatisticListener.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        if(r.equals(Result.SUCCESS))
            slaveStatistic.success();
        listener.getLogger().println("Slave statistic loaded");
    }
    
    
    private void sendOffline(String reason, Computer computer,SlaveBuildFailureStatistic slaveStatistic, UnreliableSlaveDetection settings, boolean notify){       
        if(notify){
            try {
                sendNotification(settings, computer, slaveStatistic);
                reason = reason + " A notification was sent.";
            } catch (MessagingException ex) {
                reason = reason + " Sending of notification fails.";
                Logger.getLogger(BuildStatisticListener.class.getName()).log(Level.INFO, "");
            }
        }
        final String message = reason;
        OfflineCause cause = new OfflineCause(){
                public String toString(){
                    return message;
                }
        };
        computer.setTemporarilyOffline(true, cause);
        slaveStatistic.putOffline();
    }
    
    private void sendNotification(UnreliableSlaveDetection settings, Computer computer, SlaveBuildFailureStatistic slaveStatistic) throws MessagingException{
        MimeMessage msg = new MimeMessage(Mailer.descriptor().createSession());
        msg.setSubject("Unreliable-slave-detection-plugin: " + computer.getDisplayName() + " went to offline due to a lot of failed jobs", "utf-8");
        StringBuilder buffer = new StringBuilder();
        buffer.append("Slave " + computer.getDisplayName() + " (" + Util.encode(Hudson.getInstance().getRootUrl() + computer.getUrl()) + ") " 
            + " was marked as temporary offline, because there is more than " + settings.getNumberOfFailureInRow() 
            + " jobs failed in a row and reconnection of the slave does not help.\n\n");
        buffer.append("Failed jobs:\n");
        for(String jobName: slaveStatistic.getFailedJobs().keySet()){
            buffer.append(jobName + " " + Util.encode(Hudson.getInstance().getRootUrl() + slaveStatistic.getFailedJobs().get(jobName)) + "\n");
        }
        msg.setText(buffer.toString(), "utf-8");
        msg.setFrom(new InternetAddress(Mailer.descriptor().getAdminAddress()));
        msg.setSentDate(new Date());
        msg.setRecipients(Message.RecipientType.TO, settings.getAddresses());
        Transport.send(msg); 
        msg.getMessageID();
    }
    
    public void handleUnreliableSlave(Computer computer, UnreliableSlaveDetection settings, SlaveBuildFailureStatistic slaveStatistic) throws IOException, InterruptedException{
        if(!(computer instanceof SlaveComputer)){
            return;
        }
        if(slaveStatistic.wasReconnected()){
            String notification = "";
            boolean notify = DetectionUtil.isAdminMailSet() && DetectionUtil.isStmpServerSet();
            if(!notify)
                notification = "Notification could not be send because Jenkins notification is not set properly.";
            
            sendOffline("More than " + settings.getNumberOfFailureInRow() + 
                        " different jobs failed and reconnecting a slave did not help." + notification,
                        computer, slaveStatistic, settings, notify);
        }
        else{
            ((SlaveComputer) computer).tryReconnect();
            slaveStatistic.reconnect();
        }
    }
    
    public SlaveBuildFailureStatistic getSlaveStatistic(String computerName){
         SlaveBuildFailureStatistic slaveStatistic = null;
        for(SlaveBuildFailureStatistic s : statistics){
            if(s.getSlaveName().equals(computerName)){
                slaveStatistic = s;
            }
        }
        if(slaveStatistic==null){
            slaveStatistic = new SlaveBuildFailureStatistic(computerName);
            statistics.add(slaveStatistic);
        }
        return slaveStatistic;
    }

}

