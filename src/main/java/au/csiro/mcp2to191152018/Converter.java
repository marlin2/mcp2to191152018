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

import au.csiro.utils.Xml;
import au.csiro.utils.XmlResolver;
import jeeves.JeevesJCS;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Converter {

  public static void main( String args[] ){

    final Logger logger = LogManager.getLogger("main");

    String schemaPath = "schemas/iso19115-3/src/main/plugin/iso19115-3/schema.xsd";

    String oasisCatalogFile = "schemas/iso19115-3/src/main/plugin/iso19115-3/oasis-catalog.xml";

    try {
      JeevesJCS.setConfigFilename("src/main/config/cache.ccf");
      JeevesJCS.getInstance(XmlResolver.XMLRESOLVER_JCS);
    } catch (Exception ce) {
      System.err.println("Failed to create cache for schema files");
    }

    Options options = new Options();
    options.addOption("s", false, "Skip validation. Useful for debugging because reading the schemas takes some time.");
    options.addRequiredOption("i", "input", true, "Mandatory input xml file name.");
    options.addRequiredOption("o", "output", true, "Mandatory output xml file name.");

    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = null;

    String header = "Convert mcp2 xml to 19115-3\n\n";
 
    HelpFormatter formatter = new HelpFormatter();

    try {
      cmd = parser.parse(options, args);
    } catch (Exception e) {
      formatter.printHelp("mcp2to191152018", header, options, null, true);
      System.exit(1);
    }

		// Process input file
		String inpath = cmd.getOptionValue("i");
    File theFile = new File(inpath);
    if (!theFile.exists()) {
      formatter.printHelp("mcp2to191152018", header, options, null, true);
      System.exit(1);
    }

		// Get output file name
		String outputFile = cmd.getOptionValue("o");

    System.setProperty("XML_CATALOG_FILES", oasisCatalogFile);
    Xml.resetResolver();

    Map<String,String> xsltparams = new HashMap<String,String>();

       try {
          Element mdXml = Xml.loadFile(theFile);

          // transform
			    logger.info( "Transforming "+theFile.getName()+"...");
          Element result = Xml.transform(mdXml, "schemas/iso19115-3/src/main/plugin/iso19115-3/convert/ISO19139/fromISO19139MCP2.xsl",  xsltparams);

          //logger.info("Result was \n"+Xml.getString(result));

          // validate
          boolean xmlIsValid = true;

          if (cmd.hasOption("s")) {
					  logger.info("Validation is skipped.");
          } else {
            try {
              //Xml.validate(schemaPath, result);
              Xml.validate(result);
            } catch (Exception e) {
					    logger.error("Validation of '" + theFile.getName() + "' against http://schemas.isotc211.org/19115/-3/mdb/2.0 FAILED:" );
              logger.error(e.getMessage());
              xmlIsValid = false;
            }
          }

          // output
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
         
  }
}
	
