alter table attendance_sessions
    add column class_id   bigint       null,
    add column class_name varchar(100) null;

alter table attendance_sessions
    drop column subject;

create index idx_attendance_sessions_class_id on attendance_sessions (class_id);
