//
// Copyright (c) 2012 Mirko Nasato
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the "Software"),
// to deal in the Software without restriction, including without limitation
// the rights to use, copy, modify, merge, publish, distribute, sublicense,
// and/or sell copies of the Software, and to permit persons to whom the
// Software is furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included
// in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
// THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR
// OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
// ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
// OTHER DEALINGS IN THE SOFTWARE.
//
package org.graphipedia.dataimport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public class LinkExtractor extends SimpleStaxParser {

    private static final Pattern LINK_PATTERN = Pattern.compile("\\[\\[(.+?)\\]\\]");

    private final XMLStreamWriter writer;
    private final ProgressCounter pageCounter = new ProgressCounter();

    private String title;
    private String text;

    public LinkExtractor(XMLStreamWriter writer) {
        super(Arrays.asList("page", "title", "text"));
        this.writer = writer;
    }

    public int getPageCount() {
        return pageCounter.getCount();
    }

    @Override
    protected void handleElement(String element, String value) {
        if ("page".equals(element)) {
            if (!title.contains(":")) {
                try {
                    writePage(title, text);
                } catch (XMLStreamException streamException) {
                    throw new RuntimeException(streamException);
                }
            }
            title = null;
            text = null;
        } else if ("title".equals(element)) {
            title = value;
        } else if ("text".equals(element)) {
            text = value;
        }
    }

    private void writePage(String title, String text) throws XMLStreamException {
        writer.writeStartElement("p");
        
        writer.writeStartElement("t");
        writer.writeCharacters(title);
        writer.writeEndElement();

        ArrayList<Set<String>> pageLinks = parseLinks(text);
        Set<String> links = pageLinks.get(0);
        Set<String> categories = pageLinks.get(1);

        links.remove(title);
        
        for (String link : links) {
            writer.writeStartElement("l");
            writer.writeCharacters(link);
            writer.writeEndElement();
        }

        for (String category : categories) {
            writer.writeStartElement("c");
            writer.writeCharacters(category);
            writer.writeEndElement();
        }

        writer.writeEndElement();

        pageCounter.increment();
    }

    private ArrayList<Set<String>> parseLinks(String text) {
        // Since this covers everything well enough, I'll just merge the two instead of scanning every page twice (barf)
        // because the !link.contains(":") looks like it's used to filter out Category links (among others)
        // ok what is up with java and Pair types
        ArrayList<Set<String>> pageLinks = new ArrayList<Set<String>>();

        // HELP COMPUTER
        Set<String> links = new HashSet<String>();
        Set<String> categories = new HashSet<String>();

        pageLinks.add(links);
        pageLinks.add(categories);

        if (text != null) {
            Matcher matcher = LINK_PATTERN.matcher(text);
            while (matcher.find()) {
                String link = matcher.group(1);
                if (!link.contains(":")) {
                    if (link.contains("|")) {
                        link = link.substring(0, link.lastIndexOf('|'));
                    }
                    links.add(link);
                } else if (link.startsWith("Category:")) {
                    if (link.contains("|")) {
                        link = link.substring(9, link.lastIndexOf('|'));
                    } else {
                        link = link.substring(9);
                    }
                    links.add(link);
                }
            }
        }
        return pageLinks;
    }

}
