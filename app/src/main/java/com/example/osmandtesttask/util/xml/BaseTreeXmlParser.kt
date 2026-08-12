package com.example.osmandtesttask.util.xml

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException

abstract class BaseTreeXmlParser() {

    @Throws(IOException::class, XmlPullParserException::class)
    protected fun parse(parser: XmlPullParser, rootContext: TagContext) {
        forwardUntil(parser, rootContext.contextTagName)
        if (parser.eventType == XmlPullParser.END_DOCUMENT) {
            return
        }

        val stack = ArrayDeque<TagContext>()

        rootContext.readAttributes(parser)
        stack.addLast(rootContext)

        var eventType = parser.next()
        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tagName = parser.name
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    val current = stack.last()
                    val nextContext = current.createChildContext(tagName)
                    if (nextContext == null) {
                        skip(parser)
                    } else {
                        nextContext.readAttributes(parser)
                        stack.addLast(nextContext)
                    }
                }
                XmlPullParser.TEXT -> {
                    val text = parser.text
                    if (!text.isNullOrEmpty() && stack.isNotEmpty()) {
                        stack.last().onText(text)
                    }
                }

                XmlPullParser.END_TAG -> {
                    if (stack.isNotEmpty()) {
                        val current = stack.last()

                        if (current.contextTagName == tagName) {
                            current.onEndTag()
                            stack.removeLast()

                            // 4. Pass the completed child to the parent context below it
                            if (stack.isNotEmpty()) {
                                stack.last().onChildContextCompleted(current)
                            }
                        }
                    }
                }
            }

            eventType = parser.next()
        }

        if (stack.isNotEmpty()) {
            stack.last().onEndTag()
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    protected fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            throw IllegalStateException()
        }
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }

    private fun forwardUntil(parser: XmlPullParser, tagName: String) {
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == tagName && parser.depth == 1) {
                break
            }
            eventType = parser.next()
        }
    }
}

interface TagContext {
    val contextTagName: String
    val data: Any

    @Throws(IOException::class, XmlPullParserException::class)
    fun readAttributes(parser: XmlPullParser)

    fun onEndTag()

    fun onText(text: String)

    fun createChildContext(tagName: String): TagContext?
    fun onChildContextCompleted(child: TagContext)
}

fun TagContext.readStringAttribute(parser: XmlPullParser, attributeName: String, namespace: String? = null): String?  {
    return parser.getAttributeValue(namespace, attributeName)
}