/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package javaapplication1;

import moa.streams.ArffFileStream;

import moa.classifiers.core.driftdetection.ADWIN;
import moa.classifiers.Classifier;
import moa.classifiers.lazy.kNN;
import moa.classifiers.trees.HoeffdingTree;
import moa.classifiers.bayes.NaiveBayes;
import moa.classifiers.trees.DecisionStump;
import moa.classifiers.meta.ADACC;
import moa.classifiers.meta.OzaBagAdwin;
import weka.core.Instance;
import moa.evaluation.Accuracy;

import weka.core.Instances;
import weka.classifiers.trees.J48;
import weka.core.Instances;
import weka.classifiers.functions.SMO;



import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import moa.gui.visualization.RunVisualizer.*;
//import au.com.bytecode.opencsv.CSVReader; 
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import com.opencsv.*;
import java.util.Arrays;

/**
 *
 * @author Home
 */
public class JavaApplication1 {

    /**
     * @param args the command line arguments
     */
    
      
    
    public static void main(String[] args) throws Exception {
        // TODO code application logic here
        int numInstances = 20000;
        
        double curr_acc=0;
        double curr_oba=0;
         double curr_NB=0;
        double[] acc_adacc;
        acc_adacc = new double[numInstances];
        double[] acc_oba;
        acc_oba = new double[numInstances];
        double[] acc_NB;
        acc_NB = new double[numInstances];
        double[] pred_NB;
        pred_NB = new double[numInstances];
         Instance[] window;
        window = new Instance[numInstances];
        
        Classifier tr = new DecisionStump();
        Classifier lbay = new NaiveBayes();
        Classifier learner = new HoeffdingTree();
        ADWIN adw = new ADWIN(0.1);
        Classifier knn = new kNN();
        Classifier adacc = new ADACC();
        Classifier oba = new OzaBagAdwin();
        //Evaluator ev = new Accuracy();
        Classifier rep = new repro();
        Classifier tut = new DecisionStumpTutorial();
        //J48 tree = new J48();
        SMO svm = new SMO();
        ArffFileStream readfile = new ArffFileStream();       
        
        readfile.arffFileOption.setValue("E:/PhD/data/SEA/comb_short.arff");
        readfile.prepareForUse();                 
                        
        lbay.setModelContext(readfile.getHeader());
        learner.setModelContext(readfile.getHeader());        
        tut.setModelContext(readfile.getHeader());
        knn.setModelContext(readfile.getHeader());
        adacc.setModelContext(readfile.getHeader());
        oba.setModelContext(readfile.getHeader());        
       
        learner.prepareForUse();        
        lbay.prepareForUse();
        tut.prepareForUse();
        knn.prepareForUse();
        adacc.prepareForUse();
        oba.prepareForUse();
        
        int numberSamplesCorrect = 0;
        int numberSamplesCorrectN = 0;
        int numberSamplesCorrectTR = 0;
        int numberSamplesCorrectknn = 0;
        int numberSamplesCorrectadacc = 0;
        int numberSamplesCorrectoba = 0;
        
        int numberSamples = 0;
        boolean isTesting = true ;
        while ( readfile.hasMoreInstances()&&numberSamples < numInstances )        
        {
            Instance trainInst = readfile.nextInstance();            
            
            if (isTesting)
            {                
                
                //adwin drift detection 
                //if change detected leaners are trained
                if(adw.setInput(trainInst.value(0))) //Input data into Adwin
                {   System.out.println("Change Detected at "+numberSamples);
                    System.out.println("window width is " + adw.getWidth());
                    
                        oba.trainOnInstance(trainInst);
                        adacc.trainOnInstance(trainInst);
                        lbay.trainOnInstance(trainInst);
                    
		}  
                
                       if ( learner.correctlyClassifies(trainInst))
                       {
                           numberSamplesCorrect ++;
                       }
                       if ( lbay.correctlyClassifies(trainInst))
                       {
                           numberSamplesCorrectN ++; 
                           curr_NB = 1;
                          
                       }
                       else
                       {curr_NB = 0;}
                       
                       if ( tut.correctlyClassifies(trainInst))
                       {
                           numberSamplesCorrectTR ++;
                       }
                       if ( knn.correctlyClassifies(trainInst))
                       {
                           numberSamplesCorrectknn ++;
                       }
                       if ( adacc.correctlyClassifies(trainInst))
                       {
                           numberSamplesCorrectadacc ++;
                           curr_acc = 1;
                       }
                       else
                       {curr_acc = 0;}
                       if ( oba.correctlyClassifies(trainInst))
                       {
                           numberSamplesCorrectoba ++;
                           curr_oba = 1;
                       }
                       else
                       {curr_oba = 0;}
                       
                       
            }
            
            if (numberSamples==0)
            {
                acc_adacc[numberSamples]= curr_acc;
                acc_oba[numberSamples]= curr_oba;
                acc_NB[numberSamples]= curr_NB;
            }
            
            
            if (numberSamples>0)
            {
            acc_adacc[numberSamples]= acc_adacc[numberSamples-1] + ((curr_acc - acc_adacc[numberSamples-1])/numberSamples);
            acc_oba[numberSamples]= acc_oba[numberSamples-1] + ((curr_oba - acc_oba[numberSamples-1])/numberSamples);
            acc_NB[numberSamples]= acc_NB[numberSamples-1] + ((curr_NB - acc_NB[numberSamples-1])/numberSamples);
            
            }
            
            numberSamples ++; 
            
                        if (numberSamples<5000){
                        oba.trainOnInstance(trainInst);
                        adacc.trainOnInstance(trainInst);
                        lbay.trainOnInstance(trainInst);}
                       
                        knn.trainOnInstance(trainInst);
                        lbay.trainOnInstance(trainInst);
                        learner.trainOnInstance(trainInst);
                        tut.trainOnInstance(trainInst);
        }
        
        
        
 double accuracy = 100.0*(double)numberSamplesCorrect/(double)numberSamples; 
 System.out.println(numberSamples+" instances processed with HoeffdingTree "+accuracy+"% accuracy" ) ;
 
 double accuracyN = 100.0*(double)numberSamplesCorrectN/(double)numberSamples;
 System.out.println(numberSamples+" instances processed with NaivBayes "+accuracyN+"% accuracy" ) ;
 
 double accuracyTR = 100.0*(double)numberSamplesCorrectTR/(double)numberSamples;
 System.out.println(numberSamples+" instances processed with DecisionStump "+accuracyTR+"% accuracy" ) ;
  
 double accuracyADACC = 100.0*(double)numberSamplesCorrectadacc/(double)numberSamples;
 System.out.println(numberSamples+" instances processed with ADACC "+accuracyADACC+"% accuracy" ) ;
 
 double accuracyoba = 100.0*(double)numberSamplesCorrectoba/(double)numberSamples;
 System.out.println(numberSamples+" instances processed with OzaBagAdwin "+accuracyoba+"% accuracy" ) ;
 
 double accuracyknn = 100.0*(double)numberSamplesCorrectknn/(double)numberSamples;
 System.out.println(numberSamples+" instances processed with kNN "+accuracyknn+"% accuracy" ) ;
 //System.out.println("Mean:"+adw.getEstimation());
 //System.out.println("Variance:"+adw.getVariance());
 //System.out.println("Stand. dev:"+Math.sqrt(adw.getVariance()));
 System.out.println("Number of ADWIN drift detections: "+adw.getNumberDetections());
          
String s = Arrays.toString(acc_adacc); 
s = s.substring(1, s.length() - 2);

String s2 = Arrays.toString(pred_NB); 
s2 = s2.substring(1, s2.length() - 1);

String sNB = Arrays.toString(acc_NB); 
sNB = sNB.substring(1, sNB.length() - 1);

String csv = "E:/PhD/data/SEA/accres.csv";
CSVWriter writer = new CSVWriter(new FileWriter(csv));
 
List<String[]> data = new ArrayList<>();
data.add(new String[] {s});
//data.add(new String[] {s2});

writer.writeAll(data);
System.out.println("CSV written successfully.");
writer.close();
 }          
    
}


