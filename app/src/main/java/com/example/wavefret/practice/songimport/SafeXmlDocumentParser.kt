package com.example.wavefret.practice.songimport

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Parses untrusted XML from imported files without expanding entities or
 * fetching external DTDs (XXE-safe), tolerating a leading byte-order mark
 * or whitespace before the XML declaration.
 */
class SafeXmlDocumentParser {

    /**
     * @param xmlText Document text. Type: String
     * @return The parsed DOM document. Type: Document
     * @throws IllegalArgumentException If the text is not well-formed XML.
     */
    fun parse(xmlText: String): Document {
        val documentBuilderFactory = DocumentBuilderFactory.newInstance().apply {
            isExpandEntityReferences = false
            isNamespaceAware = false
            runCatching { setFeature(LOAD_EXTERNAL_DTD_FEATURE, false) }
        }
        val documentText = xmlText.trimStart(BYTE_ORDER_MARK, ' ', '\t', '\r', '\n')
        return try {
            documentBuilderFactory.newDocumentBuilder().parse(InputSource(StringReader(documentText)))
        } catch (malformedXml: SAXException) {
            throw IllegalArgumentException("Not valid XML: ${malformedXml.message}", malformedXml)
        }
    }

    private companion object {
        const val LOAD_EXTERNAL_DTD_FEATURE = "http://apache.org/xml/features/nonvalidating/load-external-dtd"
        const val BYTE_ORDER_MARK = '﻿'
    }
}

/**
 * @receiver Element to look in. Type: Element
 * @param tagName Tag of the descendant wanted. Type: String
 * @return Trimmed text of the first descendant with that tag, or null. Type: String?
 */
internal fun Element.descendantText(tagName: String): String? =
    getElementsByTagName(tagName).item(0)?.textContent?.trim()

/**
 * @receiver Element to look in. Type: Element
 * @param tagName Tag of the direct child wanted. Type: String
 * @return The first direct child with that tag, or null. Type: Element?
 */
internal fun Element.childElement(tagName: String): Element? {
    val children = childNodes
    return (0 until children.length).map { children.item(it) }.firstOrNull { it is Element && it.tagName == tagName } as? Element
}

/**
 * @receiver Element to look in. Type: Element
 * @param tagName Tag of the descendants wanted. Type: String
 * @return Every descendant element with that tag, in document order. Type: List<Element>
 */
internal fun Element.descendantElements(tagName: String): List<Element> {
    val nodes = getElementsByTagName(tagName)
    return (0 until nodes.length).map { nodes.item(it) as Element }
}
