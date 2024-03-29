package com.studyolle.modules.study;

import com.studyolle.modules.account.Account;
import com.studyolle.modules.study.event.StudyCreatedEvent;
import com.studyolle.modules.tag.Tag;
import com.studyolle.modules.tag.TagRepository;
import com.studyolle.modules.tag.TagService;
import com.studyolle.modules.zone.Zone;
import lombok.RequiredArgsConstructor;
import net.bytebuddy.utility.RandomString;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;

import static com.studyolle.modules.study.form.StudyForm.VALID_PATH_PATTERN;

@RequiredArgsConstructor
@Transactional
@Service
public class StudyService {

    private final StudyRepository studyRepository;
    private final ModelMapper modelMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final TagService tagService;


    public Study createNewStudy(Study study, Account account) {
        Study newStudy = studyRepository.save(study);
        newStudy.addManager(account);
        return newStudy;
    }

    public Study getStudy(String path) {
        Study study = studyRepository.findByPath(path);
        studyExistingVerifier(path, study);
        return study;
    }

    public Study getStudyToUpdate(Account account, String path) {
        Study study = this.getStudy(path);
        isManagerVerifier(account, study);
        return study;
    }

    public Study getStudyToUpdateTag(Account account, String path) {
        Study study = studyRepository.findStudyWithTagsByPath(path);
        studyExistingVerifier(path, study);
        isManagerVerifier(account, study);
        return study;
    }

    public Study getStudyToUpdateZone(Account account, String path) {
        Study study = studyRepository.findStudyWithZonesByPath(path);
        studyExistingVerifier(path, study);
        isManagerVerifier(account, study);
        return study;
    }

    public void updateStudyDescription(Study study, StudyDescriptionForm studyDescriptionForm) {
        modelMapper.map(studyDescriptionForm, study);
        this.eventPublisher.publishEvent(new StudyUpdateEvent(study, "스터디 소개를 수정했습니다."));
    }

    public void updateStudyImage(Study study, String image) {
        study.setImage(image);
    }

    public void enableStudyBanner(Study study) {
        study.setUseBanner(true);
    }

    public void disableStudyBanner(Study study) {
        study.setUseBanner(false);
    }

    public void addTag(Study study, Tag tag) {
        study.getTags().add(tag);
    }

    public void removeTag(Study study, Tag tag) {
        study.getTags().remove(tag);
    }

    public void addZone(Study study, Zone zone) {
        study.getZones().add(zone);
    }

    public void removeZone(Study study, Zone zone) {
        study.getZones().remove(zone);

    }

    private void studyExistingVerifier(String path, Study study) {
        if (study == null) {
            throw new IllegalArgumentException(path + "에 해당하는 스터디가 없습니다");
        }
    }

    private void isManagerVerifier(Account account, Study study) {
        if (!study.isManagedBy(account)) {
            throw new AccessDeniedException("해당 기능을 사용할 수 없습니다.");
        }
    }

    public void publish(Study study) {
        study.publish();

        this.eventPublisher.publishEvent(new StudyCreatedEvent(study));
    }

    public void close(Study study) {
        study.close();
        this.eventPublisher.publishEvent(new StudyUpdateEvent(study, "스터디를 종료했습니다."));

    }

    public Study getStudyToUpdateStatus(Account account, String path) {
        Study study = studyRepository.findStudyWithManagersByPath(path);
        studyExistingVerifier(path, study);
        isManagerVerifier(account, study);
        return study;
    }

    public void startRecruit(Study study) {
        study.startRecruit();
        this.eventPublisher.publishEvent(new StudyUpdateEvent(study, "팀원 모집을 시작합니다."));

    }

    public void stopRecruit(Study study) {
        study.stopRecruit();
        this.eventPublisher.publishEvent(new StudyUpdateEvent(study, "팀원 모집을 종료합니다."));

    }

    public boolean isValidPath(String newPath) {
        if (!newPath.matches(VALID_PATH_PATTERN)) {
            return false;
        }

        return !studyRepository.existsByPath(newPath);

    }

    public boolean isValidTitle(String newTitle) {
        return newTitle.length() <= 50;
    }

    public void updateStudyPath(Study study, String newPath) {
        study.setPath(newPath);
    }

    public void updateStudyTitle(Study study, String newTitle) {
        study.setTitle(newTitle);
    }

    public void remove(Study study) {
        if (study.isRemovable()) {
            studyRepository.delete(study);
        } else {
            throw new IllegalArgumentException("스터디를 삭제할 수 없습니다.");
        }
    }

    public void addMember(Study study, Account account) {
        if (!study.checkMember(account)) {
            study.getMembers().add(account);
        } else {
            throw new IllegalArgumentException("스터디에 가입할 수 없습니다.");
        }
    }

    public void removeMember(Study study, Account account) {
        if (study.checkMember(account)) {
            study.removeMember(account);
        } else {
            throw new IllegalArgumentException("스터디에서 탈퇴할 수 없습니다.");
        }
    }

    public Study getStudyToEnroll(String path) {

        Study study = studyRepository.findStudyOnlyByPath(path);
        if (study == null) {
            throw new IllegalArgumentException(path + "에 해당ㅎ나느 스터대ㅣ가 없습니다.");
        }
        return study;
    }

    public void generatedTestStudies(Account account) {
        for (int i = 0; i < 30; i++) {
            String randomValue = RandomString.make(5);

            Study study = Study.builder()
                    .title("테스트 스터디 " + randomValue)
                    .path("test-" + randomValue)
                    .shortDescription("테스트용 스터디 입니다.")
                    .fullDescription("test")
                    .tags(new HashSet<>())
                    .managers(new HashSet<>())
                    .zones(new HashSet<>())
                    .members(new HashSet<>())
                    .build();

            study.publish();

            Study newStudy = this.createNewStudy(study, account);

            Tag tag = tagService.findOrCreateNew("JPA");
            newStudy.getTags().add(tag);

        }
    }
}