/*
        **** this is to create and save streams******
        
        RandomRBFGenerator stream = new RandomRBFGenerator();           
        //stream.numAttsOption.setValue(2);
              
        
        WriteStreamToARFFFile writefile = new WriteStreamToARFFFile();
        writefile.arffFileOption.setValue("c:/trial.arff");
        writefile.maxInstancesOption.setValue(numInstances);        
        writefile.streamOption.setCurrentObject(stream);
        writefile.prepareForUse();
        writefile.doTask();*/

/*
java -cp moa.jar -javaagent:"E:\MOAa\moa-release-2014\sizeofag.jar" moa.DoTask LearnModel -l trees.HoeffdingTree -s generators.WaveformGenerator -m 1000000 -O model1.moa
java -cp moa.jar -javaagent:sizeofag.jar moa.DoTask LearnModel -l trees.HoeffdingTree -s generators.WaveformGenerator -m 1000000 -O model1.moa


java -cp moa.jar -javaagent:sizeofag.jar moa.DoTask   "EvaluateModel -m (LearnModel -l trees.HoeffdingTree  -s generators.WaveformGenerator -m 1000000)  -s (generators.WaveformGenerator -i 2) -i 1000000"

java -cp moa.jar -javaagent:sizeofag.jar moa.DoTask "EvaluatePeriodicHeldOutTest -l trees.HoeffdingTree -s generators.WaveformGenerator -n 10000 -i 10000000 -f 100000" > htresult.csv
java -cp moa.jar -javaagent:sizeofag.jar moa.DoTask "EvaluatePeriodicHeldOutTest -l trees.DecisionStump -s generators.WaveformGenerator -n 10000 -i 10000000 -f 100000" > dsresult.csv



java -cp moa.jar -javaagent:sizeofag.jar moa.DoTask "EvaluatePeriodicHeldOutTest -l trees.HoeffdingTree -s (ArffFileStream -f E:\PhD\data\SEA\comb.arff) -n 10000 -i 10000000 -f 100000" > myresult.csv

EvaluatePrequential -l trees.HoeffdingTree -s (ArffFileStream -f E:\PhD\data\SEA\comb.arff) -i -1 -f 10 -q 1 -d E:\PhD\data\SEA\dumpfileHtree.csv -a 0.0
*/