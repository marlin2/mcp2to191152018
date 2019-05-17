//=============================================================================
//===	Copyright (C) 2001-2005 Food and Agriculture Organization of the
//===	United Nations (FAO-UN), United Nations World Food Programme (WFP)
//===	and United Nations Environment Programme (UNEP)
//===
//===	This library is free software; you can redistribute it and/or
//===	modify it under the terms of the GNU Lesser General Public
//===	License as published by the Free Software Foundation; either
//===	version 2.1 of the License, or (at your option) any later version.
//===
//===	This library is distributed in the hope that it will be useful,
//===	but WITHOUT ANY WARRANTY; without even the implied warranty of
//===	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
//===	Lesser General Public License for more details.
//===
//===	You should have received a copy of the GNU Lesser General Public
//===	License along with this library; if not, write to the Free Software
//===	Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
//===
//===	Contact: Jeroen Ticheler - FAO - Viale delle Terme di Caracalla 2,
//===	Rome - Italy. email: GeoNetwork@fao.org
//==============================================================================

package au.csiro.mcp2to191152018.utils;

import org.apache.xml.resolver.CatalogManager;
import org.apache.xml.resolver.tools.CatalogResolver;

import java.util.Vector;

//=============================================================================

/** Utility that handles the CatalogResolver and XmlResolver
  */

public final class Resolver
{

	private XmlResolver xmlResolver;
	private CatalogResolver catResolver;
	
	/**
	 * When path is resolved to a non existing file, return this file.
	 */
	private String blankXSLFile;
	
	/** Active readers count */
	private static int activeReaders = 0;
	/** Active writers count */
	private static int activeWriters = 0;

	//--------------------------------------------------------------------------

  public Resolver() {
			setUpXmlResolver();
	}
   
	//--------------------------------------------------------------------------

	private void setUpXmlResolver() {
		CatalogManager catMan = new CatalogManager();
		catMan.setAllowOasisXMLCatalogPI(false);
		catMan.setCatalogClassName("org.apache.xml.resolver.Catalog");
		String catFiles = System.getProperty("XML_CATALOG_FILES");
		if (catFiles == null) catFiles="";
        System.out.println("Using oasis catalog files "+catFiles);

        setBlankXSLFile(System.getProperty("XML_CATALOG_BLANKXSLFILE"));
        
		catMan.setCatalogFiles(catFiles);
		catMan.setIgnoreMissingProperties(true);
		catMan.setPreferPublic(true);
		catMan.setRelativeCatalogs(false);
		catMan.setUseStaticCatalog(false);
		String catVerbosity = System.getProperty("XML_CATALOG_VERBOSITY");
		if (catVerbosity == null) catVerbosity = "1";
		int iCatVerb = 1;
		try {
			iCatVerb = Integer.parseInt(catVerbosity);
		} catch (NumberFormatException nfe) {
			System.err.println("Failed to parse XML_CATALOG_VERBOSITY "+catVerbosity);
			nfe.printStackTrace();
		}
        System.out.println("Using catalog resolver verbosity "+iCatVerb);
		catMan.setVerbosity(iCatVerb);

		catResolver = new CatalogResolver(catMan);

		Vector catalogs = catResolver.getCatalog().getCatalogManager().getCatalogFiles();
		String[] cats = new String[catalogs.size()];
		System.arraycopy(catalogs.toArray(), 0, cats, 0, catalogs.size());

		xmlResolver = new XmlResolver(cats);
	}

	//--------------------------------------------------------------------------

	public void reset() {
		setUpXmlResolver();
	}

	//--------------------------------------------------------------------------

	public XmlResolver getXmlResolver() {
		return xmlResolver;
	}

	//--------------------------------------------------------------------------

	public CatalogResolver getCatalogResolver() {
		return catResolver;
	}

    public void setBlankXSLFile(String blankXSLFile) {
        this.blankXSLFile = blankXSLFile;
    }

}

//=============================================================================

