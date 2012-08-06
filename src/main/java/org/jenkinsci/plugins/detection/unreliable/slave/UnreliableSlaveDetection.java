package org.jenkinsci.plugins.detection.unreliable.slave;


import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

    
/**
 *
 * @author lucinka
 */
public class UnreliableSlaveDetection implements Describable<UnreliableSlaveDetection>{
    
    private InternetAddress[] addresses;
    private int numberOfFailureInRow;
    
    public UnreliableSlaveDetection(String addresses, int numberOfFailureInRow){
        parseAddresses(addresses);
        this.numberOfFailureInRow = numberOfFailureInRow;
    }
    
    @DataBoundConstructor
    public UnreliableSlaveDetection(String addresses, int tmpLow, int workspaceLow,int numberOfFailureInRow){
        parseAddresses(addresses);
        this.numberOfFailureInRow = numberOfFailureInRow;
    }

    public UnreliableSlaveDetection(){
        
    }
    
    private boolean parseAddresses(String token){
        try{
            addresses = InternetAddress.parse(token);
            return true;
        }
        catch(AddressException e){
            return false;
        }
    }
    
    public InternetAddress [] getRecipientAddresses(){
        return addresses;
    }
    
    public String getAddresses(){
        StringBuilder builder = new StringBuilder();
        for(InternetAddress address: addresses){
            builder.append(address.getAddress());
            builder.append(", ");
        }
        builder.deleteCharAt(builder.lastIndexOf(","));
        return builder.toString();
    }
    
    public int getNumberOfFailureInRow(){
        return numberOfFailureInRow;
    }

   
    public Descriptor<UnreliableSlaveDetection> getDescriptor() {
        return new DescriptorImpl();
    }
    
    @Extension
    public static class DescriptorImpl extends Descriptor<UnreliableSlaveDetection>{
        private UnreliableSlaveDetection settings = new UnreliableSlaveDetection();
        
        public DescriptorImpl(){
            load();
        }
        
        public UnreliableSlaveDetection getSettings(){         
            return settings;
        }
      
        @Override
        public String getDisplayName() {
            return "Unreliable slave plugin";
        }      
        
        public FormValidation doCheckAddresses(@QueryParameter String value) {
          if(settings.parseAddresses(value))
              return FormValidation.ok();
         return FormValidation.error("Please enter valid address");          
        }
        
        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            settings =  req.bindJSON(UnreliableSlaveDetection.class, json);
            save();
            return true;
        }
        
    }
}
