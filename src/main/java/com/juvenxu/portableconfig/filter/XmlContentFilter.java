package com.juvenxu.portableconfig.filter;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.StringUtils;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaders;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import com.juvenxu.portableconfig.ContentFilter;
import com.juvenxu.portableconfig.model.Replace;

/**
 * @author juven
 */
@Component(role = ContentFilter.class, hint = "xml")
public class XmlContentFilter implements ContentFilter
{

  private static final String CUSTOMIZED_NAMESPACE_PREFIX = "portableconfig";

  @Override
  public boolean accept(String contentType)
  {
    return (".xml").equals(contentType);
  }

  @Override
  public void filter(Reader reader, Writer writer, List<Replace> replaces) throws IOException
  {
    Document doc = null;

    try
    {
      doc = createDTDUnawareSaxBuilder().build(reader);
    }
    catch (JDOMException e)
    {
      throw new IOException("Failed to build Xml document.", e);
    }


    for (Replace replace : replaces)
    {
      XPathFactory xPathFactory = XPathFactory.instance();

      XPathExpression xPathExpression = null;

      String rootNamespaceURI = doc.getRootElement().getNamespaceURI();

      if (StringUtils.isEmpty(rootNamespaceURI))
      {
        xPathExpression = xPathFactory.compile(replace.getXpath());
      }
      else if (false)
      {

      }
      else
      {
        Namespace rootNamespace = Namespace.getNamespace(CUSTOMIZED_NAMESPACE_PREFIX, doc.getRootElement().getNamespaceURI());

        List<Namespace> x = doc.getRootElement().getAdditionalNamespaces();
        List<Namespace> allNS = new ArrayList<Namespace>();
        allNS.add(rootNamespace);
        allNS.addAll(x);

        String expression = addCustomizedNamespacePrefix(CUSTOMIZED_NAMESPACE_PREFIX, replace.getXpath());

        xPathExpression = xPathFactory.compile(expression, Filters.fpassthrough(), null, allNS);
      }

      for (Object obj : xPathExpression.evaluate(doc))
      {
        if (obj instanceof Element)
        {
          ((Element) obj).setText(replace.getValue());
        }
        else if (obj instanceof Attribute)
        {
          ((Attribute) obj).setValue(replace.getValue());
        }
        else
        {
          throw new IOException("Unsupported xpath result object: " + obj.getClass().toString());
        }
      }
    }

    XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());

    xmlOutputter.output(doc, writer);
  }

  private SAXBuilder createDTDUnawareSaxBuilder()
  {
    SAXBuilder saxBuilder = new SAXBuilder();

    // http://xerces.apache.org/xerces2-j/features.html
    saxBuilder.setXMLReaderFactory(XMLReaders.NONVALIDATING);
    saxBuilder.setFeature("http://xml.org/sax/features/validation", false);
    saxBuilder.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
    saxBuilder.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    return saxBuilder;
  }

  private String addCustomizedNamespacePrefix(String customizedNamespacePrefix, String expression)
  {
    StringBuffer result = new StringBuffer();

    List<String> parts = Arrays.asList(expression.split("/"));

    if ( expression.startsWith("//"))
    {


      parts = parts.subList( 1, parts.size());
    }

    for (String part : parts)
    {
      result.append("/");

      if ( StringUtils.isEmpty(part))
      {
        continue;
      }

      if ( part.startsWith("@"))
      {
        result.append( part);
      }
      else if ( part.contains(":"))
      {
        result.append( part);
      }
      else
      {
        result.append(customizedNamespacePrefix);
        result.append(":");
        result.append(part);
      }
    }

    //System.out.println(expression);
    //System.out.println(result.toString());
    return result.toString();

    // return expression.replaceAll("(/+)([^@/:]+)", "$1" + customizedNamespacePrefix + ":" + "$2");
  }
}
