/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.uima.util.impl;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UIMA_IllegalArgumentException;
import org.apache.uima.UIMA_IllegalStateException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.analysis_engine.TaeDescription;
import org.apache.uima.collection.CasConsumerDescription;
import org.apache.uima.collection.CasInitializerDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.collection.metadata.CpeDescription;
import org.apache.uima.internal.util.XMLUtils;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.URISpecifier;
import org.apache.uima.resource.metadata.FsIndexCollection;
import org.apache.uima.resource.metadata.ResourceManagerConfiguration;
import org.apache.uima.resource.metadata.ResourceMetaData;
import org.apache.uima.resource.metadata.TypePriorities;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.MetaDataObject_impl;
import org.apache.uima.search.IndexBuildSpecification;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.Level;
import org.apache.uima.util.SaxDeserializer;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLParser;
import org.apache.uima.util.XMLizable;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

/**
 * Reference implementation of {@link XMLParser}.
 * 
 * 
 */
public class XMLParser_impl implements XMLParser
{
  

  /**
   * resource bundle for log messages
   */
  private static final String LOG_RESOURCE_BUNDLE = "org.apache.uima.impl.log_messages";
  
  /**
   * current class 
   */
  private static final Class CLASS_NAME = XMLParser_impl.class;
   
  /**
   * Creates a new XMLParser_impl.
   * 
   * @throws ParserConfigurationException if the underlying XML parser
   *    could not be constructed
   */
  public XMLParser_impl()
    throws ParserConfigurationException
  {
  }  
        

  /**
   * @see org.apache.uima.util.XMLParser#enableSchemaValidation(boolean)
   */
  public void enableSchemaValidation(boolean aEnable)
  {
    mSchemaValidationEnabled = aEnable;
  }

  /**
   * Parses an XML input stream and produces an object.
   * 
   * @param aInput the input source from which to read the XML document
   * @param aNamespaceForSchema XML namespace for elements to be validated 
   *    against XML schema.  If null, no schema will be used.
   * @param aSchemaUrl URL to XML schema that will be used to validate the
   *    XML document.  If null, no schema will be used.
   * @param aExpandXIncludes if true, XInclude (<code>xi:include</code>)
   *    elements will be expanded according to the XInclude specification.
   * 
   * @return an <code>XMLizable</code> object constructed from the XML document
   * 
   * @throws InvalidXMLException if the input XML is not valid or does not
   *    specify a valid object
   */
  public XMLizable parse(XMLInputSource aInput, 
    String aNamespaceForSchema, URL aSchemaUrl, boolean aExpandXIncludes)
    throws InvalidXMLException
  {
  	return parse(aInput, aNamespaceForSchema, aSchemaUrl,
  	             new XMLParser.ParsingOptions(aExpandXIncludes, true));
  }

	/**
	 * Parses an XML input stream and produces an object.
	 * 
	 * @param aInput the input source from which to read the XML document
	 * @param aNamespaceForSchema XML namespace for elements to be validated 
	 *    against XML schema.  If null, no schema will be used.
	 * @param aSchemaUrl URL to XML schema that will be used to validate the
	 *    XML document.  If null, no schema will be used.
	 * @param aExpandXIncludes if true, XInclude (<code>xi:include</code>)
	 *    elements will be expanded according to the XInclude specification.
	 * 
	 * @return an <code>XMLizable</code> object constructed from the XML document
	 * 
	 * @throws InvalidXMLException if the input XML is not valid or does not
	 *    specify a valid object
	 */
	public XMLizable parse(XMLInputSource aInput, 
		String aNamespaceForSchema, URL aSchemaUrl, 
		XMLParser.ParsingOptions aOptions)
		throws InvalidXMLException
	{
        URL urlToParse = aInput.getURL();
		try
		{
			SAXParserFactory factory = SAXParserFactory.newInstance();
      
			// Turn on namespace support
			factory.setNamespaceAware(true);

			SAXParser parser = factory.newSAXParser();
       
			XMLReader reader = parser.getXMLReader();
			reader.setFeature("http://xml.org/sax/features/namespace-prefixes", true);

			//set up InputSource
			InputSource input = new InputSource();
			input.setByteStream(aInput.getInputStream());
            String systemId;
            if (urlToParse != null)
            {
              systemId = urlToParse.toString();
            }
            else
			{
			  systemId = new File(System.getProperty("user.dir")).toURL().toString();
			}
			input.setSystemId(systemId);
        
			//Parse with SaxDeserializer
			SaxDeserializer deser;
			if (mSchemaValidationEnabled)
			{
				deser = new SaxDeserializer_impl(this, aNamespaceForSchema, aSchemaUrl, aOptions);
			}
			else
			{
				deser = new SaxDeserializer_impl(this, null, null, aOptions);
			}  
      
			reader.setContentHandler(deser);
			reader.parse(input);
			XMLizable result = deser.getObject();
			if (result instanceof MetaDataObject_impl)
			{
				//set Source URL (needed to later resolve descriptor-relative paths)
				((MetaDataObject_impl)result).setSourceUrl(urlToParse);
			}
			return result;         
		}  
		catch(Exception e)
		{
          String sourceFile = urlToParse != null ? urlToParse.toString() : "<unknown source>";
          throw new InvalidXMLException(InvalidXMLException.INVALID_DESCRIPTOR_FILE,
			new Object[]{sourceFile}, e);
        }  
	}
	
