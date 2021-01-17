package milestone_two;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import milestone_one.FileCsv;

import java.io.PrintStream;


import weka.attributeSelection.CfsSubsetEval;
import weka.attributeSelection.GreedyStepwise;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.lazy.IBk;
import weka.classifiers.meta.FilteredClassifier;
import weka.classifiers.trees.RandomForest;
import weka.core.Instance;
import weka.core.Instances;

import weka.core.converters.ArffSaver;
import weka.core.converters.CSVLoader;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.supervised.attribute.AttributeSelection;
import weka.filters.supervised.instance.Resample;
import weka.filters.supervised.instance.SMOTE;
import weka.filters.supervised.instance.SpreadSubsample;


public  class Analyze {
	

	private static Properties prop = ConfigurationPath.getInstance();
	private static String general= prop.getProperty("PROJECT").toLowerCase();
	private static String pathDir =  general+"Results.csv"; 
	static String fileNK=general+"NK.csv";
	static String fileK=general+"K.csv"; 
	static FileWriter fileWriterNK= null;
	static FileWriter fileWriterK= null;
	static int count=0;
	static ArffSaver saNK;
	static ArffSaver saK;
	private static String projName=general;
	
	private static Logger logger;
	
    static {

        System.setProperty("java.util.logging.config.file", "logging.properties");
        logger = Logger.getLogger(Analyze.class.getName());
    }
	

	 //Uses RandomForest / NaiveBayes / Ibk as classifiers
    public static void classifier(Instances training, Instances testing, FilteredClassifier fc, Record r, int i) {
		Evaluation eval;
    	int trainSize = 0;
    	int testSize = 0;
    	
		trainSize = training.size();
		testSize = testing.size();
    	
		try {
			eval = new Evaluation(testing);
		
	    	//The parameter i specifies which classifier to use
	    	switch(i) {
	    	
	    	case 0:
	    		//RandomForest
	    		r.setClassifier("RandomForest");
	    		
	    	   	RandomForest randomForest = new RandomForest();        	
	        	fc.setClassifier(randomForest);
	    		fc.buildClassifier(training);		
	    		
	    		break;
	    		
	    	case 1:
	    		//NaiveBayes
	    		r.setClassifier("NaiveBayes");
	    		
	    	 	NaiveBayes naiveBayes = new NaiveBayes();        	
	        	fc.setClassifier(naiveBayes);    		
	    		fc.buildClassifier(training);
	    		
	    		break;
	    		
	    	case 2:
	    		//Ibk
	    		r.setClassifier("Ibk");
	    		
	    		IBk ibk = new IBk();
	        	fc.setClassifier(ibk);    		
	    		fc.buildClassifier(training);
	    		
	    		break;
	    		
	    	  default:
	    		    logger.severe("Illegal value for argument i");
	    		    break;
	    	
	    		
	    	}
	    	
			eval.evaluateModel(fc, testing);
	    	
			r.setTrain((float)trainSize/(trainSize+testSize));
			r.setTp(eval.numTruePositives(1));
			r.setFp(eval.numFalsePositives(1));
			r.setTn(eval.numTrueNegatives(1));
			r.setFn(eval.numFalseNegatives(1));
			r.setPrecision(eval.precision(1));
			r.setRecall(eval.recall(1));
			r.setRoc(eval.areaUnderROC(1));
			r.setKappa(eval.kappa());
	    	
		} catch (Exception e) {
			logger.severe(e.toString());
		}
		
	}
	
	
	//Applies no sampling / oversampling / undersampling / SMOTE for balancing
    public static List<Record> balancing(String project, int releases, String featureSel, Instances training, Instances testing,
    		double percent){
		    
    	Record r = null;
    	FilteredClassifier fc = null;
    	int i = 0;
    	String[] opts;
    	
    	List<Record> records = new ArrayList<>();
    	
		//no sampling
    	
    	for(i=0; i<3; i++) {
    		r = new Record(project,releases,featureSel,"No sampling");
    		classifier(training, testing, new FilteredClassifier(), r, i);
    		records.add(r);
    	}
    	
    	//oversampling
    	fc = new FilteredClassifier();

    	Resample resample = new Resample();
    	opts = new String[]  {"-B", "1.0", "-Z", String.valueOf(2*percent*100)};

    	try {
			resample.setOptions(opts);
	    	resample.setInputFormat(training);
		} catch (Exception e) {
			logger.severe(e.toString());
		}

    	fc.setFilter(resample);    	
    	
    	for(i=0; i<3; i++) {
    		r = new Record(project,releases,featureSel,"Oversampling");
    		classifier(training, testing, fc, r, i);
    		records.add(r);
    	}
    	
		//undersampling
    	fc = new FilteredClassifier();

		SpreadSubsample  spreadSubsample = new SpreadSubsample();
		opts = new String[]{ "-M", "1.0"};

		try {
			spreadSubsample.setOptions(opts);
		} catch (Exception e) {
			logger.severe(e.toString());
		}
		
		fc.setFilter(spreadSubsample);
    	
    	for(i=0; i<3; i++) {
    		r = new Record(project,releases,featureSel,"Undersampling");
    		classifier(training, testing, fc, r, i);
    		records.add(r);
    	}

    	//SMOTE
    	fc = new FilteredClassifier();
    	
	    SMOTE smote = new SMOTE();

	    try {
			smote.setInputFormat(training);
		} catch (Exception e) {
			logger.severe(e.toString());
		}
		
	    fc.setFilter(smote);
    	
    	for(i=0; i<3; i++) {
    		r = new Record(project,releases,featureSel,"SMOTE");
    		classifier(training, testing, fc, r, i);
    		records.add(r);
    	}
    	
	    return records;	
    }
	
	
	
