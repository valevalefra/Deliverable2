package milestone_one;
import java.io.BufferedReader;
import java.io.File;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.ArrayList;

import java.util.Collections;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;

import org.json.JSONObject;

import milestone_two.ConfigurationPath;

import org.json.JSONArray;

public class RetrieveTicketsInfo {
	
	private static final String GIT = "git -C ";
	private static List<Ticket> ticketList=new ArrayList<>();
	static int p = 0;
	static int n=1;
	private static Properties prop = ConfigurationPath.getInstance();
	
	
	
	 private RetrieveTicketsInfo() {
		    throw new IllegalStateException("Utility class");
		  }
	
	 
	    private static String filePath=GetReleaseInfo.PATH;
		static File path = new File(filePath);
		static String file=prop.getProperty("PROJECT").toLowerCase()+"Results.csv"; 
		static FileWriter fileWriter= null;
		static List<String> filteredFiles=new ArrayList<>();
		static List<FileCsv> csvLines = new ArrayList<>();
		static List<FileCsv> csvFinal = new ArrayList<>();
		


   public static JSONArray readJsonArrayFromUrl(String url) throws IOException {
      InputStream is = new URL(url).openStream();
      
         BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
         String jsonText = GetReleaseInfo.readAll(rd);
         JSONArray json;
         json = new JSONArray(jsonText);
         return json;
   }

   
  
   public static void takeTicket() throws IOException {
  		 fileWriter =new FileWriter(file);
  		 JSONObject json;
		 String projName =prop.getProperty("PROJECT").toLowerCase();
	     Integer j = 0;
	     Integer i=0;
	     Integer total=1;
	     
	     listOfAllFile();
      //Get JSON API for closed bugs w/ AV in the project
      do {
         //Only gets a max of 1000 at a time, so must do this multiple times if bugs >1000
         j = i + 1000;
         String url = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22"
                + projName + "%22AND%22issueType%22=%22Bug%22AND(%22status%22=%22closed%22OR"
                + "%22status%22=%22resolved%22)AND%22resolution%22=%22fixed%22&fields=key,resolutiondate,versions,fixVersions,created&startAt="
                + i.toString() + "&maxResults=" + j.toString();
          json = GetReleaseInfo.readJsonFromUrl(url);
         JSONArray issues = json.getJSONArray("issues");
         total = json.getInt("total");
         for (; i < total && i < j; i++) {
            //Iterate through each bug
        	    Object fields = issues.getJSONObject(i%1000).get("fields");
        	    //creation date of ticket
        	    String creationDate = ((JSONObject) fields).get("created").toString();
                String key = issues.getJSONObject(i%1000).get("key").toString();
              	
                compute(fields,creationDate,key);	
                
         }}while (i < total);
      
      for(int y=0;y<ticketList.size();y++) {
    	  takeVersions(ticketList.get(y));
    	  
      }
     
      for(int y=0;y<filteredFiles.size();y++) {
    	
    	  foundVersion(filteredFiles.get(y),-1,-1,-1);
      }

       orderCsv(csvLines);
       csvFinal=GetMetrics.setMetrics(csvLines);

       writeCsv(csvFinal);
        
   }

