/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package repro; 

import java.util.List;
import weka.core.Instance;
import weka.core.Instances;

/**
 *
 * @author Home
 */
public class DriftDetector {
    private final RollingWindow<Double> PredAcc_currentmodel;
    private final RollingWindow<Double> PredAcc_nextmodel;
    private final RollingWindow<Double> gt_labels;
    private final RollingWindow<Instance> instance;
    private final int win_size;
    private double drift_threshold = 0.4;
    private double drift_measure;
    
    public DriftDetector(int size)
    {
        gt_labels = new RollingWindow<>(size);        
        PredAcc_currentmodel = new RollingWindow<>(size);    
        PredAcc_nextmodel = new RollingWindow<>(size);   
        instance = new RollingWindow<>(size);
        win_size = size;
    }
        
    public boolean CheckDrift()
    {
        boolean drift = false;
        
        List<Double> list_gt_labels = gt_labels.getList();
        List<Double> list_PredAcc_currentmodel = PredAcc_currentmodel.getList();
        List<Double> list_PredAcc_nextmodel = PredAcc_nextmodel.getList();
        
        if(list_PredAcc_currentmodel.size()==list_gt_labels.size() && list_gt_labels.size()==win_size )
        {
            int counter = 0;
            drift_measure=0;
            
            //check all items in the window whether they are the same as groundtruth
            for (int i = 0; i<list_gt_labels.size(); i++)
            {
                double p = list_PredAcc_currentmodel.get(i);
                double g = list_gt_labels.get(i);                
                
                if(  p != g )
                 {
                     counter +=1;
                     
                 }                 
            }
            
            //determine accuracy based on the above adn winsize
            if ((counter/(double)win_size) > drift_threshold)
            {
                drift = true;  
                drift_measure = counter/(double)win_size;
            }  else {drift = false;}         
        }
        return drift;
    }
    
    public void Add_GT_Label(double label)
    {
        gt_labels.add(label);
    }
    
    public void AddPred_currentModel(double label)
    {
         PredAcc_currentmodel.add(label);
    }
    
    public void AddPred_nextModel(double label)
    {
        PredAcc_nextmodel.add(label);
    }
    
    public void AddInstance(Instance label)
    {
        instance.add(label);
    }
    
    public double GetAmountofData()
    {
        return gt_labels.getSize();
    }
    
    public void  Change_drift_threshold(double new_threshold)
    {
        this.drift_threshold = new_threshold;
    }
    
    public double  drift_measure()
    {
        return drift_measure;
    }
    
    public double getAccuracy()
    {
        double acc = 0;
        
        List<Double> list_gt_labels = gt_labels.getList();
        List<Double> list_PredAcc_currentmodel = PredAcc_currentmodel.getList();
        List<Double> list_PredAcc_nextmodel = PredAcc_nextmodel.getList();
        
        if(list_PredAcc_currentmodel.size()==list_gt_labels.size() && list_gt_labels.size()==win_size )
        {
            int counter = 0;
            drift_measure=0;
            
            //check all items in the window whether they are the same as groundtruth
            for (int i = 0; i<list_gt_labels.size(); i++)
            {
                double p = list_PredAcc_currentmodel.get(i);
                double g = list_gt_labels.get(i);                
                
                if(  p == g )
                 {
                     counter +=1;
                     
                 }                 
            }
            
            //determine accuracy based on the above adn winsize
            acc = (counter/(double)win_size)*100 ;                    
        }
        return acc;
    }
    
    public double getAccuracy_predicted()
    {
        double acc = 0;
        
        List<Double> list_gt_labels = gt_labels.getList();
        List<Double> list_PredAcc_currentmodel = PredAcc_currentmodel.getList();
        List<Double> list_PredAcc_nextmodel = PredAcc_nextmodel.getList();
        
        if(list_PredAcc_nextmodel.size()==list_gt_labels.size() && list_gt_labels.size()==win_size )
        {
            int counter = 0;
            drift_measure=0;
            
            //check all items in the window whether they are the same as groundtruth
            for (int i = 0; i<list_gt_labels.size(); i++)
            {
                double p = list_PredAcc_nextmodel.get(i);
                double g = list_gt_labels.get(i);                
                
                if(  p == g )
                 {
                     counter +=1;
                     
                 }                 
            }
            
            //determine accuracy based on the above adn winsize
            acc = (counter/(double)win_size)*100 ;                    
        }
        return acc;
    }
 }

