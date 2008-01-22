package gov.loc.repository.transfer.components.fileexamination;

import gov.loc.repository.packagemodeler.agents.Agent;
import gov.loc.repository.packagemodeler.packge.FileLocation;
import gov.loc.repository.transfer.components.Component;
import gov.loc.repository.transfer.components.ModelerAware;
import gov.loc.repository.transfer.components.annotations.JobType;
import gov.loc.repository.transfer.components.annotations.MapParameter;

public interface LCManifestVerifier extends Component, ModelerAware {
	
	public static final String COMPONENT_NAME = "lcmanifestverifier";
		
	/*
	 * Checks the LC Manifest against the files at the FileLocation.
	 * A VerifyAgainstManifestEvent is recorded.
	 * The FileLocation must be LC package structured so that the LC Manifest can be located.
	 * @return true if the verification is successful. 
	 */
	@JobType(name="verifylcmanifest")	
	public void verify(
			@MapParameter(name="filelocationkey") long fileLocationKey,
			@MapParameter(name="mountpath") String mountPath,
			@MapParameter(name="requestingagentid") String requestingAgentId)
			throws Exception;
	
	public void verify(FileLocation fileLocation, String mountPath, Agent requestingAgent) throws Exception;
	
}