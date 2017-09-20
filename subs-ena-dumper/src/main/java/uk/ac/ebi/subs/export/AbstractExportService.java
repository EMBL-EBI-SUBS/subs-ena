package uk.ac.ebi.subs.export;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.w3c.dom.Node;
import uk.ac.ebi.subs.data.submittable.ENASubmittable;
import uk.ac.ebi.subs.data.submittable.MappingHelper;
import uk.ac.ebi.subs.data.submittable.Submittable;
import uk.ac.ebi.subs.ena.data.SubmittableSRAInfo;
import uk.ac.ebi.subs.ena.repository.SampleRepository;
import uk.ac.ebi.subs.ena.repository.SubmittableSRARepository;

import javax.persistence.criteria.CriteriaBuilder;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Iterator;
import java.util.List;

public class AbstractExportService<K extends Submittable,V extends ENASubmittable<K>> implements ExportService {
    public static final int PAGE_SIZE = 10000;
    SubmittableSRARepository<? extends SubmittableSRAInfo> submittableSRARepository;
    Class<V> enaSubmittableClass;
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    SampleRepository sampleRepository;
    Unmarshaller unmarshaller;
    static TransformerFactory transFactory = TransformerFactory.newInstance();
    XPathFactory xPathFactory = XPathFactory.newInstance();
    XPath xPath = null;
    XPathExpression xPathExpression = null;
    ObjectMapper objectMapper;
    public AbstractExportService(SubmittableSRARepository<? extends SubmittableSRAInfo> submittableSRARepository, Class<V> enaSubmittableClass, String enaMarshaller, String rootNodeXpathExpression, ObjectMapper objectMapper) {
        this.submittableSRARepository = submittableSRARepository;
        this.enaSubmittableClass = enaSubmittableClass;
        this.objectMapper = objectMapper;
        this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        try {
            unmarshaller = MappingHelper.createUnmarshaller(enaSubmittableClass, MappingHelper.SUBMITTABLE_PACKAGE, enaMarshaller, MappingHelper.COMPONENT_PACKAGE, MappingHelper.ATTRIBUTE_MAPPING);
            xPath = xPathFactory.newXPath();
            xPathExpression = xPath.compile(rootNodeXpathExpression);
        } catch (Exception e) {
            logger.info("Exception in createUnmarshaller",  e);
        }
    }

    public void export(Path path, String submissionAccountId) {
        logger.info("Dumping in " + path.toString());
        Pageable page = new PageRequest(0, PAGE_SIZE);
        long rowCount = submittableSRARepository.countBySubmissionAccountIdAndStatusId(submissionAccountId,4);

        for (int i = 0; i * PAGE_SIZE < rowCount; i++) {
            final List<? extends SubmittableSRAInfo> submittableList = submittableSRARepository.findBySubmissionAccountIdAndStatusId(submissionAccountId, 4, page);

            for (SubmittableSRAInfo submittableSRAInfo : submittableList) {
                try {
                    final LocalDateTime localDateTime = submittableSRAInfo.getFirstCreated().toLocalDateTime();
                    Path resolve = path.resolve(Integer.toString(localDateTime.getYear())).resolve(localDateTime.getMonth().name()).resolve(Integer.toString(localDateTime.getDayOfMonth()));
                    final K submittable = getSubmittable(submittableSRAInfo);
                    Files.createDirectories(resolve);
                    final Path exportPath = resolve.resolve(submittable.getAccession() + ".json");
                    objectMapper.writeValue(exportPath.toFile(),submittable);
                    logger.trace("Dumped " + submittable.getAccession());
                }  catch (Exception e) {
                    logger.info("Error in running sampleXPathExpression",e);
                }

            }
            logger.info("dumped " + page.getPageSize() + " records in page " + page.getPageNumber());
            page = page.next();
        }

        logger.info("Dumped " + rowCount + " objects");
    }

    protected K getSubmittable(SubmittableSRAInfo submittable) throws XPathExpressionException, JAXBException, IllegalAccessException {
        Node node = submittable.getDocument();
        node = (Node) xPathExpression.evaluate(node, XPathConstants.NODE);
        final V enaSubmittable = unmarshaller.unmarshal(node, enaSubmittableClass).getValue();
        enaSubmittable.deSerialiseAttributes();
        return enaSubmittable.getBaseObject();
    }

    public static String getDocumentString(Node node) throws TransformerException {
        DOMSource source = new DOMSource(node);
        StringWriter stringWriter = new StringWriter();
        Result result = new StreamResult(stringWriter);
        Transformer transformer = transFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.transform(source, result);
        return stringWriter.toString();
    }

    public Class<? extends ENASubmittable> getEnaSubmittableClass() {
        return enaSubmittableClass;
    }

    public XPathExpression getxPathExpression() {
        return xPathExpression;
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }
}