package ch.epfl.bio410;
import ij.IJ;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Scanner; // Import the Scanner class to read text files

public class csvanalysis {
    public String[] data;
    public String[][] resultmatrix;
    /*
     ABOUT THE MATRIX
     it is meant to store imagej results csvs in a matrix
     the sixth column is for the slice and the first is for the measure number
     the third is for the mean inside the roi
     the second is for the area
     the fourth is for the std of pixel values
    */

    // constructor
    public csvanalysis(String arg) {
        File file = new File(arg);
        Scanner reader = null;
        try {
            reader = new Scanner(file);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        data = new String[]{};
        while (reader.hasNextLine()){
            data = Arrays.copyOf(data, data.length + 1);
            data[data.length - 1] = reader.nextLine();
        }
        reader.close();

        // construction de la matrice
        resultmatrix = new String[data.length-1][8]; // data.length-1 is to take out the header
        for (int i = 0; i < data.length-1; i++) { // starts at 1 to avoid the header
            String[] split = data[i+1].split(",");
            int j = 0;
            while (j < 8) {
                resultmatrix[i][j] = split[j];
                j++;
            }
        }
    }

    public void print(){
        for (String datum : data) {
            IJ.log(datum);
        }
        for (int i = 0; i < data.length-1; i++) {
            IJ.log(resultmatrix[i][2]); // show means
        }
    }
    
    public int bestmeannumber(){
        int besti = 0;
        double bestvalue = 0.0;
        for (int i = 0; i < data.length-1; i++) {
            IJ.log(resultmatrix[i][2]); // show means
            double value = Double.parseDouble(resultmatrix[i][2]);
            if (value>bestvalue){
                besti = i;
                bestvalue = value;
            }
        }
        return besti;
    }
    
}