	public static void walkForward() throws Exception {
		
		
		ArffSaver saver = convert(pathDir);
		
		List<Record> records = new ArrayList<>();
		double percent = 0;

		
		for(int i=0;i<csvInfo.size()-1;i++) {
			
		    int start = csvInfo.get(i).getRow();
	        int end = csvInfo.get(i+1).getRow();
		

			Instances testing = new Instances (saver.getInstances(), start, end-start-1);
			Instances training = new Instances (saver.getInstances(), 0, end-1);

			
			double[] infoTraining = majorityClass(training);
			double[] infoTesting = majorityClass(testing);
			double defectiveInTrain=infoTraining[0];
			double defectiveInTest=infoTesting[0];
			percent = infoTraining[1];
			
	
		    int trainData = training.numInstances();
		    
		    
		    DataSource trainSource;
		    trainSource = new DataSource(training);
    		Instances trainingNoFilter = trainSource.getDataSet();
    		
    		DataSource testSource = new DataSource(testing);
    		Instances testingNoFilter = testSource.getDataSet();
    		
    		
    		//No selection
    		int numAttrNoFilter = trainingNoFilter.numAttributes();
    		trainingNoFilter.setClassIndex(numAttrNoFilter - 1);
    		testingNoFilter.setClassIndex(numAttrNoFilter - 1);
    		
    		
    		Record r=null;
    		List<Record> noSelection = balancing(projName, i, "No selection", trainingNoFilter, testingNoFilter, percent);
    		
    		for(int j=0; j<noSelection.size(); j++) {
    			
    			r = noSelection.get(j);
    			r.setTrainRel(i+1);
    			r.setTrain((double)trainData/(trainData+csvInfo.get(i+1).getRow())); //VERIFICARE CSViNFO
    			r.setTrainDef(defectiveInTrain);
    			r.setTestDef(defectiveInTest);
    			records.add(r);
    		}
    		
    		
		    
    		//Best first feature selection
    		AttributeSelection filter = new AttributeSelection();

    		CfsSubsetEval eval = new CfsSubsetEval();
    		GreedyStepwise search = new GreedyStepwise();
    		search.setSearchBackwards(true);

    		filter.setEvaluator(eval);
    		filter.setSearch(search);
    		filter.setInputFormat(trainingNoFilter);

    		Instances trainingFiltered = Filter.useFilter(trainingNoFilter, filter);
    		Instances testingFiltered = Filter.useFilter(testingNoFilter, filter);		
    		
    		int numAttrFiltered = trainingFiltered.numAttributes();
    		
    		trainingFiltered.setClassIndex(numAttrFiltered - 1);
    		testingFiltered.setClassIndex(numAttrFiltered - 1);

    		List<Record> bestFirst = balancing(projName, i, "Best first", trainingFiltered, testingFiltered, percent);
    		
    		for(int j=0; j<bestFirst.size(); j++) {
    			
    			r = bestFirst.get(j);
    			r.setTrainRel(i+1);
    			r.setTrain((double)trainData/(trainData+csvInfo.get(i+1).getRow())); //VERIFICARE CSViNFO
    			r.setTrainDef(defectiveInTrain);
    			r.setTestDef(defectiveInTest);
    			records.add(r);
    		}
		

		
	}

	createCsv(records);
		
		
		
	}
	
