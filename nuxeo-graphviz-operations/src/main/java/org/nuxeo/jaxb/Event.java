package org.nuxeo.jaxb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.sun.xml.txw2.annotation.XmlElement;

/*
   <handler chainId="javascript.Asset_AboutToCreateModify">      
      <event>aboutToCreate</event>
      <event>beforeDocumentModification</event>
      <filters>
        <facet>Asset</facet>
        <attribute>Regular Document</attribute>
      </filters>
    </handler>
 */

@XmlElement(value="handler")
public class Event {
	
	String chainId;
    String event;
   
    public String getChainId() {
		return chainId;
	}
	public void setChainId(String chainId) {
		this.chainId = chainId;
	}
		   
    public String getEvent() {
		return event;
	}
	public void setEvent(String event) {
		this.event = event;
	}
	
}