package au.csiro.mcp2to191152018;


import java.io.StringReader;

// import java.lang.RuntimeException;

import org.xml.sax.InputSource;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.XMLConstants;

import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

import java.net.URL;

class ISO19115Validator {

	private static javax.xml.parsers.SAXParser sp;
	//private static String ISO19115_URL = "https://www.isotc211.org/2005/gmd/gmd.xsd";
	private static String ISO19115_URL = "./schemas/iso19115-3/src/main/plugin/iso19115-3/schema.xsd";

	static {
		
		SchemaFactory sf;
		Schema s;
		SAXParserFactory spf;
	
		try {
			sf = SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI );
			}
		catch( IllegalArgumentException e ){
			throw new RuntimeException( "Cannot instatinate an implementation of javax.xml.validation.SchemaFactory which supports '" 
										+ XMLConstants.W3C_XML_SCHEMA_NS_URI + "'", e );
			}
		catch( NullPointerException e ){
			throw new RuntimeException( "Something Really Bad just happened - should be unreachable code!", e );
			}

		try {
			s =  sf.newSchema( new URL( new URL("file:"), ISO19115_URL ) );
			}
		catch( java.net.MalformedURLException e ){ // Exception thrown by java.net.URL( String ) constructor
			throw new RuntimeException( "\"" + ISO19115_URL + "\" is not a valid URL according to java.net.URL", e );
			}
		catch( SAXException e ){ // Exception thrown by SchemaFactory::newSchema( )
			throw new RuntimeException( "Cannot create new schema object", e );
			}
		catch( NullPointerException e ){ // Exception thrown by SchemaFactory::newSchema( java.net.URL )
			throw new RuntimeException( "java.net.URL has munged \"" + ISO19115_URL + "\" into a null value without throwing an exception", e );
			}

		spf = SAXParserFactory.newInstance( );
/*		catch( FactoryConfigurationError e ){ // thrown by SAXParserFactory::newInstance( );
			throw new RuntimeException( "Cannot instantiate an implementation of javax.xml.parsers.SAXParserFactory", e );
			}
*/
		try {
			spf.setNamespaceAware( true );
			spf.setSchema( s );
			sp = spf.newSAXParser( );
			}
		catch( UnsupportedOperationException e ){ // thrown by SAXParserFactory::setSchema( Schema )
			throw new RuntimeException( spf.getClass( ).getName( ) + " implementation of SAXParserFactory does not support schemas", e );
			}
		catch( javax.xml.parsers.ParserConfigurationException e ){ // thrown by SAXParserFactory::newSAXParser( );
			throw new RuntimeException( "Cannot instantiate a SAXParser based on the current SAXParserFactory configuration", e );
			}
		catch( SAXException e ){ // thrown by SAXParserFactory::newSAXParser( );
			throw new RuntimeException( "Problem instantiating an implementation of javax.xml.parsers.SAXParser", e );
			}
		}
		
	public static boolean isValid( String xml ) throws IllegalArgumentException, Exception {

		XMLErrorReporter r = new XMLErrorReporter( );		
		try {		
			sp.parse( new InputSource( new StringReader( xml ) ), r );
			}
		catch( IllegalArgumentException e ){
			throw new IllegalArgumentException( "ISO19115 XML string is null" );
			}
		catch( SAXException e ){
			// Really should throw something more meaningful here
			throw new Exception( "XML Parser exception during validation", e );
			}
		catch( java.io.IOException e ){
			throw new Error( "IO problem while reading XML string" );
			}
		return r.nErrors == 0;
		}
	}
