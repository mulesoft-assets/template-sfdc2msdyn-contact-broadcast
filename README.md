
# Anypoint Template: Salesforce to MS Dynamics Contact Broadcast

# License Agreement
This template is subject to the conditions of the 
<a href="https://s3.amazonaws.com/templates-examples/AnypointTemplateLicense.pdf">MuleSoft License Agreement</a>.
Review the terms of the license before downloading and using this template. You can use this template for free 
with the Mule Enterprise Edition, CloudHub, or as a trial in Anypoint Studio.

# Use Case
As a Salesforce admin I want to migrate Contacts from Salesforce to MS Dynamics.

This Template should serve as a foundation for setting an online sync of Contacts from Salesforce instance to MS Dynamics instance. Every time there is a new Contact or a change in an already existing one, the integration will poll for changes in Salesforce source instance and it will be responsible for creating or updating the Contact in the target MS Dynamics instance.

Requirements have been set not only to be used as examples, but also to establish a starting point to adapt your integration to your requirements.

As implemented, this Anypoint Template leverages the [Batch Module](http://www.mulesoft.org/documentation/display/current/Batch+Processing)
The batch job is divided in *Process* and *On Complete* stages.
The integration is triggered by a scheduler defined in the flow that is going to trigger the application, querying newest Salesforce updates/creations matching a filter criteria and executing the batch job.
During the *Process* stage, each Salesforce Contact will be checked, if it has an existing matching Contact in the MS Dynamics instance.
The last step of the *Process* stage will insert/update Contacts in MS Dynamics.
Finally during the *On Complete* stage the Template will log output statistics data into the console.

# Considerations

To make this Anypoint Template run, there are certain preconditions that must be considered. All of them deal with the preparations in both, that must be made in order for all to run smoothly. 
**Failing to do so could lead to unexpected behavior of the template.**



## Salesforce Considerations

Here's what you need to know about Salesforce to get this template to work.

### FAQ

- Where can I check that the field configuration for my Salesforce instance is the right one? See: <a href="https://help.salesforce.com/HTViewHelpDoc?id=checking_field_accessibility_for_a_particular_field.htm&language=en_US">Salesforce: Checking Field Accessibility for a Particular Field</a>
- Can I modify the Field Access Settings? How? See: <a href="https://help.salesforce.com/HTViewHelpDoc?id=modifying_field_access_settings.htm&language=en_US">Salesforce: Modifying Field Access Settings</a>

### As a Data Source

If the user who configured the template for the source system does not have at least *read only* permissions for the fields that are fetched, then an *InvalidFieldFault* API fault displays.

```
java.lang.RuntimeException: [InvalidFieldFault [ApiQueryFault [ApiFault  exceptionCode='INVALID_FIELD'
exceptionMessage='
Account.Phone, Account.Rating, Account.RecordTypeId, Account.ShippingCity
^
ERROR at Row:1:Column:486
No such column 'RecordTypeId' on entity 'Account'. If you are attempting to use a custom field, be sure to append the '__c' after the custom field name. Reference your WSDL or the describe call for the appropriate names.'
]
row='1'
column='486'
]
]
```







## Microsoft Dynamics CRM Considerations


### As a Data Destination

There are no considerations with using Microsoft Dynamics CRM as a data destination.



# Run it!
Simple steps to get Salesforce to MS Dynamics Contact Broadcast running.


## Running On Premises
In this section we help you run your template on your computer.


### Where to Download Anypoint Studio and the Mule Runtime
If you are a newcomer to Mule, here is where to get the tools.

+ [Download Anypoint Studio](https://www.mulesoft.com/platform/studio)
+ [Download Mule runtime](https://www.mulesoft.com/lp/dl/mule-esb-enterprise)


### Importing a Template into Studio
In Studio, click the Exchange X icon in the upper left of the taskbar, log in with your
Anypoint Platform credentials, search for the template, and click **Open**.


### Running on Studio
After you import your template into Anypoint Studio, follow these steps to run it:

+ Locate the properties file `mule.dev.properties`, in src/main/resources.
+ Complete all the properties required as per the examples in the "Properties to Configure" section.
+ Right click the template project folder.
+ Hover your mouse over `Run as`
+ Click `Mule Application (configure)`
+ Inside the dialog, select Environment and set the variable `mule.env` to the value `dev`
+ Click `Run`


### Running on Mule Standalone
Complete all properties in one of the property files, for example in mule.prod.properties and run your app with the corresponding environment variable. To follow the example, this is `mule.env=prod`. 


## Running on CloudHub
While creating your application on CloudHub (or you can do it later as a next step), go to Runtime Manager > Manage Application > Properties to set the environment variables listed in "Properties to Configure" as well as the **mule.env**.


### Deploying your Anypoint Template on CloudHub
Studio provides an easy way to deploy your template directly to CloudHub, for the specific steps to do so check this


## Properties to Configure
To use this template, configure properties (credentials, configurations, etc.) in the properties file or in CloudHub from Runtime Manager > Manage Application > Properties. The sections that follow list example values.
### Application Configuration
**Batch Aggregator Configuration**
+ page.size `200`

**Scheduler configuration**
+ scheduler.frequency `10000`
+ scheduler.start.delay `100`

**Watermarking default last query timestamp e.g. 2016-12-13T03:00:59Z**
+ watermark.default.expression `YESTERDAY`

**Salesforce Connector configuration**
+ sfdc.username `joan.baez@orgb`
+ sfdc.password `JoanBaez456`
+ sfdc.securityToken `ces56arl7apQs56XTddf34X`

**MS Dynamics Connector configuration**
+ msdyn.user `msdyn_user`
+ msdyn.password `msdyn_password`
+ msdyn.url `https://{your MS Dynamics url}`
+ msdyn.retries `5`

# API Calls
Salesforce imposes limits on the number of API Calls that can be made. However, in this template, only one call per poll cycle is done to retrieve all the information required.


# Customize It!
This brief guide intends to give a high level idea of how this template is built and how you can change it according to your needs.
As Mule applications are based on XML files, this page describes the XML files used with this template.

More files are available such as test classes and Mule application files, but to keep it simple, we focus on these XML files:

* config.xml
* businessLogic.xml
* endpoints.xml
* errorHandling.xml


## config.xml
Configuration for connectors and configuration properties are set in this file. Even change the configuration here, all parameters that can be modified are in properties file, which is the recommended place to make your changes. However if you want to do core changes to the logic, you need to modify this file.

In the Studio visual editor, the properties are on the *Global Element* tab.


## businessLogic.xml
Functional aspect of the Template is implemented on this XML, directed by one flow that will poll for Salesforce creations/updates. The several message processors constitute four high level actions that fully implement the logic of this Template:

1. Firstly the Template will query Salesforce for all the existing Contacts that match the filter criteria.
2. During the *Process* stage, each Contact Name will be checked in Fullname field in MS Dynamics, if it has an existing matching Contact in MS Dynamics.
3. Then, according to the result of the second step, the update or create is performed in MS Dynamics.
4. Finally during the *On Complete* stage the Template will log output statistics data into the console.



## endpoints.xml
This file is conformed by a Flow containing the Scheduler that will periodically query Salesforce for updated/created Contacts that meet the defined criteria in the query and then executing the batch job process with the query results.



## errorHandling.xml
This is the right place to handle how your integration reacts depending on the different exceptions. 
This file provides error handling that is referenced by the main flow in the business logic.




