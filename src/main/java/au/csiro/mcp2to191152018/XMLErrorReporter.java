package au.csiro.mcp2to191152018;

import org.xml.sax.ErrorHandler;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

class XMLErrorReporter extends org.xml.sax.helpers.DefaultHandler {

	protected int nErrors = 0;

	public void warning(SAXParseException e) throws SAXException {
		printInfo( e, "Warning:"); 
		}

	public void error(SAXParseException e) throws SAXException {
		printInfo( e, "Error:");
		++nErrors;
		}

	public void fatalError(SAXParseException e) throws SAXException {
		printInfo( e, "Fatal Error:");
		throw e;
		}

	private void printInfo( SAXParseException e, String level ){
		java.io.PrintStream stdout = System.out;
		stdout.println( "\t" + level + " on line number " + e.getLineNumber( ) + ": " + e.getMessage( ) );
		}
	}
