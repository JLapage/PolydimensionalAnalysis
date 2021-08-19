package src.main.java.polydimensionalpixelspace;
import java.io.File;
import java.io.FilenameFilter;

/**
 * 
 * A filename filter.
 *
 * @author John MJ Lapage
 * @version 1.0
 */
public class QuickFilter implements FilenameFilter{
		String[] formats;
		
		public QuickFilter(String[] acceptedFormats){
			formats = acceptedFormats;
		}
		
		public boolean accept(File file, String name){
			for(int i = 0; i< formats.length; i++){
				if(name.endsWith(formats[i])){
					return true;
				}
			}
			return false;
		}
	}