 /**
  * Parses an XML input stream and produces an object.
  * 
  * @param aInput the input source from which to read the XML document
  * @param aNamespaceForSchema XML namespace for elements to be validated 
  *    against XML schema.  If null, no schema will be used.
  * @param aSchemaUrl URL to XML schema that will be used to validate the
  *    XML document.  If null, no schema will be used.
  * 
  * @return an <code>XMLizable</code> object constructed from the XML document
  * 
  * @throws InvalidXMLException if the input XML is not valid or does not
  *    specify a valid object
  */
 public XMLizable parse(XMLInputSource aInput, 
   String aNamespaceForSchema, URL aSchemaUrl)
   throws InvalidXMLException
 {
   return parse(aInput,aNamespaceForSchema, aSchemaUrl, true);
 }
 
 
  /**
   * Parses an XML input stream and produces an object.  No schema validation
   * will be done.
   * 
   * @param aInput the input source from which to read the XML document
   * 
   * @return an <code>XMLizable</code> object constructed from the XML document
   * 
   * @throws InvalidXMLException if the input XML is not valid or does not
   *    specify a valid object
   */
  public XMLizable parse(XMLInputSource aInput)
    throws InvalidXMLException
  {
    return parse(aInput, null, null);
  } 


	/* (non-Javadoc)
	 * @see org.apache.uima.util.XMLParser#parse(org.apache.uima.util.XMLInputSource, org.apache.uima.util.XMLParser.ParsingOptions)
	 */
	public XMLizable parse(XMLInputSource aInput, ParsingOptions aOptions) throws InvalidXMLException
	{
		return parse(aInput, null, null, aOptions);
	}

	/**
	 * Builds an object from its XML DOM representation.
	 * 
	 * @param aElement a DOM Element
	 * 
	 * @return an <code>XMLizable</code> object constructed from the DOM element
	 * 
	 * @throws InvaidXMLException if the XML element does not specify a valid
	 *    object
	 */
	public XMLizable buildObject(Element aElement)
		throws InvalidXMLException
	{
		return buildObject(aElement, new ParsingOptions(true,true));
	}
	
