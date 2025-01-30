package ch.epfl.bio410;

import ij.IJ;
import ij.gui.GenericDialog;
import net.imagej.ImageJ;
import net.imglib2.algorithm.Algorithm;
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
	private String selectedAlgorithm = "automatic roi fitting"; //TODO remplacer par algo2D et algo3D
	private String[] fileList = new String[]{};
	String[] choices = {"automatic roi fitting", "manual (move ROI)", "brut (for 2D images)"};
	
	// variables à relier à l'UI
	private String filetype = "2D and 3D"; // values : "2D", "3D" or "2D and 3D" //TODO créer des boutons pour sélectionner les trois boutons
	private String algo2D = selectedAlgorithm; //TODO créer des boutons pour ça
	private String algo3D = selectedAlgorithm; //TODO créer des boutons pour ça
	private String[] filelist;
	private String[] filteredlist; // liste des fichiers tiff pertinents


	/**
	 * This method is called when the command is run.
	 */
	public void run() {

		//////////////////////////////// DIALOG //////////////////////////////////
		GenericDialog dlg = new GenericDialog("Fluovolt Analysis");

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
		dlg.addRadioButtonGroup("Choose an algorithm:", choices, choices.length, 1, choices[0]);

		dlg.addMessage("______________________________________________________________________________");
		dlg.addMessage("Note: nomenclature SHOULD be the following:"+
				"\n experiement_date_day_magnification obj_fluovolt_condition_well.tif"+
				"\n Example: AK11_070125_D15_20x obj_fluovolt_Basal1_E3d.tif");

		dlg.showDialog();
		if (dlg.wasCanceled()) return;

		// Get the selected paths
		folderPath = dlg.getNextString();
		resultPath = dlg.getNextString();
		selectedAlgorithm = dlg.getNextRadioButton();


		IJ.log("Path to images: " + folderPath);
		IJ.log("Path to save results: " + resultPath);
		IJ.log("Selected algorithm: " + selectedAlgorithm);



		//////////////////////////////// FILE EXTRACTION //////////////////////////////////
		
		runanalysis(folderPath, resultPath, filetype, algo2D, algo3D);


	}

	/**
	 * function for doing all the file sorting and calling the analysis function.
	 * it does call the analysis function twice in case the 2D and 3D files are both targeted
	 * once, it gets the files in the folder. twice it sorts them to get only the ones with 2D and 3D nomenclature
	 * algo2D algo3D variables are used to choose the algorithm for each file type
	 * output is used to save the analysis function results, and is passed to it
	 * @param folder
	 * @param output
	 * @param filetype
	 * @param algo2D
	 * @param algo3D
	 */
	public void runanalysis(String folder, String output, String filetype, String algo2D, String algo3D){
		// récupération de la liste de fichiers dans le dossier input
		filelist = listfiles(folderPath);
		if (filetype.contains("2D")){
			// tri des noms de fichiers 2D
			filteredlist = filterfiles(filelist, "2D");
			analyze(folder, output, filteredlist, algo2D);
		}
		if (filetype.contains("3D")){
			// tri des noms de fichiers 2D
			filteredlist = filterfiles(filelist, "3D");
			analyze(folder, output, filteredlist, algo3D);
		}
	}

	/**
	 * this function runs the adequate macro (selected with the algorithm string) on a listoffiles
	 * the folder to iterate in is specified in path and the output folder is in outputpath
	 * @param listoffiles
	 * @param algorithm
	 * @param outputpath
	 * @param path
	 */
	public void analyze(String path, String outputpath, String[] listoffiles, String algorithm){
		IJ.log("");
		IJ.log("Analysis done with parameters :");
		IJ.log("input folder = " + path);
		IJ.log("output folder = " + outputpath);
		IJ.log("algorithm used = " + algorithm);
		IJ.log("files analysed :");

		// put the filename printing in the loop to print the filenames only when the analysis is finished
		for (String s : listoffiles){
			IJ.log(" - " + s);
		}
		return;
	}



	/**
	 * This function filters the files in the list according to the filetype.
	 * This function keeps the valid file names for the intended analysis
	 * filenames should be like :
	 * 	experimentatorname_date_dayofculture_objective_staining_drug_wellcoordinates
	 *  with wellcoordinates being :
	 * 	2D : 4 characters including '-'
	 * 	3D : 3 characters, the two firsts being the well coordinates and the last one being a letter for the specific pillar
	 * @param list
	 * @param filetype
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
	 * @throws Exception
	 */


	// LANCE IMAGEJ POUR TESTER LE PLUGIN
	public static void main(final String... args) throws Exception {
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();
	}
}
