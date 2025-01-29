package ch.epfl.bio410;

import ij.IJ;
import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;
import java.io.File;
import java.util.Arrays;
import java.util.Objects;

@Plugin(type = Command.class, menuPath = "Plugins>4Dcell>Fluovolt Analysis")
public class FluovoltAnalysis implements Command {

	// variables à relier à l'UI
	public String filetype = "3D"; // 2D ou 3D selon la sélection par l'utilisateur
	public String algorithme = "roifitting"; // au choix aussi dans l'interface

	// variables
	public String folderPath; // dossier à analyser
	public String savePath; // dossier de sorties pour les résultats
	public String[] tifList;
	public String[] filteredlist; // liste des fichiers tiff pertinents

	// FONCTION PRINCIPALE
	public void run() {
		// récupération des dossiers définis par l'utilisateur
		folderPath = IJ.getDirectory("Select the folder to analyze");
		savePath = IJ.getDirectory("Select the output folder");
		IJ.log(folderPath);
		IJ.log(savePath); // print pour vérifier

		// récupération de la liste de fichiers dans le dossier input
		tifList = listfiles(folderPath);
        for (String s : tifList) {
            IJ.log(s);
        }
		// tri des noms de fichiers en fonction du choix de l'utilisateur
		filteredlist = filterfiles(tifList, filetype);
		for (String s : filteredlist){
			IJ.log(s);
		}
	}

	public String[] filterfiles(String[] list, String filetype){
		// this function keeps the valid file names for the intended analysis
		// filenames should be like :
		// experimentatorname_date_dayofculture_objective_staining_drug_wellcoordinates
		// with wellcoordinates being :
		// 2D : 4 characters including '-'
		// 3D : 3 characters, the two firsts being the well coordinates and the last one being a letter for the specific pillar

		String[] filtered = new String[0]; // the result array containing the valid filenames at the end of the function
		String name; // to get the file name without extension, used in the loop
		// d'abord les critères communs à tous les fichiers :
		for (String s : list) {
			if (!s.contains(".")){ // si le fichier n'est pas un fichier (n'a pas d'extension)
				IJ.log(s + " is not a file");
				continue;
			}
			name = s.split("\\.")[0];
			IJ.log(name);
			IJ.log(s.split("\\.")[1]);
			String[] Conditions = name.split("_");
			if (!s.contains("tif")){ // si le fichier n'est pas un tif
				IJ.log(s + " filtered by file extension");
				continue;
			} else if (Objects.equals(filetype, "2D")){
				if (!Conditions[Conditions.length-1].contains("-")){
					IJ.log(s+" has no '-'");
					continue;
				} else if (Conditions[Conditions.length-1].length()!=4){
					IJ.log(s+" doesn't have the correct coordinates synthax");
					continue;
				}
			} else if (filetype=="3D"){
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
