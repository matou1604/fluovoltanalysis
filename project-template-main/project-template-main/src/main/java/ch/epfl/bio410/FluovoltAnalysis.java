package ch.epfl.bio410;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.OvalRoi;
import ij.gui.Roi;
import ij.gui.WaitForUserDialog;
import ij.plugin.ImageCalculator;
import ij.plugin.ZProjector;
import ij.process.ImageProcessor;
import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import ij.plugin.frame.RoiManager;


@Plugin(type = Command.class, menuPath = "Plugins>4Dcell>Fluovolt Analysis")
public class FluovoltAnalysis implements Command {

	private String folderPath = Paths.get(System.getProperty("user.home")).toString(); // dossier à analyser
	private String resultPath = Paths.get(System.getProperty("user.home")).toString();// dossier de sorties pour les resultants
	String[] allalgochoices ={"automatic roi fitting", "manual (choose ROI)", "brut (whole image)", "mask (choose threshold)", "combined (automatic roi fitting + mask)"};
	String[] choices_2D = {allalgochoices[1], allalgochoices[2], allalgochoices[3]};
	String[] choices_3D = {allalgochoices[0], allalgochoices[1], allalgochoices[2], allalgochoices[3], allalgochoices[4]};
	String[] filetypechoices = {"2D", "3D"};
	String[] headings = {
			"Analyse 2D images:      ",
			"Analyse 3D images:"};

	/**
	 * This method is called when the command is run.
	 */
	public void run() {

		//////////////////////////////// DIALOG //////////////////////////////////
		GenericDialog dlg = new GenericDialog("Fluovolt Analysis");
		// Add text explanation
		dlg.setInsets(10,20,0);
		dlg.addMessage("Welcome to our Plugin! " +
				"\nPlease provide a folder with all TIFFs of one condition to analyse and this plugin will do the following. " +
				"\nFirst extract the raw signal from this image, and then analyse this signal to extract valuable parameter. " +
				"\nResults will given as 3 CSVs.");
		// Add path entry
		dlg.setInsets(10,75,0);
		dlg.addDirectoryField("Path to images", folderPath);
		dlg.setInsets(1,150,0);
		dlg.addCheckbox("All folders in folder", false);
		dlg.setInsets(10,75,0);
		dlg.addDirectoryField("Path to save results", resultPath);
		dlg.setInsets(1,150,0);
		dlg.addCheckbox("Same as path to images", true);

		dlg.addMessage(" ");
		dlg.setInsets(10,25,0);
		dlg.addCheckboxGroup(1, 2, filetypechoices, new boolean[]{false, true}, headings);

		dlg.setInsets(10,20,0);
		dlg.addRadioButtonGroup("Choose a 2D algorithm:", choices_2D, choices_2D.length, 2, choices_2D[1]);
		dlg.setInsets(20,20,0);
		dlg.addRadioButtonGroup("Choose a 3D algorithm:", choices_3D, choices_3D.length, 1, choices_3D[0]);

		//Panel panel3D = new Panel(new GridLayout(3, 1));
		//dlg.addPanel(panel3D); // Use GridBagConstraints
		//panel3D.setEnabled(false); // Initially disable 3D panel
		//dlg.addToSameRow();
		dlg.setInsets(20,20,0);
		dlg.addNumericField("Radius divider for ring band forming. ", 3.4, 1); 	//TODO: add a numeric field for internal?
		dlg.setInsets(1,17,0);
		dlg.addMessage("For automatic roi fitting algorithm. " +
				"\nThe bigger the number, the smaller the ring thickness.");

		// Add text
		dlg.addMessage("______________________________________________________________________________");
		dlg.addMessage("Note: nomenclature SHOULD be the following:"+
				"\nexperiment_date_day_magnification obj_fluovolt_condition_well.tif"+
				"\nExample: AK11_070125_D15_20x obj_fluovolt_Basal1_E3d.tif");

		dlg.showDialog();
		if (dlg.wasCanceled()) return;


		// Get interface values
		// Get the selected paths
		folderPath = dlg.getNextString();
		boolean all_folders = dlg.getNextBoolean();
		if (dlg.getNextBoolean()){
			resultPath = folderPath;
		} else {
			resultPath = dlg.getNextString();
		}
		// Get the selected values
		String algo2D = dlg.getNextRadioButton();
        String algo3D = dlg.getNextRadioButton();

        boolean image2D = dlg.getNextBoolean();
		boolean image3D = dlg.getNextBoolean();
		if (!image2D && !image3D){
			IJ.error("You must select at least one type of image to analyse");
			return;
		}
		double radius_divider = dlg.getNextNumber();

		// Log selection
		IJ.log("Path to images: " + folderPath);
		IJ.log("Path to save results: " + resultPath);
		IJ.log("Selected algorithms: algo3D = " + algo3D + ", algo2D = " + algo2D);

		//////////////////////////////// FILE ANALYSIS //////////////////////////////////

		// if given folder contains all conditions, run the analysis on all subfolders
		if (all_folders){
			// get the big folder
			File big_folder = new File(folderPath);
			// get all subfolders
			File[] folders = big_folder.listFiles();
			if (folders != null) {
				for (File folder : folders) {
					if (folder.isDirectory()) {
						String subfolder = folder.getAbsolutePath();
						// print only subfolder name not path, splitting with backslash, not slash
						IJ.log("Analyzing folder: " + subfolder.split("\\\\")[subfolder.split("\\\\").length-1]);
						IJ.log(subfolder);
						runanalysis(subfolder, resultPath, image2D, image3D, algo2D, algo3D, radius_divider);
					} //else IJ.log("Skipping non-directory file: " + folder.getAbsolutePath());
				}
			} else IJ.error("No subfolders found in the directory: " + folderPath);
		} else runanalysis(folderPath, resultPath, image2D, image3D, algo2D, algo3D, radius_divider);
	}

