package uk.ac.ebi.subs.ena.processor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.repository.MongoRepository;

import org.springframework.oxm.Marshaller;
import uk.ac.ebi.ena.sra.ExperimentInfo;
import uk.ac.ebi.ena.sra.SRALoader;
import uk.ac.ebi.ena.sra.StudyInfo;
import uk.ac.ebi.ena.sra.xml.ExperimentType;
import uk.ac.ebi.ena.sra.xml.StudyType;
import uk.ac.ebi.subs.data.FullSubmission;
import uk.ac.ebi.subs.data.component.Archive;
import uk.ac.ebi.subs.data.component.SampleRef;
import uk.ac.ebi.subs.data.component.SampleUse;
import uk.ac.ebi.subs.data.status.ProcessingStatus;
import uk.ac.ebi.subs.data.status.ProcessingStatusEnum;
import uk.ac.ebi.subs.data.submittable.Assay;
import uk.ac.ebi.subs.data.submittable.ENAExperiment;
import uk.ac.ebi.subs.data.submittable.Sample;
import uk.ac.ebi.subs.processing.ProcessingCertificate;
import uk.ac.ebi.subs.processing.SubmissionEnvelope;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ENAExperimentProcessor extends ENAAgentProcessor<ENAExperiment> {

    static String EXPERIMENT_SCHEMA = "experiment";
    public static final String EXPERIMENT_SET_XSD = "https://github.com/enasequence/schema/blob/master/src/main/resources/uk/ac/ebi/ena/sra/schema/SRA.experiment.xsd";


    public ENAExperimentProcessor(SubmissionEnvelope submissionEnvelope, Archive archive, Marshaller marshaller, Connection connection, String submissionAccountId, SRALoader.TransactionMode transactionMode) {
        super(submissionEnvelope, archive, marshaller, connection, submissionAccountId, transactionMode);
    }

    ProcessingCertificate processData(ENAExperiment submittable, SubmissionEnvelope submissionEnvelope) {
        FullSubmission submission = submissionEnvelope.getSubmission();

        for (SampleUse su : submittable.getBaseObject().getSampleUses()){
            SampleRef sr = su.getSampleRef();
            Sample sample = sr.fillIn(submission.getSamples(),submissionEnvelope.getSupportingSamples());

            if (sample != null) {
//                enaSampleRepository.save(sample);
            }
        }

        submittable.getStudyRef().fillIn(submission.getStudies());
        return super.processData(submittable, submissionEnvelope);
    }

    @Override
    String executeSRALoader(String submissionXML, String submittableXML) throws Exception {
        String accession = null;
        if (sraLoader.eraputRestWebin(submissionXML,
                null, null, submittableXML, null, null, null, null, null, null,
                null, authResult, null, connection) == 0) {
            final Map<ExperimentType, ExperimentInfo> experiments = sraLoader.getExperiments();
            if (experiments != null) {
                if (experiments.values().iterator().hasNext()) {
                    accession = experiments.values().iterator().next().getExperimentAccession();
                    logger.info("Created ENA experiment with accession " + accession);
                }
            }
        } else {
            logValidationErrors();
        }
        if (accession == null ) {
            throw new SRALoaderAccessionException(submissionXML,submittableXML);
        }
        return accession;
    }

    @Override
    String getSchemaName() {
        return EXPERIMENT_SCHEMA;
    }

    @Override
    List<ENAExperiment> getSubmittables(FullSubmission fullSubmission) {
        List <ENAExperiment> enaExperimentList = new ArrayList<>();
        for (Assay assay : fullSubmission.getAssays()) {
            try {
                enaExperimentList.add(new ENAExperiment(assay));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return enaExperimentList;
    }
}