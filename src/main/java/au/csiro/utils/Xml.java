/* 
 * Copyright(C) 2012-2013, CSIRO Australia. All Rights Reserved.
 * 
 * See the LICENSE file provided with the ROAM distribution for a complete
 * description of the terms, conditions and disclaimers associated with
 * using and redistributing this software.
 * 
 * This software is provided 'AS IS', without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING
 * ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. CSIRO AUSTRALIA ('CSIRO')
 * AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE
 * AS A RESULT OF USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS
 * DERIVATIVES. IN NO EVENT WILL CSIRO OR ITS LICENSORS BE LIABLE FOR ANY LOST
 * REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL,
 * INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY
 * OF LIABILITY, ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE,
 * EVEN IF CSIRO HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 */
package au.csiro.utils;

import net.sf.saxon.Configuration;
import net.sf.saxon.FeatureKeys;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.transform.JDOMResult;
import org.jdom.transform.JDOMSource;
import org.jdom.xpath.XPath;
import org.jdom.output.SAXOutputter;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.ValidatorHandler;

/**
 * JDOM based XML utilities
 */

public class Xml {

  private static SAXBuilder getSAXBuilder(boolean validate) {
    SAXBuilder builder = getSAXBuilderWithoutXMLResolver(validate);
        Resolver resolver = ResolverWrapper.getInstance();
        builder.setEntityResolver(resolver.getXmlResolver());
        return builder;
  }

    private static SAXBuilder getSAXBuilderWithoutXMLResolver(boolean validate) {
        //SAXBuilder builder = new JeevesSAXBuilder(validate);
        SAXBuilder builder = new SAXBuilder(validate);
        builder.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        return builder;
    }

	public static Element loadString(String data, boolean validate)
												throws IOException, JDOMException
	{
		SAXBuilder builder = getSAXBuilderWithoutXMLResolver(validate);
		Document   jdoc    = builder.build(new StringReader(data));

		return (Element) jdoc.getRootElement().detach();
	}

  public static Element loadStream(InputStream input) throws IOException, JDOMException
  {
    SAXBuilder builder = getSAXBuilderWithoutXMLResolver(false); //new SAXBuilder();
    builder.setFeature("http://apache.org/xml/features/validation/schema",false);
    builder.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd",false);
    builder.setExpandEntities(false);
    Document   jdoc    = builder.build(input);

    return (Element) jdoc.getRootElement().detach();
  }

	//---------------------------------------------------------------------------

	public static String getString(Element data) {
		XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());