	/**
	 * @param folder folder is the path to the folder containing the tif files
	 * @param output is used to save the analysis function results, and is passed to it
	 * @param analysis2D and
	 * @param analysis3D are booleans used to choose to analyse the files corresponding to the nomenclature for 3D and/or 2D acquisitions
	 * @param algo2D is the algorithm used to analyse 2D acquisitions
	 * @param algo3D is the algorithm used to analyse 3D acquisitions
	 */
	public void runanalysis(String folder, String output, boolean analysis2D, boolean analysis3D, String algo2D, String algo3D, double radius_divider){

		// récupération de la liste de fichiers dans le dossier input
        String[] filelist = listfiles(folder); // folderpath was written here!!!)
		IJ.log("FOUND : " + filelist.length);
		// liste des fichiers tiff pertinents
        String[] filteredlist;
        if (analysis2D){
			// tri des noms de fichiers 2D
			filteredlist = filterfiles(filelist, "2D");
			IJ.log("2D files found : " + filteredlist.length);
			analyze(folder, output, filteredlist, algo2D, "2D", radius_divider);
		}
		if (analysis3D){
			// tri des noms de fichiers 3D
			filteredlist = filterfiles(filelist, "3D");
			IJ.log("3D files found : " + filteredlist.length);
			analyze(folder, output, filteredlist, algo3D, "3D", radius_divider);
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
	public void analyze(String path, String outputpath, String[] listoffiles, String algorithm, String filetype, double radius_divider){
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
				autoroi(path+"/"+s, specificoutputpath, radius_divider);
			} else if (Objects.equals(algorithm, allalgochoices[1])){
				manualroi(path+"/"+s, specificoutputpath);
			} else if (Objects.equals(algorithm, allalgochoices[2])){
				brutanalysis(path+"/"+s, specificoutputpath);
			} else if (Objects.equals(algorithm, allalgochoices[4])){
				autoroi_mask(path+"/"+s, specificoutputpath, radius_divider);
			} else if (Objects.equals(algorithm, allalgochoices[3])){
				mask(path+"/"+s, specificoutputpath);
			}
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
		//IJ.run("Close", "Results");
		ImagePlus imp = IJ.openImage(filepath);
		imp.show();
		int nFrames = imp.getStackSize();
		for (int i = 1; i <= nFrames; i++) {
			imp.setPosition(i);
			// set measurements
			IJ.run(imp, "Set Measurements...", "mean area min redirect=None decimal=3");
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

	public void autoroi(String filepath, String outputpath, double radius_divider){
		ImagePlus imp = IJ.openImage(filepath);
		int nFrames = imp.getStackSize();
		// best spot research
		// variables
		double dwidth = imp.getWidth();
		int width = imp.getWidth();
		int height = imp.getHeight();
		int radius = (int) Math.round(dwidth/3.36);
		int bandwidth = (int) (radius/radius_divider);
		int xroom = width-2*bandwidth-2*radius; // how much room for the final roi
		int yroom = height-2*bandwidth-2*radius;
		int step = Math.floorDiv(xroom, 10); // divides the remaining space by 10
		// sum of slices, roi building and measurements
		ImagePlus imp2 = ZProjector.run(imp, "sum");
		imp2.show();
		// loop to get the measures
		for (int y = 0; y < 10*step; y=y+step) {
			for (int x = 0; x < 10*step; x=x+step) {
				imp2.setRoi(new OvalRoi(x+bandwidth,y+bandwidth,2*radius,2*radius));
				IJ.run("Make Band...", "band="+bandwidth);
				IJ.run(imp, "Set Measurements...", "mean area min redirect=None decimal=3");
				IJ.run(imp2, "Measure", "");
			}
		}
		String savename = savename(outputpath, filepath, "searchresults", "csv");
		IJ.saveAs("Results", savename);
		IJ.run("Close", "Results");
		// accessing the results
		csvanalysis res = new csvanalysis(savename);
		//res.print();
		int index = res.bestmeannumber();
		int besty = index/10;
		int bestx = index - besty*10;
		besty = besty*step;
		bestx = bestx*step;
		res.del();
		// ADJUSTMENT
		step = step/4;
		for (int y = 0; y < 5*step; y=y+step) {
			for (int x = 0; x < 5*step; x=x+step) {
				imp2.setRoi(new OvalRoi(bestx+bandwidth+x-2*step,besty+bandwidth+y-2*step,2*radius,2*radius));
				IJ.run(imp2, "Make Band...", "band="+bandwidth);
				IJ.run(imp, "Set Measurements...", "mean area min redirect=None decimal=3");
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
		// measure
		imp.show();
		imp.setRoi(new OvalRoi(finalx+bandwidth,finaly+bandwidth,2*radius,2*radius));
		IJ.run("Make Band...", "band="+bandwidth);
		// add to roi manager
		RoiManager rm = new RoiManager();
		Roi roi = imp.getRoi();
		roi.setPosition(0);
		rm.addRoi(roi);
		rm.select(0);
		for (int i = 1; i <= nFrames; i++) {
			imp.setPosition(i);
			IJ.run(imp, "Set Measurements...", "mean area min redirect=None decimal=3");
			IJ.run(imp, "Measure", "");
		}
		res.del();
		savename = savename(outputpath, filepath, "rawresults", "csv");
		IJ.saveAs("Results", savename);
		IJ.run("Close", "Results");
		imp.close();
		IJ.run("Close All");
		rm.close(); // close roi manager window
		res = new csvanalysis(savename);
		savename = savename(outputpath, filepath, "rawplot", "png");
		res.makechart(savename);
	}

	public void autoroi_mask(String filepath, String outputpath, double radius_divider){
		ImagePlus imp = IJ.openImage(filepath);
		int nFrames = imp.getStackSize();
		// best spot research
		// variables
		double dwidth = imp.getWidth();
		int width = imp.getWidth();
		int radius = (int) Math.round(dwidth/3.36);
		int bandwidth = (int) (radius/radius_divider);
		int xroom = width-2*bandwidth-2*radius; // how much room for the final roi
		int step = Math.floorDiv(xroom, 10); // divides the remaining space by 10
		// sum of slices, roi building and measurements
		ImagePlus imp2 = ZProjector.run(imp, "sum");
		imp2.show();
		// loop to get the measures
		for (int y = 0; y < 10*step; y=y+step) {
			for (int x = 0; x < 10*step; x=x+step) {
				imp2.setRoi(new OvalRoi(x+bandwidth,y+bandwidth,2*radius,2*radius));
				IJ.run("Make Band...", "band="+bandwidth);
				IJ.run(imp, "Set Measurements...", "mean area min redirect=None decimal=3");
				IJ.run(imp2, "Measure", "");
			}
		}
		String savename = savename(outputpath, filepath, "searchresults", "csv");
		IJ.saveAs("Results", savename);
		IJ.run("Close", "Results");
		// accessing the results
		csvanalysis res = new csvanalysis(savename);
		//res.print();
		int index = res.bestmeannumber();
		int besty = index/10;
		int bestx = index - besty*10;
		besty = besty*step;
		bestx = bestx*step;
		res.del();
		// ADJUSTMENT
		step = step/4;
		for (int y = 0; y < 5*step; y=y+step) {
			for (int x = 0; x < 5*step; x=x+step) {
				imp2.setRoi(new OvalRoi(bestx+bandwidth+x-2*step,besty+bandwidth+y-2*step,2*radius,2*radius));
				IJ.run(imp2, "Make Band...", "band="+bandwidth);
				IJ.run(imp, "Set Measurements...", "mean area min redirect=None decimal=3");
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

		// apply a threshold and save as mask then multiply the mask with the image and measure on this new image
		imp.show();
		IJ.run("Options...", "black");
		// open threshold window image>adjust>threshold
		IJ.run("Threshold...");
		new WaitForUserDialog("Check the threshold", "Check the threshold, then click OK.").show();
		IJ.run(imp, "Convert to Mask", "");  // Convert to mask using the adjusted threshold
		//IJ.run(imp, "Convert to Mask", "method=MaxEntropy background=Dark create calculate");  // TODO: FIIIIX
		ImagePlus mask = IJ.getImage();
		IJ.run(mask, "Divide...", "value=255 stack");
		mask.show();

		ImagePlus result = ImageCalculator.run(imp, mask, "Multiply create stack");
		result.show();

		// measure
		result.show();
		result.setRoi(new OvalRoi(finalx+bandwidth,finaly+bandwidth,2*radius,2*radius));
		IJ.run("Make Band...", "band="+bandwidth);
		// add to roi manager
		RoiManager rm = new RoiManager();
		Roi roi = result.getRoi();
		roi.setPosition(0);
		rm.addRoi(roi);
		rm.select(0);

		ImageProcessor ip = result.getProcessor();
		ip.setThreshold(1, 65535, ImageProcessor.BLACK_AND_WHITE_LUT);

		for (int i = 1; i <= nFrames; i++) {
			result.setPosition(i);
			IJ.run(result, "Set Measurements...", "mean area min limit redirect=None decimal=3");
			IJ.run(result, "Measure", "");
		}
		res.del();
		savename = savename(outputpath, filepath, "rawresults", "csv");
		IJ.saveAs("Results", savename);
		IJ.run("Close", "Results");
		IJ.run("Close All");
		rm.close(); // close roi manager window
		res = new csvanalysis(savename);
		savename = savename(outputpath, filepath, "rawplot", "png");
		res.makechart(savename);
	}


	public void manualroi(String filepath, String outputpath){
		//IJ.run("Close", "Results");
		ImagePlus imp = IJ.openImage(filepath);
		imp.show();
		int nFrames = imp.getStackSize();
		WaitForUserDialog dialog = new WaitForUserDialog("Draw ROI", "Draw the ROI, then click OK. \n " +
				"\nNOTE: If you want to make a circular ring, " +
				"\nmake an inner circle using the oval tool, " +
				"\nthen use the command 'Edit > Selection > Make band(~40)'");
		dialog.setLocation(100, 100);
		dialog.show();

		// get roi on image
		RoiManager rm = new RoiManager();
		Roi roi = imp.getRoi();
		roi.setPosition(0);
		rm.addRoi(roi);
		rm.select(0);
		//rm.save(outputpath+"/roi.roi");
		//IJ.run("ROI Manager...", "");
		//rm.open(outputpath+"/roi.roi");
		for (int i = 1; i <= nFrames; i++) {
			imp.setPosition(i);
			IJ.run(imp, "Set Measurements...", "mean area min redirect=None decimal=3");
			IJ.run(imp, "Measure", "");
		}
		String savename = savename(outputpath, filepath, "rawresults", "csv");
		IJ.saveAs("Results", savename);

		IJ.run("Close", "Results");
		imp.close();
		IJ.run("Close All");
		rm.close(); // close roi manager window
		csvanalysis res = new csvanalysis(savename);
		savename = savename(outputpath, filepath, "rawplot", "png");
		res.makechart(savename);
	}


	public void mask(String filepath, String outputpath){
		ImagePlus imp = IJ.openImage(filepath);
		imp.show();
		int nFrames = imp.getStackSize();

		IJ.run("Options...", "black");
		IJ.run("Threshold...");

		new WaitForUserDialog("Check the threshold", "Choose the threshold, then click OK.").show();
		IJ.run(imp, "Convert to Mask", "background=Dark create calculate"); //TODO: FIIIX so we can modify threshold
		ImagePlus mask = IJ.getImage();
		IJ.run(mask, "Divide...", "value=255 stack");
		mask.show();

		ImagePlus result = ImageCalculator.run(imp, mask, "Multiply create stack");
		result.show();

		// measure
		ImageProcessor ip = result.getProcessor();
		ip.setThreshold(1, 65535, ImageProcessor.BLACK_AND_WHITE_LUT);

		for (int i = 1; i <= nFrames; i++) {
			imp.setPosition(i);
			// set measurements
			IJ.run(imp, "Set Measurements...", "mean area min redirect=None decimal=3");
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
//				} else if (Conditions[Conditions.length-1].length()!=4){
//					IJ.log(s+" doesn't have the correct coordinates synthax");
//					continue;
				}
			} else if (Objects.equals(filetype, "3D")){
				if (Conditions[Conditions.length-1].contains("-")){
					IJ.log(s+" has '-'. this is not expected for 3D smartheart tif file names");
					continue;
//				} else if (Conditions[Conditions.length-1].length()!=3){
//					IJ.log(s+" doesn't have the correct coordinates synthax (3 characters)");
//					continue;
				}
			}
			filtered = Arrays.copyOf(filtered, filtered.length + 1); // à chaque itération refait une place dans l'array
			filtered[filtered.length - 1] = s;

		}
		return filtered;
	}
//		//if no tiffs, error message
//		if (filtered.length == 0){
//			IJ.error("No tiff files found in the folder");
//		}
//


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