  /**
   * Builds an object from its XML DOM representation.
   * 
   * @param aElement a DOM Element
   * 
   * @return an <code>XMLizable</code> object constructed from the DOM element
   * 
   * @throws InvaidXMLException if the XML element does not specify a valid
   *    object
   */
  public XMLizable buildObject(Element aElement, ParsingOptions aOptions)
    throws InvalidXMLException
  {
    //attempt to locate a Class that can be built from the element
    Class cls = (Class)mElementToClassMap.get(aElement.getTagName());
    if (cls == null)
    {
      if ("xi:include".equals(aElement.getTagName()))
      {
        throw new InvalidXMLException(InvalidXMLException.UNRESOLVED_XINCLUDE, new Object[0]);
      }
      else
      {
        throw new InvalidXMLException(InvalidXMLException.UNKNOWN_ELEMENT,
          new Object[]{aElement.getTagName()});
      }  
    }
      
    //resolve the class name and instantiate the class
    XMLizable object;
    try
    {
      object = (XMLizable)cls.newInstance();
    }
    catch(Exception e)
    {
      throw new UIMA_IllegalStateException(
          UIMA_IllegalStateException.COULD_NOT_INSTANTIATE_XMLIZABLE,
          new Object[]{cls.getName()}, e);
    }  
      
    //construct the XMLizable object from the XML element
    object.buildFromXMLElement(aElement, this, aOptions);
      
    return object;
  }

  
  /* (non-Javadoc)
   * @see com.ibm.uima.util.XMLParser#buildObjectOrPrimitive(org.w3c.dom.Element, com.ibm.uima.util.XMLParser.ParsingOptions)
   */
  public Object buildObjectOrPrimitive(Element aElement, ParsingOptions aOptions) throws InvalidXMLException
  {
    //attempt to locate a Class that can be built from the element
    Class cls = (Class)mElementToClassMap.get(aElement.getTagName());
    if (cls == null)
    {
      if ("xi:include".equals(aElement.getTagName()))
      {
        throw new InvalidXMLException(InvalidXMLException.UNRESOLVED_XINCLUDE, new Object[0]);
      }
      else
      {
        //attempt to parse as primitive
        Object primObj = XMLUtils.readPrimitiveValue(aElement);
        if (primObj != null)
        {
          return primObj;
        }

        //unknown element - throw exception
        throw new InvalidXMLException(InvalidXMLException.UNKNOWN_ELEMENT,
          new Object[]{aElement.getTagName()});
      }  
    }
      
    //resolve the class name and instantiate the class
    XMLizable object;
    try
    {
      object = (XMLizable)cls.newInstance();
    }
    catch(Exception e)
    {
      throw new UIMA_IllegalStateException(
          UIMA_IllegalStateException.COULD_NOT_INSTANTIATE_XMLIZABLE,
          new Object[]{cls.getName()}, e);
    }  
      
    //construct the XMLizable object from the XML element
    object.buildFromXMLElement(aElement, this, aOptions);
      
    return object;  
  }  
  
  
  /**
   * Parses a ResourceSpecifier from an XML input stream.  XML schema validation
   * will be done against the {@link #RESOURCE_SPECIFIER_SCHEMA_NAME} if it
   * can be found in the classpath.
   * 
   * @param aInput the input source from which to read the XML document
   * 
   * @return a <code>ResourceSpecifier</code> object constructed from the XML 
   *    document
   * 
   * @throws InvalidXMLException if the input XML is not valid or does not
   *    specify a valid ResourceSpecifier
   */
  public ResourceSpecifier parseResourceSpecifier(XMLInputSource aInput)
    throws InvalidXMLException
  {
  	return parseResourceSpecifier(aInput, DEFAULT_PARSING_OPTIONS);
  }

	/**
	 * Parses a ResourceSpecifier from an XML input stream.  XML schema validation
	 * will be done against the {@link #RESOURCE_SPECIFIER_SCHEMA_NAME} if it
	 * can be found in the classpath.
	 * 
	 * @param aInput the input source from which to read the XML document
	 * 
	 * @return a <code>ResourceSpecifier</code> object constructed from the XML 
	 *    document
	 * 
	 * @throws InvalidXMLException if the input XML is not valid or does not
	 *    specify a valid ResourceSpecifier
	 */
	public ResourceSpecifier parseResourceSpecifier(XMLInputSource aInput, ParsingOptions aOptions)
		throws InvalidXMLException
	{
 	  //attempt to locate resource specifier schema
    URL schemaURL = getResourceSpecifierSchemaUrl();    
    XMLizable object = parse(aInput, RESOURCE_SPECIFIER_NAMESPACE, schemaURL, aOptions);
    if (object instanceof ResourceSpecifier)
    {
      return (ResourceSpecifier)object;
    }
    else
    {
      throw new InvalidXMLException(InvalidXMLException.INVALID_CLASS,
        new Object[]{ResourceSpecifier.class.getName(), 
        object.getClass().getName()});
    }  
  }
    
  /**
   * Parses a ResourceMetaData object from an XML input stream.  XML schema 
   * validation will be done against the {@link #RESOURCE_SPECIFIER_SCHEMA_NAME} 
   * if it can be found in the classpath.  
   * 
   * @param aInput the input source from which to read the XML document
   * 
   * @return a <code>ResourceMetaData</code> object constructed from the XML 
   *    document
   * 
   * @throws InvalidXMLException if the input XML is not valid or does not
   *    specify a valid ResourceSpecifier
   */
  public ResourceMetaData parseResourceMetaData(XMLInputSource aInput)
    throws InvalidXMLException
  {
  	return parseResourceMetaData(aInput, DEFAULT_PARSING_OPTIONS);
  }

