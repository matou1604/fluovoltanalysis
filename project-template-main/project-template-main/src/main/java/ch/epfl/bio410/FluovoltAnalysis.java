package ch.epfl.bio410;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.OvalRoi;
import ij.plugin.ZProjector;
import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;

@Plugin(type = Command.class, menuPath = "Plugins>4Dcell>Fluovolt Analysis")
public class FluovoltAnalysis implements Command {

	private String folderPath = Paths.get(System.getProperty("user.home")).toString(); // dossier à analyser
	private String resultPath = Paths.get(System.getProperty("user.home")).toString();// dossier de sorties pour les résultats
	String[] allalgochoices ={"automatic roi fitting", "manual (choose ROI) (not working)", "brut (whole image)"};
	String[] choices_2D = {allalgochoices[1], allalgochoices[2]};
	String[] choices_3D = {allalgochoices[0], allalgochoices[1], allalgochoices[2]};
	String[] filetypechoices = {"2D and 3D", "3D", "2D"};

	/**
	 * This method is called when the command is run.
	 */
	public void run() {

		//////////////////////////////// DIALOG //////////////////////////////////
		GenericDialog dlg = new GenericDialog("Fluovolt Analysis");

		// Interface building
		// Add text explanation
		dlg.addMessage("Welcome to our Plugin! " +
				"\nPlease provide a folder with all TIFFs of one condition to analyse and this plugin will do the following. " +
				"\nFirst extract the raw signal from this image, and then analyse this signal to extract valuable parameter. " +
				"\nResults will given as 3 csvs.");
		// Add path entry
		dlg.addDirectoryField("Path to images", folderPath);
		// Add result path entry
		dlg.addDirectoryField("Path to save results", resultPath);
		// Add checkboxes
		dlg.addRadioButtonGroup("Choose a 3D algorithm:", choices_3D, choices_3D.length, 1, choices_3D[0]);
		dlg.addRadioButtonGroup("Choose a 2D algorithm:", choices_2D, choices_2D.length, 1, choices_2D[1]);
		dlg.addRadioButtonGroup("Choose the file types:", filetypechoices, filetypechoices.length, 1, filetypechoices[0]);
		// Add text
		dlg.addMessage("______________________________________________________________________________");
		dlg.addMessage("Note: nomenclature SHOULD be the following:"+
				"\n experiement_date_day_magnification obj_fluovolt_condition_well.tif"+
				"\n Example: AK11_070125_D15_20x obj_fluovolt_Basal1_E3d.tif");
		dlg.showDialog();
		if (dlg.wasCanceled()) return;

		// Get interface values
		// Get the selected paths
		folderPath = dlg.getNextString();
		resultPath = dlg.getNextString();
		// Get the selected values
		String algo3D = dlg.getNextRadioButton();
        String algo2D = dlg.getNextRadioButton();
        String filetype = dlg.getNextRadioButton();

		// Log selection
		IJ.log("Path to images: " + folderPath);
		IJ.log("Path to save results: " + resultPath);
		IJ.log("Selected algorithms: algo3D = " + algo3D + ", algo2D = " + algo2D);


		//////////////////////////////// FILE ANALYSIS //////////////////////////////////
		runanalysis(folderPath, resultPath, filetype, algo2D, algo3D);

	}

	/**
	 * @param folder folder is the path to the folder containing the tif files
	 * @param output is used to save the analysis function results, and is passed to it
	 * @param filetype is used to choose to analyse the files corresponding to the nomenclature for 3D and/or 2D acquisitions
	 * @param algo2D is the algorithm used to analyse 2D acquisitions
	 * @param algo3D is the algorithm used to analyse 3D acquisitions
	 */
	public void runanalysis(String folder, String output, String filetype, String algo2D, String algo3D){
		// récupération de la liste de fichiers dans le dossier input
        String[] filelist = listfiles(folderPath);
        // liste des fichiers tiff pertinents
        String[] filteredlist;
        if (filetype.contains("2D")){
			// tri des noms de fichiers 2D
			filteredlist = filterfiles(filelist, "2D");
			analyze(folder, output, filteredlist, algo2D, "2D");
		}
		if (filetype.contains("3D")){
			// tri des noms de fichiers 2D
			filteredlist = filterfiles(filelist, "3D");
			analyze(folder, output, filteredlist, algo3D, "3D");
		}
	}

