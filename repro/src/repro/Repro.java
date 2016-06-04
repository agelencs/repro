package repro;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import weka.core.Instance;
import weka.core.Instances;
import weka.classifiers.trees.J48;
import weka.core.converters.ConverterUtils.DataSource;

/**
 *
 * Adam Gelencser, University of Warwick, Computer Science Department, 2016
 */
public class Repro {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {        
        
        int rolling_winsize = 30;
        int size_small_dataset = 180000;
        int temp_data_list_size = 50;
        int perm_data_list_size = 200;
        int r = 4; //number of row for the matrix
        int c= r; // number of columns for the matrix
        int[][] matrix = new int[r][c]; //initialise the matrix
        int[][] concept_lengths = new int [r][r*2]; //matrix to hold the length of observations for each model
       
        J48 tree;
        J48 current_tree = new J48();
        //J48 new_tree;
        int c_tree = 0;
        int n_tree = 0;
        int oldcount=0;
        double [][]results = new double[size_small_dataset][3];
        double [] results_short = new double[size_small_dataset/10];
         
        int acc_counter=0;
        int counter_sec=0;
        boolean prediction_ON_OFF = false;
        //setting up the path to the datafile
        //DataSource source = new DataSource("E:/PhD/data/SEA/comb.arff");        
        
        DataSource source = new DataSource("E:/PhD/data/SEA/own_sea_3c_rep_long.arff");
        Instances dataset = source.getDataSet();
        dataset.setClassIndex(dataset.numAttributes()-1);        

        //reduce the size of the data to a smaller one
        Instances small_dataset = new Instances(dataset,0,size_small_dataset);
        Instances pred_cur_dataset = new Instances(dataset,49000,5000);
        //System.out.println(small_dataset.toString());
        CECheck cecheck = new CECheck(small_dataset);        
          
        //create the driftdetector object
        DriftDetector ddetect = new DriftDetector(rolling_winsize);
        
        //create the temp data list window
        List<Instance> temp_data_list = new ArrayList<>(temp_data_list_size);
        List<Instance> perm_data_list = new ArrayList<>(perm_data_list_size);
        List<J48> J48trees_temp = new ArrayList<>(); 
        List<J48> J48trees_perm = new ArrayList<>();
        
        int count = 0;
        boolean drift = false;
        int correctly_classified =0;
        double prequential_accuracy;
        int pos=0;
        int length_of_concept=0;
        int predicted_concept=0;
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
         
          if(ddetect.CheckDrift())
          {//if there is drift detected set up drift detected flag                         
            drift = true;   
            
            if (perm_data_list.isEmpty())
            {
                
                if (oldcount==0) 
                {
                    oldcount=count;
                }
                else
                {
                    length_of_concept = count - oldcount-(int)(rolling_winsize*0.6);
                    oldcount=count;
                    int l=matrix_nonempty_length(concept_lengths, c_tree-1, -1);
                    concept_lengths[c_tree-1][l]=length_of_concept;
                    
                }
            }
            
          }
          
          if (drift)
          {
            
              if(J48trees_perm.size()>2 )
            {
                predicted_concept=0;
                for (int i = 1; i<matrix.length;i++)
                {
                    int n=matrix[c_tree][i];            
                    if(n>predicted_concept)
                    {
                        predicted_concept=i;
                    }
                }
                //predicted_concept=concept_prediction(matrix,c_tree);
                if(count>counter_sec+8000 && predicted_concept>0)
                {
                    //System.out.println("count:"+count+" predicted tree#: "+predicted_concept);   
                    
                    System.out.println("count:" + count +" observed length for previous tree'('#"+c_tree+"')'"+length_of_concept);
                    System.out.println("count:" + count +" next predicted concept is "+predicted_concept+ " with predicted length " +length_prediction(concept_lengths,predicted_concept) );
                    
                    counter_sec=count;                    
                    current_tree = J48trees_perm.get(predicted_concept-1);
                    prediction_ON_OFF = true;                  
                    
                }              
            }              
              
            if(temp_data_list.size() < temp_data_list_size && prediction_ON_OFF == false) 
                {temp_data_list.add(data);} //build up the temportary data store
            else if (temp_data_list.size() == temp_data_list_size && prediction_ON_OFF == false) 
                    {
                        //select the relevant data
                        Instances traindata = new Instances(dataset,count-temp_data_list_size,temp_data_list_size);
                        
                        //build model
                        tree = new J48();
                        tree.buildClassifier(traindata);
                        current_tree =tree;
                        J48trees_temp.add(tree); //store built model
                        temp_data_list.clear();  //delete data   
                        System.out.println("count:" + count +" temporary tree used tree#:"+J48trees_temp.size());
                    }
            
            if(perm_data_list.size() < perm_data_list_size) 
                {perm_data_list.add(data); } //build up the permamnent data store for the training 
            else if (perm_data_list.size() == perm_data_list_size) 
                    {
                        J48trees_temp.removeAll(J48trees_temp);                    
                        //select the relevant data
                        Instances traindata = new Instances(dataset,count-perm_data_list_size,perm_data_list_size);
                        //int[] similar_trees = new int[J48trees_perm.size()];
                        ArrayList<Double> similar_trees = new ArrayList<>();
                        
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
                                                             
                                if(sim>0.01) //if it is a similar tree 
                                {
                                    similar_trees.add(sim);         //add to the list of similar trees                            
                                    if (sim>t_max) {t_max = sim; n_tree=i+1;}  //keep the most similar tree                            
                                    
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
                            prediction_ON_OFF = false;                            
                        }
                        else if (similar_trees.size()==1) //exisiting one tree
                        {
                            
                            
                            current_tree = tree;                            
                            matrix[c_tree][n_tree]++;                            
                            System.out.println("count:" + count +" enough data collected. No new tree added because similar tree found "
                                    + "in store.  tree#: "+n_tree);
                            
                            c_tree=n_tree;
                            prediction_ON_OFF = false;
                            System.out.println("");
                        }
                        else if (similar_trees.size()>1) //multiple similar trees; use the more accurate one
                        {                                                      
                            current_tree = J48trees_perm.get(n_tree-1);
                            matrix[c_tree][n_tree]++;
                            System.out.println("count:" + count +" no tree added because there was already "
                                    + "similar trees stored.  prev tree#:" + c_tree + " new tree#:"+n_tree);
                            
                            c_tree=n_tree; 
                            prediction_ON_OFF = false;                            
                        }
                             
                        perm_data_list.clear();//delete data                            
                        drift = false;                        
                    }
          } 
          acc_counter++;
          count++;  
        }while (count<(small_dataset.numInstances()-1));
        
