USE pm;

CREATE TABLE IF NOT EXISTS pm_meeting (
                                          id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
                                          topic         VARCHAR(255) NOT NULL COMMENT '会议主题',
    project_id    BIGINT       NOT NULL DEFAULT 0 COMMENT '关联项目，0=不关联',
    start_time    DATETIME     NOT NULL COMMENT '开始时间',
    end_time      DATETIME     NOT NULL COMMENT '结束时间',
    location      VARCHAR(255) DEFAULT NULL COMMENT '地点',
    agenda        TEXT         COMMENT '议程',
    organizer_id  BIGINT       DEFAULT NULL COMMENT '组织人ID',
    organizer     VARCHAR(64)  DEFAULT NULL COMMENT '组织人姓名快照',
    status        VARCHAR(16)  NOT NULL DEFAULT 'PUBLISHED' COMMENT 'PUBLISHED/CANCELED',
    cancel_reason VARCHAR(500) DEFAULT NULL COMMENT '取消理由',
    cancel_time   DATETIME     DEFAULT NULL COMMENT '取消时间',
    attach_name   VARCHAR(255) DEFAULT NULL COMMENT '附件原名',
    attach_path   VARCHAR(500) DEFAULT NULL COMMENT '相对上传目录的路径',
    attach_size   BIGINT       NOT NULL DEFAULT 0 COMMENT '附件字节数',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME     DEFAULT NULL COMMENT '最后修改时间',
    creator       VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名快照',
    del_flag      TINYINT      NOT NULL DEFAULT 0 COMMENT '0正常/1删除',
    PRIMARY KEY (id),
    KEY idx_meeting_time (start_time),
    KEY idx_meeting_project (project_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会议';

CREATE TABLE IF NOT EXISTS pm_meeting_attendee (
                                                   id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
                                                   meeting_id   BIGINT      NOT NULL COMMENT '会议ID',
                                                   user_id      BIGINT      NOT NULL COMMENT '参会人ID',
                                                   user_name    VARCHAR(64) DEFAULT NULL COMMENT '姓名快照',
    reply_status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/ACCEPTED/DECLINED',
    reply_reason VARCHAR(500) DEFAULT NULL COMMENT '请假理由',
    reply_time   DATETIME    DEFAULT NULL COMMENT '回执时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_meeting_user (meeting_id, user_id),
    KEY idx_attendee_user (user_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会议参会人与回执';

-- 可选，只改注释
-- ALTER TABLE pm_remind MODIFY type VARCHAR(16) NOT NULL COMMENT 'WEEKLY/MEETING';