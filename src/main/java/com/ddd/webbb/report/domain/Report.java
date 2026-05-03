package com.ddd.webbb.report.domain;

import com.ddd.webbb.comment.domain.Comment;
import com.ddd.webbb.global.common.entity.BaseCreatedEntity;
import com.ddd.webbb.post.domain.Post;
import com.ddd.webbb.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "report")
public class Report extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_user_id", nullable = false)
    private User reporterUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id")
    private Comment comment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ReportType reportType;

    @Column(nullable = false, length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ReportStatus status = ReportStatus.PENDING;

    private LocalDateTime processedAt;

    protected Report() {}

    public static Report createPostReport(User reporterUser, Post post, String reason) {
        Report report = new Report();
        report.reporterUser = reporterUser;
        report.post = post;
        report.reportType = ReportType.POST;
        report.reason = reason;
        return report;
    }

    public static Report createCommentReport(
            User reporterUser, Post post, Comment comment, String reason) {
        Report report = new Report();
        report.reporterUser = reporterUser;
        report.post = post;
        report.comment = comment;
        report.reportType = ReportType.COMMENT;
        report.reason = reason;
        return report;
    }

    public void process(ReportStatus status) {
        this.status = status;
        this.processedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public User getReporterUser() {
        return reporterUser;
    }

    public Post getPost() {
        return post;
    }

    public Comment getComment() {
        return comment;
    }

    public ReportType getReportType() {
        return reportType;
    }

    public String getReason() {
        return reason;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }
}
