package kasyan.eugene;

import com.sforce.soap.tooling.QueryResult;
import com.sforce.soap.tooling.TestLevel;
import com.sforce.soap.tooling.ToolingConnection;
import com.sforce.soap.tooling.sobject.ApexTestResult;
import com.sforce.soap.tooling.sobject.AsyncApexJob;
import com.sforce.soap.tooling.sobject.SObject;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public class RunAsyncTests extends Task {

    private String username;
    private String password;
    private String URL;
    private ToolingConnection connection;
    private static final String JOB_COMPLETED_STATUS = "Completed";
    private static final String JOB_FAILED_STATUS = "Failed";

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getURL() {
        return URL;
    }

    public void setURL(String URL) {
        this.URL = URL;
    }

    @Override
    public void execute() throws BuildException {
        authenticate();
        String asyncApexJobId = runAsyncTestAndGetJobId();
        waitWhileAsyncJobNotCompleted(asyncApexJobId);

        SObject[] apexJobResults = selectFailedTest(asyncApexJobId);
        if(apexJobResults.length > 0) {
            showError(apexJobResults);
            System.exit(-1);
        } else {
            log("All tests were success passed");
        }
    }

    private void authenticate() {
        String endpoint = URL + "/services/Soap/T/42.0";

        ConnectorConfig connectorConfig = new ConnectorConfig();
        connectorConfig.setUsername(username);
        connectorConfig.setAuthEndpoint(endpoint);
        connectorConfig.setPassword(password);

        try {
            connection = new ToolingConnection(connectorConfig);
        } catch (ConnectionException e) {
            throw new RuntimeException(e);
        }
    }

    private String runAsyncTestAndGetJobId() {
        String asyncApexJobId;
        try {
            asyncApexJobId = connection.runTestsAsynchronous(null, null, 0,
                    TestLevel.RunAllTestsInOrg,
                    null,
                    null,
                    null);
        } catch (ConnectionException e) {
            throw new RuntimeException(e);
        }
        return asyncApexJobId;
    }

    private String loadAsyncApexJobAndGetStatus(String asyncApexJobId) throws ConnectionException {
        QueryResult query = connection.query("SELECT Status FROM AsyncApexJob WHERE Id='"
                + asyncApexJobId + "'");
        SObject[] asyncApexJobs = query.getRecords();
        AsyncApexJob asyncApexJob = (AsyncApexJob) asyncApexJobs[0];
        log("Status === > " + asyncApexJob.getStatus());
        return asyncApexJob.getStatus();
    }

    private void waitWhileAsyncJobNotCompleted(String asyncApexJobId) {
        try {
            String asyncApexJobStatus;
            Boolean isPolling = false;
            while (!isPolling) {
                asyncApexJobStatus = loadAsyncApexJobAndGetStatus(asyncApexJobId);
                Thread.sleep(5000);
                isPolling = asyncApexJobStatus.equals(JOB_FAILED_STATUS) ||
                        asyncApexJobStatus.equals(JOB_COMPLETED_STATUS);
            }
        } catch (ConnectionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private SObject[] selectFailedTest(String asyncApexJobId) {
        SObject[] apexTestResults;
        String query = "SELECT Message, MethodName, ApexClass.Name FROM ApexTestResult " +
                "WHERE AsyncApexJobId='" + asyncApexJobId + "' AND Outcome!='Pass'";
        try {
            apexTestResults = connection.query(query).getRecords();
        } catch (ConnectionException e) {
            throw new RuntimeException(e);
        }
        return apexTestResults;
    }

    private void showError(SObject[] apexTestResults) {
        Integer numberFailedTest = apexTestResults.length;
        if(numberFailedTest > 0) {
            String errorMessage = apexTestResults.length  +
                    (numberFailedTest == 1 ? " test was" : " tests were") +
                    " failed";
            log(errorMessage);
        }
        for(SObject apexTestResult : apexTestResults) {
            ApexTestResult result = (ApexTestResult)apexTestResult;
            log("\n========================");
            log("Class = " + result.getApexClass().getName());
            log("Method = " + result.getMethodName());
            log("Error message = " + result.getMessage());
            log("========================");
        }
    }
}
