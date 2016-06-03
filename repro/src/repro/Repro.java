/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package repro;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import java.util.ArrayList;

import java.util.List;

import weka.core.Instance;
import weka.core.Instances;
import weka.classifiers.trees.J48;

import weka.core.converters.ConverterUtils.DataSource;

/**
 *
 * @author Home
 */
public class Repro {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

        
        int rolling_winsize = 30;
        int size_small_dataset = 90000;
        int temp_data_list_size = 50;
        int perm_data_list_size = 200;
        int r = 4; //number of row for the matrix
        int c= r; // number of columns for the matrix
        int[][] matrix = new int[r][c]; //initialise the matrix
        J48 tree;
        J48 current_tree = new J48();
        //J48 new_tree;
        int c_tree = 0;
        int n_tree = 0;
        double [][]results = new double[90000][3];
        double [] results_short = new double[9000];
        int acc_counter=0;
        int counter_sec=0;
        //setting up the path to the datafile
        //DataSource source = new DataSource("E:/PhD/data/SEA/comb.arff");
        
        
        DataSource source = new DataSource("E:/PhD/data/SEA/own_sea_3c_rep.arff");
        Instances dataset = source.getDataSet();
        dataset.setClassIndex(dataset.numAttributes()-1);
        

        //reduce the size of the data to a smaller one
        Instances small_dataset = new Instances(dataset,0,size_small_dataset);
        Instances pred_cur_dataset = new Instances(dataset,49000,5000);
        //System.out.println(small_dataset.toString());
        CECheck cecheck = new CECheck(small_dataset);        
                
        
        
        
        //Evaluation eval = new Evaluation(small_dataset);
       // Random rand = new Random(1);
        //int folds = 10;
        
        //create the driftdetector object
        DriftDetector ddetect = new DriftDetector(rolling_winsize);
        
        //create the temp data list window
        List<Instance> temp_data_list = new ArrayList<Instance>(temp_data_list_size);
        List<Instance> perm_data_list = new ArrayList<Instance>(perm_data_list_size);
        List<J48> J48trees_temp = new ArrayList<J48>(); 
        List<J48> J48trees_perm = new ArrayList<J48>();
        
        int count = 0;
        boolean drift = false;
        int correctly_classified =0;
        double prequential_accuracy = 0;
        int pos=0;
        //read new data in one by one
        do
        {
          Instance data = small_dataset.instance(count);
            
          //add data and label to drift detection
          double gt_label = data.value(data.numAttributes()-1);
          ddetect.AddLabel(gt_label);
          
          
          if(J48trees_perm.isEmpty() && J48trees_temp.isEmpty())
          {
          ddetect.AddPred_currentModel(pred_cur_dataset.instance(count).value(pred_cur_dataset.numAttributes()-1));
          
          }
          else
          {
            double pred = current_tree.classifyInstance(data);
            
             ddetect.AddPred_currentModel(pred);
             //System.out.println(ddetect.getAccuracy()); 
            
            if (pred==gt_label) {correctly_classified++;}
              
            prequential_accuracy = (ddetect.getAccuracy());//(double)correctly_classified/acc_counter)*100;
            
            results[count][0] = gt_label;
            results[count][1] = pred;
            results[count][2] = prequential_accuracy;
            if((count%10)==0)
            {
                results_short[pos]= prequential_accuracy;
                pos++;
            }
          }
          
            
         
          if( ddetect.CheckDrift())
          {//if there is drift detected set up drift detected flag
                         
            drift = true;            
             
            //ddetect.drift_measure();
            //System.out.println("drift "+ "# samples: " + ddetect.GetAmountofData() + "measure: "+ddetect.drift_measure());              
          }
          
          if (drift)
          {
            
              if(J48trees_perm.size()>2 )
            {
                int m=0;
                for (int i = 1; i<matrix.length;i++)
                {
                    int n=matrix[c_tree][i];            
                    if(n>m)
                    {
                        m=i;
                    }
                }
                if(count>counter_sec+8000 && m>0)
                {
                    //c_tree=m;
                    //current_tree = J48trees_perm.get(c_tree-1);
                    //m=0;
                    //drift = false;
                    System.out.println("count:"+count+" predicted tree#: "+m);
                    //counter_sec=count;
                    counter_sec=count;
                }
              
            }
              
              
            if(temp_data_list.size() < temp_data_list_size) 
                {temp_data_list.add(data);} //build up the temportary data store
            else if (temp_data_list.size() == temp_data_list_size) 
                    {
                        //select the relevant data
                        Instances traindata = new Instances(dataset,count-temp_data_list_size,temp_data_list_size);
                        
                        //build model
                        tree = new J48();
                        tree.buildClassifier(traindata);
                        current_tree =tree;
                        J48trees_temp.add(tree); //store built model
                        temp_data_list.clear();  //delete data   
                        System.out.println("count:" + count +" temprary tree used tree#:"+J48trees_temp.size());
                    }
            
            if(perm_data_list.size() < perm_data_list_size) 
                {perm_data_list.add(data); } //build up the permamnent data store for the training 
            else if (perm_data_list.size() == perm_data_list_size) 
                    {
                        J48trees_temp.removeAll(J48trees_temp);
                       // acc_counter=0;
                        //correctly_classified=0;
                        //select the relevant data
                        Instances traindata = new Instances(dataset,count-perm_data_list_size,perm_data_list_size);
                        //int[] similar_trees = new int[J48trees_perm.size()];
                        ArrayList<Double> similar_trees = new ArrayList<Double>();
                        
                        boolean first = false;
                        //build model
                        tree = new J48();
                        tree.buildClassifier(traindata);
                        double t_max = 0;
                        
                        for (int i = 0; (i<J48trees_perm.size()) || J48trees_perm.isEmpty(); i++)
                        {
                            if(J48trees_perm.isEmpty()) //as storage is empty add the first perm built model to it
                            {
                                J48trees_perm.add(tree);
                                c_tree = 1;
                                current_tree = tree;
                                first =true;
                               System.out.println("count:" + count +" first tree added because there was no other tree yet stored in perm list.  prev tree#: n/a, current tree#: " + c_tree);
                            }                            
                            else {
                            
                                double sim = (cecheck.ce_check(J48trees_perm.get(i), tree)); //returns accuracy measure if it is more than threshold
                                                             
                                if(sim>0.01) //if there are similar tree already
                                {
                                    similar_trees.add(sim); 
                                    //System.out.println(sim);
                                    //System.out.println(similar_trees.size());
                                    if (sim>t_max) {t_max = sim; n_tree=i+1;}                             
                                    
                                }
                            }
                        }
                        ///////////////////////////////////
                        if (similar_trees.isEmpty() && first == false) // new tree
                        {//if it could not find any similar trees
                            
                            J48trees_perm.add(tree);   
                            n_tree = J48trees_perm.size();
                            current_tree = tree;
                            
                            System.out.println("count:" + count +" new tree added because there "
                                    + "was not any other similar already stored.  prev tree#:" + c_tree + " new tree#:"+n_tree);
                            
                            matrix[c_tree][n_tree]++;
                            c_tree = n_tree;
                            
                        }
                        else if (similar_trees.size()==1) //exisiting one tree
                        {
                            current_tree = tree;                            
                            matrix[c_tree][n_tree]++;                            
                            System.out.println("count:" + count +" no tree added because there was already "
                                    + "similar tree stored.  prev tree#:" + c_tree + " new tree#:"+n_tree);
                            
                            c_tree=n_tree;
                        }
                        else if (similar_trees.size()>1) //multiple similar trees; use the more accurate one
                        {                                                      
                            current_tree = J48trees_perm.get(n_tree-1);
                            matrix[c_tree][n_tree]++;
                            System.out.println("count:" + count +" no tree added because there was already "
                                    + "similar tree stored.  prev tree#:" + c_tree + " new tree#:"+n_tree);
                            
                            c_tree=n_tree;                           
                            
                        }
                        
                        

                                              
                        //J48trees_perm.add(tree); //store built model
                        perm_data_list.clear();//delete data    
                        
                        drift = false;
                    }
          } 
          acc_counter++;
          count++;  
        }while (count<(small_dataset.numInstances()-1));
        