	/**
	 * Parses a ResourceMetaData object from an XML input stream.  XML schema 
	 * validation will be done against the {@link #RESOURCE_SPECIFIER_SCHEMA_NAME} 
	 * if it can be found in the classpath.  
	 * 
	 * @param aInput the input source from which to read the XML document
	 * 
	 * @return a <code>ResourceMetaData</code> object constructed from the XML 
	 *    document
	 * 
	 * @throws InvalidXMLException if the input XML is not valid or does not
	 *    specify a valid ResourceSpecifier
	 */
	public ResourceMetaData parseResourceMetaData(XMLInputSource aInput,
	  ParsingOptions aOptions)
		throws InvalidXMLException
	{ 	
    //attempt to locate resource specifier schema
    URL schemaURL =  getResourceSpecifierSchemaUrl();     
    XMLizable object = parse(aInput, RESOURCE_SPECIFIER_NAMESPACE, schemaURL, aOptions);

    if (object instanceof ResourceMetaData)
    {
      return (ResourceMetaData)object;
    }
    else
    {
      throw new InvalidXMLException(InvalidXMLException.INVALID_CLASS,
        new Object[]{ResourceMetaData.class.getName(), 
        object.getClass().getName()});
    }  
  }
  
  
  /**
   * Parses a URISpecifier from an XML input stream.  XML schema validation
   * will be done against the {@link #RESOURCE_SPECIFIER_SCHEMA_NAME} if it
   * can be found in the classpath.
   * 
   * @param aInput the input source from which to read the XML document
   * 
   * @return a <code>URISpecifier</code> object constructed from the XML 
   *    document
   * 
   * @throws InvalidXMLException if the input XML is not valid or does not
   *    specify a valid URISpecifier
   */
  public URISpecifier parseURISpecifier(XMLInputSource aInput)
    throws InvalidXMLException
  {
  	return parseURISpecifier(aInput, DEFAULT_PARSING_OPTIONS);
  }

	/**
	 * Parses a URISpecifier from an XML input stream.  XML schema validation
	 * will be done against the {@link #RESOURCE_SPECIFIER_SCHEMA_NAME} if it
	 * can be found in the classpath.
	 * 
	 * @param aInput the input source from which to read the XML document
	 * 
	 * @return a <code>URISpecifier</code> object constructed from the XML 
	 *    document
	 * 
	 * @throws InvalidXMLException if the input XML is not valid or does not
	 *    specify a valid URISpecifier
	 */
	public URISpecifier parseURISpecifier(XMLInputSource aInput, ParsingOptions aOptions)
		throws InvalidXMLException
	{  	
    //attempt to locate resource specifier schema
    URL schemaURL =  getResourceSpecifierSchemaUrl();        
    XMLizable object = parse(aInput, RESOURCE_SPECIFIER_NAMESPACE, schemaURL,aOptions);

    if (object instanceof URISpecifier)
    {
      return (URISpecifier)object;
    }
    else
    {
      throw new InvalidXMLException(InvalidXMLException.INVALID_CLASS,
        new Object[]{URISpecifier.class.getName(), 
        object.getClass().getName()});
    }  
  }



  /**
   * Parses a AnalysisEngineDescription from an XML input stream.  XML schema 
   * validation will be done against the {@link #RESOURCE_SPECIFIER_SCHEMA_NAME}
   * if it can be found in the classpath.
   * 
   * @param aInput the input source from which to read the XML document
   * 
   * @return a <code>AnalysisEngineDescription</code> object constructed from 
   *    the XML document
   * 
   * @throws InvalidXMLException if the input XML is not valid or does not
   *    specify a valid AnalysisEngineDescription
   */
  public AnalysisEngineDescription parseAnalysisEngineDescription(XMLInputSource aInput)
    throws InvalidXMLException
  {
  	return parseAnalysisEngineDescription(aInput, DEFAULT_PARSING_OPTIONS);
  }
  	