	private static void compute(Object fields, String creationDate, String key) throws IOException {
		
		JSONArray versions = ((JSONObject) fields).getJSONArray("versions");
    	JSONArray fixVersions = ((JSONObject) fields).getJSONArray("fixVersions");
    	if ( fixVersions.length()!=0 && versions.length()!=0 ) {
    		
    		takeIndex(fixVersions,versions,creationDate,key,0,-1);		
    			
    		}	
    		   
    	if ( versions.length()==0 ) {
    		n++;
    		int prop=p/n;
    		takeIndex(null,null,creationDate,key,1,prop);

    	}} 
		
	
    /*
     * Take index of IV, OV, FV for each ticket and add ticket to the ticket list
     */
	private static void takeIndex(JSONArray fixVersions, JSONArray versions, String creationDate, String key, int i, int prop) throws IOException {
		
		int fv=0;
		int iv=0;
		int ov=0;
		if(i==0) {
		String fVersion = takeFV(fixVersions);
		ArrayList<String> av = takeAV(versions);
		String iVersion= av.get(0);
		 fv=readCsv(null,fVersion);
		 iv=readCsv(null,iVersion);
		 ov=takeFirstCommit(creationDate );}
		else {
        	ov=takeFirstCommit(creationDate );
        	fv =takeLastCommit(key);
            iv=calculateIV(ov,fv,prop);
		}
		
		if(fv==iv || fv==ov) {
            Ticket ticket=new Ticket();
            ticket.setFv(fv);
            ticket.setIv(iv);
            ticket.setOv(ov);
            ticket.setKey(key);
            ticket.setMark(1);
            ticketList.add(ticket);

		}
		
		if((ov<iv && iv<fv) || (fv<ov && iv<fv) ) {
			
            Ticket ticket=new Ticket();
            ticket.setFv(fv);
            ticket.setIv(iv);
            ticket.setOv(ov);
            ticket.setKey(key);
            ticket.setMark(0);
            ticketList.add(ticket);
		}
		if(iv<ov && ov<fv) {
			
			int newP=calculateP(iv,ov,fv);

			p=(p+newP);
			Versions list =new Versions();
        	list.setFv(fv);
        	list.setIv(iv);
        	list.setOv(ov);
        	list.setKey(key);

            Ticket ticket=new Ticket();
            ticket.setFv(fv);
            ticket.setIv(iv);
            ticket.setOv(ov);
            ticket.setKey(key);
            ticket.setMark(0);
            ticketList.add(ticket);}
		
	}

	private static int calculateIV(int ov, int fv, int p) {
		int iv=0;
		iv=fv-((fv-ov)*p);
		return iv;
	}

	private static int calculateP(int iv, int ov, int fv) {
		int p=0;
		p=(fv-iv)/(fv-ov);

		return p;
		
		
	}

	private static int takeFirstCommit( String creationDate) throws IOException {

		    int ov;
   
        	ov=readCsv(creationDate,null);
            return ov;
            }

	private static int takeLastCommit(String key) throws IOException {
		
		Process logGit = null;
		String s;
        int fv = 0;
	   //Take date of last commit for each ticket
		logGit = Runtime.getRuntime().exec(GIT+path +" log -1 --pretty=format:\"%cs\" --grep=" + key );
	    BufferedReader stdInput = new BufferedReader(new InputStreamReader(logGit.getInputStream()));

        while ((s = stdInput.readLine()) != null ) {
	        	 fv = readCsv(s,null);
	        	 }
			return fv;          
	}

	/*
	 *Reads the csv relating to the release information and associates
	 * a date or version with the corresponding id.
	 */
	public static int readCsv(String s, String version) throws IOException {
		
		String file = GetReleaseInfo.outname;

		try(BufferedReader br = new BufferedReader(new FileReader(file))){
	    String line;
	    int count = 0;
	    int n=0;
	    while ( (line = br.readLine()) != null ) {
	    	if(n==0) {
	    		n=1;
	    		continue;
	    	}
	    	if(n==1) {
	    		 count++;
		         if(foundId(version,line,s)) {
		        	return count;
		          } 
	         }
	     } 
	return count;}
	}
    /*
     * If date is not null then is compared with all those contained
     * in the release csv file and returns true if it finds an equal or smaller date.
     * the same thing is done for the version attribute.
     */
	private static boolean foundId(String version, String line, String date) {
		
		if(version==null) {
		String[] values = line.split(",");
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date y = new Date();
        Date v = new Date();
        try {
            v=dateFormat.parse(values[3]);
            y = dateFormat.parse(date);
        } catch (ParseException e) {
             e.getCause();
        }
     
        if(v.after(y) || v.equals(y)) {
             return true;
        }
		}
		else {
			String[] values = line.split(",");
	    	   if(values[2].equals(version)) {
	    		   return true;
	    		}
		}
		return false;
	}

