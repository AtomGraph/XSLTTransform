/*
 * Copyright 2019 Martynas Jusevičius <martynas@atomgraph.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.atomgraph.etl.aws.kinesis.transform;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.KinesisAnalyticsInputPreprocessingResponse;
import com.amazonaws.services.lambda.runtime.events.KinesisAnalyticsInputPreprocessingResponse.Record;
import com.amazonaws.services.lambda.runtime.events.KinesisFirehoseEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.xml.transform.stream.StreamSource;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.s9api.Xslt30Transformer;
import net.sf.saxon.s9api.XsltExecutable;

/**
 * AWS Lambda function that transforms Kinesis Firehose records using XSLT 3.0 processor.
 * You need to extend this base class and to supply the stylesheet and its parameters to the subclass constructor.
 * 
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
public class XSLTTransform
{

    private static final Processor PROCESSOR = new Processor(false);

    private final XsltExecutable stylesheet;
    private final Xslt30Transformer transformer;

    public XSLTTransform(StreamSource stylesheet, Map<QName, XdmValue> params) throws SaxonApiException
    {
        this(PROCESSOR.newXsltCompiler().compile(stylesheet), params);
    }
    
    public XSLTTransform(XsltExecutable stylesheet, Map<QName, XdmValue> params) throws SaxonApiException
    {
        this.stylesheet = stylesheet;
        this.transformer = stylesheet.load30();
        this.transformer.setStylesheetParameters(params);
    }
    
    public KinesisAnalyticsInputPreprocessingResponse transformRecords(KinesisFirehoseEvent event, Context context)
    {
        List<Record> transformed = event.getRecords().stream().map(this::transformRecord).collect(Collectors.toList());
        KinesisAnalyticsInputPreprocessingResponse response = new KinesisAnalyticsInputPreprocessingResponse();
        response.setRecords(transformed);
        return response;
    }
    
    public KinesisAnalyticsInputPreprocessingResponse.Record transformRecord(KinesisFirehoseEvent.Record inputRecord)
    {
        StreamSource input = new StreamSource(new ByteArrayInputStream(inputRecord.getData().array()));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Serializer output = getProcessor().newSerializer(baos);

        Record record = new Record();
        record.setRecordId(inputRecord.getRecordId());
        try
        {
            getTransformer().transform(input, output);

            record.setData(ByteBuffer.wrap(baos.toByteArray()));
            record.setResult(KinesisAnalyticsInputPreprocessingResponse.Result.Ok);
            return record;
        }
        catch (SaxonApiException ex)
        {
            log(ex);
            record.setResult(KinesisAnalyticsInputPreprocessingResponse.Result.ProcessingFailed);
            return record;
        }
    }
    
    public void log(Exception ex)
    {
        ex.printStackTrace();
    }
    
    public Processor getProcessor()
    {
        return PROCESSOR;
    }
    
    public XsltExecutable getStylesheet()
    {
        return stylesheet;
    }
    
    public Xslt30Transformer getTransformer()
    {
        return transformer;
    }
    
}
