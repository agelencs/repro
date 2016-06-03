/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package repro;

import java.util.Random;
import weka.classifiers.trees.J48;
import moa.tasks.EvaluateModel;
import moa.tasks.LearnModel;
import weka.classifiers.Evaluation;
import weka.core.FastVector;
import weka.core.Instances;
/**
 *
 * @author Home
 */
public class CECheck {
    
    private double equivalence_threshold = 0.7;
    private Instances dataset;
    Evaluation eval;
    
    
    public CECheck(Instances d) throws Exception
    {
        dataset = d;
        eval = new Evaluation(dataset);
    }
    
    public double ce_check(J48 tree1, J48 tree2) throws Exception
    {
        double result = 0;
        int s = 0;
        int sum = 0;
        for(int i = 0; i<dataset.numInstances(); i++)
        {
            double r1 = tree1.classifyInstance(dataset.instance(i));
            double r2 = tree2.classifyInstance(dataset.instance(i));         
            double l1 = dataset.instance(i).value(dataset.numAttributes()-1);
            if (r1 == r2) s=1;
            if (r1!=r2 && r1!=l1 && r2!=l1) s=0;//do nothing or increase s with 0}
            if (r1!=r2 && (r1==l1 || r2==l1))    s=-1;
            sum = sum+s;            
        }
        
        //System.out.println("counter "+sum);
        double acc = (sum/(double)dataset.numInstances());
        if(acc > equivalence_threshold) 
        {
            result = acc;
           //System.out.println("acc "+(sum/(double)dataset.numInstances()));
        }
        //if (acc == 1) result = 99;
         //System.out.println(result);
        //System.out.println(result);
        return result;
       
    }
    
     
    
    
}