	/**
	 * Parses a AnalysisEngineDescription from an XML input stream.  XML schema 
	 * validation will be done against the {@link #RESOURCE_SPECIFIER_SCHEMA_NAME}
	 * if it can be found in the classpath.
	 * 
	 * @param aInput the input source from which to read the XML document
	 * 
	 * @return a <code>AnalysisEngineDescription</code> object constructed from 
	 *    the XML document
	 * 
	 * @throws InvalidXMLException if the input XML is not valid or does not
	 *    specify a valid AnalysisEngineDescription
	 */
	public AnalysisEngineDescription parseAnalysisEngineDescription(XMLInputSource aInput,
	  ParsingOptions aOptions)
		throws InvalidXMLException
	{
    //attempt to locate resource specifier schema
    URL schemaURL =  getResourceSpecifierSchemaUrl();          
    XMLizable object = parse(aInput, RESOURCE_SPECIFIER_NAMESPACE, schemaURL, aOptions);

    if (object instanceof AnalysisEngineDescription)
    {
      return (AnalysisEngineDescription)object;
    }
    else
    {
      throw new InvalidXMLException(InvalidXMLException.INVALID_CLASS,
        new Object[]{AnalysisEngineDescription.class.getName(), 
        object.getClass().getName()});
    }  
  }

  /**
   * Parses a TaeDescription from an XML input stream.  XML schema validation
   * will be done against the {@link #RESOURCE_SPECIFIER_SCHEMA_NAME} if it
   * can be found in the classpath.
   * 
   * @param aInput the input source from which to read the XML document
   * 
   * @return a <code>TaeDescription</code> object constructed from the XML 
   *    document
   * 
   * @throws InvalidXMLException if the input XML is not valid or does not
   *    specify a valid TaeDescription
   *    
   * @deprecated since v2.0
   */
  public TaeDescription parseTaeDescription(XMLInputSource aInput)
    throws InvalidXMLException
  {
  	return parseTaeDescription(aInput, DEFAULT_PARSING_OPTIONS);
  }

	/**
	 * Parses a TaeDescription from an XML input stream.  XML schema validation
	 * will be done against the {@link #RESOURCE_SPECIFIER_SCHEMA_NAME} if it
	 * can be found in the classpath.
	 * 
	 * @param aInput the input source from which to read the XML document
	 * 
	 * @return a <code>TaeDescription</code> object constructed from the XML 
	 *    document
	 * 
	 * @throws InvalidXMLException if the input XML is not valid or does not
	 *    specify a valid TaeDescription
   *    
   * @deprecated since v2.0
	 */
	public TaeDescription parseTaeDescription(XMLInputSource aInput, ParsingOptions aOptions)
		throws InvalidXMLException
	{  	
    //attempt to locate resource specifier schema
    URL schemaURL =  getResourceSpecifierSchemaUrl();          
    XMLizable object = parse(aInput, RESOURCE_SPECIFIER_NAMESPACE, schemaURL, aOptions);

    if (object instanceof TaeDescription)
    {
      return (TaeDescription)object;
    }
    else
    {
      throw new InvalidXMLException(InvalidXMLException.INVALID_CLASS,
        new Object[]{TaeDescription.class.getName(), 
        object.getClass().getName()});
    }  
  }

   
  /**
   * @see org.apache.uima.util.XMLParser#parseResultSpecification(org.apache.uima.util.XMLInputSource)
   */
  public ResultSpecification parseResultSpecification(XMLInputSource aInput)
    throws InvalidXMLException
  {
  	return parseResultSpecification(aInput, DEFAULT_PARSING_OPTIONS);
  }
  	
	/**
	 * @see org.apache.uima.util.XMLParser#parseResultSpecification(org.apache.uima.util.XMLInputSource)
	 */
	public ResultSpecification parseResultSpecification(XMLInputSource aInput,
	  ParsingOptions aOptions)
		throws InvalidXMLException
	{
    XMLizable object = parse(aInput, RESOURCE_SPECIFIER_NAMESPACE, null, aOptions);

    if (object instanceof ResultSpecification)
    {
      return (ResultSpecification)object;
    }
    else
    {
      throw new InvalidXMLException(InvalidXMLException.INVALID_CLASS,
        new Object[]{ResultSpecification.class.getName(), 
        object.getClass().getName()});
    }  
  }


  /**
   * @see org.apache.uima.util.XMLParser#parseCasConsumerDescription(org.apache.uima.util.XMLInputSource)
   */
  public CasConsumerDescription parseCasConsumerDescription(XMLInputSource aInput)
    throws InvalidXMLException
  {
  	return parseCasConsumerDescription(aInput, DEFAULT_PARSING_OPTIONS);
  }

