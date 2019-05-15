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
package au.csiro.mcp2to191152018;

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

	public static Element loadString(String data, boolean validate)
												throws IOException, JDOMException
	{
		SAXBuilder builder = new SAXBuilder(validate);
		Document   jdoc    = builder.build(new StringReader(data));

		return (Element) jdoc.getRootElement().detach();
	}

	//---------------------------------------------------------------------------

	public static String getString(Element data) {
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

	public static void validate(String schemaPath, Element xml) throws Exception {
		StreamSource schemaFile = new StreamSource(new File(schemaPath));
		validateStreamSource(schemaFile, xml);
	}

	//---------------------------------------------------------------------------

	public static void validateSchemaFromJar(String schemaPath, Element xml) throws Exception {
		URL url = Xml.class.getClassLoader().getResource(schemaPath);
		StreamSource schemaFile = new StreamSource(url.toString());
		validateStreamSource(schemaFile, xml);
	}

	//---------------------------------------------------------------------------

	private static void validateStreamSource(StreamSource schemaFile, Element xml) throws Exception {
		Schema schema = factory().newSchema(schemaFile);
		ValidatorHandler vh = schema.newValidatorHandler();

		SAXOutputter so = new SAXOutputter(vh);
		so.output(xml);
	} 

	//---------------------------------------------------------------------------

	private static SchemaFactory factory() {
		return SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
	}

}
