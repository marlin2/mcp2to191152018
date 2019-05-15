package au.csiro.mcp2to191152018;

import java.io.File;
import java.io.FilenameFilter;
import java.io.FileOutputStream;

import java.util.HashMap;
import java.util.Map;

import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

public class Converter {

  public static void main( String args[] ){

    Options options = new Options();
    options.addOption("s", false, "Skip validation. Useful for debugging because reading the schemas takes some time.");
    options.addRequiredOption("i", "directory", true, "Mandatory directory name for input xml files.");
    options.addRequiredOption("o", "directory", true, "Mandatory directory name for output xml files.");

    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = null;

    String header = "Convert mcp2 xml files to 19115-3\n\n";
 
    HelpFormatter formatter = new HelpFormatter();

    try {
      cmd = parser.parse(options, args);
    } catch (Exception e) {
      formatter.printHelp("mcp2to191152018", header, options, null, true);
      System.exit(1);
    }

		// Process input directory
		String inpath = cmd.getOptionValue("i");
    File indirectory = new File(inpath);
    if (!indirectory.exists()) {
      formatter.printHelp("mcp2to191152018", header, options, null, true);
      System.exit(1);
    }

		// Process output directory 
		String path = cmd.getOptionValue("o");
		// Append final separator character if not already suppled
		if(!path.endsWith(File.separator)) path += File.separator;
    File op = new File(path + "invalid");
    // creates both output directory and invalid directory if they don't exist
    op.mkdirs(); 

		// Fetch list of input files from input directory
    File[] files = indirectory.listFiles(new FilenameFilter() {
         public boolean accept(File dir, String name) {
           return name.toLowerCase().endsWith(".xml");
         }
    }); 
    Map<String,String> xsltparams = new HashMap<String,String>();

    for (int i = 0;i < files.length;i++) {
       try {
          File theFile = files[i];
          Element mdXml = Xml.loadFile(theFile);

          // transform
			    System.out.println( "Transforming "+theFile.getName()+"...");
          Element result = Xml.transform(mdXml, "schemas/iso19115-3/src/main/plugin/iso19115-3/convert/ISO19139/fromISO19139MCP2.xsl",  xsltparams);

          //System.out.println("Result was \n"+Xml.getString(result));

          // validate
          boolean xmlIsValid = true;

          if (cmd.hasOption("s")) {
					  System.out.println("Validation is skipped.");
          } else {
					  System.out.println("Validating '" + theFile.getName() + "' against http://schemas.isotc211.org/19115/-3/mdb/2.0 :" );
					  xmlIsValid = ISO19115Validator.isValid( Xml.getString(result) );
          }

          // output
          String outputFile = path;
          if (!xmlIsValid) {
            outputFile += "invalid" + File.separator;
          } 
          outputFile += theFile.getName();
          XMLOutputter out = new XMLOutputter();
          Format f = Format.getPrettyFormat();  
          f.setEncoding("UTF-8");
          out.setFormat(f);  
          FileOutputStream fo = new FileOutputStream(outputFile);
          out.output(result, fo);
          fo.close();


				} catch( Exception e ) { 
          e.printStackTrace();
				}
         
		} // for each dataset				
  }
}
	
