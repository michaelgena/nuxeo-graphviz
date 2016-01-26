/**
 * 
 */

package org.nuxeo.graphviz;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.CodeSource;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.Environment;
import org.nuxeo.connect.data.DownloadablePackage;
import org.nuxeo.connect.packages.PackageManager;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.commandline.executor.service.CommandLineExecutorComponent;
import org.nuxeo.jaxb.Component;
import org.nuxeo.jaxb.Component.Extension;
import org.nuxeo.jaxb.Component.Extension.Action;
import org.nuxeo.jaxb.Component.Extension.Chain;
import org.nuxeo.jaxb.Component.Extension.Handler;
import org.nuxeo.runtime.api.Framework;

/**
 * @author mgena
 */
@Operation(id=GenerateGraph.ID, category=Constants.CAT_EXECUTION, label="GenerateGraph", description="")
public class GenerateGraph {

    public static final String ID = "GenerateGraph";
    private Log logger = LogFactory.getLog(GenerateGraph.class);
    public static final String SNAPSHOT_SUFFIX = "0.0.0-SNAPSHOT";
    public static final String EXTENSIONPOINT_CHAIN = "chain";
    public static final String EXTENSIONPOINT_EVENT_HANDLERS = "event-handlers";
    public static final String EXTENSIONPOINT_ACTIONS = "actions";

    
    @OperationMethod
    public Blob run() {
           	
    	String studioJar = "";
    	String result = "";
    	String url = "";
    	CommandLineExecutorComponent commandLineExecutorComponent = new CommandLineExecutorComponent();
    	String nuxeoHomePath = Environment.getDefault().getServerHome().getAbsolutePath();
    	    	
    	try {
	      JAXBContext jc = JAXBContext.newInstance("org.nuxeo.jaxb");
	      Unmarshaller unmarshaller = jc.createUnmarshaller();
	      //unmarshaller.setValidating(true);
	      
	      PackageManager pm = Framework.getLocalService(PackageManager.class);
	      List<DownloadablePackage> pkgs = pm.listRemoteAssociatedStudioPackages();
	      DownloadablePackage snapshotPkg = getSnapshot(pkgs);
	      String studioPackage = "";
	      if (snapshotPkg != null) {
	    	  studioPackage = snapshotPkg.getId();
	    	  studioJar = studioPackage.replace("-0.0.0-SNAPSHOT", "")+".jar";
	      } else {
	    	  logger.info("No Studio Package found.");
	      }
	      	      
	      CodeSource src = Framework.class.getProtectionDomain().getCodeSource();
	      if (src != null) {
	    	url = src.getLocation().toString();
	    	String path[] = url.split("/");
	    	url = url.replace(path[path.length-1], studioJar);	
	    	url = url.replace("file:","");
	        logger.error("Studio jar path ["+url+"]");
	        CmdParameters params2 = new CmdParameters();
	        params2.addNamedParameter("studioJar", url);
	        params2.addNamedParameter("dest", nuxeoHomePath+File.separator+"GraphViz"+File.separator+studioJar);
	        commandLineExecutorComponent.execCommand("copy-studio-jar", params2);
	        
	        
	        CmdParameters params = new CmdParameters();
	        params.addNamedParameter("dir", nuxeoHomePath+File.separator+"GraphViz");
	        params.addNamedParameter("studioJar", nuxeoHomePath+File.separator+"GraphViz"+File.separator+studioJar);
	        commandLineExecutorComponent.execCommand("extract-studio-xml", params);
	      	
	        //TODO Extract the OSGI-INF/extensions.xml
	     } 
	      	  
	    Component component = (Component) unmarshaller.unmarshal(new File(nuxeoHomePath+File.separator+"GraphViz"+File.separator+"OSGI-INF"+File.separator+"extensions.xml"));
	    
	    result = "digraph G {\n";
	    List<Extension> extensions = component.getExtension();
	    String pattern = "\\#\\{operationActionBean.doOperation\\('(.*)'\\)\\}";
	    // Create a Pattern object
	    Pattern r = Pattern.compile(pattern);
	    
	    for(Extension extension:extensions){
	    	String point = extension.getPoint();
	    	switch (point){
	    		case EXTENSIONPOINT_ACTIONS : 
	    			try{
	    				List<Action> actions = extension.getAction();
	    				for(Action action:actions){
	    					String chainId = "";
	    					try{
	    						chainId = action.getLink();
	    						// Now create matcher object.
	    					    Matcher m = r.matcher(chainId);
	    					    if (m.find( )) {
	    					    	chainId = m.group(1);
	    					    }
	    					}catch(Exception e){
	    						logger.error("Error when getting chainId", e);
	    					}
	    					String cleanedActionId = cleanUpForDot(action.getId());
    						
	    					if(chainId != null && !("").equals(chainId) && !(".").equals(chainId)){
	    						String cleanedChainId = cleanUpForDot(chainId);
	    						result += cleanedChainId + " [label=\""+action.getId()+"\",shape=box,fillcolor=\"#28A3C7\",style=\"filled\"];\n";
	    						result += cleanedActionId+" -> "+cleanedChainId;
	    					}
	    					result += cleanedActionId+" [label=\""+action.getId()+"\",shape=box,fillcolor=\"#00ADFF\",style=\"filled\"];\n";
	    						    						    					
	    				}
	    			}catch(Exception e){
	    				logger.error("Error when getting Actions", e);
	    			}
	    			break;
	    		case EXTENSIONPOINT_CHAIN :
	    			try{
	    				List<Chain> chains = extension.getChain();
	    				for(Chain chain:chains){
	    					chain.getId();
	    					result += cleanUpForDot(chain.getId())+ " [label=\""+chain.getDescription()+"\",shape=box,fillcolor=\"#28A3C7\",style=\"filled\"];\n";	    											    					
	    				}
	    			}catch(Exception e){
	    				logger.error("Error when getting Chains", e);
	    			}
	    			break;	    		
	    		case EXTENSIONPOINT_EVENT_HANDLERS : 
	    			try{
	    				List<Handler> handlers = extension.getHandler();
	    				for(Handler handler:handlers){
	    					handler.getChainId();
	    					result += cleanUpForDot(handler.getChainId())+"_handler"+" -> "+cleanUpForDot(handler.getChainId())+";\n";
	    					result += cleanUpForDot(handler.getChainId())+"_handler"+ " [label=\""+handler.getChainId()+"_handler\",shape=box,fillcolor=\"#FF462A\",style=\"filled\"];\n";	    											    					
	    				}
	    			}catch(Exception e){
	    				logger.error("Error when getting Chains", e);
	    			}
	    			break;
	    	}
	    }
    	result += "}";
    	//hack to remove unwanted characters
    	
    	
    	logger.error("result ["+result+"]");
	    writeToFile(nuxeoHomePath+File.separator+"GraphViz"+File.separator+File.separator+"input.dot", result);
	        
	    CmdParameters parameters = new CmdParameters();
	    		    
	    parameters.addNamedParameter("inputFile", nuxeoHomePath+File.separator+"GraphViz"+File.separator+"input.dot");
	    parameters.addNamedParameter("outputFile", nuxeoHomePath+File.separator+"nxserver"+File.separator+"nuxeo.war"+File.separator+"graphviz"+File.separator+"img.png");

	    logger.error("Before running command line");
	    
	    commandLineExecutorComponent.execCommand("dot", parameters);
	    logger.error("After running command line");
	        	        
	  
	    } catch (Exception e) {
	      logger.error(e);
	    }

    	return new StringBlob(result); 
    	
    }   
    public static boolean isSnapshot(DownloadablePackage pkg) {
		return ((pkg.getVersion() != null) && (pkg.getVersion().toString().endsWith("0.0.0-SNAPSHOT")));
	}

	public static DownloadablePackage getSnapshot(List<DownloadablePackage> pkgs) {
		for (DownloadablePackage pkg : pkgs) {
			if (isSnapshot(pkg)) {
				return pkg;
			}
		}
		return null;
	}
	
	 public void writeToFile(String path, String content) {
			FileOutputStream fop = null;
			File file;
			try {
				file = new File(path);
				fop = new FileOutputStream(file);

				// if file doesnt exists, then create it
				if (!file.exists()) {
					file.createNewFile();
				}
				// get the content in bytes
				byte[] contentInBytes = content.getBytes();

				fop.write(contentInBytes);
				fop.flush();
				fop.close();

			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					if (fop != null) {
						fop.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} 

	 public String cleanUpForDot(String content){
		 content = content.replaceAll("\\.", "");
		 content = content.replaceAll("\\/", "");
		 content = content.replaceAll("\\-", "_");
		 //content = content.replaceAll(".", "");
		 return content;
	 }
}
