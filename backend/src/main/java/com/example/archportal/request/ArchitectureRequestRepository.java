package com.example.archportal.request;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ArchitectureRequestRepository
        extends JpaRepository<ArchitectureRequest, Long>, JpaSpecificationExecutor<ArchitectureRequest> {

    /** Optional filters, combined only when present, so the query never binds null parameters. */
    static Specification<ArchitectureRequest> matching(String query, RequestStatus status) {
        Specification<ArchitectureRequest> spec = (root, cq, cb) -> cb.conjunction();
        if (status != null) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("status"), status));
        }
        if (query != null && !query.isBlank()) {
            String pattern = "%" + query.trim().toLowerCase() + "%";
            spec = spec.and((root, cq, cb) -> cb.or(
                    cb.like(cb.lower(root.<String>get("title")), pattern),
                    cb.like(cb.lower(root.<String>get("applicationName")), pattern)));
        }
        return spec;
    }

    default List<ArchitectureRequest> search(String query, RequestStatus status) {
        return findAll(matching(query, status), Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
