package org.jupnp.transport.impl;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Abstract class responsible for creating new {@link Document}s either for writing or already parsed from a given input
 * stream. The class manages a pool ofr {@link DocumentBuilder}s internally and reuses them in different threads saving
 * cpu time from factory and builder instantiation.
 * 
 * Default pool size is 20.
 * 
 * @author Ivan Iliev - Initial contribution and API
 *
 */
public abstract class PooledXmlProcessor {

    private final DocumentBuilderFactory documentBuilderFactory;

    private final ConcurrentLinkedQueue<DocumentBuilder> builderPool;

    private final transient Logger logger = LoggerFactory.getLogger(PooledXmlProcessor.class);

    public PooledXmlProcessor() {
        this(20);
    }

    public PooledXmlProcessor(int basePoolSize) {
        documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        builderPool = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < basePoolSize; i++) {
            try {
                builderPool.add(documentBuilderFactory.newDocumentBuilder());
            } catch (ParserConfigurationException e) {
                logger.error("Error when invoking newDocumentBuilder():", e);
            }
        }
    }

    /**
     * @return a new unused instance of {@link Document}.
     * @throws FactoryConfigurationError
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    protected Document newDocument()
            throws FactoryConfigurationError, ParserConfigurationException, SAXException, IOException {
        return getDocument(null, null);
    }

    /**
     * 
     * @param inputSource to parse from
     * @param errorHandler custom error handler for the parsing operation
     * @return The parsed {@link Document} instance.
     * 
     * @throws FactoryConfigurationError
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    protected Document readDocument(InputSource inputSource, ErrorHandler errorHandler)
            throws FactoryConfigurationError, ParserConfigurationException, SAXException, IOException {
        return getDocument(inputSource, errorHandler);
    }

    /**
     * @param inputSource to parse from
     * @return The parsed {@link Document} instance.
     * 
     * @throws FactoryConfigurationError
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    protected Document readDocument(InputSource inputSource)
            throws FactoryConfigurationError, ParserConfigurationException, SAXException, IOException {
        return getDocument(inputSource, null);
    }

    private Document getDocument(InputSource inputSource, ErrorHandler errorHandler)
            throws FactoryConfigurationError, ParserConfigurationException, SAXException, IOException {
        DocumentBuilder builder = builderPool.poll();
        if (builder == null) {
            builder = documentBuilderFactory.newDocumentBuilder();
        }

        try {

            if (errorHandler != null) {
                builder.setErrorHandler(errorHandler);
            }

            if (inputSource != null) {
                return builder.parse(inputSource);
            }

            return builder.newDocument();

        } finally {
            returnBuilder(builder);
        }
    }

    private void returnBuilder(DocumentBuilder builder) throws FactoryConfigurationError, ParserConfigurationException {
        builder.reset();
        builderPool.add(builder);
    }
}