	private static ArrayList<String> takeAV(JSONArray versions) {
		ArrayList<String> av = new ArrayList<>();
		
    	for (int k = 0; k < versions.length(); k++ ) {
    		
    		String ver="";
    		   if (versions.getJSONObject(k).has("name")) {
    		       ver = versions.getJSONObject(k).get("name").toString();
    		       av.add(ver);
                  
    		     }
    		   }return av;
		
	}

	private static String takeFV(JSONArray fixVersions) {
	    
		String lastFV="";
		   if (fixVersions.getJSONObject(0).has("name")) {
		       lastFV = fixVersions.getJSONObject(0).get("name").toString();


		     }
		   return lastFV;
	}
	
	public static void listOfAllFile() throws IOException {
		
		
		List<String> rawFiles = new ArrayList<>();
		Process logGit = null;
		logGit = Runtime.getRuntime().exec(GIT+path +" --no-pager log --pretty=format:\"\" --name-only *.java");
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(logGit.getInputStream()));
		String line;
		String prevLine="";
		while ((line=stdInput.readLine())!=null || prevLine!=null) {
			if(line!=null && !line.equals("")) 
				rawFiles.add(line);
			prevLine=line;
				
			
		}
		//delete duplicate files
		filteredFiles=new ArrayList<>(new LinkedHashSet<>(rawFiles));
		
		
	}

	private static void takeVersions(Ticket ticket) throws IOException {
		
		Process logGit = null;
		String s;
		String key=ticket.getKey();
		int iv=ticket.getIv();
		int fv=ticket.getFv();
		int j=ticket.getMark();

        String fileName;
		try{
			//Get the commit for each ticket
			logGit = Runtime.getRuntime().exec(GIT+path +" log -1 --format=%H --grep=" + key );
	        BufferedReader stdInput = new BufferedReader(new InputStreamReader(logGit.getInputStream()));

	             
			while ((s = stdInput.readLine()) != null ) {
				//Get the java files related to each commit
				Process logDiff = Runtime.getRuntime().exec(GIT+path +" diff-tree --no-commit-id --name-only -r " +s+ " *.java" );
			
				 BufferedReader stdInput1 = new BufferedReader(new InputStreamReader(logDiff.getInputStream()));
			
			     
			     while ((fileName = stdInput1.readLine()) != null ) {
                      
			    	 //Delete the files i found that have a certain commit
			    	 for(int i=0;i<filteredFiles.size();i++) {
			    	     if(fileName.equals(filteredFiles.get(i))) {
			    	    	 filteredFiles.remove(filteredFiles.get(i));
			    	     }
			    	   }

			          foundVersion(fileName,iv, fv,j);

			     }
			}
		}
			catch(Exception ex)
			  {
			        if(logGit!=null)
			        {
			              logGit.destroy();
			        }
			        ex.getCause();
			        System.exit(-1);}
	}
			          
		

	
    /*
     * For each file takes the creation and deletion date. If mark = 0 check if the file is faulty,
       otherwise it is know that the file is buggy and it is add to a list, marking it as faulty.
     */
	private static void foundVersion(String fileName, int iv, int fv, int mark) throws IOException {
		
	   Process logDC=Runtime.getRuntime().exec(GIT+path+" log --diff-filter=A --pretty=format:%cs -- "+fileName);
       Process logDD=Runtime.getRuntime().exec(GIT+path+" log --diff-filter=D --pretty=format:%cs -- "+fileName);
       BufferedReader stdInput2 = new BufferedReader(new InputStreamReader(logDC.getInputStream()));
       BufferedReader stdInput3 = new BufferedReader(new InputStreamReader(logDD.getInputStream()));
       String s;
	   String t;
	   while ((s = stdInput2.readLine()) != null)  {
       	 
		  t=stdInput3.readLine();
       	  int idCreation=readCsv(s,null);
       	  int idDelete;
       	 
       if(t==null) {
    	   idDelete= GetReleaseInfo.numberOfv-1;  
       }
       else {
    	    idDelete=readCsv(t,null);
       }
       for(int i=idCreation;i<=idDelete;i++) {
       	   if(mark==0) {
       		
   			verifyBug(i,fv,iv,fileName);
   
       		 }
       	     else {
       		 FileCsv fileCsv=new FileCsv();
       		 fileCsv.setBug(false);
   			 fileCsv.setFileName(fileName);
   			 fileCsv.setId(i);
   			 if(i<=GetReleaseInfo.lastId) {
   			 csvLines.add(fileCsv);}
       				
       		  }
          }
       
	}
 }

	private static void verifyBug(int i, int fv, int iv, String fileName) {
		 if(i<fv && i>=iv) {
    			
   			 FileCsv fileCsv=new FileCsv();
   			 fileCsv.setBug(true);
   			 fileCsv.setFileName(fileName);
   			 fileCsv.setId(i);
   			 
   			 if(i<=GetReleaseInfo.lastId) {
   			 csvLines.add(fileCsv);}
   			 	
   		 }
   		 else{
   			 FileCsv fileCsv=new FileCsv();
   			 fileCsv.setBug(false);
   			 fileCsv.setFileName(fileName);
   			 fileCsv.setId(i);
   			 if(i<=GetReleaseInfo.lastId) {
   			 csvLines.add(fileCsv);}
   			
   			 }
		
	}

	private static void writeCsv(List<FileCsv> file) throws IOException  {
		
		fileWriter.append("id");
		fileWriter.append(",");
		fileWriter.append("fileName");
		fileWriter.append(",");
		fileWriter.append("avgCghSetSize");
		fileWriter.append(",");
		fileWriter.append("avgChurn");
		fileWriter.append(",");
		fileWriter.append("avgLoc");
		fileWriter.append(",");
		fileWriter.append("cghSetSize");
		fileWriter.append(",");
		fileWriter.append("churn");
		fileWriter.append(",");
		fileWriter.append("locAdded");
		fileWriter.append(",");
		fileWriter.append("maxCghSetSize");
		fileWriter.append(",");
		fileWriter.append("maxChurn");
		fileWriter.append(",");
		fileWriter.append("maxLoc");
		fileWriter.append(",");
		fileWriter.append("bug");
		fileWriter.append("\n");

		for(int i=0; i<file.size();i++) {
	
  		try {
			int idNum=file.get(i).getId();
			fileWriter.append(String.valueOf(idNum));
			fileWriter.append(",");
			fileWriter.append(file.get(i).getFileName());
			fileWriter.append(",");
			fileWriter.append(String.valueOf(file.get(i).getAvgCghSetSize()));
			fileWriter.append(",");
			fileWriter.append(String.valueOf(file.get(i).getAvgChurn()));
			fileWriter.append(",");
			fileWriter.append(String.valueOf(file.get(i).getAvgLoc()));
			fileWriter.append(",");
			fileWriter.append(String.valueOf(file.get(i).getCghSetSize()));
			fileWriter.append(",");
			fileWriter.append(String.valueOf(file.get(i).getChurn()));
			fileWriter.append(",");
			fileWriter.append(String.valueOf(file.get(i).getLocAdded()));
			fileWriter.append(",");
			fileWriter.append(String.valueOf(file.get(i).getMaxCghSetSize()));
			fileWriter.append(",");
			fileWriter.append(String.valueOf(file.get(i).getMaxChurn()));
			fileWriter.append(",");
			fileWriter.append(String.valueOf(file.get(i).getMaxLoc()));
			fileWriter.append(",");
			if(file.get(i).isBug()) {
            fileWriter.append("yes");}
			else {
				fileWriter.append("no");
			}
			
			fileWriter.append("\n");
			
		} catch (IOException e) {
			
			e.getCause();
		}finally {
			try {
				fileWriter.flush();
				
			}
			catch(Exception e){
				e.getCause();
				
			}
		
		
		} }}
	
	
	 static void orderCsv(List<FileCsv> csvLines) {
		 
		 
	Collections.sort(csvLines,(o1,o2) ->  Integer.valueOf(o1.getId()).compareTo(o2.getId()));

		 


		    } 
		}
		
	
		