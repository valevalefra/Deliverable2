package milestone_one;


import java.io.BufferedReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

import java.nio.charset.StandardCharsets;

import java.io.FileWriter;
import java.util.ArrayList;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Collections;
import java.util.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;

import milestone_two.ConfigurationPath;

import org.json.JSONArray;


public class GetReleaseInfo {
	
	   private static HashMap<LocalDateTime, String> releaseNames;
	   private static HashMap<LocalDateTime, String> releaseID;
	    static ArrayList<LocalDateTime> releases;
	    static Integer numVersions;
	    static String outname = null;
	    static String project="PROJECT";
        static int numberOfv=0;
		static Date averageYear;
		static List<Commit> list= new ArrayList<>();
		static int lastId;
		private static Properties prop = ConfigurationPath.getInstance();
		private static String pathDirFin = "\\";
	    static final String PATH = "..\\..\\" + prop.getProperty(project).toLowerCase() +pathDirFin;
		private static String repoUrl = prop.getProperty("REPO_APACHE_PREFIX")+prop.getProperty(project).toLowerCase()+".git";
		private static final Logger log = Logger.getLogger(GetReleaseInfo.class.getName());
		private static final String EXCEPTION_THROWN = "an exception was thrown";
	   
        
	public static void main(String[] args) throws Exception{
		
		    gitClone();
		   	String projName =prop.getProperty(project);
		   	
		 //Fills the arraylist with releases dates and orders them
		 //Ignores releases with missing dates
		   releases = new ArrayList<>();
		         Integer i;
		         String url = "https://issues.apache.org/jira/rest/api/2/project/" + projName;
		         JSONObject json = readJsonFromUrl(url);
		         JSONArray versions = json.getJSONArray("versions");
		         
		         releaseNames = new HashMap<>();
		        
		         releaseID = new HashMap<> ();
		         for (i = 0; i < versions.length(); i++ ) {
		            String name = "";
		            String id = "";
		            if(versions.getJSONObject(i).has("releaseDate")) {
		               if (versions.getJSONObject(i).has("name"))
		                  name = versions.getJSONObject(i).get("name").toString();
		               if (versions.getJSONObject(i).has("id"))
		                  id = versions.getJSONObject(i).get("id").toString();
		               addRelease(versions.getJSONObject(i).get("releaseDate").toString(),
		                          name,id);
		            }
		         }
		         // order releases by date
		         Collections.sort(releases,(o1,o2) ->  o1.compareTo(o2));

		         if (releases.size() < 6)
		            return;
		        
			 

		             outname = projName + "VersionInfo.csv";
				    //Name of CSV for output
		           
		            try( FileWriter fileWriter = new FileWriter(outname)){
		         
		            fileWriter.append("Index,Version ID,Version Name,Date");
		            fileWriter.append("\n");
		            numVersions = releases.size();
		            for ( i = 0; i < releases.size(); i++) {
		               Integer index = i + 1;
		               fileWriter.append(index.toString());
		               fileWriter.append(",");
		               fileWriter.append(releaseID.get(releases.get(i)));
		               fileWriter.append(",");
		               fileWriter.append(releaseNames.get(releases.get(i)));
		               fileWriter.append(",");
		               fileWriter.append(releases.get(i).toString());
		               fileWriter.append("\n");
		               if(i>0) {
		            	   Commit c= new Commit();
		            	   c.setData(releases.get(i).toString());
		            	   c.setId((index.toString()));
		            	   list.add(c);
		            	   
		            }
		               
		            } 
		            	fileWriter.flush();
				}	    
            foundAverageDate(list);
			RetrieveTicketsInfo.takeTicket();
			
		   }
 
	
	


	private static Date foundAverageDate(List<Commit> listDateAndId) {
		ArrayList<Date> list = new ArrayList<>();
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm", Locale.getDefault());
		for(int i=0;i<listDateAndId.size();i++ ) {
			  String dateStr = listDateAndId.get(i).getData();
              LocalDateTime date = LocalDateTime.parse(dateStr, dtf);
              Date convertedDatetime = Date.from(date.atZone(ZoneId.systemDefault()).toInstant());
		      list.add(convertedDatetime);
		}
		
		long totalSeconds = 0L;
		for (Date date : list) {
		     totalSeconds += date.getTime() / 1000L;
		}
		long averageSeconds = totalSeconds / list.size();
		Date dateAverage = new Date(averageSeconds * 1000L);
		getNearestDate(listDateAndId, dateAverage);
		return dateAverage;
		
		
	}


		public static Date getNearestDate(List<Commit> listDateAndId, Date currentDate) {
			
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm", Locale.getDefault());
			  long minDiff = -1;
			  long currentTime = currentDate.getTime();
			  Date minDate = null;
			for (int i=0;i<listDateAndId.size();i++) {
				  String dateStr = listDateAndId.get(i).getData();
	              LocalDateTime date = LocalDateTime.parse(dateStr, dtf);
	              Date convertedDatetime = Date.from(date.atZone(ZoneId.systemDefault()).toInstant());
			      long diff = Math.abs(currentTime - convertedDatetime.getTime());
			      if ((minDiff == -1) || (diff < minDiff)) {
			      minDiff = diff;
			      minDate = convertedDatetime;
			      lastId = Integer.parseInt(listDateAndId.get(i).getId());
			    }
			  }
			  return minDate;
			}
		





	public static void addRelease(String strDate, String name, String id) {
		      LocalDate date = LocalDate.parse(strDate);
		      LocalDateTime dateTime = date.atStartOfDay();
		      if (!releases.contains(dateTime))
		         releases.add(dateTime);
		      releaseNames.put(dateTime, name);
		      releaseID.put(dateTime, id);
		    
		   }


	   public static JSONObject readJsonFromUrl(String url) throws IOException {
	      InputStream is = new URL(url).openStream();
	      
	         BufferedReader rd = new BufferedReader(new InputStreamReader(is,  StandardCharsets.UTF_8));
	         String jsonText = readAll(rd);
	         return new JSONObject(jsonText);

	   }
	   
	   public static String readAll(Reader rd) throws IOException {
		      StringBuilder sb = new StringBuilder();
		      int cp;
		      while ((cp = rd.read()) != -1) {
		         sb.append((char) cp);
		      }
		      return sb.toString();
		   }
	   
		//Clone the repository in the specified directory
		public static void gitClone() {
			
			String command = "git clone "+repoUrl+" "+PATH;
			Process p = null;
			try {
				//execute command
				p = Runtime.getRuntime().exec(command);
				log.info(prop.getProperty("infoGitCLone"));
				p.waitFor();
			} catch (IOException e) {
				log.log(Level.SEVERE,EXCEPTION_THROWN, e);
			} catch (InterruptedException e) {
				log.log(Level.SEVERE,EXCEPTION_THROWN, e);
				Thread.currentThread().interrupt();
			}
			exception(p);
			if (p.exitValue() != 0) {
				log.info("dirAlredyExist");
			}else {
				log.info("CloneComplete");
			}
		}
		
		private static void exception (Object o) {
			if (o == null) {
				throw new IllegalStateException();
			}
		}

	
}

