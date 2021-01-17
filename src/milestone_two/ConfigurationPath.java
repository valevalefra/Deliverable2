package milestone_two;

import java.io.FileInputStream;
import java.util.logging.Logger;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;



public class ConfigurationPath {
	
	private static Properties prop;
	private static InputStream input;
	private static Logger logger;
	
    static {

        System.setProperty("java.util.logging.config.file", "logging.properties");
        logger = Logger.getLogger(Analyze.class.getName());
    }
	
	  private ConfigurationPath() {
		    throw new IllegalStateException("Utility class");
		  }

	public static Properties getInstance()  { 
        if (prop == null) {
            prop = new Properties();
        }
        if (input == null) {
        	
				try {
					input = new FileInputStream("JavaResources\\config.properties");
				} catch (FileNotFoundException e) {
					logger.severe(e.toString());
				}
				try {
					prop.load(input);
				} catch (IOException e) {
					logger.severe(e.toString());
				}
  
    }
		return prop; 
	}	

}