        display_matrix(matrix);
        display_matrix(concept_lengths);
        write_to_file(results);
        write_to_file_short(results_short);        
        
        } // END OF MAIN LOOP
              
    public static void write_to_file_short (double[]x) throws IOException{
    BufferedWriter outputWriter ;
    outputWriter = new BufferedWriter(new FileWriter("results_short.txt"));
    for (int i = 0; i < x.length; i++) {
      
      outputWriter.write(Double.toString(x[i]));
      
      outputWriter.newLine();
    }
    outputWriter.flush();  
    outputWriter.close();  
    }
    
    public static void write_to_file (double[][]x) throws IOException{
    BufferedWriter outputWriter ;
    outputWriter = new BufferedWriter(new FileWriter("results.txt"));
        for (double[] x1 : x) {
            outputWriter.write(Double.toString(x1[0]));
            outputWriter.write(",");
            outputWriter.write(Double.toString(x1[1]));
            outputWriter.write(",");
            outputWriter.write(Double.toString(x1[2]));
            outputWriter.newLine();
        }
        
       /* the above is equal to:
        for (int i = 0; i < x.length; i++) {
      
      outputWriter.write(Double.toString(x[i][0]));
      outputWriter.write(",");
      outputWriter.write(Double.toString(x[i][1]));      
      outputWriter.write(",");
      outputWriter.write(Double.toString(x[i][2]));
      outputWriter.newLine();
    }*/
   
    outputWriter.flush();  
    outputWriter.close();  
    }

     public static int matrix_nonempty_length(int[][] m, int row, int column)
    {
        int length = 0;
         
        if (column == -1)
        {
            for(int i=0; i<m.length+1;i++)
            {
                if(m[row][i]!=0) length++;
            }   
        }
        if (row==-1)
        {
            for(int i=0; i<m.length+1;i++)
            {
                if(m[i][column]!=0) length++;
            }
        }
        return length;
        
    } 
     
     public static void display_matrix(int[][] tes)
     {
                  
         int r=tes.length;  
         int c = tes[0].length;
                  
         for(int i = 0;i<r;i++)
         {
           for (int j=1;j<c; j++)
           {
               System.out.print(tes[i][j]+" ");
           }           
            System.out.println();            
         }
     }        
 
public static double length_prediction(int[][] length_matrix, int conc)
        {
           double length = 0;
           int n = 0;
           double Sx = 0;
           double Sy = 0;
           double Sxx = 0;
           double Sxy= 0;
           int concept = conc-1;
           double a = 0;
           double b = 0;
           
           n=matrix_nonempty_length(length_matrix,concept,-1);
           if (n>1)
           {
            Sx = n*(n+1)/2;

            for (int i=0; i<n; i++)
            {
                Sy = Sy+length_matrix[concept][i];
                Sxy = Sxy + length_matrix[concept][i]*(i+1);
            }

            Sxx = (n*(n+1)*(2*n+1))/6;
            
            a = (Sy*Sxx - Sx*Sxy)/(n*Sxx-Sx*Sx);
            b =(n*Sxy-Sx*Sy)/(n*Sxx-Sx*Sx);

            length = a+b*(n+1);
           }
           
           return length;
        }
        
public static int concept_prediction(int[][] matrix, int concept)
{
    int n_concept = 0;
    
       for (int i = 1; i < matrix.length; i++) 
       {
        int n = matrix[concept][i];
        if (n > n_concept) 
        {
            n_concept = i;
        }
       }   
    
    return n_concept;
}

}