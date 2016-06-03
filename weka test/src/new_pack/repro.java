/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package new_pack;

import weka.classifiers.trees.J48;

/**
 *
 * @author Home
 */
public class repro {
    
    public static void main(String[] args) throws Exception {
    System.out.println("count:");
    J48 tree = new J48();
    tree.buildClassifier(null);
    }
    
}



