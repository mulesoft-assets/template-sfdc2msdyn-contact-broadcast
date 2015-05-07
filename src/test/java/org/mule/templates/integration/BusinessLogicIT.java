/**
 * Mule Anypoint Template
 * Copyright (c) MuleSoft, Inc.
 * All rights reserved.  http://www.mulesoft.com
 */

package org.mule.templates.integration;

import static org.junit.Assert.assertEquals;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.context.notification.NotificationException;
import org.mule.processor.chain.SubflowInterceptingChainLifecycleWrapper;
import org.mule.templates.builders.SfdcObjectBuilder;

import com.mulesoft.module.batch.BatchTestHelper;
import com.sforce.soap.partner.SaveResult;

/**
 * The objective of this class is to validate the correct behavior of the Mule
 * Template that make calls to external systems.
 * 
 * The test will update the SFDC test account, then run the poller 
 * and finally check that the corresponding customer in Workday is
 * correctly updated.
 */
public class BusinessLogicIT extends AbstractTemplateTestCase {

	private static final String TEST_DESCRIPTION = "Testing description";
	private static final String TEST_FAX = "5556666";
	private static final String TEST_PHONE = "1234567";
	private static final String PATH_TO_TEST_PROPERTIES = "./src/test/resources/mule.test.properties";
	private static final int TIMEOUT_SECONDS = 300;
	private final String name = generateUniqueName();
	private final String email = generateUniqueEmail();
	private SubflowInterceptingChainLifecycleWrapper retrieveContactFromMSDYNFlow;
	private SubflowInterceptingChainLifecycleWrapper deleteContactInMSDYNFlow;
	private SubflowInterceptingChainLifecycleWrapper createContactinSFDCFlow;
	private SubflowInterceptingChainLifecycleWrapper deleteContactInSFDCFlow;
	private BatchTestHelper helper;
	
	private static List<String> contactsCreatedInSFDC = new ArrayList<String>();
	private static List<String> contactsCreatedInMSDYN = new ArrayList<String>();

	@BeforeClass
    public static void beforeTestClass() {
        System.setProperty("poll.startDelayMillis", "8000");
        System.setProperty("poll.frequencyMillis", "30000");
        System.setProperty("page.size", "100");
        System.setProperty("watermark.default.expression","#[groovy: new Date(System.currentTimeMillis() - 100000).format(\"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'\", TimeZone.getTimeZone('UTC'))]");
    }
	
	/**
	 * Sets up the test prerequisites.
	 * 
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {
		helper = new BatchTestHelper(muleContext);

		final Properties props = new Properties();
		try {
			props.load(new FileInputStream(PATH_TO_TEST_PROPERTIES));
		} catch (Exception e) {
			logger.error("Error occured while reading mule.test.properties", e);
		}
		stopFlowSchedulers(POLL_FLOW_NAME);
		registerListeners();

		retrieveContactFromMSDYNFlow = getSubFlow("retrieveContactFromMSDYNFlow");
		retrieveContactFromMSDYNFlow.initialise();
		deleteContactInMSDYNFlow = getSubFlow("deleteContactInMSDYNFlow");
		deleteContactInMSDYNFlow.initialise();
		createContactinSFDCFlow = getSubFlow("createContactinSFDCFlow");
		createContactinSFDCFlow.initialise();
		deleteContactInSFDCFlow = getSubFlow("deleteContactInSFDCFlow");
		deleteContactInSFDCFlow.initialise();			
	}
    
    private void registerListeners() throws NotificationException {
		muleContext.registerListener(pipelineListener);
	}

	/**
	 * Creates Contact in Salesforce.
	 * 
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public void createTestDataInSandbox() throws Exception {
		// create test contact in SFDC
		Map<String, Object> contact = SfdcObjectBuilder.aContact()
				.with("LastName", name)
				.with("Phone", TEST_PHONE)
				.with("Fax", TEST_FAX)
				.with("Description", TEST_DESCRIPTION)
				.with("Email", email).build();
		List<Map<String, Object>> payload = new ArrayList<>();
		payload.add(contact);
		
		final List<SaveResult> payloadAfterExecution = (List<SaveResult>) createContactinSFDCFlow.process(getTestEvent(payload, MessageExchangePattern.REQUEST_RESPONSE)).getMessage().getPayload();
		contactsCreatedInSFDC.add(payloadAfterExecution.get(0).getId());
	}

	/**
	 * Tests if creation of a Salesforce test Contact results in MS Dynamics Contact creation
	 * 
	 * @throws Exception
	 */
	@Test
	public void testMainFlow() throws Exception {
		// create test data
		createTestDataInSandbox();
				
		Thread.sleep(10000);
		// run the main migration flow
		runSchedulersOnce(POLL_FLOW_NAME);
		waitForPollToRun();
		helper.awaitJobTermination(TIMEOUT_SECONDS * 1000, 500);
		helper.assertJobWasSuccessful();

		MuleEvent event = retrieveContactFromMSDYNFlow.process(getTestEvent(
				name, MessageExchangePattern.REQUEST_RESPONSE));

		Iterator<?> response = (Iterator<?>) event.getMessage().getPayload();
		Map<?,?> contactFromMsdyn = (Map<?,?>) (response.hasNext() ? response.next() : null);
		
		contactsCreatedInMSDYN.add(contactFromMsdyn.get("contactid").toString());
		
		//check fax, phone, description and email
		assertEquals("The emails should be the same", email, contactFromMsdyn.get("emailaddress1"));
		assertEquals("The emails should be the same", TEST_DESCRIPTION, contactFromMsdyn.get("description"));
		assertEquals("The emails should be the same", TEST_FAX, contactFromMsdyn.get("fax"));
		assertEquals("The emails should be the same", TEST_PHONE, contactFromMsdyn.get("telephone1"));
	}
	
	@After
	public void tearDown() throws MuleException, Exception {
		deleteTestData();
	}

	private void deleteTestData() throws MuleException, Exception {
		final List<String> idList = new ArrayList<String>();
		for (String contact : contactsCreatedInSFDC) {
			idList.add(contact);
		}
		deleteContactInSFDCFlow.process(getTestEvent(idList, MessageExchangePattern.REQUEST_RESPONSE));
		idList.clear();
		
		for (String contact : contactsCreatedInMSDYN) {
			idList.add(contact);
		}
		deleteContactInMSDYNFlow.process(getTestEvent(idList,MessageExchangePattern.REQUEST_RESPONSE));
		idList.clear();
		
		contactsCreatedInMSDYN.clear();
		contactsCreatedInSFDC.clear();
	}
}