	/**
	 * @see org.apache.uima.util.XMLParser#parseCasConsumerDescription(org.apache.uima.util.XMLInputSource)
	 */
	public CasConsumerDescription parseCasConsumerDescription(XMLInputSource aInput,
	  ParsingOptions aOptions)
		throws InvalidXMLException
	{   	
    //attempt to locate resource specifier schema
    URL schemaURL =  getResourceSpecifierSchemaUrl();         
    XMLizable object = parse(aInput, RESOURCE_SPECIFIER_NAMESPACE, schemaURL, aOptions);

    if (object instanceof CasConsumerDescription)
    {
      return (CasConsumerDescription)object;
    }
    else
    {
      throw new InvalidXMLException(InvalidXMLException.INVALID_CLASS,
        new Object[]{CasConsumerDescription.class.getName(), 
        object.getClass().getName()});
    }  
  }
    
  /**
   * @deprecated
   */
  public CasInitializerDescription parseCasInitializerDescription(XMLInputSource aInput)
	  throws InvalidXMLException
	{
		return parseCasInitializerDescription(aInput, DEFAULT_PARSING_OPTIONS);
	}

  /**
   * @deprecated
   */
	public CasInitializerDescription parseCasInitializerDescription(XMLInputSource aInput,
	  ParsingOptions aOptions)
		throws InvalidXMLException
	{		
    //attempt to locate resource specifier schema
    URL schemaURL =  getResourceSpecifierSchemaUrl();        
    XMLizable object = parse(aInput, RESOURCE_SPECIFIER_NAMESPACE, schemaURL, aOptions);

	  if (object instanceof CasInitializerDescription)
	  {
		return (CasInitializerDescription)object;
	  }
	  else
	  {
		throw new InvalidXMLException(InvalidXMLException.INVALID_CLASS,
		  new Object[]{CasInitializerDescription.class.getName(), 
		  object.getClass().getName()});
	  }  
	}

	public CollectionReaderDescription parseCollectionReaderDescription(XMLInputSource aInput)
		 throws InvalidXMLException
  {
  	return parseCollectionReaderDescription(aInput, DEFAULT_PARSING_OPTIONS);
  }
  
	public CollectionReaderDescription parseCollectionReaderDescription(XMLInputSource aInput,
	   ParsingOptions aOptions)
		 throws InvalidXMLException
	{  	
    //attempt to locate resource specifier schema
    URL schemaURL =  getResourceSpecifierSchemaUrl();        
    XMLizable object = parse(aInput, RESOURCE_SPECIFIER_NAMESPACE, schemaURL, aOptions);

		 if (object instanceof CollectionReaderDescription)
		 {
		   return (CollectionReaderDescription)object;
		 }
		 else
		 {
		   throw new InvalidXMLException(InvalidXMLException.INVALID_CLASS,
			 new Object[]{CollectionReaderDescription.class.getName(), 
			 object.getClass().getName()});
		 }  
	 }


   
	/* (non-Javadoc)
	 * @see org.apache.uima.util.XMLParser#parseCpeDescription(org.apache.uima.util.XMLInputSource)
	 */
	public CpeDescription parseCpeDescription(XMLInputSource aInput)
		throws InvalidXMLException
	{
		XMLizable object = parse(aInput);

			 if (object instanceof CpeDescription)
			 {
			   return (CpeDescription)object;
			 }
			 else
			 {
			   throw new InvalidXMLException(InvalidXMLException.INVALID_CLASS,
				 new Object[]{CpeDescription.class.getName(), 
				 object.getClass().getName()});
			 }  
	}
  
	/* (non-Javadoc)
	 * @see org.apache.uima.util.XMLParser#parseTypePriorities(org.apache.uima.util.XMLInputSource)
	 */
	public TypePriorities parseTypePriorities(XMLInputSource aInput)
		throws InvalidXMLException
	{
		return parseTypePriorities(aInput, DEFAULT_PARSING_OPTIONS);
	}
	
	/* (non-Javadoc)
	 * @see org.apache.uima.util.XMLParser#parseTypePriorities(org.apache.uima.util.XMLInputSource)
	 */
	public TypePriorities parseTypePriorities(XMLInputSource aInput, ParsingOptions aOptions)
		throws InvalidXMLException
	{
		//attempt to locate resource specifier schema
    URL schemaURL =  getResourceSpecifierSchemaUrl();        
		XMLizable object = parse(aInput, RESOURCE_SPECIFIER_NAMESPACE, schemaURL, aOptions);

		if (object instanceof TypePriorities)
		{
			return (TypePriorities)object;
		}
		else
		{
			throw new InvalidXMLException(InvalidXMLException.INVALID_CLASS,
				new Object[]{TypePriorities.class.getName(), 
				object.getClass().getName()});
		}  
	}

