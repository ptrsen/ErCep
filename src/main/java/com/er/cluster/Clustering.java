package com.er.cluster;

import com.er.erCep.Main;
import com.sun.org.apache.xpath.internal.operations.Bool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


//import org.ejml.simple.SimpleMatrix;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import weka.classifiers.trees.J48;
import weka.clusterers.ClusterEvaluation;
import weka.clusterers.XMeans;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.LibSVMSaver;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.*;

import weka.core.Utils;
import weka.filters.unsupervised.instance.RemoveWithValues;


public class Clustering {

    private String path_train,path_test;
    private static final Logger log = LoggerFactory.getLogger(Main.class);



    public Clustering(String path_train,String path_test){
        this.path_train = path_train;
        this.path_test = path_test;
    }


    public void  ClModel()  {

        // Loading Data. .arff data without missing values, outliers, etc, pre-processing before
        Instances data_train = null;
        Instances data_test = null;
        try {
            data_train = new Instances(readDataFile(path_train));
            data_test = new Instances(readDataFile(path_test));

        } catch (IOException e) {
            e.printStackTrace();
            log.info("Can't  read train and test files for clustering");
        }

        int noAttributes_train = data_train.numAttributes();  // number of attributes with class
        int noAttributes_test = data_test.numAttributes();

        data_train.setClassIndex(noAttributes_train - 1);    // defining the class attribute , in Arff is the last
        data_test.setClassIndex(noAttributes_test - 1);


        // Filter to remove Class Attribute from Train data to clustering
        Remove filter_ClassRemove = new Remove();
        filter_ClassRemove.setAttributeIndices("" + (data_train.classIndex() + 1));


        // Filter to Normalize or  Standarize data (both)
 //       Normalize filterNorm = new Normalize();
 //       Standardize filterStan = new Standardize();


        Instances dataToCluster = null;   //  data to cluster (without class attribute)
        Instances ClusteredData = null;   // clustered data

        String[] options = null;          // options for commands


        // Expected Train Rule tree
        try {
            J48 train_rules = new J48();
            options = Utils.splitOptions("-C 0.25 -M 2");
            train_rules.setOptions(options);
            train_rules.buildClassifier(data_train);

            log.info(train_rules.toString() );
        } catch (Exception e) {
            e.printStackTrace();
        }


        XMeans xmean = new XMeans();      // model for clustering

        // Distances measures EuclideanDistance, ChebyshevDistance, ManhattanDistance,MinkowskiDistance

        try {

            filter_ClassRemove.setInputFormat(data_train);
            dataToCluster = filter_ClassRemove.useFilter(data_train, filter_ClassRemove);
            options = Utils.splitOptions("-I 1 -M 1000 -J 1000 -L 2 -H 10 -B 1.0 -C 0.5 -D \"weka.core.EuclideanDistance -R first-last\" -S 10");
            xmean.setOptions(options);
            xmean.buildClusterer(dataToCluster);

            // Cluster Evaluation
            log.info("Evaluation:");
            ClusterEvaluation eval = new ClusterEvaluation();
            eval.setClusterer(xmean);
            eval.evaluateClusterer(data_test);
            log.info(eval.clusterResultsToString());

        } catch (Exception e) {
            e.printStackTrace();
        }


        try {
            // Assigning Cluster class
            AddCluster assignclusters= new AddCluster();
            assignclusters.setClusterer(xmean);
            assignclusters.setInputFormat(dataToCluster);
            ClusteredData = Filter.useFilter(dataToCluster, assignclusters);
            ClusteredData.setClassIndex(noAttributes_train - 1);
/*
            LibSVMSaver s= new LibSVMSaver();
            s.setInstances(ClusteredData);
            s.setFile(new File("src/main/resources/svm/DataXmeanClustered.libSvm"));
            s.writeBatch();
*/

/*
            // Save file with Cluster class assigned
            ArffSaver s= new ArffSaver();
            s.setInstances(ClusteredData);
            s.setFile(new File("src/main/resources/svm/DataXmeanClustered.arff"));
            s.writeBatch();
*/

        } catch (Exception e) {
            e.printStackTrace();
        }


        int no_ClusterClass = ClusteredData.numClasses();
        log.info("Number of clases: " + Integer.toString(no_ClusterClass));

        // Expected Cluster Rule tree
        try {
            J48 cluster_rules = new J48();
            options = Utils.splitOptions("-C 0.25 -M 2");
            cluster_rules.setOptions(options);
            cluster_rules.buildClassifier(ClusteredData);
            log.info(cluster_rules.toString() );
        } catch (Exception e) {
            e.printStackTrace();
        }



        //  weka.filters.unsupervised.instance.RemoveWithValues -S 0.0 -C last -L 3,4 -V -H  cluster 3 <-class 2 cluster 4 <-class 1

/*
        // Filters to extract each cluster data into list
        ArrayList<Instances> sepa = new ArrayList<>();	 // <-- list of instances, each position has all the data that belongs to each Cluster class

        for (int i =1 ; i< (no_ClusterClass+1); i++) {
            Instances a = null;
            Instances b = null;

            try {

                //filter by cluster
                RemoveWithValues filter2 = new RemoveWithValues();
               // options = Utils.splitOptions("-S 0.0 -C last -L "+Integer.toString(i)+" -V -H");
                options = Utils.splitOptions("-S 0.0 -C last -L "+Integer.toString(i)+" -V");
                filter2.setInputFormat(ClusteredData);
                filter2.setOptions(options);
                a = Filter.useFilter(ClusteredData, filter2); // just instances of one cluster1 then cluster2 ..
               // a.setClassIndex(noAttributes_train - 1);


                sepa.add(a); // separate in List


/*
                //	log.info(a.toString()); // data from one cluster
                // change name to +1 for one-class svm
                RenameNominalValues renameFilter = new RenameNominalValues();
                renameFilter.setInputFormat(a);
                options = Utils.splitOptions("-R last -N cluster4:6 ");
                renameFilter.setOptions(options);
                b = Filter.useFilter(a,renameFilter);
             //   	log.info(b.toString()); // rename class cluster
/
            /*    // remove class
                filter_ClassRemove = new Remove();
                filter_ClassRemove.setAttributeIndices("" + (b.classIndex() + 1));
                filter_ClassRemove.setInputFormat(b);
                b = Filter.useFilter(b, filter_ClassRemove);

                	log.info(b.toString()); // data from one cluster withoutClass
             /

            } catch (Exception e) {
                e.printStackTrace();
            }



           // sepa.add(b);    // Weka instances for each class in each array list position
        }

*/
        //Instances clas1_2 = Instances.mergeInstances(sepa.get(0),sepa.get(1));

        // log.info(  "two headers ------------------"+Boolean.toString(sepa.get(0).equalHeaders(sepa.get(1)))   );
/*
        try
            // Save files for one cluster
           // Instances clas1_2 = Instances.mergeInstances(sepa.get(0),sepa.get(1));
            LibSVMSaver s= new LibSVMSaver();
            s.setInstances(clas1_2);
            s.setFile(new File("src/main/resources/svm/cluster1.libSvm"));
            s.writeBatch();
        } catch (Exception e) {
            e.printStackTrace();
        }
*/


    }


    private static BufferedReader readDataFile(String filename) {
        BufferedReader inputReader = null;
        try {
            inputReader = new BufferedReader(new FileReader(filename));
        }
        catch (FileNotFoundException ex) {
            log.info("File not found: " + filename);
        }
        return inputReader;
    }



}