        display_matrix(matrix);
        write_to_file(results);
        write_to_file_short(results_short);
        
        //System.out.println(J48trees_temp.size());
        //System.out.println(J48trees_perm.size());
        
       //List<J48> J48trees_perm = new ArrayList<J48>();
        
     
      
        /*for(int i = 0;i<r;i++)
        {
           matrix[0][i] = i; 
        }
        display_matrix(matrix);
        
        
        int y = matrix_nonempty_length(matrix,-1,1);
                
        */
    
 
//        cecheck.ce_check(J48trees_perm.get(0), J48trees_perm.get(1));
        }
        //LearnModel lm = new LearnModel(htree, readfile, 10, 1);
        
        //print out statistics from weka
        /*
        eval.crossValidateModel(tree, small_dataset, folds, rand);
        System.out.println( eval.toSummaryString("eval summary:", false));
        System.out.println( eval.pctCorrect());
        System.out.println( eval.pctIncorrect());*/
        
       //Prepare and start the evaluation
       // ClassificationPerformanceEvaluator evaluator = 	new BasicClassificationPerformanceEvaluator();
        /*EvaluateModel em = new EvaluateModel();
        em.modelOption.setCurrentObject(resultingModel);
        em.streamOption.setCurrentObject(rbfDriftStream);
        em.maxInstancesOption.setValue(maxInstances);
        em.evaluatorOption.setCurrentObject(evaluator);
        Object resultingEvaluation = em.doTask();*/
       
    public static void write_to_file_short (double[]x) throws IOException{
    BufferedWriter outputWriter = null;
    outputWriter = new BufferedWriter(new FileWriter("results_short.txt"));
    for (int i = 0; i < x.length; i++) {
      
      outputWriter.write(Double.toString(x[i]));
      
      outputWriter.newLine();
    }
    outputWriter.flush();  
    outputWriter.close();  
    }
    
    public static void write_to_file (double[][]x) throws IOException{
    BufferedWriter outputWriter = null;
    outputWriter = new BufferedWriter(new FileWriter("results.txt"));
    for (int i = 0; i < x.length; i++) {
      
      outputWriter.write(Double.toString(x[i][0]));
      outputWriter.write(",");
      outputWriter.write(Double.toString(x[i][1]));      
      outputWriter.write(",");
      outputWriter.write(Double.toString(x[i][2]));
      outputWriter.newLine();
    }
    outputWriter.flush();  
    outputWriter.close();  
    }

     public static int matrix_nonempty_length(int[][] m, int row, int column)
    {
        int length = 0;
        int i=0;
        
        if (column == -1)
        {
            for(i=0; i<m.length;i++)
            {
                if(m[row][i]!=0) length++;
            }   
        }
        if (row==-1)
        {
            for(i=0; i<m.length;i++)
            {
                if(m[i][column]!=0) length++;
            }
        }
        return length;
        
    } 
     
     public static void display_matrix(int[][] tes)
     {
         int r=tes.length;
         int c=r;
         
                  
         for(int i = 1;i<r;i++)
        {
           for (int j=1;j<r; j++)
           {
               System.out.print(tes[i][j]+" ");
           }           
            System.out.println();
            
        }
     }
        
 }
    