	/* (non-Javadoc)
	 * @see org.apache.uima.util.XMLParser#parseTypeSystemDescription(org.apache.uima.util.XMLInputSource)
	 */
	public TypeSystemDescription parseTypeSystemDescription(XMLInputSource aInput)
		throws InvalidXMLException
	{
		return parseTypeSystemDescription(aInput, DEFAULT_PARSING_OPTIONS);
	}

	/* (non-Javadoc)
	 * @see org.apache.uima.util.XMLParser#parseTypeSystemDescription(org.apache.uima.util.XMLInputSource)
	 */
	public TypeSystemDescription parseTypeSystemDescription(XMLInputSource aInput, 
	  ParsingOptions aOptions)
		throws InvalidXMLException
	{	
		//attempt to locate resource specifier schema
    URL schemaURL =  getResourceSpecifierSchemaUrl();     
    
		XMLizable object = parse(aInput, RESOURCE_SPECIFIER_NAMESPACE, schemaURL, aOptions);

		if (object instanceof TypeSystemDescription)
		{
			return (TypeSystemDescription)object;
		}
		else
		{
			throw new InvalidXMLException(InvalidXMLException.INVALID_CLASS,
				new Object[]{TypeSystemDescription.class.getName(), 
				object.getClass().getName()});
		}  
	}

	/* (non-Javadoc)
	 * @see org.apache.uima.util.XMLParser#parseFsIndexCollection(org.apache.uima.util.XMLInputSource)
	 */
	public FsIndexCollection parseFsIndexCollection(XMLInputSource aInput)
		throws InvalidXMLException
	{
		return parseFsIndexCollection(aInput, DEFAULT_PARSING_OPTIONS);
	}

	/* (non-Javadoc)
	 * @see org.apache.uima.util.XMLParser#parseFsIndexCollection(org.apache.uima.util.XMLInputSource)
	 */
	public FsIndexCollection parseFsIndexCollection(XMLInputSource aInput, ParsingOptions aOptions)
		throws InvalidXMLException
	{
 		//attempt to locate resource specifier schema
    URL schemaURL =  getResourceSpecifierSchemaUrl();       
		XMLizable object = parse(aInput, RESOURCE_SPECIFIER_NAMESPACE, schemaURL, aOptions);

		if (object instanceof FsIndexCollection)
		{
			return (FsIndexCollection)object;
		}
		else
		{
			throw new InvalidXMLException(InvalidXMLException.INVALID_CLASS,
				new Object[]{FsIndexCollection.class.getName(), 
				object.getClass().getName()});
		}  
	}
	
	/* (non-Javadoc)
	 * @see org.apache.uima.util.XMLParser#parseResourceManagerConfiguration(org.apache.uima.util.XMLInputSource)
	 */
	public ResourceManagerConfiguration parseResourceManagerConfiguration(XMLInputSource aInput)
		throws InvalidXMLException
	{
		return parseResourceManagerConfiguration(aInput, DEFAULT_PARSING_OPTIONS);
	}

	/* (non-Javadoc)
	 * @see org.apache.uima.util.XMLParser#parseResourceManagerConfiguration(org.apache.uima.util.XMLInputSource)
	 */
	public ResourceManagerConfiguration parseResourceManagerConfiguration(XMLInputSource aInput, ParsingOptions aOptions)
		throws InvalidXMLException
	{
		//attempt to locate resource specifier schema
    URL schemaURL =  getResourceSpecifierSchemaUrl();       
		XMLizable object = parse(aInput, RESOURCE_SPECIFIER_NAMESPACE, schemaURL, aOptions);

		if (object instanceof ResourceManagerConfiguration)
		{
			return (ResourceManagerConfiguration)object;
		}
		else
		{
			throw new InvalidXMLException(InvalidXMLException.INVALID_CLASS,
				new Object[]{ResourceManagerConfiguration.class.getName(), 
				object.getClass().getName()});
		}  
	}

