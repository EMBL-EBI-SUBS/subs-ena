package uk.ac.ebi.subs.ena.processor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.ac.ebi.subs.data.component.Archive;
import uk.ac.ebi.subs.data.status.ProcessingStatusEnum;
import uk.ac.ebi.subs.data.submittable.*;
import uk.ac.ebi.subs.ena.loader.SRALoaderService;
import uk.ac.ebi.subs.processing.ProcessingCertificate;
import uk.ac.ebi.subs.processing.SubmissionEnvelope;
import uk.ac.ebi.subs.validator.data.SingleValidationResult;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
public class ENAStudyProcessor extends AbstractENAProcessor<ENAStudy>  {

    @Autowired
    public void setLoader(SRALoaderService<ENAStudy> sraLoaderService) {
        this.sraLoaderService = sraLoaderService;
    }

    @Override
    public String getSubmittableObjectTypeAsAString() {
        return Study.class.getSimpleName();
    }

    public ENAStudy convertFromSubmittableToENASubmittable(Submittable submittable, Collection<SingleValidationResult> singleValidationResultList) throws InstantiationException, IllegalAccessException {
        final ENAStudy enaStudy = ENASubmittable.create(ENAStudy.class, submittable);
        singleValidationResultList.addAll(enaStudy.getValidationResultList());
        return enaStudy;
    }

    @Override
    public List<Study> getSubmittables(SubmissionEnvelope submissionEnvelope) {
        return submissionEnvelope.getStudies();
    }

}
