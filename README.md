[![New Relic Experimental header](https://github.com/newrelic/opensource-website/raw/master/src/images/categories/Experimental.png)](https://opensource.newrelic.com/oss-category/#new-relic-experimental)

![GitHub forks](https://img.shields.io/github/forks/newrelic-experimental/nri-cisco-rtmt?style=social)
![GitHub stars](https://img.shields.io/github/stars/newrelic-experimental/nri-cisco-rtmt?style=social)
![GitHub watchers](https://img.shields.io/github/watchers/newrelic-experimental/nri-cisco-rtmt?style=social)

![GitHub all releases](https://img.shields.io/github/downloads/newrelic-experimental/nri-cisco-rtmt/total)
![GitHub release (latest by date)](https://img.shields.io/github/v/release/newrelic-experimental/nri-cisco-rtmt)
![GitHub last commit](https://img.shields.io/github/last-commit/newrelic-experimental/nri-cisco-rtmt)
![GitHub Release Date](https://img.shields.io/github/release-date/newrelic-experimental/nri-cisco-rtmt)


![GitHub issues](https://img.shields.io/github/issues/newrelic-experimental/nri-cisco-rtmt)
![GitHub issues closed](https://img.shields.io/github/issues-closed/newrelic-experimental/nri-cisco-rtmt)
![GitHub pull requests](https://img.shields.io/github/issues-pr/newrelic-experimental/nri-cisco-rtmt)
![GitHub pull requests closed](https://img.shields.io/github/issues-pr-closed/newrelic-experimental/nri-cisco-rtmt)

# Cisco RTMT Collector

## Requirements
In order to run the collector, Java 8 or later must be installed.   
   
## Installation
   
1. Download the release archive and extract it to the disk.  We will refer to the directory where it is extracted as the installation directory
2. In the installation directory, copy the file cisco-rtmt-collector.json.sample to cisco-rtmt-collector.json
3. Edit cisco-rtmt-collector.json according to the Configuration section
4. Save cisco-rtmt-collector.json
5. Run the installation script as root
 ./install.sh
     
## Configuration
You will need the account number and the license key for New Relic account that the collector will report to.
1. Edit cisco-rtmt-collector.json.  
2. Edit the global section.  
   a. Replace “enter account number” with your New Relic account number.  
   b. Replace “enter account number” with your New Relic account number.  
   c. (If reporting to EU New Relic datacenter).  Add attribute “useEU” with a value of true.   
  ![image](https://user-images.githubusercontent.com/8822859/136819149-c56225a2-f292-4801-b25b-635975984097.png).  
   d. (Optional).  The collector will collect metrics every minute.  To query less often add a “frequency” element and give it an integer value that is the number of minutes between queries and reporting.  If not present then the frequency will be set to 1.   
3. Collect the RTMT urls that you want to query along with the RTMT hosts associated with each URL.    
4. You will need to create an “agent” for each RTMT url that you want to query.  Each agent is a JSON entry in the “agents” array.  
   a.  Enter a unique name for the agent.  This can be used to differentiate which agent collected the RTMT event.   
   b.  Enter the url for RTMT.   
   c.  Enter a comma separated list of host names that you want query from this url in the “hosts” entry.   
   d. If a username and password are required to access the RTMT counters then:   
        i.  Add a “security” JSON stanza to the agent.   
        ii.  Add “username” and “password” attributes to the JSON object with values that match those necessary.    
![image](https://user-images.githubusercontent.com/8822859/136821260-77008a59-aee4-48bb-9520-42e10f0b69f7.png)
   
5. (Optional).  If you plan to query for session counters then populate the sessions stanza.  The collector will open a session, add the configured counters and query for them each period.  To configure them add a “sessions” stanza.  The file sessions.json.sample contains the stanza.  Note that multiple sessions can be used. Copy the contents to the agent and configure as follows:   
   a.  Replace “unique name for session” with a unique for the set of session counters being collected.   
   b.  Populate “counters” by adding a counter name to the array. Note that these counter names include ‘\’ and the JSON parser sees it as an escape character so if the name includes a \ denote it as \\.  Note that the counter name should follow the guidelines found here: [Add Session Counter](https://developer.cisco.com/docs/sxml/?_gl=1*g6f7o3*_ga*MTAzNDQzODc3Mi4xNjI2OTcxODU1*_gid*MTU3ODQ3NTQ4OS4xNjMwNzAyNzMy*_fplc*N0lMRjVNbEdtazJ1JTJGeDZ3elA4OVN3YUxtamdXeTRLelY5WHhhdTFDM3hqQzhxZ3FqN3pVVSUyQm1lNDVaTVpjckR0eEVId1VHU2JPNDZ1anYzT3g2Q3MzVTZOaVpNa0NlJTJCVjZqMkJZSzhvJTJGVDc5T3dRYjUzODdFUTJjeEtTQ0ElM0QlM0Q.#!perfmon-api-reference/perfmonaddcounter).  
  
   c.  Separate each name with a comma except the last one.  
   d.  Repeat if you want to set up additional sessions and counters.
6.  Repeat if more agents are needed.
7.  (Optional).  Congfigure Filters as needed.  See the next section.   
8.  Save cisco-rtmt-collector.json.   
      
## Filters
   
Three types of filters can be configured.  Each filter depends on the scope of the filter.   
The three types are:    
1.  Global - applies to all urls and hosts
2.  Agent - applies to all hosts associated with the agent's URL.    
3.  Host - applies only to the host with that name.   
   
The filter can either be include or exclude. The default is exclude.    
### Exclude
Collect all RTMT counters except the configured counters.   
### Include
Only collect the confgured RTMT counters.    

## Getting Started

>[Simple steps to start working with the software similar to a "Hello World"]

## Usage

>[**Optional** - Include more thorough instructions on how to use the software. This section might not be needed if the Getting Started section is enough. Remove this section if it's not needed.]

## Building

>[**Optional** - Include this section if users will need to follow specific instructions to build the software from source. Be sure to include any third party build dependencies that need to be installed separately. Remove this section if it's not needed.]

## Testing

>[**Optional** - Include instructions on how to run tests if we include tests with the codebase. Remove this section if it's not needed.]

## Support

New Relic has open-sourced this project. This project is provided AS-IS WITHOUT WARRANTY OR DEDICATED SUPPORT. Issues and contributions should be reported to the project here on GitHub.

>[Choose 1 of the 2 options below for Support details, and remove the other one.]

>[Option 1 - no specific thread in Community]
>We encourage you to bring your experiences and questions to the [Explorers Hub](https://discuss.newrelic.com) where our community members collaborate on solutions and new ideas.

>[Option 2 - thread in Community]
>New Relic hosts and moderates an online forum where customers can interact with New Relic employees as well as other customers to get help and share best practices. Like all official New Relic open source projects, there's a related Community topic in the New Relic Explorers Hub.
>You can find this project's topic/threads here: [URL for Community thread]

## Contributing

We encourage your contributions to improve [Project Name]! Keep in mind when you submit your pull request, you'll need to sign the CLA via the click-through using CLA-Assistant. You only have to sign the CLA one time per project. If you have any questions, or to execute our corporate CLA, required if your contribution is on behalf of a company, please drop us an email at opensource@newrelic.com.

**A note about vulnerabilities**

As noted in our [security policy](../../security/policy), New Relic is committed to the privacy and security of our customers and their data. We believe that providing coordinated disclosure by security researchers and engaging with the security community are important means to achieve our security goals.

If you believe you have found a security vulnerability in this project or any of New Relic's products or websites, we welcome and greatly appreciate you reporting it to New Relic through [HackerOne](https://hackerone.com/newrelic).

## License

[Project Name] is licensed under the [Apache 2.0](http://apache.org/licenses/LICENSE-2.0.txt) License.

>[If applicable: [Project Name] also uses source code from third-party libraries. You can find full details on which libraries are used and the terms under which they are licensed in the third-party notices document.]