	private static void createCsv(List<Record> records) throws FileNotFoundException {
		
		String output = general+"Weka.csv";
		PrintStream printer = new PrintStream(new File(output));
		
		printer.println("Dataset"+";"+"TrainRelease"+";"+"%Train"+";"+"TrainDefective"+";"+"TestDefective"+";"
				+"getClassifier"+";"+"Balancing"+";"+"FeatureSelection"+";"+"Tp"+";"+"Fp"+";"+"Tn"+";"
				+"Fn"+";"+"Precision"+";"+"Recall"+";"+"Roc"+";"+"Kappa");
		
    	for(int i=0; i<records.size(); i++) {
    		
    		Record r = records.get(i);
    		
    		printer.println(r.getDataset()+";"+r.getTrainRel()+";"+r.getTrain()+";"+r.getTrainDef()+";"+r.getTestDef()+";"
    				+r.getClassifier()+";"+r.getBalancing()+";"+r.getFeatureSel()+";"+r.getTp()+";"+r.getFp()+";"+r.getTn()+";"
    				+r.getFn()+";"+r.getPrecision()+";"+r.getRecall()+";"+r.getRoc()+";"+r.getKappa());
    		
    	}
    	
    	printer.close();
		
	}


	private static double[] majorityClass(Instances dataset) {
		
		double buggy =0;

				
				int yes=0;
				int no = 0;
				int tot=0;
				for (int i = 0; i <= dataset.numInstances() - 1; i++) {
					tot++;
				    Instance instance = dataset.instance(i);
				    if((instance.stringValue(11)).equals("no")) {
				    	no++;
				    }
				    else {
				    	yes++;
				    	buggy++;
				    	
				    }
				     
				}
				
				if(yes>no) {
					double[] info = new double[2];
					double y=(double)yes/(double)tot;
					info[0]=buggy;
					info[1]=y;
					return info;
				}
				else {
					double y = 0;
					double[] info = new double[2];
					if(tot!=0) {
					y=(double)no/(double)tot;
					}
					
					info[0]=buggy;
					info[1]=y;
					return info;
				}
		
	}



	static List<FileCsv> csvInfo = new ArrayList<>();
	
	/*
	 *Compute a list of information about fileCsv
	 *Each items of list content id and numbers of rows with same id
	 */
	private static void takeLineVersion() throws IOException {
		try(BufferedReader br = new BufferedReader(new FileReader(pathDir))){
		    String line;
           
            int row=1;
			int n=0;
			int id=1;
			while ( (line = br.readLine()) != null ) {
			    	if(n==0) {
			    		n=1;
			    		continue;
			    	}
			    	if(n==1) {
		
			    		 String[] values = line.split(",");
			    		 if(Integer.parseInt(values[0])==(id)) {
			    		    row++; }
			    		 else {
			    			 FileCsv file=new FileCsv();
			    			 file.setRow(row);
			    			 file.setId(id);
			    			 csvInfo.add(file);
			    			 row++;
			    			 id++;
			    			 
			         }
			     } 
			
		    }
			 FileCsv file=new FileCsv();

			 file.setRow(row);
			 file.setId(id);
			 csvInfo.add(file);  
		}
		
	}

	public static ArffSaver convert(String file) throws IOException{
	   // load CSV
    CSVLoader loader = new CSVLoader();
    loader.setSource(new File(file));
    Instances data = loader.getDataSet();//get instances object

    // save ARFF
    ArffSaver saver = new ArffSaver();
    saver.setInstances(data);//set the dataset we want to convert
    //and save as ARFF
    
    saver.setFile(new File(general+".arff"));
    saver.writeBatch();
    
    return saver;
    
}
	
	
	public static void main(String[] args) throws Exception {
		takeLineVersion();
		walkForward();
	}

   
	
}

