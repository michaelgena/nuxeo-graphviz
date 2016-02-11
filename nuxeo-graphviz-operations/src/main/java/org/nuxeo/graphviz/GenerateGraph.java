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
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.io.FileUtils;
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
import org.nuxeo.ecm.platform.commandline.executor.api.CommandNotAvailable;
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
    public static final String EXTENSIONPOINT_CHAIN = "chains";
    public static final String EXTENSIONPOINT_EVENT_HANDLERS = "event-handlers";
    public static final String EXTENSIONPOINT_ACTIONS = "actions";

    @OperationMethod
    public Blob run() {           	
    	String studioJar = "";
    	String map = "";
    	String url = "";
    	CommandLineExecutorComponent commandLineExecutorComponent = new CommandLineExecutorComponent();
    	String nuxeoHomePath = Environment.getDefault().getServerHome().getAbsolutePath();	
    	try {		      
		    studioJar = getStudioJar();
		    
		    //build the studio jar path
		    CodeSource src = Framework.class.getProtectionDomain().getCodeSource();
		    if (src != null) {
		    	url = src.getLocation().toString();
		    	String path[] = url.split("/");
		    	url = url.replace(path[path.length-1], studioJar);	
		    	url = url.replace("file:","");
		    } 
		    
		    copyStudioJar(url, studioJar, nuxeoHomePath, commandLineExecutorComponent);
		    String graphVizFolderPath = nuxeoHomePath+File.separator+"GraphViz";
		    extractXMLFromStudioJar(studioJar, graphVizFolderPath);
		    String studioProjectName = studioJar.replace(".jar", "");
		    String destinationPath = nuxeoHomePath+File.separator+"nxserver"+File.separator+"nuxeo.war"+File.separator+"graphviz";
		    map = generateGraphFromXML(studioProjectName, destinationPath, graphVizFolderPath, commandLineExecutorComponent);
	    } catch (Exception e) {
	      logger.error("Exception while ",e);
	    }
    	return new StringBlob(map);     	
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
			logger.error("Error while writing into file ", e);
		} finally {
			try {
				if (fop != null) {
					fop.close();
				}
			} catch (IOException e) {
				logger.error("Error while writing into file ", e);
			}
		}
	} 

	public static String cleanUpForDot(String content){
		 content = content.replaceAll("\\.", "");
		 content = content.replaceAll("\\/", "");
		 content = content.replaceAll("\\-", "_");
		 //content = content.replaceAll(".", "");
		 return content;
	}
	 
	public String getStudioJar(){
		String studioJar = "";
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
	    return studioJar;
	}
	 
	public void copyStudioJar(String url, String studioJar, String nuxeoHomePath, CommandLineExecutorComponent commandLineExecutorComponent) throws CommandNotAvailable, IOException{
    	//Create the GraphViz folder if it doesn't exist
    	File dir = new File(nuxeoHomePath+File.separator+"GraphViz");
    	if(!dir.exists()) { 
    		try{
    			dir.mkdir();
    	    } 
    	    catch(SecurityException se){
    	       logger.error("Error while creating the directory [GraphViz]", se);
    	    }
    	}
    	
        CmdParameters params2 = new CmdParameters();
        params2.addNamedParameter("studioJar", url);
        params2.addNamedParameter("dest", nuxeoHomePath+File.separator+"GraphViz"+File.separator+studioJar);
        commandLineExecutorComponent.execCommand("copy-studio-jar", params2);	        		       	    
	}
	 
	public void extractXMLFromStudioJar(String studioJar, String graphVizFolderPath) throws CommandNotAvailable, IOException{
		Runtime rt = Runtime.getRuntime();
	    String[] cmd = { "/bin/sh", "-c", "cd "+graphVizFolderPath+"; jar xf "+studioJar };
	    rt.exec(cmd);
	}
	 
	public String generateGraphFromXML(String studioProjectName, String destinationPath, String graphVizFolderPath, CommandLineExecutorComponent commandLineExecutorComponent) throws JAXBException, CommandNotAvailable, IOException{
		JAXBContext jc = JAXBContext.newInstance("org.nuxeo.jaxb");
		Unmarshaller unmarshaller = jc.createUnmarshaller();
		String result = "";
		String map = "";
		Component component = (Component) unmarshaller.unmarshal(new File(graphVizFolderPath+File.separator+"OSGI-INF"+File.separator+"extensions.xml"));
		    
		String rank = "subgraph entryPoint {\n"+
		    			  "		rank=\"same\";\n";
		String rankChain = "";    
		result = "digraph G {\nrankdir=\"LR\";\n"+
		    "graph [fontname = \"helvetica\", fontsize=11];\n"+
		    "node [fontname = \"helvetica\", fontsize=11];\n"+
		    "edge [fontname = \"helvetica\", fontsize=11];\n";
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
		    					if(chainId == null || chainId.startsWith("/")){
		    						continue;
		    					}
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
		    					String refChainId = chainId.startsWith("javascript.")? chainId.replace("javascript.", "")+".scriptedOperation" : chainId+".ops";
		    					result += cleanedChainId + " [URL=\"https://connect.nuxeo.com/nuxeo/site/studio/ide?project="+studioProjectName+"#@feature:"+refChainId+"\", label=\""+chainId+"\",shape=box,fontcolor=white,color=\"#28A3C7\",fillcolor=\"#28A3C7\",style=\"filled\"];\n";  						
		    					result += cleanedActionId+" -> "+cleanedChainId+";\n";
		    				}
		    				
		    				result += cleanedActionId+" [URL=\"https://connect.nuxeo.com/nuxeo/site/studio/ide?project="+studioProjectName+"#@feature:"+action.getId()+".action\", label=\""+action.getId()+"\n"+(action.getLabel()!= null ? action.getLabel():"")+"\",shape=box,fontcolor=white,color=\"#00ADFF\",fillcolor=\"#00ADFF\",style=\"filled\"];\n";
		    				rank += cleanedActionId+";";	    						    					
		    			}
		    		}catch(Exception e){
		    			logger.error("Error when getting Actions", e);
		    		}
		    		break;
		    	case EXTENSIONPOINT_CHAIN :
		    		try{
		    			List<Chain> chains = extension.getChain();
		    			for(Chain chain:chains){
		    				String chainId = chain.getId();
		    				String refChainId = chainId.startsWith("javascript.")? chainId.replace("javascript.", "")+".scriptedOperation" : chainId+".ops";
		    				logger.error("chain description "+chain.getDescription());
	    					result += cleanUpForDot(chain.getId()) + " [URL=\"https://connect.nuxeo.com/nuxeo/site/studio/ide?project="+studioProjectName+"#@feature:"+refChainId+"\", label=\""+chainId+"\n"+(chain.getDescription() != null ? chain.getDescription():"")+"\",shape=box,fontcolor=white,color=\"#28A3C7\",fillcolor=\"#28A3C7\",style=\"filled\"];\n";  						
	    					
		    				//handle the link between Automation chains
	    					if(chain.getOperation() != null){
	    						for(org.nuxeo.jaxb.Component.Extension.Chain.Operation operation:chain.getOperation()){
	    							if(("RunOperation").equals(operation.getId())){
	    								for(org.nuxeo.jaxb.Component.Extension.Chain.Operation.Param param : operation.getParam()){
	    									if(("id").equals(param.getName())){
	    										
	    										result += cleanUpForDot(chain.getId())+" -> "+cleanUpForDot(param.getValue())+";\n";
	    									}
	    								}
	    							}
	    						}
	    					}
	    					rankChain += cleanUpForDot(chain.getId())+";";
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
	    					
	    					result += cleanUpForDot(handler.getChainId())+"_handler"+ " [label=\""+handler.getChainId()+"_handler\",shape=box,fontcolor=white,color=\"#FF462A\",fillcolor=\"#FF462A\",style=\"filled\"];\n";
	    					result += cleanUpForDot(handler.getChainId())+ " [label=\""+handler.getChainId()+"\",shape=box,fontcolor=white,color=\"#28A3C7\",fillcolor=\"#28A3C7\",style=\"filled\"];\n";
	    					result += cleanUpForDot(handler.getChainId())+"_handler"+" -> "+cleanUpForDot(handler.getChainId())+";\n";
	    					rank += cleanUpForDot(handler.getChainId())+"_handler;";
	    				}
	    			}catch(Exception e){
	    				logger.error("Error when getting Chains", e);
	    			}
	    			break;
	    	}
	    }
	    result += rank+"\n"+rankChain+"\n}\n";
    	result += "}";
	
	    writeToFile(graphVizFolderPath+File.separator+File.separator+"input.dot", result);
		        
	    CmdParameters parameters = new CmdParameters();
		    
	    //Generate png from dot
	    parameters.addNamedParameter("inputFile", graphVizFolderPath+File.separator+"input.dot");
	    parameters.addNamedParameter("format", "png");
	    parameters.addNamedParameter("outputFile", destinationPath+File.separator+"img.png");
	    commandLineExecutorComponent.execCommand("dot", parameters);
		    
	    //Generate map from dot
	    parameters.addNamedParameter("format", "cmapx");
	    parameters.addNamedParameter("outputFile", destinationPath+File.separator+"img.cmapx");
	    commandLineExecutorComponent.execCommand("dot", parameters);
	    map = FileUtils.readFileToString(new File(destinationPath+File.separator+"img.cmapx"));
	    return map;
	}
	
	public static void main(String[] args){
		System.out.println("ddd");
		String graphVizFolderPath = "/Users/mgena/Documents/GraphViz/nuxeo-cap-8.1-tomcat/GraphViz";
		String studioProjectName = "mgena-SANDBOX";
		try{
		JAXBContext jc = JAXBContext.newInstance("org.nuxeo.jaxb");
		Unmarshaller unmarshaller = jc.createUnmarshaller();
		String result = "";
		String map = "";
		Component component = (Component) unmarshaller.unmarshal(new File("/Users/mgena/Documents/GraphViz/nuxeo-cap-8.1-tomcat/GraphViz/OSGI-INF/extensions.xml"));
		
		String rank = "subgraph entryPoint {\n"+
		    			  "		rank=\"same\";\n";
		    
		result = "digraph G {\nrankdir=\"LR\";\n"+
		    "graph [fontname = \"helvetica\", fontsize=11];\n"+
		    "node [fontname = \"helvetica\", fontsize=11];\n"+
		    "edge [fontname = \"helvetica\", fontsize=11];\n";
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
		    					if(chainId == null || chainId.startsWith("/")){
		    						continue;
		    					}
		    					// Now create matcher object.		    						
		    				    Matcher m = r.matcher(chainId);
		    				    if (m.find( )) {
		    				    	chainId = m.group(1);
		    				    }
		    				}catch(Exception e){
		    					e.printStackTrace();
		    				}
		    				String cleanedActionId = cleanUpForDot(action.getId());
	    						
		    				if(chainId != null && !("").equals(chainId) && !(".").equals(chainId)){
		    					String cleanedChainId = cleanUpForDot(chainId);
		    					String refChainId = chainId.startsWith("javascript.")? chainId.replace("javascript.", "")+".scriptedOperation" : chainId+".ops";
		    					result += cleanedChainId + " [URL=\"https://connect.nuxeo.com/nuxeo/site/studio/ide?project="+studioProjectName+"#@feature:"+refChainId+"\", label=\""+chainId+"\",shape=box,fontcolor=white,color=\"#28A3C7\",fillcolor=\"#28A3C7\",style=\"filled\"];\n";  						
		    					result += cleanedActionId+" -> "+cleanedChainId+";\n";
		    				}
		    				
		    				result += cleanedActionId+" [URL=\"https://connect.nuxeo.com/nuxeo/site/studio/ide?project="+studioProjectName+"#@feature:"+action.getId()+".action\", label=\""+action.getId()+"\n"+(action.getLabel()!= null ? action.getLabel():"")+"\",shape=box,fontcolor=white,color=\"#00ADFF\",fillcolor=\"#00ADFF\",style=\"filled\"];\n";
		    				rank += cleanedActionId+";";	    						    					
		    			}
		    		}catch(Exception e){
		    			e.printStackTrace();
		    		}
		    		break;
		    	case EXTENSIONPOINT_CHAIN :
		    		try{
		    			List<Chain> chains = extension.getChain();
		    			for(Chain chain:chains){
		    				String chainId = chain.getId();
		    				String refChainId = chainId.startsWith("javascript.")? chainId.replace("javascript.", "")+".scriptedOperation" : chainId+".ops";
		    				System.out.println("chain description "+chain.getDescription());
	    					result += cleanUpForDot(chain.getId()) + " [URL=\"https://connect.nuxeo.com/nuxeo/site/studio/ide?project="+studioProjectName+"#@feature:"+refChainId+"\", label=\""+chainId+"\n"+chain.getDescription()+"\",shape=box,fontcolor=white,color=\"#28A3C7\",fillcolor=\"#28A3C7\",style=\"filled\"];\n";  						
	    					
		    				//handle the link between Automation chains
	    					if(chain.getOperation() != null){
	    						for(org.nuxeo.jaxb.Component.Extension.Chain.Operation operation:chain.getOperation()){
	    							if(("RunOperation").equals(operation.getId())){
	    								for(org.nuxeo.jaxb.Component.Extension.Chain.Operation.Param param : operation.getParam()){
	    									if(("id").equals(param.getName())){
	    										
	    										result += cleanUpForDot(chain.getId())+" -> "+cleanUpForDot(param.getValue())+";\n";
	    									}
	    								}
	    							}
	    						}
	    					}
	    				
	    					//result += cleanUpForDot(chain.getId())+ " [label=\""+chain.getDescription()+"\",shape=box,fontcolor=white,color=\"#28A3C7\",fillcolor=\"#28A3C7\",style=\"filled\"];\n";	    											    					
	    				}
	    			}catch(Exception e){
	    				e.printStackTrace();
	    			}
	    			break;	    		
	    		case EXTENSIONPOINT_EVENT_HANDLERS : 
	    			try{
	    				List<Handler> handlers = extension.getHandler();
	    				for(Handler handler:handlers){
	    					handler.getChainId();
	    					
	    					result += cleanUpForDot(handler.getChainId())+"_handler"+ " [label=\""+handler.getChainId()+"_handler\",shape=box,fontcolor=white,color=\"#FF462A\",fillcolor=\"#FF462A\",style=\"filled\"];\n";
	    					result += cleanUpForDot(handler.getChainId())+ " [label=\""+handler.getChainId()+"\",shape=box,fontcolor=white,color=\"#28A3C7\",fillcolor=\"#28A3C7\",style=\"filled\"];\n";
	    					result += cleanUpForDot(handler.getChainId())+"_handler"+" -> "+cleanUpForDot(handler.getChainId())+";\n";
	    					rank += cleanUpForDot(handler.getChainId())+"_handler;";
	    				}
	    			}catch(Exception e){
	    				e.printStackTrace();
	    			}
	    			break;
	    	}
	    }
	    result += rank+"\n}\n";
    	result += "}";
    	System.out.println(result);
		}catch(Exception e){
			System.out.println(e);
		}
	}
	
}
