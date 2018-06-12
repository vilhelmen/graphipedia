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
            if (!title.contains(":") || title.startsWith("Category:")) {
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
        // Set<String> categories = pageLinks.get(1);

        links.remove(title);
        
        for (String link : links) {
            writer.writeStartElement("l");
            writer.writeCharacters(link);
            writer.writeEndElement();
        }

        /*
        for (String category : categories) {
            writer.writeStartElement("c");
            writer.writeCharacters(category);
            writer.writeEndElement();
        }
        */

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
        //Set<String> categories = new HashSet<String>();

        pageLinks.add(links);
        //pageLinks.add(categories);

        if (text != null) {
            Matcher matcher = LINK_PATTERN.matcher(text);
            while (matcher.find()) {
                String link = matcher.group(1);
                if (!link.contains(":") || link.startsWith("Category:")) {
                    if (link.contains("|")) {
                        link = link.substring(0, link.lastIndexOf('|'));
                    }
                    links.add(link);
                }
            }

            // Ok, we're gonna need to process category links more carefully. After diving through the dump more
            // I realized i was assuming ANY category link in the page meant the page was in that category, which is wrong
            // Additionally, category pagers ARE in the dump, but filtered out by the ':' title check, so they aren't nodes

            // I've fought back and forth whether categories should be a special node/link type or just a property on the node
            // and I think I'm just going to make it a property

            // So, from analyzing dump pages, it looks like the best way to identify category links is to look backwards
            // from EOF and just process a contiguous link chunk
            // This way we'll only get the links at the very bottom, which is categories. We'll stop as soon as we hit
            // anything article-related or {{}} format stuff

            // I've also noticed that this system picks up redirect pages as well as disambiguation (I think?)
            // I have not verified if there are links pointing to redirect pages.
            // I would like to remove redirect entries, but if anything links to a redirect I either need to add logic
            // to track and fix redirect chains or we lose linkage data. So I'm just leaving it as is for now.

            // So, category pattern will look for, roughly, ([[[C|c]ategory:.*]])* at the bottom of pages
            // I'm not sure there are any lowercase c categories, there's probably a linter or something

            // I'm still not sure if I should enable category pages as nodes or not.
            // There are articles that link to them
            // ex "Group A opposed the actions of [[Category:Group B organizations]]"

            // I think for now, I'll enable category pages and links to them and not do properties.
        }
        return pageLinks;
    }

}
