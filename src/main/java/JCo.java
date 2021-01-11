// Calling SAP RFC enabled FMs with SAP JCo Connector
package com.company;
import com.sap.conn.jco.*;
import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.JCoDestinationManager;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.Map;
import java.util.Properties;
import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.Callable;

@Command(name = "sapjcorfc", mixinStandardHelpOptions = true, version = "sapjcorfc 0.1",
        description = "Connects to SAP NW ABAP AS with JCo and calls an RFC enabled FM.\n\n" +
                "The program has three working modes:\n" +
                "1. Connection test: specify only connection parameters with -pc <connection_credentials>.\n" +
                "2. Testing existence of a remote function module on SAP side and get the list of its parameters: specify -pc <connection_credentials> <rfc_module>.\n" +
                "3. Calling remote FM with parameters (note that tables and structures are not supported): specify -pc <connection_credentials> -pi <import_parameters> -pch <changing_parameters> <rfc_module> .\n")
class SAPJcoRFC implements Callable<Integer> {

    @Parameters(index = "0", arity = "0..1", description = "Remote Function Call enabled Function Module, e.g. STFC_CHANGING")
    private String rfc_module;

    @Option(names = {"-pc", "--conn_parameters"}, required = true, description = "Connection parameters supported by the installed version of SAP JCo omitting preceding 'jco.client.', e.g.\n" +
            "* for user/pass connection: -pc ashost=icpxxxx.wdf.sap.corp -pc sysnr=00 -pc client=800 -pc user=ddic -pc passwd=******** \n" +
            "* for snc connection: -pc ashost=icpxxxx.wdf.sap.corp -pc sysnr=00 -pc client=800 -pc snc_mode=1 -pc snc_partnername=p:CN=Cxxxxxxx,O=SAP-AG,C=DE -pc client.snc_lib=/home/oracle/sec/libsapcrypto.so"
    )
    Map<String, String> connection_pars;

    @Option(names = {"-pi", "--import_par"}, description = "import (inbound) parameters for RFC Call, e.g.\n" +
            "-pi START_VALUE=0 -pi COUNTER=1"
    )
    Map<String, String> rfc_pars_in;

    @Option(names = {"-pch", "--changing_par"}, description = "changing parameters for RFC Call, e.g.\n" +
            "-ph START_VALUE=0 -i COUNTER=1"
    )
    Map<String, String> rfc_pars_cha;

    @Override
    public Integer call() throws Exception {
        System.out.println("Connecting to SAP System..");
        JCoDestination destination = connect(connection_pars);
        if (rfc_module != null) {
            System.out.println("Checking FM '" + rfc_module + "'..");
            JCoFunction function = getFM(destination, rfc_module);
            if (function != null) {
                System.out.println("Getting import parameters:");
                JCoParameterList listImpParam = function.getImportParameterList();
                listParameters(listImpParam);
                System.out.println("Getting changing parameters:");
                JCoParameterList listChaParam = function.getChangingParameterList();
                listParameters(listChaParam);
                try {
                    System.out.println("\nSetting parameters:");
                    setParameters(listImpParam, rfc_pars_in);
                    setParameters(listChaParam, rfc_pars_cha);
                    System.out.println("\nExecuting FM '" + rfc_module + "'..");
                    function.execute(destination);
                    System.out.println("\nSuccess.");
                    getResult(function);
                } catch (JCoException e) {
                    e.printStackTrace();
                }
            }
        }
        return 0;
    }

    static JCoDestination connect(Map<String, String> connection_pars) {
        String DESTINATION_NAME1 = "SAPSystem";
        JCoDestination destination = null;
        Properties connectProperties = new Properties();
        connection_pars.forEach((k, v) ->
                connectProperties.setProperty("jco.client." + k, v));
        createDataFile(DESTINATION_NAME1, connectProperties);
        // This will use that destination file to connect to SAP
        try {
            destination = JCoDestinationManager.getDestination("SAPSystem");
            System.out.println("Attributes:");
            System.out.println(destination.getAttributes());
            System.out.println();
            destination.ping();

        } catch (JCoException e) {
            e.printStackTrace();
        }
        return destination;
    }

    static JCoFunction getFM(JCoDestination destination, String functionModule) {
        JCoFunction function = null;
        try {
            function = destination.getRepository().getFunction(functionModule);
            if (function == null)
                throw new RuntimeException("'" + functionModule + "' not found in SAP.");
            destination.getRepository().getFunction(functionModule);
            System.out.println("Module found in the remote system.");
        } catch (JCoException e) {
            e.printStackTrace();
        }
        return function;
    }


    static void listParameters(JCoParameterList listParam) {
        if (listParam != null) {
            JCoParameterFieldIterator it = listParam.getParameterFieldIterator();
            while (it.hasNextField()) {
                JCoParameterField field = it.nextParameterField();
                System.out.println("\tname: " + field.getName() + ", type: " + field.getTypeAsString() + ", optional: " + field.isOptional() + ", value: " + field.getValue());
            }
        }
    }

    static void setParameters(JCoParameterList listParam, Map<String, String> rfc_pars) {
        if (listParam != null && rfc_pars != null) {
            for (Map.Entry<String, String> entry : rfc_pars.entrySet()) {
                JCoParameterFieldIterator it = listParam.getParameterFieldIterator();
                while (it.hasNextField()) {
                    JCoParameterField field = it.nextParameterField();
                    String parKey = "";
                    if (entry.getKey().equals(field.getName())) {
                        parKey = field.getName();
                        System.out.println("Initializing parameter " + field.getName() + " with " + entry.getValue());
                        listParam.setValue(field.getName(), entry.getValue());
                    }
                    if (parKey.isEmpty())
                        throw new RuntimeException("Given parameter " + field.getName() + " not recognized.");
                }
            }
        }
    }

    static void getResult(JCoFunction function) {
        System.out.println("Getting export parameters:");
        JCoParameterList listExpParam = function.getExportParameterList();
        listParameters(listExpParam);
        System.out.println("Getting changed parameters:");
        JCoParameterList listChangedParam = function.getChangingParameterList();
        listParameters(listChangedParam);
    }


    static void createDataFile(String destinationName, Properties connectProperties) {
        File destCfg = new File(destinationName + ".jcoDestination");
        try {
            FileOutputStream fos = new FileOutputStream(destCfg, false);
            connectProperties.store(fos, "Connection details");
            fos.close();
        } catch (Exception e) {
            throw new RuntimeException("Unable to create the destination files", e);
        }
    }
}

public class JCo {

    public static void main(String[] args) {
        int exitCode = new CommandLine(new SAPJcoRFC()).execute(args);
        System.exit(exitCode);
    }
}