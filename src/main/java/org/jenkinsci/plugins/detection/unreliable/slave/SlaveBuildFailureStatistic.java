/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jenkinsci.plugins.detection.unreliable.slave;

import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author lucinka
 */
public class SlaveBuildFailureStatistic {
    
    private String slaveName;
    
    private Map<String,String> failedJobsInRow = new TreeMap<String,String>();
    
    private boolean reconnectedAndNotSuccess;
    
    public SlaveBuildFailureStatistic(String slaveName){
        this.slaveName = slaveName;
    }
    
    public void failure(String jobName, String buildUrl){
        failedJobsInRow.put(jobName, buildUrl);
    }
    
    public Map<String,String> getFailedJobs(){
        System.out.println("failed jobs (get): " + failedJobsInRow);
        return failedJobsInRow;
    }
    
    public String getSlaveName(){
        return slaveName;
    }
    
    @Override
    public boolean equals(Object o){
        if(o instanceof SlaveBuildFailureStatistic && o!=null){
            return ((SlaveBuildFailureStatistic)o).getSlaveName().equals(slaveName);
        }
        return false;
    }
    
    public boolean wasReconnected(){
        return reconnectedAndNotSuccess;
    }
    
    public int getNumberOfFailuresInRow(){
        return failedJobsInRow.keySet().size();
    }
    
    public void success(){
        resetStatistic();
        reconnectedAndNotSuccess = false;
        System.out.println("failed jobs (success): " + failedJobsInRow);
    }
    
    public void putOffline(){
        resetStatistic();
        reconnectedAndNotSuccess = false;
        System.out.println("failed jobs (putOffline): " + failedJobsInRow);
    }
    
    public void reconnect(){
        resetStatistic();
        reconnectedAndNotSuccess = true;
        System.out.println("failed jobs (reconnect): " + failedJobsInRow);
    }
    
    protected void resetStatistic(){
        failedJobsInRow.clear();
    }
}