	/* (non-Javadoc)
	 * @see org.apache.uima.util.XMLParser#parseIndexBuildSpecification(org.apache.uima.util.XMLInputSource)
	 */
	public IndexBuildSpecification parseIndexBuildSpecification(XMLInputSource aInput)
		throws InvalidXMLException
	{
		return parseIndexBuildSpecification(aInput, DEFAULT_PARSING_OPTIONS);
	}

	/* (non-Javadoc)
	 * @see org.apache.uima.util.XMLParser#parseIndexBuildSpecification(org.apache.uima.util.XMLInputSource, org.apache.uima.util.XMLParser.ParsingOptions)
	 */
	public IndexBuildSpecification parseIndexBuildSpecification(
		XMLInputSource aInput,
		ParsingOptions aOptions)
		throws InvalidXMLException
	{
		XMLizable object = parse(aInput, aOptions);

		if (object instanceof IndexBuildSpecification)
		{
			return (IndexBuildSpecification)object;
		}
		else
		{
			throw new InvalidXMLException(InvalidXMLException.INVALID_CLASS,
				new Object[]{IndexBuildSpecification.class.getName(), 
				object.getClass().getName()});
		}  
	}
	
  /**
   * Configures this XMLParser by registering a mapping between
   * the name of an XML element and the Class of object to be built
   * from elements with that name.
   * 
   * @param aElementName the name of an XML element
   * @param aClassName the name of a Class of object to be built.
   *    This class must implement {@link XMLizable} and have a
   *    zero-argument constructor.
   * 
   * @throws ClassNotFoundException if the class named by 
   *    <code>aClassName</code> could not be found
   * @throws UIMA_IllegalArgumentException if the class named by 
   *    <code>aClassName</code> does not implement <code>XMLIzable</code>.
   * 
   * @
   */
  public void addMapping(String aElementName, String aClassName)
    throws ClassNotFoundException
  {
    //resolve the class name and ensure that it implements XMLizable
    Class cls = Class.forName(aClassName);
    if (XMLizable.class.isAssignableFrom(cls))
    {
      //add to the map
      mElementToClassMap.put(aElementName, cls);
    }
    else
    {
      throw new UIMA_IllegalArgumentException(
        UIMA_IllegalArgumentException.MUST_IMPLEMENT_XMLIZABLE,
        new Object[]{aClassName});
    }
  }
  
   
  /**
   * @see org.apache.uima.util.XMLParser#newSaxDeserializer()
   */
  public SaxDeserializer newSaxDeserializer()
  {
    return new SaxDeserializer_impl(this, null, null, new XMLParser.ParsingOptions(true,true));
  }

  
  /**
   * @see org.apache.uima.util.XMLParser#newSaxDeserializer(java.lang.String, java.net.URL, boolean)
   */
  public SaxDeserializer newSaxDeserializer(String aNamespaceForSchema,
    URL aSchemaUrl, boolean aExpandXIncludes)
  {
    return new SaxDeserializer_impl(this, aNamespaceForSchema, aSchemaUrl, 
                                    new XMLParser.ParsingOptions(aExpandXIncludes,true));
  }  

	/**
	 * @see org.apache.uima.util.XMLParser#newSaxDeserializer(java.lang.String, java.net.URL, boolean)
	 */
	public SaxDeserializer newSaxDeserializer(String aNamespaceForSchema,
		URL aSchemaUrl, XMLParser.ParsingOptions aOptions)
	{
		return new SaxDeserializer_impl(this, aNamespaceForSchema, aSchemaUrl, aOptions);
	}   

  /**
   * @return The URL to the Resource Specifier XML Schema file
   */
  private URL getResourceSpecifierSchemaUrl()
  {
    //TODO: could cache this
    URL schemaURL = getClass().getResource(RESOURCE_SPECIFIER_SCHEMA_NAME);
    if (schemaURL == null)
    {
      UIMAFramework.getLogger(CLASS_NAME).logrb(Level.WARNING,CLASS_NAME.getName(),
        "getSchemaURL",LOG_RESOURCE_BUNDLE, "UIMA_resource_specifier_schema_not_found__WARNING");
    }    
    return schemaURL;
  }

 /**
  * Map from XML element names to Class objects.
  */ 
  protected Map mElementToClassMap = 
      Collections.synchronizedMap(new HashMap());  

  /**
   * Whether schema validation is enabled.
   */ 
  protected boolean mSchemaValidationEnabled = false;
  
  protected static final ParsingOptions DEFAULT_PARSING_OPTIONS = new ParsingOptions(true,true);
}
