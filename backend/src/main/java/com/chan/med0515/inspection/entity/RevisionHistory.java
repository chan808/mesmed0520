package com.chan.med0515.inspection.entity;

import com.chan.med0515.global.entity.BaseEntity;
import com.chan.med0515.material.entity.Material;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

import java.time.LocalDate;

@Entity
@Getter
@Table(name = "revisionHistory")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RevisionHistory extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "standard_id", nullable = false)
    private InspectionStandard standard;

    @Column(nullable = false)
    private int rev;

    @Column(nullable = false)
    private LocalDate revisionDate;

    @Column(nullable = false, length = 100)
    private String revisionNote;

    @Column(length = 20)
    private String confirmedBy;

    @Builder
    public RevisionHistory(InspectionStandard standard, int rev,LocalDate revisionDate,
                           String revisionNote, String confirmedBy) {
        Assert.notNull(standard, "기준서는 필수입니다");
        Assert.notNull(revisionDate, "개정일은 필수입니다");
        Assert.hasText(revisionNote, "개정내용은 필수입니다");
        this.standard = standard;
        this.rev = rev;
        this.revisionDate = revisionDate;
        this.revisionNote = revisionNote;
        this.confirmedBy = confirmedBy;
    }
}