	/**
	 * this function runs the adequate macro (selected with the algorithm string) on a listoffiles
	 * the folder to iterate in is specified in path and the output folder is in outputpath
	 * @param listoffiles is the list of files to analyze
	 * @param algorithm is which macro algorithm to use
	 * @param outputpath is the path to the output folder to create the csv and graphs
	 * @param path is the path to the folder containing the files
	 */
	public void analyze(String path, String outputpath, String[] listoffiles, String algorithm, String filetype){
		IJ.log("");
		IJ.log("Analysis done with parameters :");
		IJ.log("input folder = " + path);
		IJ.log("output folder = " + outputpath);
		IJ.log("algorithm used = " + algorithm);
		IJ.log("files analysed :");

		for (String s : listoffiles){
			// TODO complete all the functions
			String specificoutputpath = getfinalpath(outputpath, s);
			boolean mkdirs = new File(specificoutputpath).mkdirs();

			// Choosing the algorithm the options are {"automatic roi fitting", "manual (move ROI)", "brut (for 2D images)"}
			if (Objects.equals(algorithm, allalgochoices[0])){
				autoroi(path+"/"+s, specificoutputpath);
			} else if (Objects.equals(algorithm, allalgochoices[1])){
				manualroi(path+"/"+s, specificoutputpath);
			} else if (Objects.equals(algorithm, allalgochoices[2])){
				brutanalysis(path+"/"+s, specificoutputpath);
			}

			// TODO setup a matrix return type for the analysis functions to pass it to the analysis. We can still save the raw data gathered.

			// TODO in my opinion the graphical analysis should be here

			IJ.log(" - " + s); // the filename prints only when the analysis is done to be able to see which one were done and which doesn't if something goes wrong
		}
	}

	public String getfinalpath(String outputpath, String name){
		String[] splittedname = name.split("_");
		String drug = splittedname[5];
		String day = splittedname[2];
		return outputpath+"/"+day+"/"+drug;
	}

	public void brutanalysis(String filepath, String outputpath){
		ImagePlus imp = IJ.openImage(filepath);
		int nFrames = imp.getStackSize();
		for (int i = 1; i <= nFrames; i++) {
			imp.setPosition(i);
			IJ.run(imp, "Measure", "");
		}
		String savename = savename(outputpath, filepath, "rawresults", "csv");
		IJ.saveAs("Results", savename);
		IJ.run("Close", "Results");
		imp.close();
		IJ.run("Close All");
		csvanalysis res = new csvanalysis(savename);
		savename = savename(outputpath, filepath, "rawplot", "png");
		res.makechart(savename);
	}

	public void autoroi(String filepath, String outputpath){
		ImagePlus imp = IJ.openImage(filepath);
		int nFrames = imp.getStackSize();
		// best spot research
		// variables
		double dwidth = imp.getWidth();
		int width = imp.getWidth();
		int height = imp.getHeight();
		int radius = (int) Math.round(dwidth/3.36);
		int bandwidth = radius/5;
		int xroom = width-2*bandwidth-2*radius; // how much room for the final roi
		int yroom = height-2*bandwidth-2*radius;
		int step = xroom/10; // divides the remaining space by 10
		// sum of slices, roi building and measurements
		ImagePlus imp2 = ZProjector.run(imp, "sum");
		imp2.show();
		// loop to get the measures
		for (int y = 0; y < yroom; y=y+step) {
			for (int x = 0; x < xroom; x=x+step) {
				imp2.setRoi(new OvalRoi(x+bandwidth,y+bandwidth,2*radius,2*radius));
				IJ.run("Make Band...", "band="+bandwidth);
				IJ.run(imp2, "Measure", "");
			}
		}
		String savename = savename(outputpath, filepath, "searchresults", "csv");
		IJ.saveAs("Results", savename);
		IJ.run("Close", "Results");
		// accessing the results
		csvanalysis res = new csvanalysis(savename);
		res.print();
		int index = res.bestmeannumber();
		int besty = index/10;
		int bestx = index - besty*10;
		besty = besty*step;
		bestx = bestx*step;
		res.del();
		// AJUSTEMENT
		step = step/4;
		for (int y = 0; y < 5*step; y=y+step) {
			for (int x = 0; x < 5*step; x=x+step) {
				imp2.setRoi(new OvalRoi(bestx+bandwidth+x-2*step,besty+bandwidth+y-2*step,2*radius,2*radius));
				IJ.run(imp2, "Make Band...", "band="+bandwidth);
				IJ.run(imp2, "Measure", "");
			}
		}
		IJ.saveAs("Results", savename);
		IJ.run("Close", "Results");
		res = new csvanalysis(savename);
		index = res.bestmeannumber();
		int corry = index/5;
		int corrx = index - corry*5;
		imp2.close();
		int finalx = bestx+(corrx-2)*step;
		int finaly = besty+(corry-2)*step;
		// mesure
		imp.show();
		for (int i = 1; i <= nFrames; i++) {
			imp.setRoi(new OvalRoi(finalx+bandwidth,finaly+bandwidth,2*radius,2*radius));
			IJ.run("Make Band...", "band="+bandwidth);
			imp.setPosition(i);
			IJ.run(imp, "Measure", "");
		}
		res.del();
		savename = savename(outputpath, filepath, "rawresults", "csv");
		IJ.saveAs("Results", savename);
		IJ.run("Close", "Results");
		imp.close();
		IJ.run("Close All");
		res = new csvanalysis(savename);
		savename = savename(outputpath, filepath, "rawplot", "png");
		res.makechart(savename);
	}

