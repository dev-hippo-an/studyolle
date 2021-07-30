package com.studyolle.modules.study.validator;

import com.querydsl.jpa.JPQLQuery;
import com.studyolle.modules.account.QAccount;
import com.studyolle.modules.study.QStudy;
import com.studyolle.modules.study.Study;
import com.studyolle.modules.study.StudyRepositoryQueryDsl;
import com.studyolle.modules.tag.QTag;
import com.studyolle.modules.zone.QZone;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;


import java.util.List;

import static com.studyolle.modules.study.QStudy.study;

public class StudyRepositoryQueryDslImpl extends QuerydslRepositorySupport implements StudyRepositoryQueryDsl {

    public StudyRepositoryQueryDslImpl() {
        super(Study.class);
    }

    @Override
    public List<Study> findByKeyword(String keyword) {

        QStudy study = QStudy.study;

        JPQLQuery<Study> query = from(study).where(study.published.isTrue()
                .and(study.title.containsIgnoreCase(keyword))
                .or(study.tags.any().title.containsIgnoreCase(keyword))
                .or(study.zones.any().localNameOfCity.containsIgnoreCase(keyword)))
                .leftJoin(study.tags, QTag.tag).fetchJoin()
                .leftJoin(study.zones, QZone.zone).fetchJoin()
                .leftJoin(study.members, QAccount.account).fetchJoin()
                .distinct();

        return query.fetch();
    }
}