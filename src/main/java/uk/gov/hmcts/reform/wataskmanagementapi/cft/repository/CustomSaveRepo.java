package uk.gov.hmcts.reform.wataskmanagementapi.cft.repository;

public interface CustomSaveRepo<T> {
    <S extends T> S insert(S entity);
}