		return outputter.outputString(data);
	}

  public static String getString(Document data) {
    XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());

    return outputter.outputString(data);
  }

	//---------------------------------------------------------------------------

	public static Element loadFile(File file) throws IOException, JDOMException
  {
    SAXBuilder builder = new SAXBuilder();
    Document   jdoc    = builder.build(file);

    return (Element) jdoc.getRootElement().detach();
  }

	//---------------------------------------------------------------------------

	public static Element loadFile(String fileName) throws IOException, JDOMException
  {
		return loadFile(new File(fileName));
	}

	//---------------------------------------------------------------------------

	public static Element loadFileFromJar(String fileName) throws IOException, JDOMException
  {
		InputStream is = null;
		Document jdoc;
		try {
			is = Xml.class.getClassLoader().getResourceAsStream(fileName);
    	SAXBuilder builder = new SAXBuilder();
    	jdoc    = builder.build(is);
		} finally {
			if (is != null) is.close();
		}

    return (Element) jdoc.getRootElement().detach();
	}

	//---------------------------------------------------------------------------

	private static XPath prepareXPath(Element xml, String xpath, List<Namespace> theNSs) throws JDOMException
	{
		XPath xp = XPath.newInstance (xpath);
		for (Namespace ns : theNSs ) {
			xp.addNamespace(ns);
		}

		return xp;
	}

	//---------------------------------------------------------------------------

	public static Object selectSingle(Element xml, String xpath, List<Namespace> theNSs) throws JDOMException {

		XPath xp = prepareXPath(xml, xpath, theNSs);

		return xp.selectSingleNode(xml);
	}

	//---------------------------------------------------------------------------

	public static Element selectElement(Element xml, String xpath) throws JDOMException {
		return selectElement(xml, xpath, new ArrayList<Namespace>());
	}

	//---------------------------------------------------------------------------

	public static Element selectElement(Element xml, String xpath, List<Namespace> theNSs) throws JDOMException {
		Object result = selectSingle(xml, xpath, theNSs);
		if (result == null) {
			return null;
		} else if (result instanceof Element) {
			Element elem = (Element)result;
			return (Element)(elem);
		} else {
			//-- Found something but not an element
			return null;
		}
	}

	//---------------------------------------------------------------------------

	public static List<?> selectNodes(Element xml, String xpath, List<Namespace> theNSs) throws JDOMException {
		XPath xp = prepareXPath(xml, xpath, theNSs);
		return xp.selectNodes(xml);
	}

	//---------------------------------------------------------------------------

	public static List<?> selectNodes(Element xml, String xpath) throws JDOMException {
		return selectNodes(xml, xpath, new ArrayList<Namespace>());
	}
	
	//---------------------------------------------------------------------------

	public static Element transform(Element xml, String styleSheetPath, Map<String,String> params) throws Exception {

		System.setProperty("javax.xml.transform.TransformerFactory",
		                  "net.sf.saxon.TransformerFactoryImpl");
    File styleSheet = new File(styleSheetPath);
		Source srcXml   = new JDOMSource(new Document((Element)xml.detach()));
    Source srcSheet = new StreamSource(styleSheet);

		TransformerFactory transFact = TransformerFactory.newInstance();
    try {
      transFact.setAttribute(FeatureKeys.VERSION_WARNING,false);
      transFact.setAttribute(FeatureKeys.LINE_NUMBERING,true);
      transFact.setAttribute(FeatureKeys.PRE_EVALUATE_DOC_FUNCTION,false);
      transFact.setAttribute(FeatureKeys.RECOVERY_POLICY,Configuration.RECOVER_SILENTLY);
      // Add the following to get timing info on xslt transformations
      //transFact.setAttribute(FeatureKeys.TIMING,true);
    } catch (IllegalArgumentException e) {
        System.err.println("WARNING: transformerfactory doesnt like saxon attributes!");
      //e.printStackTrace();
    } finally {
		  Transformer t = transFact.newTransformer(srcSheet);
		  if (params != null) {
			  for (String param : params.keySet()) {
				  t.setParameter(param,params.get(param));
			  }
      }
      JDOMResult result = new JDOMResult();
		  t.transform(srcXml, result);
      return (Element)result.getDocument().getRootElement().detach();
		}
	}

	//---------------------------------------------------------------------------

  public synchronized static void validate(Element xml) throws Exception {
    Schema schema = factory().newSchema();
    ErrorHandler eh = new ErrorHandler();
    validateRealGuts(schema, xml, eh);
    if (eh.errors()) {
      Element xsdXPaths = eh.getXPaths();
      throw new Exception("XSD Validation error(s):\n"+getString(xsdXPaths));
    }
  }

	public static void validate(String schemaPath, Element xml) throws Exception {
    Element xsdXPaths = validateInfo(schemaPath,xml);
    if (xsdXPaths != null && xsdXPaths.getContent().size() > 0) throw new Exception("XSD Validation error(s):\n"+getString(xsdXPaths));
	}

	//---------------------------------------------------------------------------

  public static Element validateInfo(String schemaPath, Element xml) throws Exception
  {
    ErrorHandler eh = new ErrorHandler();
    validateGuts(schemaPath, xml, eh);
    if (eh.errors()) {
      return eh.getXPaths();
    } else {
      return null;
    }
  }

	//---------------------------------------------------------------------------

  private static void validateGuts(String schemaPath, Element xml, ErrorHandler eh) throws Exception {
    StreamSource schemaFile = new StreamSource(new File(schemaPath));
    Schema schema = factory().newSchema(schemaFile);
    validateRealGuts(schema, xml, eh);
  }

	//---------------------------------------------------------------------------

  private static void validateRealGuts(Schema schema, Element xml, ErrorHandler eh) throws Exception {

    Resolver resolver = ResolverWrapper.getInstance();

    ValidatorHandler vh = schema.newValidatorHandler();
    vh.setResourceResolver(resolver.getXmlResolver());
    vh.setErrorHandler(eh);

    SAXOutputter so = new SAXOutputter(vh);
    eh.setSo(so);

    so.output(xml);
  }

	//---------------------------------------------------------------------------

	private static SchemaFactory factory() {
		return SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
	}

	//---------------------------------------------------------------------------

    /**
     * Error handler that collects up validation errors.
     *
     */
	public static class ErrorHandler extends DefaultHandler {

		private int errorCount = 0;
		private Element xpaths;
		private Namespace ns = Namespace.NO_NAMESPACE;
		private SAXOutputter so;
		
		public void setSo(SAXOutputter so) {
			this.so = so;
		}
		
		public boolean errors() {
			return errorCount > 0;
		}

		public Element getXPaths() {
			return xpaths;
		}

		public void addMessage ( SAXParseException exception, String typeOfError ) {
			if (errorCount == 0) xpaths = new Element("xsderrors", ns);
			errorCount++;

			Element elem = (Element) so.getLocator().getNode();
			Element x = new Element("xpath", ns);
			try {
				String xpath = au.csiro.utils.XPath.getXPath(elem);
				//-- remove the first element to ensure XPath fits XML passed with
				//-- root element
				if (xpath.startsWith("/")) { 
					int ind = xpath.indexOf('/',1);
					if (ind != -1) {
						xpath = xpath.substring(ind+1);
					} else {
						xpath = "."; // error to be placed on the root element
					}
				}
				x.setText(xpath);
			} catch (JDOMException e) {
				e.printStackTrace();
				x.setText("nopath");
			}
			String message = exception.getMessage() + " (Element: " + elem.getQualifiedName();
			String parentName;
			if (!elem.isRootElement()) {
				Element parent = (Element)elem.getParent();
				if (parent != null)
					parentName = parent.getQualifiedName();
				else
					parentName = "Unknown";
			} else {
				parentName = "/";
			}
			message += " with parent element: " + parentName + ")";
			
			Element m = new Element("message", ns).setText(message);
			Element errorType = new Element("typeOfError", ns).setText(typeOfError);
			Element errorNumber = new Element("errorNumber", ns).setText(String.valueOf(errorCount));
			Element e = new Element("error", ns);
			e.addContent(errorType);
			e.addContent(errorNumber);
			e.addContent(m);
			e.addContent(x);
			xpaths.addContent(e);
		}
		
		public void error( SAXParseException parseException ) throws SAXException {
			addMessage( parseException, "ERROR" );
		}

		public void fatalError( SAXParseException parseException ) throws SAXException {
			addMessage( parseException, "FATAL ERROR" );
		}

		public void warning( SAXParseException parseException ) throws SAXException {
			addMessage( parseException, "WARNING" );
		}

		/**
		 * Set namespace to use for report elements
		 * @param ns
		 */
		public void setNs(Namespace ns) {
			this.ns = ns;
		}

		public Namespace getNs() {
			return ns;
		}
	}

  public static void resetResolver() {
    Resolver resolver = ResolverWrapper.getInstance();
    resolver.reset();
  }
}
