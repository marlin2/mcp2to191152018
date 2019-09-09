package au.csiro.mcp2to191152018;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.net.URISyntaxException;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

import java.security.CodeSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import au.csiro.utils.Xml;
import au.csiro.utils.XmlResolver;
import jeeves.JeevesJCS;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Converter {

  public static void main( String args[] ){

    final Logger logger = LogManager.getLogger("main");

    CodeSource codeSource = Converter.class.getProtectionDomain().getCodeSource();
    String jarDir = "";
    try {
      File jarFile = new File(codeSource.getLocation().toURI().getPath());
      jarDir = jarFile.getParentFile().getParentFile().getPath();
    } catch (URISyntaxException ue) {
      System.err.println("Failed to get directory of jar");
      System.exit(1);
    }

    String schemaPath = jarDir + "/" + "schemas/iso19115-3/src/main/plugin/iso19115-3/schema.xsd";

    String oasisCatalogFile = jarDir + "/" + "schemas/iso19115-3/src/main/plugin/iso19115-3/oasis-catalog.xml";

    try {
      JeevesJCS.setConfigFilename(jarDir + "/" + "src/main/config/cache.ccf");
      JeevesJCS.getInstance(XmlResolver.XMLRESOLVER_JCS);
    } catch (Exception ce) {
      System.err.println("Failed to create cache for schema files");
    }

    Options options = new Options();
    options.addOption("s", false, "Skip validation. Useful for debugging because reading the schemas takes some time.");
    options.addOption("d", "input_directory", true, "Directory name containing xml file name at some depth in the directory structure ");
    options.addRequiredOption("i", "file_name", true, "Input xml file name.");
    options.addRequiredOption("o", "output_file_name", true, "Output xml file name.");

    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = null;

    String header = "Convert mcp2 xml files to 19115-3\n\n"+
      "If the -d option is specified then the directory will be recursively searched for any file with the name specified by the -i option. The converted file will be created with the name specified by the -o option at the same level in the directory structure as the input file.";
 
    HelpFormatter formatter = new HelpFormatter();

    try {
      cmd = parser.parse(options, args);
    } catch (Exception e) {
      formatter.printHelp("mcp2to191152018", header, options, null, true);
      System.exit(1);
    }

		// Process input directory
		String inpath = null;
    File indirectory = null;
    if (cmd.hasOption("d")) {
		  inpath = cmd.getOptionValue("d");
      indirectory = new File(inpath);
      if (!indirectory.exists()) {
        formatter.printHelp("mcp2to191152018", header, options, null, true);
        System.exit(1);
      }
    }

		// Process input file name 
		String input = cmd.getOptionValue("i");

		// Process output file name 
		String output = cmd.getOptionValue("o");

    System.setProperty("XML_CATALOG_FILES", oasisCatalogFile);
    Xml.resetResolver();

		// Fetch list of input files from input directory
    List<Path> files = new ArrayList<Path>();
    if (inpath != null) {
      getFileNames(files, indirectory.toPath(), input);
    } else {
      files.add(new File(input).toPath());
    }

    Map<String,String> xsltparams = new HashMap<String,String>();

    int invalid = 0;
    for (Path file : files) {
       try {
			    if (inpath != null) logger.debug(file.getParent()+"...");
          File theFile = file.toFile();
          Element mdXml = Xml.loadFile(theFile);

          // transform
			    logger.debug( "Transforming "+theFile.getName()+"...");
          Element result = Xml.transform(mdXml, jarDir + "/" + "schemas/iso19115-3/src/main/plugin/iso19115-3/convert/ISO19139/fromISO19139MCP2.xsl",  xsltparams);

          //logger.debug("Result was \n"+Xml.getString(result));

          // validate
          boolean xmlIsValid = true;

          if (cmd.hasOption("s")) {
					  logger.info("Validation is skipped.");
          } else {
            try {
              //Xml.validate(schemaPath, result);
              Xml.validate(result);
            } catch (Exception e) {
              invalid++;
			        if (inpath != null) logger.error("In "+file.getParent()+"...");
					    logger.error("Validation of '" + theFile.getName() + "' against http://schemas.isotc211.org/19115/-3/mdb/2.0 FAILED:" );
              logger.error(e.getMessage());
              xmlIsValid = false;
            }
          }

          // output file name is either that directly specified by output or 
          // the output file in the same directory as the input file
          String outputFile = output;
          if (inpath != null) {
           outputFile = file.getParent() + File.separator + output;  
          }
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

		} // for each path				
    logger.info("Total records: "+files.size()+" INVALID: "+invalid);
         
  }

  // From: https://stackoverflow.com/questions/2534632/list-all-files-from-a-directory-recursively-with-java
  private static List<Path> getFileNames(List<Path> fileNames, Path dir, String infile) {
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
        for (Path path : stream) {
            if(path.toFile().isDirectory()) {
                getFileNames(fileNames, path, infile);
            } else {
                // only add filenames that match infile
                boolean theTest = path.getFileName().toString().equals(infile);
                if (theTest) {
                  //fileNames.add(path.toAbsolutePath().toString());
                  fileNames.add(path);
                }
            }
        }
    } catch(IOException e) {
        e.printStackTrace();
    }
    return fileNames;
  }
}
	
