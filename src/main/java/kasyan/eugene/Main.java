package kasyan.eugene;

import com.sforce.soap.tooling.QueryResult;
import com.sforce.soap.tooling.ToolingConnection;
import com.sforce.soap.tooling.sobject.ApexClass;
import com.sforce.soap.tooling.sobject.SObject;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;

public class Main {

    private static String SF_USER = System.getenv("SF_USER");
    private static String SF_PASS = System.getenv("SF_PASS");
    private static String SF_TOKEN = System.getenv("SF_TOKEN");
    private static String SF_URL = System.getenv("SF_URL");

    public static void main(String[] args) throws ConnectionException {
        String toolingURL = SF_URL + "/services/Soap/T/42.0";
        ConnectorConfig connectorConfig = new ConnectorConfig();
        connectorConfig.setUsername(SF_USER);
        connectorConfig.setAuthEndpoint(toolingURL);
        connectorConfig.setPassword(SF_PASS + SF_TOKEN);
        ToolingConnection toolingConnection = new ToolingConnection(connectorConfig);

        String query = "SELECT Id, Name, Status, ApiVersion, FullName, ManageableState FROM ApexClass WHERE ManageableState='unmanaged'";

        QueryResult classes = toolingConnection.query(query);
        SObject[] records = classes.getRecords();
        ApexClass apexClass = (ApexClass)records[0];
        System.out.println(apexClass);
    }
}