	public void manualroi(String filepath, String outputpath){
		// TODO manual roi drawing macro translation
		ImagePlus imp = IJ.openImage(filepath);
		// IJ.error("Manual Roi method not implemented yet");
//		String savename = savename(outputpath, filepath, "rawresults");
//		IJ.saveAs("Results", savename);
//		IJ.run("Close", "Results");
		imp.close();
	}

	public String savename(String outputpath, String filepath, String complement, String format){
		String name = getthename(filepath);
		// creating the file name
		name = complement+"_"+name+"."+format; // name now contains the name of the csv file
		outputpath = outputpath+"/"+name;
		return outputpath; // full path name included
	}

	/*
	gets the file name without extension from a path
	 */
	public String getthename(String path){
		//extracting the file name without extension
		String name = path.split("\\.")[0];
		String[] splitname = name.split("/");
		name = splitname[splitname.length-1];
		return name;
	}

	/**
	 * This function filters the files in the list according to the filetype.
	 * This function keeps the valid file names for the intended analysis
	 * filenames should be like :
	 * 	experimentatorname_date_dayofculture_objective_staining_drug_wellcoordinates
	 *  with wellcoordinates being :
	 * 	2D : 4 characters including '-'
	 * 	3D : 3 characters, the two firsts being the well coordinates and the last one being a letter for the specific pillar
	 * @param list is the list of files to sort from
	 * @param filetype is the expected filetype (used to choose which nomenclature to research)
	 * @return filtered list of only TIFF files
	 */
	public String[] filterfiles(String[] list, String filetype){

		String[] filtered = new String[0]; // the result array containing the valid filenames at the end of the function
		String name; // to get the file name without extension, used in the loop
		IJ.log("");
		IJ.log("Sorting files...");
		// d'abord les critères communs à tous les fichiers :
		for (String s : list) {
			if (!s.contains(".")){ // si le fichier n'est pas un fichier (n'a pas d'extension)
				continue;
			}
			name = s.split("\\.")[0];
			String[] Conditions = name.split("_");
			if (!s.contains("tif")){ // si le fichier n'est pas un tif
				continue;
			} else if (Objects.equals(filetype, "2D")){
				if (!Conditions[Conditions.length-1].contains("-")){
					continue;
				} else if (Conditions[Conditions.length-1].length()!=4){
					IJ.log(s+" doesn't have the correct coordinates synthax");
					continue;
				}
			} else if (Objects.equals(filetype, "3D")){
				if (Conditions[Conditions.length-1].contains("-")){
					IJ.log(s+" has '-'. this is not expected for 3D smartheart tif file names");
					continue;
				} else if (Conditions[Conditions.length-1].length()!=3){
					IJ.log(s+" doesn't have the correct coordinates synthax (3 characters)");
					continue;
				}
			}
			filtered = Arrays.copyOf(filtered, filtered.length + 1); // à chaque itération refait une place dans l'array
			filtered[filtered.length - 1] = s;
		}
		return filtered;
	}

	public String[] listfiles(String path){
		/*
		prend le chemin du dossier en entrée
		renvoie un array de strings taille variable contenant les noms des fichiers à l'intérieur
		*/

		File folder = new File(path);      // crée un objet de type File ?
		File[] files = folder.listFiles(); // récupère les noms de fichiers avec la méthode de la classe File ?
		String[] filelist = new String[0]; // sortie

		if (files != null) { // si y'a des fichiers dans le dossier
			for (File file : files) { // loop dans les fichiers ?
				filelist = Arrays.copyOf(filelist, filelist.length + 1); // à chaque itération refait une place dans l'array
				filelist[filelist.length - 1] = file.getName();  // ajoute le nouveau fichier
			}
		} else {
			IJ.log("The directory is empty or does not exist."); // quand il n'y a pas de fichier log le message dans imagej
		}
		return filelist;
	}

	/**
	 * This main function serves for development purposes.
	 * It allows you to run the plugin immediately out of
	 * your integrated development environment (IDE).
	 *
	 * @param args whatever, it's ignored
	 * @throws Exception whatever it's doing
	 */
	// LANCE IMAGEJ POUR TESTER LE PLUGIN
	public static void main(final String... args) throws Exception {
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();
	}
}
