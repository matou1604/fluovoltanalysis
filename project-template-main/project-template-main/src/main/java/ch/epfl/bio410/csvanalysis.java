package ch.epfl.bio410;
import ij.IJ;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Scanner; // Import the Scanner class to read text files
import org.knowm.xchart.*;
import org.knowm.xchart.style.Styler;
import java.io.IOException;

public class csvanalysis {
    public String[] data;
    public String[][] resultmatrix;
    public File file;
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
        file = new File(arg);
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
        int ncols = data[1].split(",").length;
        // construction de la matrice
        resultmatrix = new String[data.length-1][ncols]; // data.length-1 is to take out the header
        for (int i = 0; i < data.length-1; i++) { // starts at 1 to avoid the header
            String[] split = data[i+1].split(",");
            int j = 0;
            while (j < ncols) {
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

    public void makechart(String savename){
        double[] xData = new double[data.length-1];
        double[] yData = new double[data.length-1];
        for (int i = 0; i < data.length-1; i++) {
            xData[i] = Double.parseDouble(resultmatrix[i][0]);
            yData[i] = Double.parseDouble(resultmatrix[i][2]);
        }
        XYChart chart = new XYChartBuilder()
                .width(800)
                .height(600)
                .title("Sample Plot")
                .xAxisTitle("X-Axis")
                .yAxisTitle("Y-Axis")
                .build();
        chart.getStyler().setChartBackgroundColor(Color.WHITE);
        chart.getStyler().setChartTitleFont(new Font("Arial", Font.BOLD, 20));
        chart.getStyler().setChartTitleBoxBorderColor(Color.BLACK);
        chart.getStyler().setMarkerSize(0);  // Set marker size to 0 to hide the dots
        chart.addSeries("Mean values", xData, yData);
        try {
            BitmapEncoder.saveBitmap(chart, savename, BitmapEncoder.BitmapFormat.PNG);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void del(){
        boolean delet = file.delete();
    }

}
