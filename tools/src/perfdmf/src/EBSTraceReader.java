package edu.uoregon.tau.perfdmf;

import java.io.*;
import java.util.*;

public class EBSTraceReader {

    private DataSource dataSource;
    private Map sampleMap = new HashMap();
    private int node = -1;

    public EBSTraceReader(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private void addLocation(String location, String callpath) {
        Object obj = sampleMap.get(callpath);
        if (obj != null) {
            List list = (List) obj;
            list.add(location);
        } else {
            List list = new ArrayList();
            list.add(location);
            sampleMap.put(callpath, list);
        }
    }

    private void processMap() {
        Group callpathGroup = dataSource.getGroup("TAU_CALLPATH");
        // Process the map we've generated
        for (Iterator it = sampleMap.keySet().iterator(); it.hasNext();) {
            String callpath = (String) it.next();
            List list = (List) sampleMap.get(callpath);
            
            Function function = dataSource.getFunction(callpath);
            
            if (function == null) {
                System.err.println("Error: callpath not found in profile: " + callpath);
                continue;
            }
            
            int size = list.size();
            
            Thread thread = dataSource.getThread(node, 0, 0);
            
            FunctionProfile fp = thread.getFunctionProfile(function);
            double exclusive = fp.getExclusive(0);
            
            double chunk = exclusive / size;
            fp.setExclusive(0,0);
            
            for (Iterator it2 = list.iterator(); it2.hasNext();) {
                String location = (String) it2.next();
                Function newFunc = dataSource.addFunction(callpath + " => " + location);
                newFunc.addGroup(callpathGroup);
                
                FunctionProfile functionProfile = thread.getFunctionProfile(newFunc);
                if (functionProfile == null) {
                    functionProfile = new FunctionProfile(newFunc);
                    thread.addFunctionProfile(functionProfile);   
                }
                
                functionProfile.setInclusive(0,functionProfile.getInclusive(0)+chunk);
                functionProfile.setExclusive(0,functionProfile.getExclusive(0)+chunk);
                functionProfile.setNumCalls(functionProfile.getNumCalls()+1);
            }
        }
        
    }
    
    private void processEBSTrace(DataSource dataSource, File file) {
        try {
            // reset data
            sampleMap.clear();
            node = -1;

            FileInputStream fis = new FileInputStream(file);
            InputStreamReader inReader = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(inReader);

            String inputString = br.readLine();
            while (inputString != null) {

                if (inputString.startsWith("#")) {
                    if (inputString.startsWith("# node:")) {
                        String node_text = inputString.substring(8);
                        node = Integer.parseInt(node_text);
                    }
                } else {

                    String fields[] = inputString.split("\\|");
                    String location = fields[3].trim();
                    if (!location.startsWith("??:")) {
                        long timestamp = Long.parseLong(fields[0].trim());
                        long deltaBegin = Long.parseLong(fields[1].trim());
                        long deltaEnd = Long.parseLong(fields[2].trim());
                        String metrics = fields[4].trim();
                        String callpath = fields[5].trim();

                        addLocation(location, callpath);
                    }
                }

                inputString = br.readLine();
            }
            
            processMap();

        } catch (Exception ex) {
            ex.printStackTrace();
            if (!(ex instanceof IOException || ex instanceof FileNotFoundException)) {
                throw new RuntimeException(ex == null ? null : ex.toString());
            }
        }
    }

    public static void processEBSTraces(DataSource dataSource, File path) {

        if (path.isDirectory() == false) {
            return;
        }

        FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                if (name.startsWith("ebstrace.processed.")) {
                    return true;
                }
                return false;
            }
        };

        File files[] = path.listFiles(filter);

        EBSTraceReader ebsTraceReader = new EBSTraceReader(dataSource);
        for (int i = 0; i < files.length; i++) {
            ebsTraceReader.processEBSTrace(dataSource, files[i]);
        }
    }

}