# XSLTTransform

XSLTTransform is a generic [AWS Lambda function](https://aws.amazon.com/lambda/) designed specifically for [Kinesis Data Firehose Data Transformation](https://docs.aws.amazon.com/firehose/latest/dev/data-transformation.html).

XSLTTransform uses Saxon-HE 9.9 processor to perform an XSLT 3.0 transformation of input records to output records. XSLT 1.0 and 2.0 stylesheets should be backwards-compatible.
Naturally, the input records have to be XML. JSON can work too, if you use a 3.0 stylesheet and its [JSON processing](https://www.w3.org/TR/xslt-30/#json) features.

Passing a map of stylesheet parameters is supported. Environment variables can be retrieved using `System.getenv()` and forwarded as stylesheet parameters.

## Usage

The XSLT stylesheet is user-defined and has to be supplied to the function. It can be done by extending the [`XSLTTransform`](src/main/java/com/atomgraph/etl/aws/kinesis/transform/XSLTTransform.java) base class and overriding its constructor with stylesheet and parameter map as arguments.

The simplest way to embed a stylesheet is to put it under `/src/main/resources`, from where it can be read using `getResourceAsStream()`. That way the stylesheet is built into the function JAR itself. Alternatively, it should be possible to load it from S3.

In AWS Lambda configuration, specify `transformRecords` method (which is inherited by your subclass) as the function handler.

## Example

The following example shows `/src/main/resources/custom.xsl` used as the XSLT stylesheet, as well as `my-param` stylesheet parameter initialized with the value of `MY_PARAM` environment variable.

```java
package custom;

import com.atomgraph.etl.aws.kinesis.transform.XSLTTransform;
import java.util.Collections;
import javax.xml.transform.stream.StreamSource;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmAtomicValue;

public class CustomXSLTTransform extends XSLTTransform
{
    
    public CustomXSLTTransform() throws SaxonApiException
    {
        super(new StreamSource(CustomXSLTTransform.class.getResourceAsStream("/custom.xsl")),
            Collections.singletonMap(new QName("my-param"), new XdmAtomicValue(System.getenv("MY_PARAM"))));
    }

}
```

The function handler would be `custom.CustomXSLTTransform::transformRecords` in this case.

## Build

Create a new Java project and include XSLTTransform as dependency:

```xml
<dependency>
    <groupId>com.atomgraph.etl.aws.kinesis</groupId>
    <artifactId>XSLTTransform</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

You might also need add `maven-shade-plugin`, as per [Creating a .jar Deployment Package Using Maven without any IDE (Java)](https://docs.aws.amazon.com/lambda/latest/dg/java-create-jar-pkg-maven-no-ide.html#java-create-jar-pkg-maven-no-ide-create-project).

Package the extended function for upload to AWS Lambda by running `mvn package`.