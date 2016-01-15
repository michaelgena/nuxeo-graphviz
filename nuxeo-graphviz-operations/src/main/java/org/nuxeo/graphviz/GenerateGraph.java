/**
 * 
 */

package org.nuxeo.graphviz;

import java.io.File;
import java.security.CodeSource;
import java.util.List;

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
import org.nuxeo.jaxb.Bibliotheque;
import org.nuxeo.jaxb.Livre;
import org.nuxeo.runtime.api.Framework;

/**
 * @author mgena
 */
@Operation(id=GenerateGraph.ID, category=Constants.CAT_EXECUTION, label="GenerateGraph", description="")
public class GenerateGraph {

    public static final String ID = "GenerateGraph";
    private Log logger = LogFactory.getLog(GenerateGraph.class);
    public static final String SNAPSHOT_SUFFIX = "0.0.0-SNAPSHOT";
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
	        
	        CmdParameters params2 = new CmdParameters();
	        params2.addNamedParameter("studioJar", url);
	        params2.addNamedParameter("dest", nuxeoHomePath+File.separator+"nxserver"+File.separator+"nuxeo.war"+File.separator+"graphviz"+File.separator+studioJar);
	        commandLineExecutorComponent.execCommand("copy-studio-jar", params2);

	        CmdParameters params = new CmdParameters();
	        params.addNamedParameter("dir", nuxeoHomePath+File.separator+"nxserver"+File.separator+"nuxeo.war"+File.separator+"graphviz");
	        params.addNamedParameter("studioJar", nuxeoHomePath+File.separator+"nxserver"+File.separator+"nuxeo.war"+File.separator+"graphviz"+File.separator+studioJar);
	        commandLineExecutorComponent.execCommand("extract-studio-xml", params);
	      	
	        //TODO Extract the OSGI-INF/extensions.xml
	      } 
	      	  
	      Bibliotheque bibliotheque = (Bibliotheque) unmarshaller.unmarshal(new File("/Users/mgena/Documents/GraphViz/test.xml"));

	      List livres = bibliotheque.getLivre();
	      for (int i = 0; i < livres.size(); i++) {
	        Livre livre = (Livre) livres.get(i);
	        logger.info("Livre ");
	        logger.info("Titre   : " + livre.getTitre());
	        logger.info("Auteur  : " + livre.getAuteur());
	        logger.info("Editeur : " + livre.getEditeur());
	    		  
		    CmdParameters parameters = new CmdParameters();
		    		    
		    parameters.addNamedParameter("inputFile", "/Users/mgena/Documents/GraphViz/input.dot");
		    parameters.addNamedParameter("outputFile", nuxeoHomePath+File.separator+"nxserver"+File.separator+"nuxeo.war"+File.separator+"graphviz"+File.separator+"img.png");

		    logger.info("Before running command line");
		    
		    commandLineExecutorComponent.execCommand("dot", parameters);
		    logger.info("After running command line");
	        	        
	      }
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

}
